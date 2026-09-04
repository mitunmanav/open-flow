/* partials: shared header/footer, injected once per page. No-JS fallback links stay in the divs. */
(function() {
  'use strict';
  window.OpenFlow = window.OpenFlow || {};
  var OpenFlow = window.OpenFlow;
  OpenFlow.reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  var DOWNLOAD_URL = 'install.html';
  var DISCUSS_URL = 'https://github.com/mitunmanav/open-flow/discussions';

  var NAV_LINKS = [
    { href: 'index.html', label: 'Home' },
    { href: 'install.html', label: 'Install' },
    { href: 'compare.html', label: 'Compare' },
    { href: 'architecture.html', label: 'Built' },
    { href: 'guide.html', label: 'Guide' },
    { href: 'privacy.html', label: 'Privacy' },
    { href: DISCUSS_URL, label: 'Talk', ext: true }
  ];

  function headerHTML(page) {
    var links = NAV_LINKS.map(function(l) {
      var cur = (l.href === page + '.html') ? ' aria-current="page"' : '';
      return '<a href="' + l.href + '"' + cur + '>' + l.label + '</a>';
    }).join('\n      ');
    return '<a class="skip" href="#main">Skip to text</a>\n' +
      '<header>\n  <div class="wrap">\n' +
      '    <a class="brand" href="index.html"><img src="icon.svg" width="32" height="32" alt="Open Flow">Open Flow</a>\n' +
      '    <nav>\n      ' + links + '\n' +
      '      <a class="nav-cta" href="' + DOWNLOAD_URL + '">Download</a>\n' +
      '    </nav>\n  </div>\n</header>\n' +
      '<div class="sticky-cta" id="sticky-cta">\n' +
      '  <span><img src="icon.svg" width="22" height="22" alt="Open Flow"> Open Flow · Free for Android</span>\n' +
      '  <a class="btn" href="' + DOWNLOAD_URL + '">Download</a>\n</div>';
  }

  function footerHTML() {
    return '<footer>\n  <div class="wrap">\n' +
      '    <span><img src="icon.svg" alt="Open Flow"> Open Flow · MIT · v0.1.9 · Sep 2026</span>\n' +
      '    <span>\n' +
      '      <a href="compare.html">Compare</a> ·\n' +
      '      <a href="architecture.html">How it is built</a> ·\n' +
      '      <a href="roadmap.html">Roadmap</a> ·\n' +
      '      <a href="report.html">Report a bug</a> ·\n' +
      '      <a href="https://github.com/mitunmanav/open-flow">GitHub</a>\n' +
      '    </span>\n  </div>\n</footer>';
  }

  OpenFlow.renderPartials = function() {
    var page = (document.body && document.body.getAttribute('data-page')) || 'index';
    var h = document.querySelector('[data-partial="header"]');
    if (h && !h.getAttribute('data-done')) {
      h.innerHTML = headerHTML(page);
      h.removeAttribute('data-partial');
      h.setAttribute('data-done', '1');
    }
    var f = document.querySelector('[data-partial="footer"]');
    if (f && !f.getAttribute('data-done')) {
      f.innerHTML = footerHTML();
      f.removeAttribute('data-partial');
      f.setAttribute('data-done', '1');
    }
  };

  OpenFlow.updateNavLinks = function(targetFilename) {
    var navLinks = document.querySelectorAll('header nav a');
    navLinks.forEach(function(link) {
      var href = link.getAttribute('href') || '';
      if (href === targetFilename) {
        link.setAttribute('aria-current', 'page');
      } else if (!link.classList.contains('nav-cta')) {
        link.removeAttribute('aria-current');
      }
    });
  };

  // Bottom CTA band on every page: install target, guide-next on install page
  OpenFlow.appendCtaBand = function() {
    var main = document.querySelector('main');
    if (!main || main.querySelector('.cta-band')) return;
    var page = (document.body && document.body.getAttribute('data-page')) || 'index';
    var band = document.createElement('section');
    band.className = 'cta-band';
    if (page === 'install') {
      band.innerHTML = '<div><strong>Installed? Learn the voice edits.</strong>' +
        '<p>Strike that, new paragraph, numbers — all by voice.</p></div>' +
        '<a class="btn" href="guide.html">Read the guide</a>';
    } else {
      band.innerHTML = '<div><strong>Free for Android 8+. No account.</strong>' +
        '<p>Talk messy, send clean text. Set up in minutes.</p></div>' +
        '<a class="btn" href="install.html">Install in minutes</a>';
    }
    main.appendChild(band);
  };

  // Marquee dupes stay out of the tab order
  OpenFlow.initMarqueeA11y = function() {
    document.querySelectorAll('.app-matrix .app-pill[aria-hidden="true"]').forEach(function(pill) {
      pill.setAttribute('tabindex', '-1');
    });
  };
})();
