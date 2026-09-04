/* demo: dictation sim, app pills, speech tabs, cleanup loop. */
(function() {
  'use strict';
  window.OpenFlow = window.OpenFlow || {};
  var OpenFlow = window.OpenFlow;

  // App context demo messages
  var appDemos = {
    'WhatsApp': {
      title: 'WhatsApp · Simulated preview',
      text: 'Running 5 minutes late! Grab a table inside, see you soon.'
    },
    'Gmail': {
      title: 'Gmail · Quick reply',
      text: 'Thanks for reaching out. Let\'s connect tomorrow at 10 AM to review the details.'
    },
    'Slack': {
      title: 'Slack · Standup update',
      text: 'Deployed the latest build to staging. Everything looks solid for release.'
    },
    'Telegram': {
      title: 'Telegram · Simulated preview',
      text: 'Sent the docs over. Let me know what you think when you get a chance.'
    },
    'Notion': {
      title: 'Notion · Action items',
      text: 'Q4 Priorities: 1. On-device Whisper 2. Zero-latency bubble 3. Clean UI.'
    },
    'Docs': {
      title: 'Docs · Draft summary',
      text: 'Meeting conclusion: the team agreed on the local-first storage architecture.'
    },
    'Browser': {
      title: 'Browser · Search query',
      text: 'Open source voice dictation floating bubble for Android.'
    },
    'Linear': {
      title: 'Linear · New issue',
      text: 'Fix audio buffer drop on low-spec devices during rapid bubble taps.'
    }
  };

  // Interactive Dictation Simulation & Copy Chip + App Matrix Switcher
  OpenFlow.initDictationDemo = function() {
    var reducedMotion = OpenFlow.reducedMotion;
    var bubble = document.querySelector('.demo.kb .bubble');
    var composeEl = document.querySelector('.demo.kb .compose');
    var waveBars = document.querySelector('.demo.kb .wave-bars');
    var copyChip = document.getElementById('copy-chip');
    var phoneHeader = document.querySelector('.demo.kb .phone-header span:first-child');

    if (!bubble || !composeEl) return;

    var isSimulating = false;
    var currentTimer = null;
    var chipTimer = null;
    var currentTargetText = 'Meet you at 6. Don\'t forget milk.';

    function stopSimulation() {
      isSimulating = false;
      if (currentTimer) clearTimeout(currentTimer);
      if (waveBars) waveBars.classList.remove('active');
      bubble.classList.remove('is-active');
    }

    function triggerCopyChip() {
      if (!copyChip) return;
      if (chipTimer) clearTimeout(chipTimer);

      copyChip.classList.add('is-visible');
      chipTimer = setTimeout(function() {
        copyChip.classList.remove('is-visible');
      }, 2400);
    }

    function startSimulation(textToType) {
      if (isSimulating) {
        stopSimulation();
        return;
      }

      var text = textToType || currentTargetText;
      isSimulating = true;

      // Visual listening state
      bubble.classList.add('is-active');
      if (waveBars) waveBars.classList.add('active');
      if (copyChip) copyChip.classList.remove('is-visible');

      if (reducedMotion) {
        composeEl.innerHTML = text + '<span class="caret"></span>';
        stopSimulation();
        triggerCopyChip();
        return;
      }

      composeEl.innerHTML = '<span class="caret"></span>';
      var charIndex = 0;

      function typeNextChar() {
        if (!isSimulating) return;

        if (charIndex < text.length) {
          charIndex++;
          composeEl.innerHTML = text.substring(0, charIndex) + '<span class="caret"></span>';
          var delay = 35 + Math.floor(Math.random() * 25);
          var char = text.charAt(charIndex - 1);
          if (char === '.' || char === '!' || char === ',') {
            delay += 120;
          }
          currentTimer = setTimeout(typeNextChar, delay);
        } else {
          stopSimulation();
          triggerCopyChip();
        }
      }

      currentTimer = setTimeout(typeNextChar, 250);
    }

    bubble.addEventListener('click', function(e) {
      e.preventDefault();
      startSimulation();
    });
    // Native <button> fires click on Enter/Space — no extra key handler.

    var watchBtns = document.querySelectorAll('a[href="#watch-action"]');
    watchBtns.forEach(function(watchBtn) {
      watchBtn.addEventListener('click', function(e) {
        e.preventDefault();
        var demoEl = document.getElementById('watch-action');
        if (demoEl) {
          demoEl.scrollIntoView({ behavior: reducedMotion ? 'auto' : 'smooth', block: 'center' });
          setTimeout(function() {
            startSimulation();
          }, reducedMotion ? 0 : 400);
        }
      });
    });

    // Interactive App Matrix Switcher
    var appPills = document.querySelectorAll('.app-matrix .app-pill');
    appPills.forEach(function(pill) {
      pill.style.cursor = 'pointer';
      pill.setAttribute('role', 'button');
      pill.setAttribute('tabindex', '0');

      function selectPill() {
        appPills.forEach(function(p) { p.classList.remove('is-active'); });
        pill.classList.add('is-active');

        var appName = pill.querySelector('span') ? pill.querySelector('span').textContent.trim() : '';
        var demoData = appDemos[appName];
        if (demoData) {
          if (phoneHeader) phoneHeader.textContent = demoData.title;
          currentTargetText = demoData.text;
          startSimulation(demoData.text);

          var demoEl = document.getElementById('watch-action');
          if (demoEl && window.innerWidth < 768) {
            demoEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
          }
        }
      }

      pill.addEventListener('click', selectPill);
      pill.addEventListener('keydown', function(e) {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          selectPill();
        }
      });
    });
  };

  // Interactive Before / After Speech Transformation
  OpenFlow.initSpeechTransformToggle = function() {
    var rawBubble = document.querySelector('.speech-bubble-raw');
    var cleanBubble = document.querySelector('.speech-bubble-clean');
    if (!rawBubble || !cleanBubble) return;

    var existingTabs = document.querySelector('.speech-tabs');
    if (existingTabs) return;

    var controls = document.createElement('div');
    controls.className = 'speech-tabs';
    controls.innerHTML =
      '<button type="button" class="speech-tab active" data-tab="all">Comparison</button>' +
      '<button type="button" class="speech-tab" data-tab="clean">Polished only</button>' +
      '<button type="button" class="speech-tab" data-tab="raw">Raw voice</button>';

    var parent = document.querySelector('.speech-transform');
    if (parent && parent.parentNode) {
      parent.parentNode.insertBefore(controls, parent);

      var tabs = controls.querySelectorAll('.speech-tab');
      tabs.forEach(function(tab) {
        tab.addEventListener('click', function() {
          tabs.forEach(function(t) { t.classList.remove('active'); });
          tab.classList.add('active');

          var mode = tab.getAttribute('data-tab');
          if (mode === 'clean') {
            rawBubble.style.display = 'none';
            cleanBubble.style.display = 'block';
            cleanBubble.classList.add('shimmer-active');
          } else if (mode === 'raw') {
            rawBubble.style.display = 'block';
            cleanBubble.style.display = 'none';
          } else {
            rawBubble.style.display = 'block';
            cleanBubble.style.display = 'block';
            cleanBubble.classList.add('shimmer-active');
          }

          setTimeout(function() {
            cleanBubble.classList.remove('shimmer-active');
          }, 1200);
        });
      });
    }
  };

  // Auto cleanup loop: raw -> cleaning tags -> polished, pauses off-screen
  var cleanupTimer = null;
  OpenFlow.initCleanupLoop = function() {
    var reducedMotion = OpenFlow.reducedMotion;
    var zone = document.querySelector('[data-cleanup]');
    var status = document.querySelector('[data-cleanup-status]');
    if (!zone || !status) return;
    if (cleanupTimer) clearInterval(cleanupTimer);
    if (reducedMotion) {
      zone.classList.add('is-cleaning');
      status.textContent = 'Polished on your phone';
      return;
    }
    var phases = [
      { text: 'Listening…', cleaning: false },
      { text: 'Cleaning up…', cleaning: true },
      { text: 'Polished ✓', cleaning: true }
    ];
    var idx = 0;
    var visible = true;
    if ('IntersectionObserver' in window) {
      new IntersectionObserver(function(entries) {
        visible = entries[0].isIntersecting;
      }, { threshold: 0.2 }).observe(zone);
    }
    cleanupTimer = setInterval(function() {
      if (!visible || document.hidden) return;
      idx = (idx + 1) % phases.length;
      status.textContent = phases[idx].text;
      zone.classList.toggle('is-cleaning', phases[idx].cleaning);
    }, 2600);
  };
})();
