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
      this._metrics();
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
        + '.ba-cards{display:flex;gap:12px;flex-wrap:wrap;margin-bottom:14px}'
        + '.ba-card{flex:1;min-width:120px;background:#131929;border:1px solid #1e2d45;border-radius:12px;padding:12px 14px}'
        + '.ba-v{font-size:22px;font-weight:800;color:#43e8b0;line-height:1;font-variant-numeric:tabular-nums}'
        + '.ba-l{font-size:10px;color:#5a6b85;text-transform:uppercase;letter-spacing:.8px;margin-top:6px}'
        + '.ba-canvas{width:100%;height:260px;display:block;background:#0b0f1a;border:1px solid #1e2d45;border-radius:12px}'
        + '.ba-cfg{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:12px;color:#8892a4;margin-top:10px}'
        + '.ba-cfg b{color:#7c6fff}'
        + '</style>'
        + '<div class="ba-cards">'
        + '<div class="ba-card"><div class="ba-v" id="ba-points">—</div><div class="ba-l">Values / frame</div></div>'
        + '<div class="ba-card"><div class="ba-v" id="ba-fps">—</div><div class="ba-l">Frames / sec</div></div>'
        + '<div class="ba-card"><div class="ba-v" id="ba-rate">—</div><div class="ba-l">Typed values / sec</div></div>'
        + '<div class="ba-card"><div class="ba-v" id="ba-bin">—</div><div class="ba-l">Binary KB / frame</div></div>'
        + '<div class="ba-card"><div class="ba-v" id="ba-save">—</div><div class="ba-l">Smaller than JSON</div></div>'
        + '<div class="ba-card"><div class="ba-v" id="ba-total">0</div><div class="ba-l">Total streamed</div></div>'
        + '</div>'
        + '<canvas class="ba-canvas" id="ba-canvas" width="940" height="260"></canvas>'
        + '<div class="ba-cfg"><b>chart config (pure binary):</b> ' + this._cfg
        + ' — server streams every point; the chart downsamples to the canvas width for display.</div>';
      this._canvas = this.element.querySelector('#ba-canvas');
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
      ctx.clearRect(0, 0, w, h);
      ctx.strokeStyle = 'rgba(124,111,255,0.07)'; ctx.lineWidth = 1;
      for (var gx = 0; gx <= w; gx += 47) { ctx.beginPath(); ctx.moveTo(gx, 0); ctx.lineTo(gx, h); ctx.stroke(); }
      for (var gy = 0; gy <= h; gy += 52) { ctx.beginPath(); ctx.moveTo(0, gy); ctx.lineTo(w, gy); ctx.stroke(); }
      var min = Infinity, max = -Infinity;
      for (s = 0; s < seriesArr.length; s++) {
        arr = seriesArr[s];
        for (i = 0; i < arr.length; i++) { if (arr[i] < min) min = arr[i]; if (arr[i] > max) max = arr[i]; }
      }
      if (!isFinite(min) || min === max) { min = 0; max = 1; }
      var range = max - min;
      for (s = 0; s < seriesArr.length; s++) {
        arr = seriesArr[s];
        var nn = arr.length;
        if (nn < 2) continue;
        // Downsample: never draw more than ~1 point per horizontal pixel — the wire still carried them all.
        var step = Math.max(1, Math.floor(nn / w));
        var col = this._palette[s % this._palette.length];
        ctx.beginPath();
        ctx.moveTo(0, h - ((arr[0] - min) / range) * (h - 24) - 12);
        for (i = step; i < nn; i += step) {
          ctx.lineTo((i / (nn - 1)) * w, h - ((arr[i] - min) / range) * (h - 24) - 12);
        }
        ctx.lineTo(w, h - ((arr[nn - 1] - min) / range) * (h - 24) - 12);
        ctx.strokeStyle = col; ctx.lineWidth = this._lineWidth; ctx.lineJoin = 'round'; ctx.stroke();
        if (this._fill && s === 0) {
          ctx.lineTo(w, h); ctx.lineTo(0, h); ctx.closePath();
          var g = ctx.createLinearGradient(0, 0, 0, h);
          g.addColorStop(0, col + '2e'); g.addColorStop(1, col + '00');
          ctx.fillStyle = g; ctx.fill();
        }
      }
    }

  });
})();
