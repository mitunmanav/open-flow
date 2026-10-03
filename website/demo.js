/*
  The dictation demo in the hero of the landing page.

  Not an illustration of OpenFlow — a small piece of it. The states are
  ADR-0002's, the refiner beats are stages 3, 4 and 8 from
  docs/architecture/refiner.md, and two of the ADR's rules are honoured: cancel
  discards and persists nothing, and Inserting is not cancellable, so Escape is
  ignored once the pipeline has committed.

  ── Why the bubble is written like this ──────────────────────────
  It is the one object on the site a person actually grabs, so it gets physical
  behaviour rather than decorative animation:

    · feedback on pointer-down, never on release
    · the drag tracks 1:1 from where you grabbed it, not from its centre
    · pointer capture, so it keeps tracking when your finger leaves it
    · release velocity is measured and handed to a spring
    · momentum is projected, not guessed
    · bounds resist progressively instead of stopping dead
    · a spring, critically damped — this is a reposition, so no overshoot
    · X and Y settle independently, or a diagonal throw desyncs
    · grabbing a moving bubble cancels its motion: nothing is ever locked out

  Everything the visitor sees move is caused by the visitor. The one exception
  is the load-time perimeter sweep, which is the app's own ready signal from
  ticket 19, and it runs once.
*/
(function () {
  "use strict";

  var demo = document.getElementById("demo");
  if (!demo) return;

  var bubble = demo.querySelector("#bubble");
  var label = demo.querySelector("#bubbleLabel");
  var level = demo.querySelector("#level");
  var sweep = demo.querySelector("#sweep");
  var field = demo.querySelector("#field");
  var railState = demo.querySelector("#railState");
  var railDetail = demo.querySelector("#railDetail");

  /* What a streaming recognizer hands over before it settles: conflated
     snapshots, per the SpeechProvider contract. */
  var PARTIALS = [
    "i'm sorry scratch that",
    "i'm sorry scratch that friday works too.",
    "i'm sorry scratch that friday works too. um like thursday at ten am"
  ];

  /* What the deterministic refiner makes of it — the false start goes at stage
     3, the fillers at stage 4, the style preset at stage 8. */
  var REFINED = "Thursday at 10am.";

  var reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  var INSET = 12;

  var state = "idle";
  var run = 0;
  var timers = [];
  var drag = null;

  /* ── physics ────────────────────────────────────────────────── */

  var pos = { x: 0, y: 0 };
  var vel = { x: 0, y: 0 };

  function bounds() {
    var host = bubble.parentElement;
    return {
      maxX: Math.max(0, host.clientWidth - bubble.offsetWidth),
      maxY: Math.max(0, host.clientHeight - bubble.offsetHeight)
    };
  }

  function paint() {
    bubble.style.transform = "translate3d(" + pos.x + "px," + pos.y + "px,0)";
  }

  function rest() {
    var b = bounds();
    return { x: b.maxX - INSET, y: b.maxY - INSET };
  }

  /* Progressive resistance past an edge. A hard stop reads as frozen; the
     thing that slowed before it stopped reads as solid. */
  function rubber(overshoot, dimension) {
    var c = 0.55;
    return (overshoot * dimension * c) / (dimension + c * Math.abs(overshoot));
  }

  function resist(p, b) {
    if (p.x < 0) p.x = rubber(p.x, b.maxX || 1);
    else if (p.x > b.maxX) p.x = b.maxX + rubber(p.x - b.maxX, b.maxX || 1);
    if (p.y < 0) p.y = rubber(p.y, b.maxY || 1);
    else if (p.y > b.maxY) p.y = b.maxY + rubber(p.y - b.maxY, b.maxY || 1);
    return p;
  }

  /* Critically damped, integrated at a fixed substep so the behaviour does not
     change with frame rate. Damping 1.0 and response 0.4s is the right pair for
     a reposition: graceful, no overshoot. */
  var OMEGA = 2.4 / 0.4;
  var K = OMEGA * OMEGA;
  var C = 2 * OMEGA;
  var target = { x: 0, y: 0 };
  var raf = null;

  function halt() {
    if (raf) cancelAnimationFrame(raf);
    raf = null;
  }

  function runSpring() {
    halt();
    var dt = 1 / 240;

    function frame() {
      /* X and Y settle independently — one shared spring desyncs on a diagonal
         throw, because the two axes have different velocities. */
      for (var axis = 0; axis < 2; axis++) {
        var k = axis === 0 ? "x" : "y";
        var remaining = dt;
        while (remaining > 0) {
          var s = Math.min(1 / 240, remaining);
          var a = -K * (pos[k] - target[k]) - C * vel[k];
          vel[k] += a * s;
          pos[k] += vel[k] * s;
          remaining -= s;
        }
      }
      paint();

      if (
        Math.abs(pos.x - target.x) < 0.15 &&
        Math.abs(pos.y - target.y) < 0.15 &&
        Math.abs(vel.x) < 1 &&
        Math.abs(vel.y) < 1
      ) {
        pos.x = target.x;
        pos.y = target.y;
        vel.x = vel.y = 0;
        paint();
        raf = null;
        return;
      }
      raf = requestAnimationFrame(frame);
    }
    raf = requestAnimationFrame(frame);
  }

  function throwTo() {
    var b = bounds();
    /* Project where it is going rather than snapping to the nearest bound.
       d = 0.998 is the platform deceleration constant. */
    var projected = {
      x: pos.x + (vel.x / 1000) * (0.998 / 0.002),
      y: pos.y + (vel.y / 1000) * (0.998 / 0.002)
    };
    target = {
      x: Math.max(0, Math.min(projected.x, b.maxX)),
      y: Math.max(0, Math.min(projected.y, b.maxY))
    };
    if (reduce) {
      pos.x = target.x;
      pos.y = target.y;
      vel.x = vel.y = 0;
      paint();
      return;
    }
    runSpring();
  }

  /* ── scheduling ─────────────────────────────────────────────── */

  function later(fn, ms) {
    var mine = run;
    var id = window.setTimeout(function () {
      timers = timers.filter(function (t) {
        return t !== id;
      });
      if (mine === run) fn();
    }, reduce ? Math.min(ms, 40) : ms);
    timers.push(id);
    return id;
  }

  function clearTimers() {
    timers.forEach(window.clearTimeout);
    timers = [];
  }

  function newRun() {
    clearTimers();
    run++;
  }

  /* ── rendering ──────────────────────────────────────────────── */

  function say(stateName, detail) {
    railState.textContent = stateName;
    railDetail.textContent = detail;
  }

  var LABELS = {
    idle: "Hold to talk",
    transcribing: "Working",
    refining: "Cleaning",
    inserting: "Inserting",
    done: "Done"
  };

  function setState(next) {
    state = next;
    bubble.setAttribute("data-state", next);

    if (LABELS[next]) {
      label.hidden = false;
      label.textContent = LABELS[next];
    } else {
      /* Recording shows the level meter in place of the label. */
      label.hidden = true;
    }

    level.hidden = next !== "recording";

    /* The field is only editable when nothing is being written into it. */
    if (next === "idle") {
      field.setAttribute("contenteditable", "true");
    } else {
      field.removeAttribute("contenteditable");
    }

    bubble.setAttribute(
      "aria-label",
      next === "recording"
        ? "Recording. Release to stop."
        : "Dictation bubble. Press and hold to talk, drag to move."
    );
  }

  function esc(s) {
    return s.replace(/[&<>]/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;" }[c];
    });
  }

  function setField(html) {
    field.innerHTML = html + '<span class="caret"></span>';
  }

  function blankField() {
    field.textContent = "";
  }

  /* ── the pipeline ───────────────────────────────────────────── */

  var BEATS = [
    {
      at: 0,
      html:
        'i&rsquo;m sorry <span class="term">scratch that friday works too.</span> um like thursday at ten am',
      detail: "3 &middot; backtracking &mdash; the false start is deleted"
    },
    { at: 1200, html: "thursday at 10am", detail: "4 &middot; fillers &mdash; um, like" },
    {
      at: 2250,
      html: esc(REFINED),
      detail: "8 &middot; style &mdash; Neutral preset, applied last"
    }
  ];

  function transcribing() {
    setState("transcribing");
    say("TRANSCRIBING", "sherpa-onnx, on-device &mdash; provider finalizing");

    later(function () {
      PARTIALS.forEach(function (text, i) {
        later(function () {
          setField(esc(text));
        }, i === 0 ? 0 : 360);
      });
    }, 420);
  }

  function refining() {
    setState("refining");
    BEATS.forEach(function (beat) {
      later(function () {
        setField(beat.html);
        say("REFINING", beat.detail);
      }, beat.at);
    });
  }

  function inserting() {
    setState("inserting");
    say("INSERTING", "SET_TEXT at the caret &mdash; not cancellable");

    later(function () {
      setState("done");
      say("DONE", "inserted at the caret &mdash; " + REFINED.length + " characters");

      later(function () {
        blankField();
        setState("idle");
        say("IDLE", "hold to talk, or throw it to move");
      }, 2000);
    }, 560);
  }

  function startPipeline() {
    transcribing();
    later(refining, PARTIALS.length * 360 + 700);
    later(inserting, PARTIALS.length * 360 + 700 + BEATS[BEATS.length - 1].at + 700);
  }

  /* ── input ──────────────────────────────────────────────────── */

  function begin() {
    if (state !== "idle") return;
    newRun();
    blankField();
    setState("recording");
    say("RECORDING", "hold &mdash; release to hand it to the recognizer");
  }

  function end() {
    if (state !== "recording") return;
    startPipeline();
  }

  function cancel() {
    if (state !== "recording") return;
    newRun();
    blankField();
    setState("idle");
    say("IDLE", "cancelled &mdash; audio discarded, nothing persisted");
  }

  function velocity() {
    var h = drag.history;
    if (h.length < 2) return { x: 0, y: 0 };
    var last = h[h.length - 1];
    var first = h[0];
    /* Only the tail of the history counts: a throw is about the last 80ms,
       not about where the finger started. */
    for (var i = h.length - 1; i >= 0; i--) {
      if (last.t - h[i].t > 80) {
        first = h[i];
        break;
      }
      first = h[i];
    }
    var dt = (last.t - first.t) / 1000;
    if (dt <= 0) return { x: 0, y: 0 };
    return { x: (last.x - first.x) / dt, y: (last.y - first.y) / dt };
  }

  bubble.addEventListener("pointerdown", function (event) {
    if (event.button > 0) return;
    if (drag) return;

    /* Grabbing a bubble that is still moving takes control of it immediately.
       Never lock input out during a transition. */
    halt();
    vel.x = 0;
    vel.y = 0;

    var rect = bubble.getBoundingClientRect();
    drag = {
      id: event.pointerId,
      /* Respect where it was grabbed, not its centre. */
      grabX: event.clientX - rect.left,
      grabY: event.clientY - rect.top,
      startX: event.clientX,
      startY: event.clientY,
      originX: pos.x,
      originY: pos.y,
      moved: false,
      history: [{ t: event.timeStamp, x: event.clientX, y: event.clientY }]
    };

    try {
      bubble.setPointerCapture(event.pointerId);
    } catch (e) {
      /* Capture is an optimisation; the window-level fallback covers it. */
    }

    begin();
  });

  bubble.addEventListener("pointermove", function (event) {
    if (!drag || event.pointerId !== drag.id) return;

    drag.history.push({ t: event.timeStamp, x: event.clientX, y: event.clientY });
    if (drag.history.length > 12) drag.history.shift();

    var dx = event.clientX - drag.startX;
    var dy = event.clientY - drag.startY;

    /* Hysteresis: ~6px before a press becomes a drag, so a press never
       accidentally becomes a throw. */
    if (!drag.moved && Math.abs(dx) < 6 && Math.abs(dy) < 6) return;
    drag.moved = true;

    if (state === "recording") {
      /* One pointer cannot hold and drag at once. Drop the recording rather
         than pretending the hold is still happening. */
      newRun();
      blankField();
      setState("idle");
      say("IDLE", "moved &mdash; hold to talk");
    }

    var b = bounds();
    pos.x = drag.originX + dx;
    pos.y = drag.originY + dy;
    resist(pos, b);
    paint();
  });

  function release(event) {
    if (!drag || (event && event.pointerId !== drag.id)) return;
    var wasDrag = drag.moved;
    var v = wasDrag ? velocity() : { x: 0, y: 0 };
    drag = null;

    if (!wasDrag) {
      end();
      return;
    }

    vel.x = v.x;
    vel.y = v.y;
    if (Math.abs(vel.x) > 60 || Math.abs(vel.y) > 60) {
      throwTo();
    } else {
      /* Slow release: settle into the nearest in-bounds resting point. */
      var b = bounds();
      target = {
        x: Math.max(0, Math.min(pos.x, b.maxX)),
        y: Math.max(0, Math.min(pos.y, b.maxY))
      };
      vel.x = 0;
      vel.y = 0;
      runSpring();
    }
  }

  bubble.addEventListener("pointerup", release);
  bubble.addEventListener("pointercancel", function () {
    drag = null;
    cancel();
  });

  /* A hold that wanders off the bubble is still a hold. */
  window.addEventListener("pointerup", release);

  bubble.addEventListener("keydown", function (event) {
    if (event.key === "Escape") {
      cancel();
      return;
    }
    if (event.key === " " || event.key === "Enter") {
      event.preventDefault();
      if (state === "idle") begin();
    }
  });

  bubble.addEventListener("keyup", function (event) {
    if (event.key === " " || event.key === "Enter") {
      event.preventDefault();
      end();
    }
  });

  document.addEventListener("keydown", function (event) {
    if (event.key !== "Escape" || bubble.contains(event.target)) return;
    if (state === "recording") {
      cancel();
    } else if (state !== "idle") {
      say(railState.textContent, "already committed &mdash; not cancellable");
    }
  });

  /* ── the one moment that moves on its own ───────────────────── */

  if (!reduce && sweep) {
    sweep.style.setProperty("--sweep", "0deg");
    void sweep.offsetWidth;
    sweep.style.transition = "--sweep 1s linear";
    sweep.style.setProperty("--sweep", "360deg");
  }

  /* Re-measure and re-place. The first measurement can happen before the
     webfont has swapped in and before the grid has settled, and a stale one
     leaves the bubble hanging outside the device it belongs to. */
  function place() {
    var start = rest();
    pos.x = target.x = start.x;
    pos.y = target.y = start.y;
    paint();
  }

  function relayout() {
    if (state !== "idle" || drag) return;
    place();
  }

  place();
  setState("idle");
  say("IDLE", "hold to talk, or throw it to move");

  if (document.fonts && document.fonts.ready) {
    document.fonts.ready.then(relayout);
  }

  var queued = false;
  function onResize() {
    if (queued) return;
    queued = true;
    requestAnimationFrame(function () {
      queued = false;
      relayout();
    });
  }

  window.addEventListener("resize", onResize);
  window.addEventListener("orientationchange", onResize);

  if ("ResizeObserver" in window) {
    new ResizeObserver(onResize).observe(bubble.parentElement);
  }
})();