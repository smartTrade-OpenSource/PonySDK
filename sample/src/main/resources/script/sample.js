(function() {
  "use strict";

  AbstractAddon.defineAddon("com.ponysdk.sample.client.page.addon.LoggerAddOn", {

    init: function () {
      console.log("LabelPAddOn created");
    },

    log: function () {
      console.log("LabelPAddOn");
      this.sendDataToServer({
        "info": "Init done"
      });
      this.sendDataToServer({
        info: "Get Data",
        info2: "Get Data2"
      }, function(response) {
          console.log(JSON.parse(response));
      });
    },

    logWithText: function (value) {
      console.log("LabelPAddOn : " + value);
      this.sendDataToServer({
        "info": "Init done"
      });
      this.sendDataToServer({
        info: "Get Data",
        info2: value
      }, function(response) {
          console.log(JSON.parse(response));
      });
    },

  });

  AbstractAddon.defineAddon("com.ponysdk.sample.client.page.addon.PElementAddOn", {

    init: function () {
        console.log("PElementAddOn created")
    },

    text: function (value) {
        this.element.innerHTML = value
    }

  });

  AbstractAddon.defineAddon("com.ponysdk.sample.client.page.addon.SelectizeAddon", {

    init: function () {
    },

    addTag: function (value) {
        console.log(value.tag)
    },

    updateTag: function (value) {
        console.log(value.id)
        console.log(value.oldTag)
        console.log(value.newTag)

        if(value.found == 'no'){
            console.log('red item')
            this.jqelement[0].selectize.getItem(value.oldTag)[0].classList.add('unmatch')
        }else{
            this.jqelement[0].setAttribute('process','true')
            this.jqelement[0].setAttribute('type',value.type)

            console.log('remove and create')
            this.jqelement[0].selectize.removeItem(value.oldTag, true)
            this.jqelement[0].selectize.createItem(value.newTag + '{' + value.desc + '}' ,false)
        }
    },

    text: function (value) {
        var that = this;

        this.jqelement.selectize({
            plugins: ['remove_button'],
            delimiter: ',',
            persist: false,
            create: function (input) {
                return {
                    value: input,
                    text: input
                }
            },
            onItemAdd: function (value, $item) {
                console.log('on add item')
                var process = that.jqelement[0].getAttribute('process')
                var type = that.jqelement[0].getAttribute('type')
                
                if(process == 'true'){
                    that.jqelement[0].setAttribute('process','false')    
                    that.jqelement[0].setAttribute('type','-1')
                    
                    if(type == '1'){
                        $item[0].classList.add('qty-px')
                    }
                    
                    return;
                }
            
                var itemID = $item[0].getAttribute("id")

                    itemID = 'item' + Date.now();
                //    $item[0].setAttribute("id",itemID);
                //}

                pony.sendDataToServer(that.id, {
                    type: 'add',
                    id: itemID,
                    tag: value
                });
            },

            onItemRemove: function(value, $item) {
                console.log('on remove item')
                pony.sendDataToServer(that.id, {
                    type: 'remove',
                    tag: value
                });
            }
        });
    }

  });

  AbstractAddon.defineAddon("com.ponysdk.sample.client.page.addon.BinaryArgsAddOn", {

    init: function () {
      var o = this.options || [];
      window.__createLen = o.length; // deterministic proof for the browser IT (5 typed creation args)
      // The whole chart is configured from pure-binary typed creation args:
      // [color(String), maxPoints(int), lineWidth(double), fill(boolean), seed(long)]
      this._color = (typeof o[0] === "string") ? o[0] : "#43e8b0";
      this._lineWidth = (typeof o[2] === "number") ? o[2] : 2;
      this._fill = !!o[3];
      this._palette = [this._color, "#7c6fff", "#f59e0b", "#f87171"];
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
        + '.ba-card{background:rgba(255,255,255,.018);border:1px solid rgba(255,255,255,.07);'
        +   'border-radius:12px;padding:13px 15px;transition:border-color .15s}'
        + '.ba-card:hover{border-color:rgba(255,255,255,.14)}'
        + '.ba-v{font-size:22px;font-weight:700;line-height:1;color:#fafafa;font-variant-numeric:tabular-nums;'
        +   'font-family:ui-monospace,SFMono-Regular,Menlo,monospace;letter-spacing:-.01em}'
        + '.ba-l{font-size:9.5px;color:#6b6b76;text-transform:uppercase;letter-spacing:.8px;margin-top:8px}'
        + '.ba-chart{background:#0a0b0e;border:1px solid rgba(255,255,255,.07);border-radius:14px;padding:14px 16px}'
        + '.ba-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:10px}'
        + '.ba-live{display:flex;align-items:center;gap:7px;font-size:10.5px;font-weight:600;letter-spacing:1px;'
        +   'text-transform:uppercase;color:#8b8b94}'
        + '.ba-dot{width:7px;height:7px;border-radius:50%;background:var(--accent2,#43e8b0);'
        +   'box-shadow:0 0 6px var(--accent2,#43e8b0);animation:ba-pulse 1.6s infinite}'
        + '@keyframes ba-pulse{0%,100%{opacity:1}50%{opacity:.35}}'
        + '.ba-legend{display:flex;gap:14px;flex-wrap:wrap}'
        + '.ba-leg{display:flex;align-items:center;gap:6px;font-size:11px;color:#8b8b94}'
        + '.ba-leg i{width:10px;height:2px;border-radius:2px;display:inline-block}'
        + '.ba-canvas{width:100%;height:250px;display:block;border-radius:8px}'
        + '.ba-cfg{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:11.5px;color:#6b6b76;margin-top:12px;line-height:1.5}'
        + '.ba-cfg b{color:#8b8b94;font-weight:600}'
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
      ctx.fillStyle = '#0a0b0e'; ctx.fillRect(0, 0, w, h);
      ctx.strokeStyle = 'rgba(255,255,255,0.04)'; ctx.lineWidth = 1;
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
})();
