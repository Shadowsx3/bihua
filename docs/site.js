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

// Live ink pad on the landing page. Free practice only: no schedule, no upload.
(() => {
  const canvas = document.getElementById("ink-pad");
  if (!canvas) return;
  const board = document.getElementById("pad-board");
  const glyph = document.getElementById("pad-glyph");
  const status = document.getElementById("pad-status");
  const undo = document.getElementById("pad-undo");
  const clear = document.getElementById("pad-clear");
  const guide = document.getElementById("pad-guide");
  const chips = [...document.querySelectorAll(".pad-chip")];
  const ctx = canvas.getContext("2d");
  let strokes = [];
  let current = null;
  let target = 5;
  let width = 600;
  let height = 600;
  let brush = "#17211a";

  const readInk = () => getComputedStyle(document.documentElement).getPropertyValue("--ink").trim() || "#17211a";

  function segmentWidth(stroke, pressure) {
    const p = Math.min(1, Math.max(0, pressure));
    return Math.max(1, stroke.w * width * (0.55 + p * 0.9));
  }

  function drawSegment(a, b, stroke) {
    ctx.beginPath();
    ctx.strokeStyle = brush;
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
    ctx.lineWidth = segmentWidth(stroke, (a.p + b.p) / 2);
    ctx.moveTo(a.x * width, a.y * height);
    ctx.lineTo(b.x * width, b.y * height);
    ctx.stroke();
  }

  function redraw() {
    ctx.clearRect(0, 0, width, height);
    for (const stroke of strokes) {
      if (stroke.points.length === 1) {
        const point = stroke.points[0];
        ctx.beginPath();
        ctx.fillStyle = brush;
        ctx.arc(point.x * width, point.y * height, segmentWidth(stroke, point.p) / 2, 0, Math.PI * 2);
        ctx.fill();
        continue;
      }
      for (let i = 1; i < stroke.points.length; i++) {
        drawSegment(stroke.points[i - 1], stroke.points[i], stroke);
      }
    }
  }

  function updateStatus() {
    undo.disabled = strokes.length === 0;
    clear.disabled = strokes.length === 0;
    const n = strokes.length;
    status.textContent = `${n} of ${target} stroke${target === 1 ? "" : "s"} · count only · nothing leaves this page`;
  }

  function resize() {
    const rect = canvas.getBoundingClientRect();
    if (!rect.width) return;
    width = rect.width;
    height = rect.height;
    const dpr = Math.min(window.devicePixelRatio || 1, 2.5);
    canvas.width = Math.round(width * dpr);
    canvas.height = Math.round(height * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    redraw();
  }

  function pointFrom(event) {
    const rect = canvas.getBoundingClientRect();
    return {
      x: (event.clientX - rect.left) / rect.width,
      y: (event.clientY - rect.top) / rect.height,
      p: event.pressure > 0 ? event.pressure : 0.5,
    };
  }

  function start(event) {
    if (event.button !== undefined && event.button !== 0) return;
    if (event.isPrimary === false) return;
    event.preventDefault();
    try { canvas.setPointerCapture(event.pointerId); } catch (_) {}
    brush = readInk();
    const point = pointFrom(event);
    current = { points: [point], w: width < 420 ? 0.028 : 0.022 };
    strokes.push(current);
    ctx.beginPath();
    ctx.fillStyle = brush;
    ctx.arc(point.x * width, point.y * height, segmentWidth(current, point.p) / 2, 0, Math.PI * 2);
    ctx.fill();
    updateStatus();
  }

  function move(event) {
    if (!current) return;
    event.preventDefault();
    const coalesced = event.getCoalescedEvents ? event.getCoalescedEvents() : [];
    const points = coalesced.length ? coalesced : [event];
    for (const item of points) {
      const point = pointFrom(item);
      const last = current.points[current.points.length - 1];
      if (Math.hypot((point.x - last.x) * width, (point.y - last.y) * height) < 1.1) continue;
      current.points.push(point);
      drawSegment(last, point, current);
    }
  }

  function end(event) {
    if (!current) return;
    if (event.pointerId !== undefined && canvas.hasPointerCapture?.(event.pointerId)) {
      try { canvas.releasePointerCapture(event.pointerId); } catch (_) {}
    }
    current = null;
  }

  canvas.addEventListener("pointerdown", start);
  canvas.addEventListener("pointermove", move);
  canvas.addEventListener("pointerup", end);
  canvas.addEventListener("pointercancel", end);
  canvas.addEventListener("contextmenu", (event) => event.preventDefault());

  undo.addEventListener("click", () => {
    strokes.pop();
    redraw();
    updateStatus();
  });
  clear.addEventListener("click", () => {
    strokes = [];
    redraw();
    updateStatus();
  });
  guide.addEventListener("click", () => {
    const on = guide.getAttribute("aria-pressed") !== "true";
    guide.setAttribute("aria-pressed", String(on));
    guide.textContent = on ? "Guide on" : "Guide off";
    board.classList.toggle("guide-off", !on);
  });
  chips.forEach((chip) => chip.addEventListener("click", () => {
    chips.forEach((item) => {
      const active = item === chip;
      item.classList.toggle("is-active", active);
      item.setAttribute("aria-pressed", String(active));
    });
    const value = chip.dataset.glyph || "永";
    target = Number(chip.dataset.strokes) || 5;
    glyph.textContent = value;
    canvas.setAttribute("aria-label", `Drawing pad for the character ${value}. Use a finger, mouse, or stylus.`);
    strokes = [];
    current = null;
    redraw();
    updateStatus();
  }));

  if ("ResizeObserver" in window) {
    new ResizeObserver(resize).observe(canvas);
  } else {
    window.addEventListener("resize", resize);
  }
  new MutationObserver(() => {
    brush = readInk();
    redraw();
  }).observe(document.documentElement, { attributes: true, attributeFilter: ["data-theme"] });

  brush = readInk();
  resize();
  updateStatus();
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
