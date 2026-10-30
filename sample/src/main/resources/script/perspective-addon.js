/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 *
 * The JavaScript of com.ponysdk.sample.perspective.PPerspective (docs/decisions/0029-perspective.md): a
 * <perspective-viewer> over a table that the server feeds with typed arrays. Listed with the application's scripts,
 * after ponysdk.js. It is a few KB: the library itself (script/perspective/, 0.8 MB of JavaScript and 4.2 MB of
 * WebAssembly) is loaded by the first PPerspective of a page, from the application's own origin, and by no other page.
 */
(function () {
  "use strict";

  // script/perspective/, where the build copies the files of the packages (sample/build.gradle, copyPerspective)
  var BASE = new URL('perspective/', document.currentScript.src).href;
  var DAY_MS = 86400000;
  var THEMES = { light: 'Pro Light', dark: 'Pro Dark' };
  var REPORT_DELAY_MS = 100;

  var library = null;  // the Perspective client (an engine in a worker), once loaded
  var charts = null;   // the chart plugin, loaded by the first viewer that asks for it

  function load(path) {
    return import(BASE + path);
  }

  function stylesheet(file) {
    return new Promise(function (resolve) {
      var link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = BASE + 'viewer/dist/css/' + file;
      link.onload = link.onerror = function () { resolve(); };
      document.head.appendChild(link);
    });
  }

  // The engine, the viewer and its grid: one load per page, shared by its viewers. No CDN, no eval: the modules, the
  // two WebAssembly files and the engine's worker are files of the application (its policy needs 'wasm-unsafe-eval',
  // ApplicationConfiguration.setWebAssemblyEnabled)
  function loadLibrary() {
    if (library === null) {
      library = (async function () {
        var viewer = (await load('viewer/dist/esm/perspective-viewer.js')).default;
        var glue = await load('viewer/dist/wasm/perspective-viewer.js');
        var perspective = (await load('client/dist/esm/perspective.js')).default;
        perspective.init_server(fetch(BASE + 'server/dist/wasm/perspective-server.wasm'));
        // The glue passed in is the module the viewer would otherwise import from a blob: URL (which a policy without
        // blob: in script-src refuses); the worker is a file for the same reason
        await viewer.init_client(fetch(BASE + 'viewer/dist/wasm/perspective-viewer.wasm'), glue);
        await Promise.all([load('viewer-datagrid/dist/cdn/perspective-viewer-datagrid.js'),
          stylesheet('pro.css'), stylesheet('pro-dark.css')]);
        var worker = new Worker(BASE + 'client/dist/cdn/perspective-server.worker.js', { type: 'module' });
        return perspective.worker(Promise.resolve(worker));
      })();
    }
    return library;
  }

  // The charts draw in a worker of their own, started from a blob: URL (the policy's script-src names blob:), and add 285 KB:
  // only when asked
  function loadCharts() {
    if (charts === null) charts = load('viewer-charts/dist/cdn/perspective-viewer-charts.js');
    return charts;
  }

  // A configuration as the server sent it: JSON text, which the terminal may already have parsed
  /**
   * Frees a viewer and its table. The element left the page with its tab: its resize observer has queued a task that
   * still reads the view ("null pointer passed to rust" once freed). Two frames let it run, flush waits for the
   * renders under way, then the viewer and the table go.
   */
  async function free(viewer, table) {
    await new Promise(function (done) { requestAnimationFrame(function () { requestAnimationFrame(done); }); });
    await viewer.flush();
    await viewer.delete();
    viewer.remove();
    if (table) await table.delete();
  }

  function parsed(json) {
    return typeof json === 'string' ? JSON.parse(json) : json;
  }

  // The part of a viewer's configuration that the server models: what is reported back, and compared
  function modeled(config) {
    return {
      plugin: config.plugin, group_by: config.group_by, split_by: config.split_by, columns: config.columns,
      sort: config.sort, filter: config.filter, aggregates: config.aggregates, plugin_config: config.plugin_config
    };
  }

  AbstractAddon.defineAddon("com.ponysdk.sample.perspective.PPerspective", {

    // Creation arguments: index, theme, charts, configuration (JSON or ""), column count, then name, type, decimals
    // of each column
    init: function () {
      var o = this.options, i = 0;
      this._index = o[i++];
      this._theme = o[i++];
      var withCharts = o[i++];
      var initial = o[i++];
      var count = o[i++];
      this._schema = {};
      this._decoders = [];   // the columns but the key, in the order of the row masks
      this._datetime = false;
      for (var c = 0; c < count; c++) {
        var column = { name: o[i++], type: o[i++], scale: Math.pow(10, o[i++]) };
        this._schema[column.name] = column.type;
        if (column.name === this._index) this._key = column;
        else this._decoders.push(column);
        if (column.type === 'datetime') this._datetime = true;
      }
      this._table = null;
      this._viewer = null;
      this._reported = null;
      this._timer = 0;
      this._destroyed = false;
      this._applying = false;

      var self = this;
      // Every call waits for the library and the table, then runs in the order it came
      this._chain = loadLibrary().then(function (client) {
        return withCharts ? loadCharts().then(function () { return client; }) : client;
      }).then(function (client) {
        return self._build(client, initial);
      }).catch(function (e) {
        console.error('[PPerspective] ' + (e && e.message ? e.message : e));
        self.element.textContent = 'Perspective could not be loaded: ' + (e && e.message ? e.message : e);
      });
    },

    _build: async function (client, initial) {
      if (this._destroyed) return;
      var name = 'pony-' + this.id;
      var table = await client.table(this._schema, { index: this._index, name: name });
      var viewer = document.createElement('perspective-viewer');
      viewer.style.display = 'block';
      viewer.style.width = '100%';
      viewer.style.height = '100%';
      this.element.appendChild(viewer);
      await viewer.load(client);
      if (this._destroyed) return free(viewer, table); // left while it loaded: destroy() had nothing to free yet
      this._table = table;
      this._viewer = viewer;

      var self = this;
      viewer.addEventListener('perspective-config-update', function () { self._reportLater(); });
      viewer.addEventListener('perspective-click', function (e) {
        self.sendDataToServer({ type: 'click', row: e.detail.row });
      });
      viewer.addEventListener('perspective-select', function (e) {
        self._selected(e.detail).then(function (row) {
          self.sendDataToServer({ type: 'select', selected: row !== null, row: row });
        }).catch(function (error) { console.error('[PPerspective] selection failed', error); });
      });
      this._scheme = window.matchMedia('(prefers-color-scheme: dark)');
      this._onScheme = function () { if (self._theme === 'auto') self._restore({}); };
      this._scheme.addEventListener('change', this._onScheme);

      await this._restore(Object.assign({ table: name }, initial ? parsed(initial) : {}));
      if (!this._destroyed) this.sendDataToServer({ type: 'ready' });
    },

    // The row selected: a grid reports the window of the view that is selected (the first row of it is the one given),
    // a chart the row itself; null when nothing is selected
    _selected: async function (detail) {
      if (!detail) return null; // a grid reports a cleared selection with no detail
      if (typeof detail.start_row === 'number') {
        var rows = await (await this._viewer.getView()).to_json({ start_row: detail.start_row, end_row: detail.start_row + 1 });
        return rows.length > 0 ? rows[0] : null;
      }
      return detail.selected && detail.row ? detail.row : null;
    },

    // Applies a configuration, with the theme, then remembers what the viewer holds so that this is not reported as
    // a change of the user's
    _restore: async function (config) {
      var viewer = this._viewer; // destroy() may take it meanwhile: this one is the one being configured
      var theme = this._theme === 'auto' ? (this._scheme.matches ? 'dark' : 'light') : this._theme;
      this._applying = true;
      try {
        await viewer.restore(Object.assign({ theme: THEMES[theme] }, config));
        if (!this._destroyed) this._reported = JSON.stringify(modeled(await viewer.save()));
      } finally {
        this._applying = false;
      }
    },

    // What the user changes (drag a column, a sort, a plugin) is reported to the server, once it has settled
    _reportLater: function () {
      if (this._applying) return;
      var self = this;
      clearTimeout(this._timer);
      this._timer = setTimeout(async function () {
        var viewer = self._viewer;
        if (self._destroyed || viewer === null) return;
        var config = modeled(await viewer.save());
        if (self._destroyed) return;
        var text = JSON.stringify(config);
        if (text === self._reported) return;
        self._reported = text;
        self.sendDataToServer({ type: 'config', config: config });
      }, REPORT_DELAY_MS);
    },

    // The value of a cell, from its wire form: a float is a whole number of 10^-decimals, a datetime the time since the
    // batch's base, a date a day number
    _value: function (column, value, base) {
      if (value === null) return null;
      switch (column.type) {
        case 'float': return value / column.scale;
        case 'datetime': return value + base;
        case 'date': return value * DAY_MS;
        default: return value;
      }
    },

    // One batch: flags (1: replace the rows of the table), the base time when the table has a datetime, the keys to
    // remove, then the rows: key, the mask of the columns it carries, their values (PPerspective's javadoc)
    // (not "update": AbstractAddon's own method, which dispatches the calls of the server)
    batch: function () {
      var a = arguments, i = 0;
      var replace = (a[i++] & 1) === 1;
      var base = this._datetime ? a[i++] : 0;
      var removed = new Array(a[i++]);
      for (var r = 0; r < removed.length; r++) removed[r] = this._value(this._key, a[i++], base);
      var rows = new Array(a[i++]);
      for (var n = 0; n < rows.length; n++) {
        var row = {};
        row[this._index] = this._value(this._key, a[i++], base);
        var mask = a[i++];
        for (var c = 0; mask !== 0; c++, mask >>>= 1) {
          if ((mask & 1) === 1) row[this._decoders[c].name] = this._value(this._decoders[c], a[i++], base);
        }
        rows[n] = row;
      }
      var self = this;
      this._chain = this._chain.then(async function () {
        var table = self._table;
        if (table === null || self._destroyed) return;
        if (replace) {
          if (rows.length > 0) await table.replace(rows);
          else await table.clear();
        } else {
          if (removed.length > 0) await table.remove(removed);
          if (rows.length > 0) await table.update(rows);
        }
      }).catch(function (e) { console.error('[PPerspective] update failed', e); });
    },

    // A configuration from the server (JSON): the viewer shows it, and does not report it back
    configure: function (json) {
      var self = this;
      this._chain = this._chain.then(function () {
        if (self._viewer !== null && !self._destroyed) return self._restore(parsed(json));
      }).catch(function (e) { console.error('[PPerspective] configure failed', e); });
    },

    reset: function () {
      var self = this;
      this._chain = this._chain.then(function () {
        if (self._viewer !== null && !self._destroyed) return self._viewer.reset().then(function () { return self._restore({}); });
      }).catch(function (e) { console.error('[PPerspective] reset failed', e); });
    },

    destroy: function () {
      this._destroyed = true;
      clearTimeout(this._timer);
      if (this._scheme) this._scheme.removeEventListener('change', this._onScheme);
      var viewer = this._viewer, table = this._table;
      this._viewer = this._table = null;
      // The engine holds the table's rows in WebAssembly memory: freed with the viewer, after what it still does
      this._chain = this._chain.then(function () {
        return viewer ? free(viewer, table) : null;
      }).catch(function () { /* already gone with the page */ });
    }
  });
})();
