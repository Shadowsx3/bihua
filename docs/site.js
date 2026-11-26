(() => {
  const root = document.documentElement;
  const stored = localStorage.getItem("bihua-theme");
  root.dataset.theme = stored === "dark" ? "dark" : "light";

  const themeMeta = document.querySelector('meta[name="theme-color"]');
  const syncThemeMeta = () => {
    if (themeMeta) themeMeta.setAttribute("content", root.dataset.theme === "dark" ? "#0f1511" : "#f7f2e6");
  };
  syncThemeMeta();

  const toggle = document.querySelector("[data-theme-toggle]");
  const updateLabel = () => {
    if (!toggle) return;
    const dark = root.dataset.theme === "dark";
    toggle.setAttribute("aria-label", dark ? "Dark theme, switch to light" : "Light theme, switch to dark");
    toggle.setAttribute("aria-pressed", dark ? "true" : "false");
    const icon = toggle.querySelector(".theme-icon");
    const label = toggle.querySelector(".theme-label");
    if (icon) icon.textContent = dark ? "☾" : "☀";
    if (label) label.textContent = dark ? "Dark" : "Light";
  };
  updateLabel();
  toggle?.addEventListener("click", () => {
    const next = root.dataset.theme === "dark" ? "light" : "dark";
    root.dataset.theme = next;
    localStorage.setItem("bihua-theme", next);
    syncThemeMeta();
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

  const paceChips = [...document.querySelectorAll(".pace-chip")];
  const paceWeek = document.getElementById("pace-week");
  const paceMonth = document.getElementById("pace-month");
  const paceCta = document.getElementById("pace-cta");
  if (paceChips.length) {
    const setPace = (pace) => {
      paceChips.forEach((chip) => {
        const on = Number(chip.dataset.pace) === pace;
        chip.classList.toggle("is-active", on);
        chip.setAttribute("aria-pressed", on ? "true" : "false");
      });
      if (paceWeek) paceWeek.textContent = String(pace * 7);
      if (paceMonth) paceMonth.textContent = String(pace * 30);
      if (paceCta) paceCta.textContent = String(pace);
    };
    paceChips.forEach((chip) => {
      chip.addEventListener("click", () => setPace(Number(chip.dataset.pace)));
    });
    setPace(3);
  }

  const menuToggle = document.querySelector("[data-menu-toggle]");
  const menu = document.querySelector("[data-menu]");
  const closeMenu = () => {
    if (!menu || !menuToggle) return;
    menu.hidden = true;
    menuToggle.setAttribute("aria-expanded", "false");
    menuToggle.setAttribute("aria-label", "Open menu");
    menuToggle.textContent = "☰";
    document.body.classList.remove("menu-open");
  };
  const openMenu = () => {
    if (!menu || !menuToggle) return;
    menu.hidden = false;
    menuToggle.setAttribute("aria-expanded", "true");
    menuToggle.setAttribute("aria-label", "Close menu");
    menuToggle.textContent = "×";
    document.body.classList.add("menu-open");
  };
  menuToggle?.addEventListener("click", () => {
    if (menu?.hidden) openMenu(); else closeMenu();
  });
  menu?.querySelectorAll("a").forEach((link) => link.addEventListener("click", closeMenu));
  document.addEventListener("keydown", (event) => { if (event.key === "Escape") closeMenu(); });
  window.addEventListener("resize", () => { if (innerWidth > 980) closeMenu(); });

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

/* Interactive grimoire sample: chapter tabs, spell pages, one detail card. */
(() => {
  const root = document.getElementById("grimoire");
  if (!root) return;
  const tabs = root.querySelector(".grimoire-tabs");
  const grid = root.querySelector("#grimoire-grid");
  const chapterLabel = root.querySelector("#grimoire-chapter");
  const countLabel = root.querySelector("#grimoire-count");
  const detail = root.querySelector("#grimoire-detail");
  if (!tabs || !grid || !chapterLabel || !detail) return;

  let chapters = [];
  let active = 0;

  function renderDetail(spell) {
    if (!spell) return;
    detail.innerHTML = "";
    const glyph = document.createElement("div");
    glyph.className = "grimoire-glyph";
    glyph.textContent = spell.c;
    const copy = document.createElement("div");
    copy.className = "grimoire-copy";
    const reading = document.createElement("div");
    reading.className = "grimoire-reading";
    reading.textContent = spell.r;
    const meaning = document.createElement("div");
    meaning.className = "grimoire-meaning";
    meaning.textContent = spell.m;
    const stamp = document.createElement("div");
    stamp.className = "grimoire-stamp";
    stamp.textContent = "A met page";
    copy.append(reading, meaning, stamp);
    detail.append(glyph, copy);
  }

  function render(index, selected = 0) {
    const chapter = chapters[index];
    if (!chapter) return;
    active = index;
    chapterLabel.textContent = chapter.name;
    countLabel.textContent = `${chapter.spells.length} pages`;
    Array.from(tabs.children).forEach((tab, i) => {
      tab.setAttribute("aria-selected", String(i === index));
      tab.tabIndex = i === index ? 0 : -1;
    });
    grid.innerHTML = "";
    chapter.spells.forEach((spell, i) => {
      const button = document.createElement("button");
      button.type = "button";
      button.className = "grimoire-spell";
      button.setAttribute("aria-pressed", String(i === selected));
      const glyph = document.createElement("span");
      glyph.textContent = spell.c;
      const hint = document.createElement("small");
      hint.textContent = spell.m;
      button.append(glyph, hint);
      button.addEventListener("click", () => {
        Array.from(grid.children).forEach((item) => item.setAttribute("aria-pressed", "false"));
        button.setAttribute("aria-pressed", "true");
        renderDetail(spell);
      });
      grid.append(button);
    });
    renderDetail(chapter.spells[selected] || chapter.spells[0]);
  }

  fetch(root.dataset.src || "assets/grimoire.json")
    .then((response) => {
      if (!response.ok) throw new Error("grimoire unavailable");
      return response.json();
    })
    .then((data) => {
      chapters = Array.isArray(data.chapters) ? data.chapters : [];
      if (!chapters.length) return;
      chapters.forEach((chapter, index) => {
        const tab = document.createElement("button");
        tab.type = "button";
        tab.className = "grimoire-tab";
        tab.setAttribute("role", "tab");
        tab.setAttribute("aria-controls", "grimoire-panel");
        tab.setAttribute("aria-selected", String(index === 0));
        tab.tabIndex = index === 0 ? 0 : -1;
        tab.textContent = chapter.name;
        tab.addEventListener("click", () => render(index));
        tab.addEventListener("keydown", (event) => {
          const delta = event.key === "ArrowRight" ? 1 : event.key === "ArrowLeft" ? -1 : 0;
          if (!delta) return;
          event.preventDefault();
          const next = (index + delta + chapters.length) % chapters.length;
          render(next);
          tabs.children[next]?.focus();
        });
        tabs.append(tab);
      });
      render(0);
    })
    .catch(() => {
      // Leave the static fallback detail in place; the rest of the page works.
    });
})();
