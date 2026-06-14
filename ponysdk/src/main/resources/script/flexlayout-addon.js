(function () {
  "use strict";

  if (typeof FlexLayout === 'undefined') {
    console.error('[FlexLayoutAddon] FlexLayout library not loaded. Ensure flexlayout.js is included before this script.');
    return;
  }

  var _uid = 0;
  function genId() { return 'fl_p_' + (++_uid).toString(36) + '_' + Date.now().toString(36); }
  function parseJson(val) { return (typeof val === 'string') ? JSON.parse(val) : val; }
  function debounce(fn, ms) {
    var timer;
    return function () {
      var args = arguments, ctx = this;
      clearTimeout(timer);
      timer = setTimeout(function () { fn.apply(ctx, args); }, ms);
    };
  }

  AbstractAddon.defineAddon("com.ponysdk.core.ui.flexlayout.FlexLayoutAddon", {

    init: function () {
      this._layout = null;
      this._model = null;
      this._layoutContainer = null;
      this._factory = null;
      this._notifyModelChange = false;
      this._notifyActions = false;
      this._enablePopOut = true;
      this._tabWidgetMap = {};
      this._pendingTimeouts = [];
      this._autoSaveEnabled = false; // Feature 14
    },

    initDom: function () {
      var self = this;
      var el = this.element;
      el.style.cssText = 'width:100%;height:100%;overflow:hidden;position:relative;';

      var config = this.options || {};
      var modelJson = config.model || { type: 'row', children: [{ type: 'tabset', weight: 100, children: [] }] };
      var borders = config.borders || null;
      var theme = config.theme || '';

      this._layoutContainer = document.createElement('div');
      this._layoutContainer.style.cssText = 'width:100%;position:absolute;top:0;left:0;right:0;bottom:20px;';
      el.appendChild(this._layoutContainer);

      // Feature 10: Status Bar
      this._statusBar = document.createElement('div');
      this._statusBar.className = 'fl-status-bar';
      el.appendChild(this._statusBar);

      this._factory = this._createFactory();
      var modelDef = { layout: modelJson };
      if (borders) modelDef.borders = borders;
      this._model = FlexLayout.Model.fromJson(modelDef);
      this._layout = this._createLayout();

      if (theme) this._layoutContainer.classList.add(theme);
    },

    _createFactory: function () {
      var self = this;
      return function (tabNode) {
        var cfg = tabNode.getConfig();
        var host = document.createElement('div');
        host.className = 'fl-pony-widget-host';
        host.dataset.tabId = tabNode.getId();
        host.style.cssText = 'width:100%;height:100%;overflow:auto;';
        if (cfg && cfg.widgetId) {
          host.dataset.widgetId = cfg.widgetId;
          setTimeout(function () { self._moveWidgetToHost(cfg.widgetId, host); }, 50);
        } else {
          host.innerHTML = '';
        var placeholder = document.createElement('span');
        placeholder.style.cssText = 'color:var(--fl-text-dim,#6c7086);font-size:12px;padding:8px;display:block;';
        placeholder.textContent = tabNode.getName();
        host.appendChild(placeholder);
        }
        return host;
      };
    },

    _createLayout: function () {
      var self = this;
      // Blocklists for onBeforeAction hook
      this._blockedActions = [];
      this._blockedTabs = [];

      // Debounce full model sync (only if explicitly requested via enableModelChangeNotification)
      this._debouncedModelChange = debounce(function (model) {
        if (self._notifyModelChange) {
          self.sendDataToServer({ type: 'modelChange', model: JSON.stringify(model.toJson()) });
        }
      }, 250);

      // Feature 14: Auto-save debounced (2s inactivity)
      this._debouncedAutoSave = debounce(function (model) {
        if (self._autoSaveEnabled) {
          self.sendDataToServer({ type: 'autoSave', model: JSON.stringify(model.toJson()) });
        }
      }, 2000);

      var layout = new FlexLayout.Layout({
        model: this._model,
        container: this._layoutContainer,
        factory: this._factory,
        tabSetButtons: function (tsNode) {
          if (!self._enablePopOut) return null;
          var btn = document.createElement('button');
          btn.type = 'button';
          btn.className = 'fl-tbtn fl-tbtn-popout';
          btn.title = 'Pop out (overlay)';
          btn.innerHTML = '⧉';
          btn.style.cssText = 'font-size:11px;';
          btn.addEventListener('click', function () {
            var selNode = tsNode.getSelectedNode();
            if (!selNode) return;
            var rect = self._getTabContentRect(tsNode);
            self.popOut(selNode.getId(), selNode.getName(), rect.x, rect.y, rect.w, rect.h, 'float');
          });
          var btn2 = document.createElement('button');
          btn2.type = 'button';
          btn2.className = 'fl-tbtn fl-tbtn-popout-win';
          btn2.title = 'Pop out (new window)';
          btn2.innerHTML = '↗';
          btn2.style.cssText = 'font-size:12px;';
          btn2.addEventListener('click', function () {
            var selNode = tsNode.getSelectedNode();
            if (!selNode) return;
            var rect = self._getTabContentRect(tsNode);
            self.popOut(selNode.getId(), selNode.getName(), rect.x, rect.y, rect.w, rect.h, 'window');
          });
          return [btn, btn2];
        },
        onModelChange: function (model) {
          self._debouncedModelChange(model);
          self._debouncedAutoSave(model); // Feature 14
        },
        onAction: function (action) {
          return self._handleAction(action);
        },
        onUndoRedo: function () {
          self._rehydrateAfterUndoRedo();
        }
      });

      // Hook 1: onBeforeAction with blocklist
      layout.onBeforeAction = function (action) {
        if (self._blockedActions.indexOf(action.type) >= 0) return false;
        if (self._blockedTabs.length > 0 && action.tabId && self._blockedTabs.indexOf(action.tabId) >= 0) return false;
        return undefined; // allow
      };

      return layout;
    },

    _handleAction: function (action) {
      if (action.type === 'CLOSE_TAB' || action.type === 'CLOSE_BORDER_TAB') {
        var tabId = action.tabId;
        var widgetId = this._tabWidgetMap[tabId] || null;
        delete this._tabWidgetMap[tabId];
        this.sendDataToServer({ type: 'tabClosed', tabId: tabId, widgetId: widgetId });
      }
      if ((action.type === 'ADD_TAB' || action.type === 'ADD_TAB_SPLIT' || action.type === 'ADD_BORDER_TAB') &&
          action.tab && action.tab.component && action.tab.component !== 'pwidget') {
        this.sendDataToServer({ type: 'externalDrop', tabId: action.tab.id, component: action.tab.component, tabName: action.tab.name, config: action.tab.config ? JSON.stringify(action.tab.config) : null });
      }
      // Hook 3: tabVisible / tabHidden for SELECT_TAB
      if (action.type === 'SELECT_TAB') {
        this.sendDataToServer({ type: 'tabSelected', tabId: action.tabId, tabsetId: action.tabsetId });
        // Find previously visible tab in this tabset
        var ts = this._model.getRoot().findById(action.tabsetId);
        if (ts) {
          var prevSel = ts.getSelectedNode();
          if (prevSel && prevSel.getId() !== action.tabId) {
            this.sendDataToServer({ type: 'tabHidden', tabId: prevSel.getId() });
          }
        }
        this.sendDataToServer({ type: 'tabVisible', tabId: action.tabId });
      }
      // Hook 3: tabVisible / tabHidden for SELECT_BORDER_TAB
      if (action.type === 'SELECT_BORDER_TAB') {
        var border = this._model.getBorder(action.side);
        if (border) {
          var prevBorderSel = border.getSelectedNode();
          var idx = border.children.findIndex ? -1 : -1;
          for (var bi = 0; bi < border.children.length; bi++) {
            if (border.children[bi].id === action.tabId) { idx = bi; break; }
          }
          var isToggleOff = (idx >= 0 && border.getSelected() === idx);
          if (isToggleOff) {
            this.sendDataToServer({ type: 'tabHidden', tabId: action.tabId });
          } else {
            if (prevBorderSel && prevBorderSel.getId() !== action.tabId) {
              this.sendDataToServer({ type: 'tabHidden', tabId: prevBorderSel.getId() });
            }
            this.sendDataToServer({ type: 'tabVisible', tabId: action.tabId });
          }
        }
      }
      // Lightweight action notification (much smaller than full model)
      if (this._notifyActions) {
        this.sendDataToServer(this._serializeAction(action));
      }
      // Feature 4: Broadcast for collaboration
      if (this._onActionBroadcast) {
        this._onActionBroadcast(action);
      }
      return true;
    },

    _serializeAction: function (action) {
      // Compact action encoding — only essential fields, no full model tree
      var msg = { type: 'action', a: action.type };
      switch (action.type) {
        case 'SELECT_TAB':
          msg.ts = action.tabsetId; msg.t = action.tabId; break;
        case 'CLOSE_TAB':
          msg.t = action.tabId; break;
        case 'MOVE_TAB':
          msg.t = action.tabId; msg.to = action.toId; msg.l = action.location;
          if (action.insertIndex != null) msg.i = action.insertIndex; break;
        case 'MAXIMIZE_TOGGLE':
          msg.ts = action.tabsetId; break;
        case 'ADD_TAB': case 'ADD_TAB_SPLIT':
          msg.ts = action.tabsetId; msg.to = action.toId; msg.l = action.location;
          if (action.tab) { msg.t = action.tab.id; msg.n = action.tab.name; msg.c = action.tab.component; }
          break;
        case 'RENAME_TAB':
          msg.t = action.tabId; msg.n = action.name; break;
        case 'SELECT_BORDER_TAB':
          msg.s = action.side; msg.t = action.tabId; break;
        case 'CLOSE_BORDER_TAB':
          msg.s = action.side; msg.t = action.tabId; break;
        case 'ADD_BORDER_TAB':
          msg.s = action.side; if (action.tab) { msg.t = action.tab.id; msg.n = action.tab.name; msg.c = action.tab.component; } break;
        case 'MOVE_TO_BORDER':
          msg.t = action.tabId; msg.s = action.side; break;
        case 'MOVE_FROM_BORDER':
          msg.s = action.side; msg.t = action.tabId; msg.to = action.toId; msg.l = action.location; break;
        case 'RESIZE_BORDER':
          msg.s = action.side; msg.sz = action.size; break;
        case 'TOGGLE_BORDER':
          msg.s = action.side; break;
      }
      return msg;
    },

    _moveWidgetToHost: function (widgetId, hostEl) {
      var self = this;
      var wid = String(widgetId);
      var children = this.element.children;

      // Strategy 1: match by data-objectid (if GWT sets it)
      for (var i = 0; i < children.length; i++) {
        var child = children[i];
        if (child === this._layoutContainer) continue;
        if (child.dataset && child.dataset.objectid === wid) {
          hostEl.appendChild(child);
          return;
        }
      }
      // Strategy 2: last non-layout child (widget was just appended by PonySDK)
      for (var i = children.length - 1; i >= 0; i--) {
        var child = children[i];
        if (child === this._layoutContainer) continue;
        hostEl.appendChild(child);
        return;
      }
      // Retry with backoff
      if (!hostEl._retryCount) hostEl._retryCount = 0;
      if (++hostEl._retryCount <= 20) {
        var tid = setTimeout(function () { self._moveWidgetToHost(widgetId, hostEl); }, 100);
        self._pendingTimeouts.push(tid);
      }
    },



    // ─── Server API ─────────────────────────────────────────────

    addTab: function (tabId, tabName, widgetId, tabsetId) {
      if (!this._model) return;
      this._tabWidgetMap[tabId] = widgetId;
      this._model.doAction({
        type: 'ADD_TAB', tabsetId: tabsetId || null, select: true,
        tab: { id: tabId, name: tabName, component: 'pwidget', config: { widgetId: widgetId } }
      });
    },

    addPinnedTab: function (tabId, tabName, widgetId, tabsetId) {
      if (!this._model) return;
      this._tabWidgetMap[tabId] = widgetId;
      this._model.doAction({
        type: 'ADD_TAB', tabsetId: tabsetId || null, select: true,
        tab: { id: tabId, name: tabName, component: 'pwidget', config: { widgetId: widgetId }, enableClose: false, enableDrag: false }
      });
    },

    attachWidget: function (tabId, widgetId) {
      var self = this;
      this._tabWidgetMap[tabId] = widgetId;
      var tabNode = this._model.findById(tabId);
      if (tabNode) tabNode.config = { widgetId: widgetId };
      setTimeout(function () {
        var host = self._layoutContainer.querySelector('.fl-pony-widget-host[data-tab-id="' + CSS.escape(tabId) + '"]');
        if (host) {
          host.innerHTML = '';
          host.dataset.widgetId = widgetId;
          self._moveWidgetToHost(widgetId, host);
        }
      }, 100);
    },

    removeTab: function (tabId) {
      if (!this._model) return;
      delete this._tabWidgetMap[tabId];
      // Close float popout if exists
      if (this._popOuts && this._popOuts[tabId]) {
        var info = this._popOuts[tabId];
        if (info.win) info.win.remove();
        delete this._popOuts[tabId];
      }
      this._model.doAction({ type: 'CLOSE_TAB', tabId: tabId });
    },

    // ─── Sidebar API ────────────────────────────────────────────

    addBorderTab: function (side, tabId, tabName, widgetId, index, icon) {
      if (!this._model) return;
      this._tabWidgetMap[tabId] = widgetId;
      var tabDef = { id: tabId, name: tabName, component: 'pwidget', config: { widgetId: widgetId } };
      if (icon) tabDef.icon = icon;
      this._model.doAction({
        type: 'ADD_BORDER_TAB', side: side, select: true,
        index: index != null ? index : undefined,
        tab: tabDef
      });
    },

    removeBorderTab: function (side, tabId) {
      if (!this._model) return;
      delete this._tabWidgetMap[tabId];
      this._model.doAction({ type: 'CLOSE_BORDER_TAB', side: side, tabId: tabId });
    },

    selectBorderTab: function (side, tabId) {
      if (!this._model) return;
      this._model.doAction({ type: 'SELECT_BORDER_TAB', side: side, tabId: tabId });
    },

    moveToBorder: function (tabId, side) {
      if (!this._model) return;
      this._model.doAction({ type: 'MOVE_TO_BORDER', tabId: tabId, side: side });
    },

    moveFromBorder: function (side, tabId, toTabsetId) {
      if (!this._model) return;
      this._model.doAction({ type: 'MOVE_FROM_BORDER', side: side, tabId: tabId, toId: toTabsetId || null, location: 'center' });
    },

    toggleBorder: function (side) {
      if (!this._model) return;
      this._model.doAction({ type: 'TOGGLE_BORDER', side: side });
    },

    setBorderTabStyle: function (side, tabStyle) {
      if (!this._model) return;
      var border = this._model.getBorder(side);
      if (border) {
        border.tabStyle = tabStyle;
        this._model.emit('change', this._model);
      }
    },

    setTheme: function (theme) {
      if (!this._layoutContainer) return;
      this._layoutContainer.className = this._layoutContainer.className.replace(/fl-theme-\S+/g, '').trim();
      if (theme) this._layoutContainer.classList.add(theme);
    },

    loadModel: function (modelJsonInput, migrateFn) {
      if (!this._layout) return;
      this._layout.destroy();
      this._tabWidgetMap = {};
      this._popOuts = {};
      this._model = FlexLayout.Model.fromJson(parseJson(modelJsonInput), migrateFn || null);
      this._layout = this._createLayout();
      // Rehydrate: notify server of all tabs that need widget creation (single batch message)
      var tabs = [];
      this._collectRehydrateTabs(this._model.getRoot(), tabs);
      // Also scan borders
      var borders = this._model.getBorders ? this._model.getBorders() : [];
      for (var b = 0; b < borders.length; b++) {
        var border = borders[b];
        for (var c = 0; c < border.children.length; c++) {
          var child = border.children[c];
          var comp = child.getComponent ? child.getComponent() : null;
          if (comp && comp !== '' && comp !== 'pwidget') {
            var cfg = child.getConfig ? child.getConfig() : null;
            tabs.push({ tabId: child.getId(), component: comp, tabName: child.getName(), config: cfg ? JSON.stringify(cfg) : null });
          }
        }
      }
      if (tabs.length > 0) this.sendDataToServer({ type: 'rehydrate', tabs: JSON.stringify(tabs) });
    },

    _collectRehydrateTabs: function (node, out) {
      if (!node) return;
      if (node.getType() === 'tab') {
        var component = node.getComponent ? node.getComponent() : null;
        if (component && component !== '' && component !== 'pwidget') {
          var cfg = node.getConfig ? node.getConfig() : null;
          out.push({ tabId: node.getId(), component: component, tabName: node.getName(), config: cfg ? JSON.stringify(cfg) : null });
        }
        return;
      }
      var children = node.getChildren ? node.getChildren() : [];
      for (var i = 0; i < children.length; i++) {
        this._collectRehydrateTabs(children[i], out);
      }
    },

    enableModelChangeNotification: function (enabled) {
      this._notifyModelChange = !!enabled;
    },

    enableActionNotification: function (enabled) {
      this._notifyActions = !!enabled;
    },

    enableAutoSave: function (enabled) {
      this._autoSaveEnabled = !!enabled;
    },

    setModelChangeDebounce: function (delayMs) {
      var self = this;
      var ms = parseInt(delayMs) || 0;
      this._debouncedModelChange = ms > 0
        ? debounce(function (model) {
            if (self._notifyModelChange) {
              self.sendDataToServer({ type: 'modelChange', model: JSON.stringify(model.toJson()) });
            }
          }, ms)
        : function (model) {
            if (self._notifyModelChange) {
              self.sendDataToServer({ type: 'modelChange', model: JSON.stringify(model.toJson()) });
            }
          };
    },

    getModel: function () {
      if (!this._model) return;
      this.sendDataToServer({ type: 'modelSnapshot', model: JSON.stringify(this._model.toJson()) });
    },

    startExternalDrag: function (tabDef) {
      this._pendingExternalDrag = { tabDef: parseJson(tabDef) };
    },

    registerDragSource: function (sourceWidgetId, tabDefJson) {
      var self = this;
      var tabDef = parseJson(tabDefJson);
      var retries = 0;
      (function tryAttach() {
        var el = document.getElementById(String(sourceWidgetId))
              || document.querySelector('[data-objectid="' + sourceWidgetId + '"]');
        if (!el) { if (++retries < 30) { var tid = setTimeout(tryAttach, 100); self._pendingTimeouts.push(tid); } return; }
        self._bindDragSource(el, tabDef);
      })();
    },

    registerDragSourceByClass: function (cssClass, tabDefJson) {
      var self = this;
      var tabDef = parseJson(tabDefJson);
      var retries = 0;
      (function tryAttach() {
        var els = document.querySelectorAll('.' + cssClass);
        if (els.length === 0) { if (++retries < 30) { var tid = setTimeout(tryAttach, 100); self._pendingTimeouts.push(tid); } return; }
        els.forEach(function (el) {
          if (!el.dataset.flDragBound) self._bindDragSource(el, tabDef);
        });
      })();
    },

    _bindDragSource: function (el, tabDef) {
      var self = this;
      el.dataset.flDragBound = '1';
      el.style.touchAction = 'none';
      el.style.cursor = 'grab';
      el.addEventListener('pointerdown', function (ev) {
        if (ev.button !== 0) return;
        ev.preventDefault();
        self._layout.startExternalDrag(ev, Object.assign({}, tabDef, { id: genId() }), el);
      });
    },

    // ─── Pop-out / Pop-in ──────────────────────────────────────────

    popOut: function (tabId, title, x, y, w, h, mode) {
      if (!this._model) return;
      var self = this;
      if (!this._popOuts) this._popOuts = {};
      var popMode = mode || 'float'; // 'float' or 'window'

      var tabNode = this._model.findById(tabId);
      if (!tabNode) return;
      var tabsetNode = tabNode.getParent();
      var tabsetId = tabsetNode ? tabsetNode.getId() : null;
      var tabIdx = tabsetNode ? tabsetNode.getChildren().indexOf(tabNode) : 0;

      // Save tabset context for restoration
      var tabsetWeight = tabsetNode ? tabsetNode.getWeight() : 50;
      var tabsetParent = tabsetNode ? tabsetNode.getParent() : null;
      var tabsetParentId = tabsetParent ? tabsetParent.getId() : null;
      var tabsetIdx = tabsetParent ? tabsetParent.getChildren().indexOf(tabsetNode) : 0;
      var parentDirection = tabsetParent ? tabsetParent.getDirection() : 'row';
      var siblingId = null;
      if (tabsetParent && tabsetParent.getChildren().length > 1) {
        var siblings = tabsetParent.getChildren();
        siblingId = siblings[tabsetIdx > 0 ? tabsetIdx - 1 : 1].getId();
      }

      // Find the widget DOM for this tab
      var widgetId = this._tabWidgetMap[tabId];
      var widgetEl = this._extractWidgetEl(tabId);

      // Detach from layout DOM BEFORE close (re-render would destroy it)
      if (widgetEl && widgetEl.parentNode) widgetEl.parentNode.removeChild(widgetEl);

      // Remove tab from model
      this._model.doAction({ type: 'CLOSE_TAB', tabId: tabId });

      var info = { widgetEl: widgetEl, widgetId: widgetId, tabsetId: tabsetId, tabIdx: tabIdx, title: title, mode: popMode,
                   siblingId: siblingId, tabsetWeight: tabsetWeight, tabsetIdx: tabsetIdx, parentDirection: parentDirection };

      if (popMode === 'window') {
        this._popOutToWindow(tabId, info, x, y, w, h);
      } else {
        this._popOutToFloat(tabId, info, x, y, w, h);
      }

      this._popOuts[tabId] = info;
      this.sendDataToServer({ type: 'popOut', tabId: tabId, tabsetId: tabsetId, tabIdx: tabIdx, mode: popMode, title: title, w: w || 500, h: h || 400 });
    },

    _extractWidgetEl: function (tabId) {
      var widgetId = this._tabWidgetMap[tabId];
      // Find host by tab ID (most reliable)
      var host = this._layoutContainer.querySelector('.fl-pony-widget-host[data-tab-id="' + CSS.escape(tabId) + '"]');
      if (host) return host;
      // Fallback by widget ID
      if (widgetId) {
        host = this._layoutContainer.querySelector('.fl-pony-widget-host[data-widget-id="' + CSS.escape(String(widgetId)) + '"]');
        if (host) return host;
      }
      return null;
    },

    _popOutToFloat: function (tabId, info, x, y, w, h) {
      var self = this;
      var win = document.createElement('div');
      win.className = 'fl-popout-window';
      win.dataset.tabId = tabId;
      win.style.cssText = 'position:fixed;z-index:10000;background:var(--fl-panel,#181825);border:1px solid var(--fl-border,#313244);border-radius:6px;box-shadow:0 8px 32px rgba(0,0,0,.5);display:flex;flex-direction:column;overflow:hidden;'
        + 'left:' + (x || 100) + 'px;top:' + (y || 100) + 'px;width:' + (w || 400) + 'px;height:' + (h || 300) + 'px;';

      var titleBar = document.createElement('div');
      titleBar.className = 'fl-popout-titlebar';
      titleBar.style.cssText = 'display:flex;align-items:center;padding:4px 8px;background:var(--fl-strip,#11111b);border-bottom:1px solid var(--fl-border,#313244);cursor:move;user-select:none;flex-shrink:0;';
      var titleSpan = document.createElement('span');
      titleSpan.style.cssText = 'flex:1;font-size:12px;color:var(--fl-text,#cdd6f4)';
      titleSpan.textContent = info.title || 'Tab';
      titleBar.appendChild(titleSpan);

      var popInBtn = document.createElement('button');
      popInBtn.type = 'button'; popInBtn.textContent = '⏎'; popInBtn.title = 'Pop back in';
      popInBtn.style.cssText = 'background:none;border:1px solid var(--fl-border,#313244);color:var(--fl-text,#cdd6f4);border-radius:3px;cursor:pointer;font-size:12px;padding:2px 6px;';
      popInBtn.addEventListener('click', function () { self.popIn(tabId); });
      titleBar.appendChild(popInBtn);
      win.appendChild(titleBar);

      var content = document.createElement('div');
      content.style.cssText = 'flex:1;overflow:auto;position:relative;';
      if (info.widgetEl) content.appendChild(info.widgetEl);
      win.appendChild(content);

      this._makeDraggable(win, titleBar);
      this._makeResizable(win);
      document.body.appendChild(win);
      info.win = win;
    },

    _popOutToWindow: function (tabId, info, x, y, w, h) {
      var self = this;
      var popup = window.open('about:blank', 'fl_popout_' + tabId,
        'popup=yes,width=' + (w || 500) + ',height=' + (h || 400) + ',left=' + (x || 100) + ',top=' + (y || 100)
        + ',menubar=no,toolbar=no,location=no,status=no,resizable=yes,scrollbars=yes');
      if (!popup) { this._popOutToFloat(tabId, info, x, y, w, h); return; }

      var styles = document.querySelectorAll('link[rel="stylesheet"], style');
      popup.document.title = info.title || 'Tab';
      styles.forEach(function (s) { popup.document.head.appendChild(s.cloneNode(true)); });
      popup.document.body.style.cssText = 'margin:0;padding:0;background:#181825;overflow:hidden;width:100%;height:100%;display:flex;flex-direction:column;font-family:system-ui,sans-serif;';

      var toolbar = popup.document.createElement('div');
      toolbar.style.cssText = 'display:flex;align-items:center;padding:4px 8px;background:#11111b;border-bottom:1px solid #313244;flex-shrink:0;';
      var titleSpan2 = popup.document.createElement('span');
      titleSpan2.style.cssText = 'flex:1;font-size:12px;color:#cdd6f4';
      titleSpan2.textContent = info.title || 'Tab';
      toolbar.appendChild(titleSpan2);
      var btn = popup.document.createElement('button');
      btn.textContent = '\u23CE Pop back in';
      btn.style.cssText = 'background:#313244;border:1px solid #89b4fa;color:#89b4fa;border-radius:4px;padding:3px 10px;cursor:pointer;font-size:12px;';
      btn.addEventListener('click', function () { self.popIn(tabId); });
      toolbar.appendChild(btn);
      popup.document.body.appendChild(toolbar);

      var content = popup.document.createElement('div');
      content.style.cssText = 'flex:1;overflow:auto;';
      if (info.widgetEl) content.appendChild(info.widgetEl);
      popup.document.body.appendChild(content);

      popup.addEventListener('beforeunload', function () {
        try { if (popup.location.origin !== window.location.origin) return; } catch(e) { return; }
        if (self._popOuts && self._popOuts[tabId]) {
          if (info.widgetEl && info.widgetEl.parentNode) self.element.appendChild(info.widgetEl);
          self._doPopIn(tabId);
        }
      });
      info.popup = popup;
    },

    popIn: function (tabId) {
      if (!this._popOuts || !this._popOuts[tabId]) return;
      var info = this._popOuts[tabId];

      if (info.mode === 'window' && info.popup) {
        if (info.widgetEl && info.widgetEl.parentNode) this.element.appendChild(info.widgetEl);
        delete this._popOuts[tabId];
        this._restoreTab(tabId, info);
        // Close popup AFTER restoring (clicking pop-in from inside the popup would kill execution)
        if (!info.popup.closed) info.popup.close();
      } else if (info.win) {
        // Float mode
        var content = info.win.querySelector('div:last-child');
        if (content && content.firstChild) {
          info.widgetEl = content.firstChild;
          this.element.appendChild(info.widgetEl);
        }
        info.win.remove();
        delete this._popOuts[tabId];
        this._restoreTab(tabId, info);
      }
    },

    _doPopIn: function (tabId) {
      if (!this._popOuts || !this._popOuts[tabId]) return;
      var info = this._popOuts[tabId];
      delete this._popOuts[tabId];
      this._restoreTab(tabId, info);
    },

    _restoreTab: function (tabId, info) {
      var targetTabset = info.tabsetId;
      var tabDef = { id: tabId, name: info.title || 'Tab', component: 'pwidget', config: { widgetId: info.widgetId } };

      // If popped out from a border, restore there
      if (info.borderSide) {
        var border = this._model.getBorder(info.borderSide);
        if (border) {
          this._model.doAction({ type: 'ADD_BORDER_TAB', side: info.borderSide, tab: tabDef, index: info.tabIdx, select: true });
          this._tabWidgetMap[tabId] = info.widgetId;
          this.sendDataToServer({ type: 'popIn', tabId: tabId });
          return;
        }
      }

      if (targetTabset && this._model.getRoot().findById(targetTabset)) {
        // Original tabset still exists — just add back
        this._model.doAction({ type: 'ADD_TAB', tabsetId: targetTabset, tab: tabDef, index: info.tabIdx, select: true });
      } else if (info.siblingId && this._model.getRoot().findById(info.siblingId)) {
        // Original tabset gone but sibling exists — split to recreate at correct position
        // Direction: row parent → left/right split, column parent → top/bottom split
        var location;
        if (info.parentDirection === 'column') {
          location = info.tabsetIdx === 0 ? 'top' : 'bottom';
        } else {
          location = info.tabsetIdx === 0 ? 'left' : 'right';
        }
        this._model.doAction({ type: 'ADD_TAB_SPLIT', tab: tabDef, toId: info.siblingId, location: location });
      } else {
        // Fallback: add to first available tabset
        this._model.doAction({ type: 'ADD_TAB', tabsetId: null, tab: tabDef, select: true });
      }
      this._tabWidgetMap[tabId] = info.widgetId;
      this.sendDataToServer({ type: 'popIn', tabId: tabId });
    },

    getPopOutState: function () {
      if (!this._popOuts) { this.sendDataToServer({ type: 'popOutState', state: '{}' }); return; }
      var state = {};
      for (var tabId in this._popOuts) {
        var info = this._popOuts[tabId];
        if (info.mode === 'window') {
          state[tabId] = { tabsetId: info.tabsetId, tabIdx: info.tabIdx, title: info.title, widgetId: info.widgetId, mode: 'window' };
        } else if (info.win) {
          var r = info.win.getBoundingClientRect();
          state[tabId] = { tabsetId: info.tabsetId, tabIdx: info.tabIdx, title: info.title, widgetId: info.widgetId, mode: 'float',
            x: Math.round(r.left), y: Math.round(r.top), w: Math.round(r.width), h: Math.round(r.height) };
        }
      }
      this.sendDataToServer({ type: 'popOutState', state: JSON.stringify(state) });
    },

    _getTabContentRect: function (tsNode) {
      var el = this._layoutContainer.querySelector('[data-fl-tabset="' + tsNode.getId() + '"] .fl-content');
      if (el) {
        var r = el.getBoundingClientRect();
        return { x: Math.round(r.left + window.screenX), y: Math.round(r.top + window.screenY), w: Math.round(r.width), h: Math.round(r.height) };
      }
      return { x: 150, y: 150, w: 500, h: 350 };
    },

    _makeDraggable: function (win, handle) {
      var ox, oy, sx, sy;
      handle.addEventListener('pointerdown', function (e) {
        if (e.target.tagName === 'BUTTON') return;
        e.preventDefault();
        ox = e.clientX; oy = e.clientY;
        sx = win.offsetLeft; sy = win.offsetTop;
        handle.setPointerCapture(e.pointerId);
        var move = function (ev) { win.style.left = (sx + ev.clientX - ox) + 'px'; win.style.top = (sy + ev.clientY - oy) + 'px'; };
        var up = function () { handle.removeEventListener('pointermove', move); handle.removeEventListener('pointerup', up); };
        handle.addEventListener('pointermove', move);
        handle.addEventListener('pointerup', up);
      });
    },

    _makeResizable: function (win) {
      var grip = document.createElement('div');
      grip.style.cssText = 'position:absolute;bottom:0;right:0;width:12px;height:12px;cursor:nwse-resize;';
      win.appendChild(grip);
      grip.addEventListener('pointerdown', function (e) {
        e.preventDefault(); e.stopPropagation();
        var ox = e.clientX, oy = e.clientY, ow = win.offsetWidth, oh = win.offsetHeight;
        grip.setPointerCapture(e.pointerId);
        var move = function (ev) { win.style.width = Math.max(200, ow + ev.clientX - ox) + 'px'; win.style.height = Math.max(100, oh + ev.clientY - oy) + 'px'; };
        var up = function () { grip.removeEventListener('pointermove', move); grip.removeEventListener('pointerup', up); };
        grip.addEventListener('pointermove', move);
        grip.addEventListener('pointerup', up);
      });
    },

    setBorderMinSize: function (side, minSize) {
      if (!this._layout) return;
      this._layout.setBorderMinSize(side, minSize);
    },

    setBorderMaxSize: function (side, maxSize) {
      if (!this._layout) return;
      this._layout.setBorderMaxSize(side, maxSize);
    },

    setBadge: function (tabId, badge, color) {
      if (!this._layout) return;
      this._layout.setBadge(tabId, badge, color);
    },

    enableUndoRedo: function (enabled) {
      if (!this._layout) return;
      this._layout.setUndoEnabled(!!enabled);
    },

    enableKeyboardShortcuts: function (enabled) {
      if (!this._layout) return;
      this._layout.setKeyboardEnabled(!!enabled);
    },

    setKeymap: function (keymapJson) {
      if (!this._layout) return;
      this._layout.setKeymap(parseJson(keymapJson));
    },

    enableTouchGestures: function (enabled) {
      if (!this._layout) return;
      this._layout.setTouchEnabled(!!enabled);
    },

    enableContextMenu: function (enabled) {
      if (!this._layout) return;
      this._layout.setContextMenuEnabled(!!enabled);
    },

    undo: function () {
      if (!this._layout) return;
      this._layout.undo();
    },

    redo: function () {
      if (!this._layout) return;
      this._layout.redo();
    },

    _rehydrateAfterUndoRedo: function () {
      // After undo/redo, check for tabs that need widget recreation
      var tabs = [];
      this._collectRehydrateTabs(this._model.getRoot(), tabs);
      var borders = this._model.getBorders ? this._model.getBorders() : [];
      for (var b = 0; b < borders.length; b++) {
        var border = borders[b];
        for (var c = 0; c < border.children.length; c++) {
          var child = border.children[c];
          var comp = child.getComponent ? child.getComponent() : null;
          if (comp && comp !== '' && comp !== 'pwidget') {
            var cfg = child.getConfig ? child.getConfig() : null;
            tabs.push({ tabId: child.getId(), component: comp, tabName: child.getName(), config: cfg ? JSON.stringify(cfg) : null });
          }
        }
      }
      // Only rehydrate tabs that don't already have a widget
      var missing = [];
      for (var i = 0; i < tabs.length; i++) {
        if (!this._tabWidgetMap[tabs[i].tabId]) missing.push(tabs[i]);
      }
      if (missing.length > 0) this.sendDataToServer({ type: 'rehydrate', tabs: JSON.stringify(missing) });
    },

    reorderBorderTab: function (side, tabId, newIndex) {
      if (!this._model) return;
      this._model.doAction({ type: 'REORDER_BORDER_TAB', side: side, tabId: tabId, toIndex: newIndex });
    },

    maximizeBorder: function (side) {
      if (!this._layout) return;
      var w = this._layoutContainer.offsetWidth;
      var maxSize = w > 0 ? Math.round(w * 0.5) : 400;
      this._layout._act({ type: 'MAXIMIZE_BORDER', side: side, maxSize: maxSize });
    },

    // ─── Feature 1: Model Migration API ─────────────────────────
    loadModelWithMigration: function (modelJsonInput, migrateFn) {
      if (!this._layout) return;
      this._layout.destroy();
      this._tabWidgetMap = {};
      this._popOuts = {};
      this._model = FlexLayout.Model.fromJson(parseJson(modelJsonInput), migrateFn);
      this._layout = this._createLayout();
      var tabs = [];
      this._collectRehydrateTabs(this._model.getRoot(), tabs);
      var borders = this._model.getBorders ? this._model.getBorders() : [];
      for (var b = 0; b < borders.length; b++) {
        var border = borders[b];
        for (var c = 0; c < border.children.length; c++) {
          var child = border.children[c];
          var comp = child.getComponent ? child.getComponent() : null;
          if (comp && comp !== '' && comp !== 'pwidget') {
            var cfg = child.getConfig ? child.getConfig() : null;
            tabs.push({ tabId: child.getId(), component: comp, tabName: child.getName(), config: cfg ? JSON.stringify(cfg) : null });
          }
        }
      }
      if (tabs.length > 0) this.sendDataToServer({ type: 'rehydrate', tabs: JSON.stringify(tabs) });
    },

    // ─── Feature 3: Sidebar Pop-out ─────────────────────────────
    popOutBorder: function (side) {
      if (!this._model) return;
      var border = this._model.getBorder(side);
      if (!border || !border.isOpen()) return;
      var selTab = border.getSelectedNode();
      if (!selTab) return;
      var tabId = selTab.getId();
      var self = this;
      if (!this._popOuts) this._popOuts = {};

      var widgetId = this._tabWidgetMap[tabId];
      var widgetEl = this._extractWidgetEl(tabId);
      if (widgetEl && widgetEl.parentNode) widgetEl.parentNode.removeChild(widgetEl);

      var tabIdx = border.getChildren().indexOf(selTab);
      this._model.doAction({ type: 'CLOSE_BORDER_TAB', side: side, tabId: tabId });

      var rect = this._layoutContainer.getBoundingClientRect();
      var info = { widgetEl: widgetEl, widgetId: widgetId, tabsetId: null, tabIdx: tabIdx, title: selTab.getName(), mode: 'float',
                   siblingId: null, tabsetWeight: 50, tabsetIdx: 0, parentDirection: 'row', borderSide: side };
      this._popOutToFloat(tabId, info, Math.round(rect.left + 50), Math.round(rect.top + 50), border.size || 400, 300);
      this._popOuts[tabId] = info;
      this.sendDataToServer({ type: 'popOut', tabId: tabId, tabsetId: null, tabIdx: tabIdx, mode: 'float', title: selTab.getName(), w: border.size || 400, h: 300 });
    },

    // ─── Feature 4: Collaboration API ───────────────────────────
    applyRemoteAction: function (actionJson) {
      if (!this._layout || !this._model) return;
      var action = parseJson(actionJson);
      this._layout.applyRemote(action);
    },

    // ─── Feature 7: Command Palette ─────────────────────────────
    showCommandPalette: function () {
      if (!this._layout) return;
      this._layout._showCommandPalette(this._commandPaletteItems || null);
    },

    setCommandPaletteItems: function (items) {
      this._commandPaletteItems = parseJson(items);
    },

    // ─── Feature 9: Tab Groups ──────────────────────────────────
    setTabGroup: function (tabId, groupName, color) {
      if (!this._model) return;
      this._model.doAction({ type: 'SET_TAB_GROUP', tabId: tabId, group: groupName, groupColor: color });
    },

    // ─── New Features: maxTabs, locked, tabConfig ────────────────
    setMaxTabs: function (tabsetId, max) {
      if (!this._layout) return;
      this._layout.setMaxChildren(tabsetId, max > 0 ? max : null);
    },

    setLocked: function (locked) {
      if (!this._layout) return;
      this._layout.setLocked(!!locked);
    },

    setTabConfig: function (tabId, configJson) {
      if (!this._model) return;
      var tab = this._model.findById(tabId);
      if (tab) tab.config = parseJson(configJson);
    },

    getTabConfig: function (tabId) {
      if (!this._model) return;
      var tab = this._model.findById(tabId);
      var cfg = tab ? JSON.stringify(tab.config || {}) : '{}';
      this.sendDataToServer({ type: 'tabConfig', tabId: tabId, config: cfg });
    },

    // ─── Feature 10: Status Bar ─────────────────────────────────
    setStatusBar: function (widgetId) {
      if (!this._statusBar) return;
      var self = this;
      this._statusBar.innerHTML = '';
      setTimeout(function () { self._moveWidgetToHost(widgetId, self._statusBar); }, 50);
    },

    // ─── Feature 11: Notification/Toast ─────────────────────────
    showNotification: function (message, type, duration) {
      var self = this;
      var toast = document.createElement('div');
      toast.className = 'fl-toast fl-toast-' + (type || 'info');
      toast.textContent = message;
      if (!this._toastContainer) {
        this._toastContainer = document.createElement('div');
        this._toastContainer.className = 'fl-toast-container';
        this.element.appendChild(this._toastContainer);
      }
      this._toastContainer.appendChild(toast);
      var dur = duration || 3000;
      var t1 = setTimeout(function () { toast.classList.add('fl-toast-hide'); var t2 = setTimeout(function () { toast.remove(); }, 300); self._pendingTimeouts.push(t2); }, dur);
      this._pendingTimeouts.push(t1);
    },

    // ─── Hook 1: Blocked actions/tabs ─────────────────────────────
    setBlockedActions: function (actionsJson) {
      this._blockedActions = parseJson(actionsJson) || [];
    },

    setBlockedTabs: function (tabIdsJson) {
      this._blockedTabs = parseJson(tabIdsJson) || [];
    },

    // ─── Hook 2: getOpenTabs / getLayoutSummary ─────────────────
    getOpenTabs: function () {
      if (!this._layout) return;
      this.sendDataToServer({ type: 'openTabs', data: JSON.stringify(this._layout.getOpenTabs()) });
    },

    getLayoutSummary: function () {
      if (!this._layout) return;
      this.sendDataToServer({ type: 'layoutSummary', data: JSON.stringify(this._layout.getLayoutSummary()) });
    },

    destroy: function () {
      if (this._pendingTimeouts) { this._pendingTimeouts.forEach(clearTimeout); this._pendingTimeouts = []; }
      if (this._layout) { this._layout.destroy(); this._layout = null; }
      // Close all popout windows
      if (this._popOuts) {
        for (var id in this._popOuts) {
          var info = this._popOuts[id];
          if (info.win) info.win.remove();
          if (info.popup && !info.popup.closed) info.popup.close();
        }
        this._popOuts = {};
      }
      this._debouncedModelChange = function () {};
      this._debouncedAutoSave = function () {};
      this._model = null;
      this._tabWidgetMap = {};
    }
  });
})();
