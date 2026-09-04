/* core: reveals, router, hero enter, sticky CTA, boot. Load last. */
(function() {
  'use strict';
  window.OpenFlow = window.OpenFlow || {};
  var OpenFlow = window.OpenFlow;

  // Scroll-driven reveal observer
  function initScrollReveals() {
    if (OpenFlow.reducedMotion || !('IntersectionObserver' in window)) {
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

  // Re-run every page-level init after a router swap
  function initPage() {
    OpenFlow.appendCtaBand();
    initScrollReveals();
    OpenFlow.initDictationDemo();
    OpenFlow.initSpeechTransformToggle();
    initHeroEnter();
    OpenFlow.initCleanupLoop();
    initStickyCta();
    OpenFlow.initMarqueeA11y();
  }

  // Smooth 60fps Client-Side Router
  function initSmoothNavigation() {
    if (document.documentElement.getAttribute('data-router') === '1') return;
    document.documentElement.setAttribute('data-router', '1');
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
      document.body.setAttribute('data-page', targetFile.replace('.html', ''));
      OpenFlow.updateNavLinks(targetFile);

      window.scrollTo(0, 0);
      initPage();

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

          if (document.startViewTransition && !OpenFlow.reducedMotion) {
            document.startViewTransition(function() {
              renderPage(newDoc, targetUrl, push);
            });
          } else if (!OpenFlow.reducedMotion && currentMain) {
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

  // Hero entrance stagger (transform + opacity only)
  function initHeroEnter() {
    var items = document.querySelectorAll('[data-enter]');
    if (!items.length) return;
    if (OpenFlow.reducedMotion) {
      items.forEach(function(el) { el.classList.add('is-in'); });
      return;
    }
    items.forEach(function(el, i) {
      el.style.transitionDelay = (i * 0.09) + 's';
    });
    requestAnimationFrame(function() {
      requestAnimationFrame(function() {
        items.forEach(function(el) { el.classList.add('is-in'); });
      });
    });
  }

  // Sticky mini CTA: past the hero, or past 400px on hero-less pages
  function initStickyCta() {
    var bar = document.getElementById('sticky-cta');
    if (!bar) return;
    if (bar.getAttribute('data-watching') === '1') {
      updateBar();
      return;
    }
    bar.setAttribute('data-watching', '1');
    var ticking = false;
    function updateBar() {
      ticking = false;
      var hero = document.querySelector('section.hero');
      var past = hero
        ? window.scrollY > (hero.offsetHeight - 120)
        : window.scrollY > 400;
      bar.classList.toggle('is-on', past);
    }
    window.addEventListener('scroll', function() {
      if (!ticking) {
        ticking = true;
        requestAnimationFrame(updateBar);
      }
    }, { passive: true });
    updateBar();
  }

  // Initialize on DOM ready
  function initAll() {
    OpenFlow.renderPartials();
    initPage();
    initSmoothNavigation();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAll);
  } else {
    initAll();
  }
})();
