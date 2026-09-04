/**
 * Open Flow — Interactive 60fps Landing Page Controller & Client Router
 * Handles dictation simulation, soundwave equalizer, copy chip,
 * app matrix context switcher, speech transform toggle, scroll reveals,
 * and smooth 60fps tab switching without layout shifts.
 */
(function() {
  'use strict';

  var reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  // App context demo messages
  var appDemos = {
    'WhatsApp': {
      title: 'WhatsApp · Live dictation',
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
      title: 'Telegram · Live dictation',
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

  // 1. Scroll-driven reveal observer
  function initScrollReveals() {
    if (reducedMotion || !('IntersectionObserver' in window)) {
      var revealEls = document.querySelectorAll('.reveal-on-scroll');
      for (var i = 0; i < revealEls.length; i++) {
        revealEls[i].classList.add('is-revealed');
      }
      return;
    }

    var observer = new IntersectionObserver(function(entries) {
      entries.forEach(function(entry) {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-revealed');
          observer.unobserve(entry.target);
        }
      });
    }, {
      threshold: 0.12,
      rootMargin: '0px 0px -40px 0px'
    });

    var elements = document.querySelectorAll('.reveal-on-scroll');
    elements.forEach(function(el) {
      observer.observe(el);
    });
  }

  // 2. Interactive Dictation Simulation & Copy Chip
  function initDictationDemo() {
    var bubble = document.querySelector('.demo.kb .bubble');
    var watchBtn = document.querySelector('a[href="#watch-action"]');
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

    if (watchBtn) {
      watchBtn.addEventListener('click', function(e) {
        e.preventDefault();
        var demoEl = document.getElementById('watch-action');
        if (demoEl) {
          demoEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
          setTimeout(function() {
            startSimulation();
          }, 400);
        }
      });
    }

    // 3. Interactive App Matrix Switcher
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
  }

  // 4. Interactive Before / After Speech Transformation
  function initSpeechTransformToggle() {
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
  }

  // 5. Smooth 60fps Client-Side Router
  function initSmoothNavigation() {
    var isNavigating = false;

    function getTargetFilename(urlStr) {
      try {
        var u = new URL(urlStr, window.location.href);
        var parts = u.pathname.split('/');
        var last = parts[parts.length - 1];
        return last || 'index.html';
      } catch (e) {
        return '';
      }
    }

    function updateNavLinks(targetFilename) {
      var navLinks = document.querySelectorAll('header nav a');
      navLinks.forEach(function(link) {
        var href = link.getAttribute('href') || '';
        var linkFilename = getTargetFilename(href);
        if (linkFilename === targetFilename) {
          link.setAttribute('aria-current', 'page');
        } else if (!link.classList.contains('nav-cta')) {
          link.removeAttribute('aria-current');
        }
      });
    }

    function renderPage(newDoc, targetUrl, push) {
      var newMain = newDoc.querySelector('main');
      var currentMain = document.querySelector('main');
      var newHero = newDoc.querySelector('section.hero');
      var currentHero = document.querySelector('section.hero');

      if (!newMain || !currentMain) {
        window.location.href = targetUrl;
        return;
      }

      if (newDoc.title) {
        document.title = newDoc.title;
      }

      var newTheme = newDoc.querySelector('meta[name="theme-color"]');
      var curTheme = document.querySelector('meta[name="theme-color"]');
      if (newTheme && curTheme) {
        curTheme.setAttribute('content', newTheme.getAttribute('content'));
      }

      if (newHero) {
        if (!currentHero) {
          currentMain.parentNode.insertBefore(newHero.cloneNode(true), currentMain);
        } else {
          currentHero.replaceWith(newHero.cloneNode(true));
        }
      } else if (currentHero) {
        currentHero.remove();
      }

      currentMain.innerHTML = newMain.innerHTML;
      currentMain.className = newMain.className;

      var targetFile = getTargetFilename(targetUrl);
      updateNavLinks(targetFile);

      window.scrollTo(0, 0);

      initScrollReveals();
      initDictationDemo();
      initSpeechTransformToggle();

      if (push) {
        history.pushState({ path: targetUrl }, '', targetUrl);
      }
    }

    function loadPage(targetUrl, push) {
      if (isNavigating) return;
      isNavigating = true;

      var currentMain = document.querySelector('main');
      var currentHero = document.querySelector('section.hero');

      fetch(targetUrl)
        .then(function(res) {
          if (!res.ok) throw new Error('Network error');
          return res.text();
        })
        .then(function(html) {
          var parser = new DOMParser();
          var newDoc = parser.parseFromString(html, 'text/html');

          if (document.startViewTransition && !reducedMotion) {
            document.startViewTransition(function() {
              renderPage(newDoc, targetUrl, push);
            });
          } else if (!reducedMotion && currentMain) {
            currentMain.classList.add('page-leaving');
            if (currentHero) currentHero.classList.add('page-leaving');
            setTimeout(function() {
              renderPage(newDoc, targetUrl, push);
              currentMain.classList.remove('page-leaving');
              currentMain.classList.add('page-entering');
              setTimeout(function() {
                currentMain.classList.remove('page-entering');
              }, 250);
            }, 120);
          } else {
            renderPage(newDoc, targetUrl, push);
          }
        })
        .catch(function() {
          window.location.href = targetUrl;
        })
        .finally(function() {
          setTimeout(function() {
            isNavigating = false;
          }, 180);
        });
    }

    document.addEventListener('click', function(e) {
      var link = e.target.closest('a');
      if (!link) return;

      if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey || link.target === '_blank') {
        return;
      }

      var href = link.getAttribute('href');
      if (!href) return;

      if (href.startsWith('http://') || href.startsWith('https://') || href.startsWith('mailto:') || href.startsWith('tel:') || href.startsWith('#')) {
        return;
      }

      var targetFile = getTargetFilename(href);
      var validPages = [
        'index.html', 'install.html', 'guide.html', 'privacy.html',
        'compare.html', 'architecture.html', 'roadmap.html', 'report.html'
      ];
      if (validPages.indexOf(targetFile) === -1) {
        return;
      }

      var currentFile = getTargetFilename(window.location.href);
      if (targetFile === currentFile && !href.includes('#')) {
        e.preventDefault();
        window.scrollTo({ top: 0, behavior: 'smooth' });
        return;
      }

      e.preventDefault();
      loadPage(href, true);
    });

    window.addEventListener('popstate', function() {
      loadPage(window.location.href, false);
    });
  }

  // Initialize on DOM ready
  function initAll() {
    initScrollReveals();
    initDictationDemo();
    initSpeechTransformToggle();
    initSmoothNavigation();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAll);
  } else {
    initAll();
  }
})();
