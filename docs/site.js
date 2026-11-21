(() => {
  const root = document.documentElement;
  const stored = localStorage.getItem("bihua-theme");
  if (stored === "dark" || stored === "light") root.dataset.theme = stored;
  else if (window.matchMedia("(prefers-color-scheme: dark)").matches) root.dataset.theme = "dark";

  const toggle = document.querySelector("[data-theme-toggle]");
  const updateLabel = () => {
    if (!toggle) return;
    const dark = root.dataset.theme === "dark";
    toggle.setAttribute("aria-label", dark ? "Use light theme" : "Use dark theme");
    toggle.textContent = dark ? "☀" : "☾";
  };
  updateLabel();
  toggle?.addEventListener("click", () => {
    const next = root.dataset.theme === "dark" ? "light" : "dark";
    root.dataset.theme = next;
    localStorage.setItem("bihua-theme", next);
    updateLabel();
  });

  const header = document.querySelector("header.site");
  const onScroll = () => header?.classList.toggle("scrolled", window.scrollY > 8);
  onScroll();
  window.addEventListener("scroll", onScroll, { passive: true });

  document.documentElement.classList.add("js");
  const reveals = document.querySelectorAll(".reveal");
  if ("IntersectionObserver" in window) {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("visible");
          observer.unobserve(entry.target);
        }
      });
    }, { rootMargin: "0px 0px -8% 0px", threshold: 0.08 });
    reveals.forEach((el) => observer.observe(el));
  } else {
    reveals.forEach((el) => el.classList.add("visible"));
  }

  const progress = document.querySelector("[data-reading-progress]");
  if (progress) {
    const update = () => {
      const max = document.documentElement.scrollHeight - innerHeight;
      const pct = max > 0 ? Math.min(100, Math.max(0, (scrollY / max) * 100)) : 0;
      progress.style.transform = `scaleX(${pct / 100})`;
    };
    update();
    window.addEventListener("scroll", update, { passive: true });
    window.addEventListener("resize", update);
  }
})();
