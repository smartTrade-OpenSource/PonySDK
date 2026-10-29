(function() {
  "use strict";

  AbstractAddon.defineAddon("com.ponysdk.sample.client.page.addon.BinaryArgsAddOn", {

    init: function () {
      var o = this.options || [];
      window.__createLen = o.length; // deterministic proof for the browser IT (5 typed creation args)
      // The whole chart is configured from pure-binary typed creation args:
      // [color(String), maxPoints(int), lineWidth(double), fill(boolean), seed(long)]
      this._color = (typeof o[0] === "string") ? o[0] : "#4f46e5";
      this._lineWidth = (typeof o[2] === "number") ? o[2] : 2;
      this._fill = !!o[3];
      this._palette = [this._color, "#0ea5e9", "#f59e0b", "#e11d48"];
      this._frames = 0;
      this._winCount = 0;
      this._winStart = 0;
      this._fps = 0;
      this._total = 0;
      this._lastTotal = 0;
      this._pending = null;   // latest frame awaiting a render tick
      this._stopped = false;
      this._cfg = "color=" + this._color + ", lineWidth=" + this._lineWidth + ", fill=" + this._fill
        + "  (" + o.length + " typed binary args)";
      this._buildDom();
      this._startRender();    // decouple drawing from the incoming stream rate
    },

    // Deterministic verification frame (1000 ints, 0..999). Sets the IT proof globals; never overwritten.
    verify: function () {
      var arr = Array.prototype.slice.call(arguments);
      var sum = 0;
      for (var i = 0; i < arr.length; i++) sum += arr[i];
      window.__bigLen = arr.length; // 1000
      window.__bigSum = sum;        // 499500
      this._pending = [arr];
      this._syncLegend(1);
    },

    // Live streaming frame. Structured typed binary payload: [n(int), m(int), n*m doubles].
    // We only stash the latest frame + update the (cheap) counters here; the actual canvas draw
    // happens at display refresh in _startRender, so a fast stream never blocks or backs up the UI.
    stream: function () {
      var arr = Array.prototype.slice.call(arguments);
      var n = arr[0] | 0, m = arr[1] | 0, s;
      var series = [];
      for (s = 0; s < n; s++) series.push(arr.slice(2 + s * m, 2 + (s + 1) * m));
      var total = n * m;
      var now = (window.performance && performance.now) ? performance.now() : Date.now();
      if (!this._winStart) this._winStart = now;
      this._frames++;
      this._winCount++;
      var dw = now - this._winStart;
      if (dw >= 1000) { this._fps = this._winCount * 1000 / dw; this._winCount = 0; this._winStart = now; }
      this._total += total;
      this._lastTotal = total;
      window.__liveFrames = this._frames;
      window.__livePoints = total;
      this._pending = series;
      this._syncLegend(n);
      this._metrics();
    },

    _syncLegend: function (n) {
      if (n === this._lastN) return;
      this._lastN = n;
      var el = document.getElementById('ba-legend');
      if (!el) return;
      var html = '';
      for (var i = 0; i < n; i++) {
        html += '<span class="ba-leg"><i style="background:' + this._palette[i % this._palette.length] + '"></i>series ' + (i + 1) + '</span>';
      }
      el.innerHTML = html;
    },

    _startRender: function () {
      var self = this;
      var raf = window.requestAnimationFrame || function (f) { return setTimeout(f, 16); };
      var loop = function () {
        if (self._stopped) return;
        if (self._pending) { self._drawMulti(self._pending); self._pending = null; }
        self._rafId = raf(loop);
      };
      this._rafId = raf(loop);
    },

    onDetached: function () {
      this._stopped = true;
      if (this._rafId) { (window.cancelAnimationFrame || clearTimeout)(this._rafId); this._rafId = null; }
    },

    destroy: function () { this.onDetached(); },

    _buildDom: function () {
      if (!this.element) return;
      this.element.innerHTML = ''
        + '<style>'
        + '.ba-cards{display:grid;grid-template-columns:repeat(6,1fr);gap:10px;margin-bottom:16px}'
        + '@media(max-width:860px){.ba-cards{grid-template-columns:repeat(3,1fr)}}'
        + '.ba-card{background:#fff;border:1px solid #e2e8f0;box-shadow:0 1px 2px rgba(15,23,42,.04);'
        +   'border-radius:12px;padding:13px 15px}'
        + '.ba-card:hover{border-color:#cbd5e1}'
        + '.ba-v{font-size:22px;font-weight:700;line-height:1;color:#0f172a;font-variant-numeric:tabular-nums;'
        +   'font-family:ui-monospace,SFMono-Regular,Menlo,monospace;letter-spacing:-.01em}'
        + '.ba-l{font-size:12px;color:#64748b;margin-top:6px}'
        + '.ba-chart{background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:14px 16px;box-shadow:0 1px 2px rgba(15,23,42,.04)}'
        + '.ba-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:10px}'
        + '.ba-live{display:flex;align-items:center;gap:7px;font-size:13px;font-weight:600;color:#0f172a}'
        + '.ba-dot{width:7px;height:7px;border-radius:50%;background:#10b981;'
        +   'box-shadow:0 0 0 3px rgba(16,185,129,.18);animation:ba-pulse 1.6s infinite}'
        + '@keyframes ba-pulse{0%,100%{opacity:1}50%{opacity:.35}}'
        + '.ba-legend{display:flex;gap:14px;flex-wrap:wrap}'
        + '.ba-leg{display:flex;align-items:center;gap:6px;font-size:12px;color:#64748b}'
        + '.ba-leg i{width:10px;height:2px;border-radius:2px;display:inline-block}'
        + '.ba-canvas{width:100%;height:250px;display:block;border-radius:8px}'
        + '.ba-cfg{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:12px;color:#64748b;margin-top:12px;line-height:1.5}'
        + '.ba-cfg b{color:#334155;font-weight:600}'
        + '</style>'
        + '<div class="ba-cards">'
        + this._card('ba-points', '—', 'Values / frame')
        + this._card('ba-fps', '—', 'Frames / sec')
        + this._card('ba-rate', '—', 'Typed values / sec')
        + this._card('ba-bin', '—', 'Binary KB / frame')
        + this._card('ba-save', '—', 'Smaller than JSON')
        + this._card('ba-total', '0', 'Total streamed')
        + '</div>'
        + '<div class="ba-chart">'
        +   '<div class="ba-head"><div class="ba-live"><span class="ba-dot"></span>Live binary stream</div>'
        +   '<div class="ba-legend" id="ba-legend"></div></div>'
        +   '<canvas class="ba-canvas" id="ba-canvas" width="940" height="250"></canvas>'
        + '</div>'
        + '<div class="ba-cfg"><b>chart config (pure binary):</b> ' + this._cfg
        + ' &mdash; server streams every point; the chart downsamples to the canvas width for display.</div>';
      this._canvas = this.element.querySelector('#ba-canvas');
    },

    _card: function (id, val, label) {
      return '<div class="ba-card"><div class="ba-v" id="' + id + '">' + val + '</div><div class="ba-l">' + label + '</div></div>';
    },

    _metrics: function () {
      var total = this._lastTotal;
      var binBytes = total * 9 + 8;    // 1 type tag + 8 bytes per double, + 2 int header + key/length
      var jsonBytes = total * 18;      // ~ average chars per double in a JSON array
      this._set('ba-points', total.toLocaleString());
      this._set('ba-fps', this._fps ? Math.round(this._fps).toString() : '—');
      this._set('ba-rate', this._fps ? Math.round(total * this._fps).toLocaleString() : '—');
      this._set('ba-bin', (binBytes / 1024).toFixed(1));
      this._set('ba-save', (100 * (1 - binBytes / jsonBytes)).toFixed(0) + '%');
      this._set('ba-total', this._fmt(this._total));
    },

    _set: function (id, v) { var e = document.getElementById(id); if (e) e.textContent = v; },

    _fmt: function (v) {
      if (v >= 1e6) return (v / 1e6).toFixed(2) + 'M';
      if (v >= 1e3) return (v / 1e3).toFixed(1) + 'k';
      return String(v);
    },

    _drawMulti: function (seriesArr) {
      var c = this._canvas;
      if (!c || !c.getContext) return;
      var ctx = c.getContext('2d'), w = c.width, h = c.height, s, i, arr;
      ctx.fillStyle = '#ffffff'; ctx.fillRect(0, 0, w, h);
      ctx.strokeStyle = '#f1f5f9'; ctx.lineWidth = 1;
      for (var gx = 47; gx < w; gx += 47) { ctx.beginPath(); ctx.moveTo(gx, 0); ctx.lineTo(gx, h); ctx.stroke(); }
      for (var gy = 50; gy < h; gy += 50) { ctx.beginPath(); ctx.moveTo(0, gy); ctx.lineTo(w, gy); ctx.stroke(); }
      var min = Infinity, max = -Infinity;
      for (s = 0; s < seriesArr.length; s++) {
        arr = seriesArr[s];
        for (i = 0; i < arr.length; i++) { if (arr[i] < min) min = arr[i]; if (arr[i] > max) max = arr[i]; }
      }
      if (!isFinite(min) || min === max) { min = 0; max = 1; }
      var range = max - min, pad = 16;
      var yOf = function (v) { return h - ((v - min) / range) * (h - 2 * pad) - pad; };
      ctx.lineJoin = 'round'; ctx.lineCap = 'round'; ctx.lineWidth = this._lineWidth;
      for (s = 0; s < seriesArr.length; s++) {
        arr = seriesArr[s];
        var nn = arr.length;
        if (nn < 2) continue;
        // Downsample: never draw more than ~1 point per horizontal pixel — the wire still carried them all.
        var step = Math.max(1, Math.floor(nn / w));
        var col = this._palette[s % this._palette.length];
        ctx.beginPath();
        ctx.moveTo(0, yOf(arr[0]));
        for (i = step; i < nn; i += step) ctx.lineTo((i / (nn - 1)) * w, yOf(arr[i]));
        var lastY = yOf(arr[nn - 1]);
        ctx.lineTo(w, lastY);
        ctx.strokeStyle = col; ctx.stroke();
        // small leading-edge marker (the latest value)
        ctx.beginPath(); ctx.arc(w - 1, lastY, 2.6, 0, 6.283); ctx.fillStyle = col; ctx.fill();
      }
    }

  });

  // AskAddOn: asks its question by an HTTP request of its own, writes the answer on its element
  AbstractAddon.defineAddon("com.ponysdk.sample.client.page.addon.AskAddOn", {
    initDom: function () {
      var element = this.element;
      this.sendDataToServer({ question: this.options.question }, function (answer) {
        element.setAttribute("data-answer", answer);
      });
    }
  });

  // HighlightAddOn: its creation arguments are a JSON object, {"color": ...}
  AbstractAddon.defineAddon("com.ponysdk.sample.client.page.addon.HighlightAddOn", {
    init: function () {
      if (!this.options.color) throw new Error("HighlightAddOn: no color");
    },
    initDom: function () {
      this.element.style.outline = "3px solid " + this.options.color;
    }
  });
})();

// Functions the showcase calls by name (PScript.call, TextFunction.named): the showcase runs without eval (ADR-0015)

/** The "P" monogram of the top bar as the favicon, inline SVG. */
function showcaseFavicon() {
  var l = document.createElement('link');
  l.rel = 'icon';
  l.type = 'image/svg+xml';
  l.href = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32">'
    + '<rect width="32" height="32" rx="8" fill="#0f172a"/><text x="16" y="22" font-family="Arial" '
    + 'font-size="17" font-weight="700" fill="#fff" text-anchor="middle">P</text></svg>');
  document.head.appendChild(l);
}

/**
 * The Theme page's frame (ThemePage): the default theme loaded in this page, after the framework's stylesheets, and the
 * showcase's own stylesheet left out, so that the widgets show the theme and nothing else.
 */
function showcaseTheme() {
  var showcase = document.getElementById('showcase');
  if (showcase) showcase.disabled = true;
  var link = document.createElement('link');
  link.id = 'pony-theme';
  link.rel = 'stylesheet';
  link.href = 'style/pony-theme.css';
  document.head.appendChild(link);
  document.documentElement.classList.add('pony-theme'); // where the theme applies: the whole page
}

/** The colour scheme of the Theme page's frame: "light" or "dark" sets [data-theme] on its page, "system" removes it. */
function showcaseThemeScheme(scheme) {
  var frame = document.querySelector('.th-frame');
  if (!frame || !frame.contentDocument) return;
  var root = frame.contentDocument.documentElement;
  if (scheme === 'system') root.removeAttribute('data-theme');
  else root.setAttribute('data-theme', scheme);
}

/** The text of the oracle's functional label: the same as its Java function. */
function ponies(args) {
  return args[0] + ' ponies';
}

/** The oracle's script whose result comes back. */
function oracleProduct(a, b) {
  return a * b;
}

/** The oracle's cookies, read back through a script. */
function oracleCookies() {
  return document.cookie;
}

/** The native binding of the terminal pages' widget (TerminalPages.widget): writes what the server sends. */
function terminalBinding(id, element) {
  return { update: function (data) { element.textContent = 'Native: ' + data.text; } };
}

/** The function the terminal pages call on a widget's element (TerminalPages.generic). */
function terminalMark(element, text, times) {
  element.setAttribute('data-mark', text === undefined ? 'marked' : text + ' x' + times);
}
