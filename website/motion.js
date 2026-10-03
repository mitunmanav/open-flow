/*
  Shared motion driver.

  Two jobs, and deliberately not three:

    1. Fire the one orchestrated page-load sequence, so it starts after the
       stylesheet has settled rather than on DOMContentLoaded.

    2. Observe [data-draw] elements and add .is-in once, when they arrive. The
       transition itself lives in CSS. This class is a one-way latch: nothing
       ever removes it, because an element that un-draws when you scroll back
       up is a page that animates at you rather than with you.

  There is deliberately no scroll-linked parallax and no per-element fade-in.
  Both are on the list of things that make a page read as generated.
*/
(function () {
  "use strict";

  var reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  function ready() {
    document.documentElement.classList.add("is-ready");
  }

  /* Two frames: one for the stylesheet to apply, one so the first paint is not
     competing with the animations it is about to start. */
  requestAnimationFrame(function () {
    requestAnimationFrame(ready);
  });

  var targets = document.querySelectorAll("[data-draw]");
  watchClaims();

  if (!targets.length) return;

  /* Reduced motion still gets the class — the end state is the same, it simply
     arrives without the transition. Suppressing the state change instead would
     leave diagrams permanently invisible. */
  if (!("IntersectionObserver" in window)) {
    for (var i = 0; i < targets.length; i++) targets[i].classList.add("is-in");
    return;
  }

  var observer = new IntersectionObserver(
    function (entries) {
      entries.forEach(function (entry) {
        if (!entry.isIntersecting) return;
        entry.target.classList.add("is-in");
        observer.unobserve(entry.target);
      });
    },
    /* Fire a little before the element's top edge reaches the fold, so the
       draw is finishing by the time it is properly on screen. */
    { rootMargin: "0px 0px -12% 0px", threshold: 0.15 }
  );

  for (var j = 0; j < targets.length; j++) observer.observe(targets[j]);

  /*
    The scroll narrative: the phone is pinned while the claims scroll past it,
    and its caption names whichever claim is currently in the reading position.
    This is the one place the page tells a story through scroll rather than
    decorating with it — the object you are looking at is always the object the
    sentence is about.

    A claim becomes active at the reading line rather than the moment it first
    appears, which is why the margin is negative: a claim that has only just
    entered from the bottom of the screen has not been read yet.
  */
  function watchClaims() {
    var caption = document.getElementById("deviceCaption");
    var claims = Array.prototype.slice.call(document.querySelectorAll(".claim[data-caption]"));
    if (!caption || !claims.length || !("IntersectionObserver" in window)) return;

    var IDLE = caption.textContent.trim();
    var current = null;

    function show(text) {
      if (text === caption.textContent.trim()) return;
      caption.textContent = text;
      /* Re-trigger the swap animation. Removing the class and forcing a reflow
         is what makes the animation restart; setting textContent alone would
         only play it the first time. */
      caption.classList.remove("is-swapping");
      void caption.offsetWidth;
      caption.classList.add("is-swapping");
    }

    var claimObserver = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) current = entry.target;
        });
        show(current ? current.getAttribute("data-caption") : IDLE);
      },
      /* The band just below the reading line counts as "the current claim", so
         the caption changes as the next one arrives rather than after it has
         left. */
      { rootMargin: "-38% 0px -52% 0px", threshold: 0 }
    );

    claims.forEach(function (claim) {
      claimObserver.observe(claim);
    });
  }

  void reduce;
})();