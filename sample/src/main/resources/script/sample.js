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
      this._maxPoints = (typeof o[1] === "number") ? o[1] : 1024;
      this._lineWidth = (typeof o[2] === "number") ? o[2] : 2;
      this._fill = !!o[3];
      this._frames = 0;
      this._last = 0;
      this._fps = 0;
      this._cfg = "color=" + this._color + ", maxPoints=" + this._maxPoints
        + ", lineWidth=" + this._lineWidth + ", fill=" + this._fill + "  (" + o.length + " typed binary args)";
      this._buildDom();
    },

    // Deterministic verification frame (1000 ints, 0..999). Sets the IT proof globals; never overwritten.
    verify: function () {
      var arr = Array.prototype.slice.call(arguments);
      var sum = 0;
      for (var i = 0; i < arr.length; i++) sum += arr[i];
      window.__bigLen = arr.length; // 1000
      window.__bigSum = sum;        // 499500
      this._draw(arr);
    },

    // Live streaming frame (N typed doubles). Animates the canvas + live metrics.
    stream: function () {
      var arr = Array.prototype.slice.call(arguments);
      var now = (window.performance && performance.now) ? performance.now() : Date.now();
      if (this._last) { var dt = now - this._last; if (dt > 0) this._fps = 1000 / dt; }
      this._last = now;
      this._frames++;
      window.__liveFrames = this._frames;
      window.__livePoints = arr.length;
      this._draw(arr);
      this._metrics(arr.length);
    },

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
        + '<div class="ba-card"><div class="ba-v" id="ba-points">—</div><div class="ba-l">Points / frame</div></div>'
        + '<div class="ba-card"><div class="ba-v" id="ba-fps">—</div><div class="ba-l">Frames / sec</div></div>'
        + '<div class="ba-card"><div class="ba-v" id="ba-rate">—</div><div class="ba-l">Typed values / sec</div></div>'
        + '<div class="ba-card"><div class="ba-v" id="ba-bin">—</div><div class="ba-l">Binary KB / frame</div></div>'
        + '<div class="ba-card"><div class="ba-v" id="ba-save">—</div><div class="ba-l">Smaller than JSON</div></div>'
        + '</div>'
        + '<canvas class="ba-canvas" id="ba-canvas" width="940" height="260"></canvas>'
        + '<div class="ba-cfg"><b>chart config (pure binary):</b> ' + this._cfg + '</div>';
      this._canvas = this.element.querySelector('#ba-canvas');
    },

    _metrics: function (points) {
      var binBytes = points * 9 + 6;   // 1 type tag + 8 bytes per double, + model key/length overhead
      var jsonBytes = points * 18;     // ~ average chars per double in a JSON array
      var fps = this._fps || 0;
      this._set('ba-points', points);
      this._set('ba-fps', fps ? fps.toFixed(0) : '—');
      this._set('ba-rate', fps ? Math.round(points * fps).toLocaleString() : '—');
      this._set('ba-bin', (binBytes / 1024).toFixed(1));
      this._set('ba-save', (100 * (1 - binBytes / jsonBytes)).toFixed(0) + '%');
    },

    _set: function (id, v) { var e = document.getElementById(id); if (e) e.textContent = v; },

    _draw: function (arr) {
      var c = this._canvas;
      if (!c || !c.getContext) return;
      var ctx = c.getContext('2d'), w = c.width, h = c.height, i, n = arr.length;
      ctx.clearRect(0, 0, w, h);
      ctx.strokeStyle = 'rgba(124,111,255,0.07)'; ctx.lineWidth = 1;
      for (var gx = 0; gx <= w; gx += 47) { ctx.beginPath(); ctx.moveTo(gx, 0); ctx.lineTo(gx, h); ctx.stroke(); }
      for (var gy = 0; gy <= h; gy += 52) { ctx.beginPath(); ctx.moveTo(0, gy); ctx.lineTo(w, gy); ctx.stroke(); }
      if (n < 2) return;
      var min = Infinity, max = -Infinity;
      for (i = 0; i < n; i++) { if (arr[i] < min) min = arr[i]; if (arr[i] > max) max = arr[i]; }
      if (min === max) max = min + 1;
      ctx.beginPath();
      for (i = 0; i < n; i++) {
        var x = (i / (n - 1)) * w;
        var y = h - ((arr[i] - min) / (max - min)) * (h - 24) - 12;
        if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
      }
      ctx.strokeStyle = this._color; ctx.lineWidth = this._lineWidth; ctx.lineJoin = 'round';
      ctx.shadowColor = this._color; ctx.shadowBlur = 10; ctx.stroke(); ctx.shadowBlur = 0;
      if (this._fill) {
        ctx.lineTo(w, h); ctx.lineTo(0, h); ctx.closePath();
        var g = ctx.createLinearGradient(0, 0, 0, h);
        g.addColorStop(0, this._color + '44'); g.addColorStop(1, this._color + '00');
        ctx.fillStyle = g; ctx.fill();
      }
    }

  });
})();
