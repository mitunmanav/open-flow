/*
  Shared chrome. Deliberately tiny: the four pages are hand-written, so anything
  that behaves rather than merely renders lives here, once.
*/
(function () {
  "use strict";

  var KEY = "openflow-theme";

  function systemTheme() {
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  }

  function apply(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    var btn = document.querySelector("[data-theme-toggle]");
    if (!btn) return;
    // The label says what the control will do, not what state it is in.
    btn.textContent = theme === "dark" ? "Light" : "Dark";
    btn.setAttribute(
      "aria-label",
      theme === "dark" ? "Switch to the light theme" : "Switch to the dark theme"
    );
  }

  var stored = null;
  try {
    stored = localStorage.getItem(KEY);
  } catch (e) {
    /* Private mode, or storage disabled. The system preference is a fine answer. */
  }

  apply(stored || systemTheme());

  document.addEventListener("click", function (event) {
    var btn = event.target.closest("[data-theme-toggle]");
    if (!btn) return;
    var next =
      document.documentElement.getAttribute("data-theme") === "dark" ? "light" : "dark";
    apply(next);
    try {
      localStorage.setItem(KEY, next);
    } catch (e) {
      /* Preference simply will not persist. */
    }
  });
})();