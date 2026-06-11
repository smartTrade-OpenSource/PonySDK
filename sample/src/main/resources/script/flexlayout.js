/**
 * FlexLayout — Pure Vanilla JS/CSS dockable layout manager
 * Inspired by https://github.com/caplin/FlexLayout
 */
(function (global, document) {
  'use strict';

  const NT = Object.freeze({ ROW: 'row', TABSET: 'tabset', TAB: 'tab', BORDER: 'border' });
  const DL = Object.freeze({ TOP: 'top', LEFT: 'left', RIGHT: 'right', BOTTOM: 'bottom', CENTER: 'center' });
  const BORDER_SIDES = ['left', 'right', 'bottom'];
  const EDGE = 0.22;

  let _uid = 0;
  function genId() { return 'fl_' + (++_uid).toString(36); }
  function clamp(v, lo, hi) { return Math.max(lo, Math.min(hi, v)); }

  // ── EventEmitter ─────────────────────────────────────────────────
  class Emitter {
    constructor() { this._h = {}; }
    on(e, fn)    { (this._h[e] = this._h[e] || []).push(fn); return this; }
    off(e, fn)   { this._h[e] = (this._h[e] || []).filter(h => h !== fn); }
    emit(e, ...a){ (this._h[e] || []).slice().forEach(h => h(...a)); }
  }

  // ── Nodes ─────────────────────────────────────────────────────────
  class Node extends Emitter {
    constructor(type, id) {
      super();
      this.type = type; this.id = id || genId();
      this.parent = null; this.children = []; this._weight = 100;
    }
    getType()     { return this.type; }
    getId()       { return this.id; }
    getParent()   { return this.parent; }
    getChildren() { return this.children; }
    getWeight()   { return this._weight; }
    setWeight(w)  { this._weight = Math.max(0.1, w); }
    addChild(child, idx) {
      child.parent = this;
      if (idx == null || idx >= this.children.length) this.children.push(child);
      else this.children.splice(Math.max(0, idx), 0, child);
    }
    removeChild(child) {
      const i = this.children.indexOf(child);
      if (i < 0) return -1;
      this.children.splice(i, 1); child.parent = null; return i;
    }
    findById(id) {
      if (this.id === id) return this;
      for (const c of this.children) { const f = c.findById(id); if (f) return f; }
      return null;
    }
  }

  class TabNode extends Node {
    constructor(cfg) {
      super(NT.TAB, cfg.id);
      this.name        = cfg.name        != null ? cfg.name        : 'Tab';
      this.component   = cfg.component   != null ? cfg.component   : '';
      this.config      = cfg.config      != null ? cfg.config      : {};
      this.icon        = cfg.icon        || null;
      this.enableClose = cfg.enableClose !== false;
      this.enableDrag  = cfg.enableDrag  !== false;
      this._weight     = cfg.weight || 100;
    }
    getName()       { return this.name; }
    setName(n)      { this.name = n; }
    getComponent()  { return this.component; }
    getConfig()     { return this.config; }
    getIcon()       { return this.icon; }
    isEnableClose() { return this.enableClose; }
    isEnableDrag()  { return this.enableDrag; }
    toJson() {
      return { type: NT.TAB, id: this.id, name: this.name, component: this.component,
               config: this.config, icon: this.icon, enableClose: this.enableClose,
               enableDrag: this.enableDrag, weight: this._weight };
    }
  }

  class TabSetNode extends Node {
    constructor(cfg) {
      super(NT.TABSET, cfg.id);
      this._selected      = cfg.selected != null ? cfg.selected : 0;
      this._weight        = cfg.weight   != null ? cfg.weight   : 100;
      this._maximized     = false;
      this.name           = cfg.name           || '';
      this.enableMaximize = cfg.enableMaximize !== false;
      this.enableDrop     = cfg.enableDrop     !== false;
    }
    getSelected()     { return this._selected; }
    setSelected(i)    { const n = this.children.length; this._selected = n === 0 ? 0 : clamp(i, 0, n - 1); }
    getSelectedNode() { return this.children[this._selected] || null; }
    isMaximized()     { return this._maximized; }
    toJson() {
      return { type: NT.TABSET, id: this.id, weight: this._weight, selected: this._selected,
               name: this.name, enableMaximize: this.enableMaximize,
               children: this.children.map(c => c.toJson()) };
    }
  }

  class RowNode extends Node {
    constructor(cfg) {
      super(NT.ROW, cfg.id);
      this._weight   = cfg.weight    != null ? cfg.weight    : 100;
      this.direction = cfg.direction || 'row';
    }
    getDirection() { return this.direction; }
    toJson() {
      return { type: NT.ROW, id: this.id, weight: this._weight, direction: this.direction,
               children: this.children.map(c => c.toJson()) };
    }
  }

  // ── BorderNode ──────────────────────────────────────────────────────
  class BorderNode extends Node {
    constructor(cfg) {
      super(NT.BORDER, cfg.id);
      this.side = cfg.side || 'left';
      this.size = cfg.size != null ? cfg.size : 200;
      this._selected = cfg.selected != null ? cfg.selected : -1;
      this.tabStyle = cfg.tabStyle || 'auto';
      this._hidden = cfg.hidden || false;
    }
    getSelected()     { return this._selected; }
    setSelected(i)    { this._selected = i; }
    getSelectedNode() { return this._selected >= 0 ? (this.children[this._selected] || null) : null; }
    isOpen()          { return this._selected >= 0 && this.children.length > 0; }
    toJson() {
      const j = { type: NT.BORDER, id: this.id, side: this.side, size: this.size,
               selected: this._selected, tabStyle: this.tabStyle,
               children: this.children.map(c => c.toJson()) };
      if (this._hidden) j.hidden = true;
      return j;
    }
  }

  // ── Model ─────────────────────────────────────────────────────────
  class Model extends Emitter {
    constructor() { super(); this._root = null; this._maximized = null; this._borders = []; }

    static fromJson(json) {
      const m = new Model();
      m._root = Model._parse(json.layout || json, 'row');
      if (json.borders) {
        m._borders = json.borders.map(b => {
          const bn = new BorderNode(b);
          (b.children || []).forEach(c => bn.addChild(new TabNode(c)));
          return bn;
        });
      }
      return m;
    }
    static _parse(cfg, forcedDir) {
      const type = cfg.type || NT.ROW;
      if (type === NT.TAB) return new TabNode(cfg);
      if (type === NT.TABSET) {
        const n = new TabSetNode(cfg);
        (cfg.children || []).forEach(c => n.addChild(Model._parse(c, null)));
        n.setSelected(n._selected); return n;
      }
      const dir  = cfg.direction || forcedDir || 'row';
      const node = new RowNode({ ...cfg, direction: dir });
      const childDir = dir === 'row' ? 'column' : 'row';
      (cfg.children || []).forEach(c => node.addChild(Model._parse(c, c.type === NT.ROW ? childDir : null)));
      return node;
    }

    getRoot()      { return this._root; }
    getBorders()   { return this._borders; }
    getBorder(side){ return this._borders.find(b => b.side === side) || null; }
    getMaximized() { return this._maximized; }
    doAction(action) { this._apply(action); this.emit('change', this, action); }

    // Find by id across root AND borders
    findById(id) {
      const r = this._root.findById(id);
      if (r) return r;
      for (const b of this._borders) { const f = b.findById(id); if (f) return f; }
      return null;
    }

    _apply(action) {
      switch (action.type) {
        case 'SELECT_TAB': {
          const ts = this._root.findById(action.tabsetId); if (!ts) return;
          const i = ts.children.findIndex(t => t.id === action.tabId);
          if (i >= 0) ts.setSelected(i); break;
        }
        case 'CLOSE_TAB': {
          const tab = this.findById(action.tabId); if (!tab) return;
          const ts = tab.getParent(); if (!ts) return;
          const i = ts.removeChild(tab);
          if (ts.type === NT.TABSET) ts.setSelected(Math.min(i, ts.children.length - 1));
          if (ts.type === NT.BORDER) {
            if (ts.getSelected() >= ts.children.length) ts.setSelected(ts.children.length - 1);
          }
          this._cleanup(this._root); break;
        }
        case 'MAXIMIZE_TOGGLE': {
          const ts = this._root.findById(action.tabsetId); if (!ts) return;
          if (this._maximized === ts) { ts._maximized = false; this._maximized = null; }
          else { if (this._maximized) this._maximized._maximized = false; ts._maximized = true; this._maximized = ts; }
          break;
        }
        case 'MOVE_TAB': {
          const { tabId, toId, location } = action;
          const tab = this.findById(tabId); if (!tab) return;
          const from = tab.getParent(); const oldI = from.removeChild(tab);
          if (from.type === NT.TABSET) from.setSelected(Math.min(oldI, from.children.length - 1));
          if (from.type === NT.BORDER) {
            if (from.getSelected() >= from.children.length) from.setSelected(from.children.length - 1);
          }
          const dest = this.findById(toId);
          if (!dest) { from.addChild(tab, oldI); return; }
          if (dest.type === NT.BORDER) {
            // Dropping into a border
            let at = action.insertIndex != null ? action.insertIndex : dest.children.length;
            dest.addChild(tab, at); dest.setSelected(dest.children.indexOf(tab));
          } else if (location === DL.CENTER && dest.type === NT.TABSET) {
            let at = action.insertIndex != null ? action.insertIndex : dest.children.length;
            // insertIndex was computed including the dragged tab; if it sat before
            // the target slot in the SAME tabset, removing it shifts everything left by one
            if (dest === from && oldI < at) at -= 1;
            at = Math.max(0, Math.min(at, dest.children.length));
            dest.addChild(tab, at); dest.setSelected(dest.children.indexOf(tab));
          } else { this._split(tab, dest, location); }
          this._cleanup(this._root); break;
        }
        case 'REORDER_TAB': {
          const { tabId, toTabId } = action;
          const tab = this._root.findById(tabId);   if (!tab) return;
          const to  = this._root.findById(toTabId); if (!to)  return;
          const ts  = tab.getParent();              if (ts !== to.getParent()) return;
          ts.removeChild(tab); ts.addChild(tab, ts.children.indexOf(to));
          ts.setSelected(ts.children.indexOf(tab)); break;
        }
        case 'ADD_TAB': {
          const ts = action.tabsetId ? this._root.findById(action.tabsetId) : this._firstTabSet(this._root);
          if (!ts) return;
          const tab = new TabNode(action.tab);
          const idx = action.index != null ? action.index : ts.children.length;
          ts.addChild(tab, idx);
          if (action.select !== false) ts.setSelected(ts.children.indexOf(tab)); break;
        }
        case 'ADD_TAB_SPLIT': {
          // Create new tabset with a new tab, then split it into target
          const dest = this._root.findById(action.toId); if (!dest) return;
          const tab  = new TabNode(action.tab);
          this._splitWithTab(tab, dest, action.location);
          this._cleanup(this._root); break;
        }
        case 'RENAME_TAB': {
          const tab = this._root.findById(action.tabId); if (!tab) return;
          tab.setName(action.name); break;
        }
        case 'SELECT_BORDER_TAB': {
          const border = this.getBorder(action.side); if (!border) return;
          const i = border.children.findIndex(t => t.id === action.tabId);
          // Toggle: clicking already-selected tab closes the panel
          border.setSelected(i >= 0 && border.getSelected() === i ? -1 : i);
          break;
        }
        case 'CLOSE_BORDER_TAB': {
          const border = this.getBorder(action.side); if (!border) return;
          const tab = border.findById(action.tabId); if (!tab) return;
          const i = border.removeChild(tab);
          if (border.getSelected() >= border.children.length)
            border.setSelected(border.children.length - 1);
          break;
        }
        case 'ADD_BORDER_TAB': {
          const border = this.getBorder(action.side); if (!border) return;
          const tab = new TabNode(action.tab);
          const idx = action.index != null ? action.index : border.children.length;
          border.addChild(tab, idx);
          if (action.select !== false) border.setSelected(border.children.indexOf(tab));
          break;
        }
        case 'MOVE_TO_BORDER': {
          // Move a tab from layout into a border
          const tab = this.findById(action.tabId); if (!tab) return;
          const from = tab.getParent(); if (!from) return;
          const oldI = from.removeChild(tab);
          if (from.type === NT.TABSET) from.setSelected(Math.min(oldI, from.children.length - 1));
          if (from.type === NT.BORDER) {
            if (from.getSelected() >= from.children.length) from.setSelected(from.children.length - 1);
          }
          const border = this.getBorder(action.side); if (!border) { from.addChild(tab, oldI); return; }
          const idx = action.index != null ? action.index : border.children.length;
          border.addChild(tab, idx);
          border.setSelected(border.children.indexOf(tab));
          this._cleanup(this._root);
          break;
        }
        case 'MOVE_FROM_BORDER': {
          // Move a tab from a border into the layout
          const border = this.getBorder(action.side); if (!border) return;
          const tab = border.findById(action.tabId); if (!tab) return;
          border.removeChild(tab);
          if (border.getSelected() >= border.children.length) border.setSelected(border.children.length - 1);
          const dest = this._root.findById(action.toId);
          if (dest && action.location === DL.CENTER) {
            const at = action.insertIndex != null ? action.insertIndex : dest.children.length;
            dest.addChild(tab, at); dest.setSelected(dest.children.indexOf(tab));
          } else if (dest) {
            this._split(tab, dest, action.location);
          } else {
            const ts = this._firstTabSet(this._root);
            if (ts) { ts.addChild(tab); ts.setSelected(ts.children.indexOf(tab)); }
          }
          this._cleanup(this._root);
          break;
        }
        case 'RESIZE_BORDER': {
          const border = this.getBorder(action.side); if (!border) return;
          border.size = Math.max(50, action.size);
          break;
        }
        case 'TOGGLE_BORDER': {
          const border = this.getBorder(action.side);
          if (border) {
            border._hidden = !border._hidden;
          } else {
            const size = action.size || (action.side === 'bottom' ? 180 : 220);
            const bn = new BorderNode({ side: action.side, size, selected: -1 });
            this._borders.push(bn);
          }
          break;
        }
      }
    }

    _split(tab, target, location) {
      const parent = target.getParent(); if (!parent) return;
      const isH    = location === DL.LEFT || location === DL.RIGHT;
      const before = location === DL.LEFT || location === DL.TOP;
      const w = target.getWeight(); const half = w / 2;
      const newTs = new TabSetNode({ weight: half, enableMaximize: true, enableDrop: true });
      newTs.addChild(tab); newTs.setSelected(0); target.setWeight(half);
      const sameDir = (isH && parent.direction === 'row') || (!isH && parent.direction === 'column');
      if (sameDir) {
        const idx = parent.children.indexOf(target);
        parent.addChild(newTs, before ? idx : idx + 1);
      } else {
        const idx = parent.children.indexOf(target);
        parent.removeChild(target);
        const wrap = new RowNode({ direction: isH ? 'row' : 'column', weight: w });
        if (before) { wrap.addChild(newTs); wrap.addChild(target); }
        else        { wrap.addChild(target); wrap.addChild(newTs); }
        parent.addChild(wrap, idx);
      }
    }

    _splitWithTab(tab, target, location) {
      const parent = target.getParent(); if (!parent) return;
      const isH    = location === DL.LEFT || location === DL.RIGHT;
      const before = location === DL.LEFT || location === DL.TOP;
      const w = target.getWeight(); const half = w / 2;
      const newTs = new TabSetNode({ weight: half, enableMaximize: true, enableDrop: true });
      newTs.addChild(tab); newTs.setSelected(0); target.setWeight(half);
      const sameDir = (isH && parent.direction === 'row') || (!isH && parent.direction === 'column');
      if (sameDir) {
        const idx = parent.children.indexOf(target);
        parent.addChild(newTs, before ? idx : idx + 1);
      } else {
        const idx = parent.children.indexOf(target);
        parent.removeChild(target);
        const wrap = new RowNode({ direction: isH ? 'row' : 'column', weight: w });
        if (before) { wrap.addChild(newTs); wrap.addChild(target); }
        else        { wrap.addChild(target); wrap.addChild(newTs); }
        parent.addChild(wrap, idx);
      }
    }

    _cleanup(node) {
      if (!node || node.type !== NT.ROW) return;
      [...node.children].forEach(c => this._cleanup(c));
      [...node.children].filter(c => c.type === NT.TABSET && c.children.length === 0)
        .forEach(c => node.removeChild(c));
      if (node.children.length === 1 && node.parent) {
        const [child] = node.children; const par = node.parent;
        const idx = par.children.indexOf(node);
        child.setWeight(node.getWeight()); par.removeChild(node); par.addChild(child, idx);
        if (par.type === NT.ROW) this._cleanup(par); return;
      }
      if (node.children.length === 0 && node.parent) node.parent.removeChild(node);
    }

    _firstTabSet(node) {
      if (node.type === NT.TABSET) return node;
      for (const c of node.children) { const f = this._firstTabSet(c); if (f) return f; }
      return null;
    }

    toJson() {
      const json = { layout: this._root ? this._root.toJson() : null };
      if (this._borders.length > 0) json.borders = this._borders.map(b => b.toJson());
      return json;
    }
  }

  // ── Actions ───────────────────────────────────────────────────────
  const Actions = {
    selectTab:     (tabsetId, tabId)              => ({ type: 'SELECT_TAB',     tabsetId, tabId }),
    closeTab:      (tabId)                         => ({ type: 'CLOSE_TAB',      tabId }),
    moveTab:       (tabId, toId, location, insertIndex) => ({ type: 'MOVE_TAB', tabId, toId, location, insertIndex }),
    reorderTab:    (tabId, toTabId)               => ({ type: 'REORDER_TAB',    tabId, toTabId }),
    maximizeToggle:(tabsetId)                      => ({ type: 'MAXIMIZE_TOGGLE',tabsetId }),
    addTab:        (tabsetId, tab, index, select) => ({ type: 'ADD_TAB',        tabsetId, tab, index, select }),
    addTabSplit:   (tab, toId, location)          => ({ type: 'ADD_TAB_SPLIT',  tab, toId, location }),
    renameTab:     (tabId, name)                  => ({ type: 'RENAME_TAB',     tabId, name }),
    selectBorderTab: (side, tabId)                => ({ type: 'SELECT_BORDER_TAB', side, tabId }),
    closeBorderTab:  (side, tabId)                => ({ type: 'CLOSE_BORDER_TAB', side, tabId }),
    addBorderTab:    (side, tab, index, select)   => ({ type: 'ADD_BORDER_TAB', side, tab, index, select }),
    moveToBorder:    (tabId, side, index)         => ({ type: 'MOVE_TO_BORDER', tabId, side, index }),
    moveFromBorder:  (side, tabId, toId, location, insertIndex) => ({ type: 'MOVE_FROM_BORDER', side, tabId, toId, location, insertIndex }),
    resizeBorder:    (side, size)                 => ({ type: 'RESIZE_BORDER', side, size }),
    toggleBorder:    (side)                       => ({ type: 'TOGGLE_BORDER', side }),
  };

  // ── Layout ────────────────────────────────────────────────────────
  class Layout {
    constructor(opts) {
      this.model         = opts.model;
      this.container     = opts.container;
      this.factory       = opts.factory        || (() => null);
      this.tabSetButtons = opts.tabSetButtons  || null;   // fn(tabSetNode) → HTMLElement[]
      this.onAction      = opts.onAction       || null;
      this.onModelChange = opts.onModelChange  || null;
      this.onTabClose    = opts.onTabClose     || null;

      this._nodeEls        = new Map();
      this._contentEls     = new Map();
      this._activeTabsetId = null;
      this._drag           = null;
      this._resize         = null;
      this._ghost          = null;
      this._dropInd        = this._mkDropInd();

      // Bound pointer handlers — attached to the captured element during a gesture
      this._pmDrag = this._onDragMove.bind(this);
      this._puDrag = this._onDragUp.bind(this);
      this._pmRes  = this._onResMove.bind(this);
      this._puRes  = this._onResUp.bind(this);

      this.model.on('change', (m, action) => {
        if (action && action.type === 'SELECT_BORDER_TAB') {
          this._updateBorderSelection(action.side);
        } else {
          this._render();
        }
      });
      this.container.classList.add('fl-layout');
      this._render();
    }

    // ── Public API ───────────────────────────────────────────────────
    addTab(tabsetId, def, index, select) { this._act(Actions.addTab(tabsetId, def, index, select)); }
    addTabToActiveTabSet(def)            { this._act(Actions.addTab(this._getActiveId(), def)); }
    renameTab(tabId, name)               { this._act(Actions.renameTab(tabId, name)); }
    getActiveTabSetId()                  { return this._getActiveId(); }
    toJson()                             { return this.model.toJson(); }

    // Start a drag for a brand-new tab from any external element (e.g. a toolbar button).
    // `ev` must be a pointerdown event; `sourceEl` is the element that captures the pointer.
    startExternalDrag(ev, tabDef, sourceEl) {
      sourceEl = sourceEl || ev.currentTarget;
      try { sourceEl.setPointerCapture(ev.pointerId); } catch (e) {}
      const ox = 50, oy = 14;
      this._drag = {
        tab: null, isNew: true, isStarted: false, tabDef, tabset: null,
        offX: ox, offY: oy, x0: ev.clientX, y0: ev.clientY,
        rect: { left: ev.clientX - ox, top: ev.clientY - oy, width: 100 },
        drop: null, captureEl: sourceEl, pointerId: ev.pointerId
      };
      sourceEl.addEventListener('pointermove',   this._pmDrag);
      sourceEl.addEventListener('pointerup',     this._puDrag);
      sourceEl.addEventListener('pointercancel', this._puDrag);
    }

    destroy() {
      this._endDragCleanup();
      if (this._dropInd.parentNode) this._dropInd.remove();
      this.container.classList.remove('fl-layout');
      this.container.innerHTML = '';
    }

    // ── Internals ────────────────────────────────────────────────────
    _getActiveId() {
      if (this._activeTabsetId && this.model.getRoot().findById(this._activeTabsetId))
        return this._activeTabsetId;
      const first = this.model._firstTabSet(this.model.getRoot());
      return first ? first.id : null;
    }

    _act(action) {
      if (this.onAction && this.onAction(action) === false) return;
      this.model.doAction(action);
      if (this.onModelChange) this.onModelChange(this.model);
    }

    _render() {
      // Purge orphaned content elements
      const live = new Set();
      this._collectTabIds(this.model.getRoot(), live);
      for (const b of this.model.getBorders()) b.children.forEach(t => live.add(t.id));
      for (const [id] of this._contentEls) { if (!live.has(id)) this._contentEls.delete(id); }

      this._nodeEls.clear();
      this.container.innerHTML = '';
      const root = this.model.getRoot(); if (!root) return;

      const borders = this.model.getBorders();
      const leftBorders = borders.filter(b => !b._hidden && (b.side === 'left' || b.side === 'left-top' || b.side === 'left-bottom'));
      const rightBorders = borders.filter(b => !b._hidden && (b.side === 'right' || b.side === 'right-top' || b.side === 'right-bottom'));
      const bottomBorder = borders.find(b => !b._hidden && b.side === 'bottom');

      // Calculate insets from open sidebars
      const leftOpen = leftBorders.find(b => b.isOpen());
      const rightOpen = rightBorders.find(b => b.isOpen());
      const leftSize = leftOpen ? Math.max(...leftBorders.map(b => b.size)) : 0;
      const rightSize = rightOpen ? Math.max(...rightBorders.map(b => b.size)) : 0;
      const bottomSize = bottomBorder && bottomBorder.isOpen() ? bottomBorder.size : 0;
      // Strip widths (fixed)
      const stripW = 30;
      const hasLeft = leftBorders.length > 0;
      const hasRight = rightBorders.length > 0;
      const hasBottom = !!bottomBorder;

      // Render main layout row with padding for sidebars
      const rowEl = this._renderNode(root);
      rowEl.style.position = 'absolute';
      rowEl.style.top = '0';
      rowEl.style.bottom = hasBottom ? stripW + bottomSize + 'px' : '0';
      rowEl.style.left = hasLeft ? stripW + leftSize + 'px' : '0';
      rowEl.style.right = hasRight ? stripW + rightSize + 'px' : '0';
      rowEl.style.flex = '';
      this.container.appendChild(rowEl);

      // Render border panels as absolutely positioned siblings
      if (hasLeft) this.container.appendChild(this._renderBorderGroup(leftBorders, 'left'));
      if (hasRight) this.container.appendChild(this._renderBorderGroup(rightBorders, 'right'));
      if (bottomBorder) this.container.appendChild(this._renderBorder(bottomBorder));

      const max = this.model.getMaximized();
      if (max) {
        this.container.classList.add('fl-has-maximized');
        const el = this._nodeEls.get(max.id); if (el) el.classList.add('fl-maximized');
      } else {
        this.container.classList.remove('fl-has-maximized');
      }

      this.container.querySelectorAll('[data-fl-ph]').forEach(ph => {
        const c = this._contentEls.get(ph.dataset.flPh);
        if (c) ph.replaceWith(c);
      });
    }

    _collectTabIds(node, set) {
      if (!node) return;
      if (node.type === NT.TAB) { set.add(node.id); return; }
      if (node.type === NT.BORDER) { node.children.forEach(c => set.add(c.id)); return; }
      node.children.forEach(c => this._collectTabIds(c, set));
    }

    _renderBorderGroup(borders, baseSide) {
      if (borders.length === 1) return this._renderBorder(borders[0]);
      // Multiple borders on same side: shared strip, split panel when both open
      const stripW = 30;
      const hasBottom = this.model.getBorders().some(b => !b._hidden && b.side === 'bottom');
      const openBorders = borders.filter(b => b.isOpen());
      const maxSize = Math.max(...borders.map(b => b.size), 200);
      const size = openBorders.length > 0 ? maxSize : 0;

      const wrapper = document.createElement('div');
      wrapper.className = `fl-sidebar fl-sidebar-${baseSide}`;
      if (baseSide === 'left') {
        wrapper.style.cssText = `position:absolute;top:0;left:0;bottom:${hasBottom ? stripW : 0}px;display:flex;flex-direction:row;z-index:2;`;
      } else {
        wrapper.style.cssText = `position:absolute;top:0;right:0;bottom:${hasBottom ? stripW : 0}px;display:flex;flex-direction:row;z-index:2;`;
      }

      // Strip column with sections
      const strip = document.createElement('div');
      strip.className = `fl-sidebar-strip fl-sidebar-strip-${baseSide}`;
      strip.style.cssText = `display:flex;flex-direction:column;width:${stripW}px;overflow:hidden;flex-shrink:0;background:var(--fl-strip,#11111b);border-${baseSide === 'left' ? 'right' : 'left'}:1px solid var(--fl-border,#313244);`;

      borders.forEach((border, bIdx) => {
        const section = document.createElement('div');
        section.className = 'fl-sidebar-section';
        section.style.cssText = `flex:1;display:flex;flex-direction:column;overflow:auto;${bIdx > 0 ? 'border-top:1px solid var(--fl-border,#313244);' : ''}`;
        section.dataset.flBorder = border.side;
        this._nodeEls.set(border.id, section);
        border.children.forEach((tab, i) => section.appendChild(this._mkBorderTabBtn(tab, border, i)));
        strip.appendChild(section);
      });

      // Panel area
      const panel = this._mkBorderPanel(openBorders, baseSide, true, size);

      if (baseSide === 'right') { wrapper.appendChild(panel); wrapper.appendChild(strip); }
      else { wrapper.appendChild(strip); wrapper.appendChild(panel); }
      return wrapper;
    }

    _mkBorderTabBtn(tab, border, i) {
      const side = border.side;
      const btn = document.createElement('div');
      btn.className = `fl-sidebar-tab${i === border.getSelected() ? ' fl-sidebar-tab-active' : ''}`;
      btn.dataset.flBorderTab = tab.id;
      btn.dataset.flBorderSide = side;
      // Icon and/or label
      const icon = tab.getIcon();
      const ts = border.tabStyle;
      const showIcon = icon && ts !== 'label';
      const showLabel = ts === 'label' || ts === 'iconLabel' || (!icon && ts !== 'icon') || (!icon && ts === 'auto');
      if (showIcon) {
        const ic = document.createElement('span');
        ic.className = 'fl-sidebar-tab-icon';
        ic.textContent = icon;
        btn.appendChild(ic);
      }
      if (showLabel) {
        const lbl = document.createElement('span');
        lbl.className = 'fl-sidebar-tab-label';
        lbl.textContent = tab.getName();
        btn.appendChild(lbl);
      }
      btn.title = tab.getName();
      if (tab.isEnableClose()) {
        const x = document.createElement('button');
        x.type = 'button'; x.className = 'fl-sidebar-tab-x'; x.innerHTML = '×';
        x.addEventListener('click', ev => {
          ev.stopPropagation();
          this._act(Actions.closeBorderTab(side, tab.id));
        });
        btn.appendChild(x);
      }
      btn.style.touchAction = 'none';
      btn.addEventListener('pointerdown', ev => {
        if (ev.button !== 0 || ev.target.closest('.fl-sidebar-tab-x')) return;
        ev.preventDefault();
        const startX = ev.clientX, startY = ev.clientY;
        let dragging = false;
        const onMove = me => {
          if (!dragging && (Math.abs(me.clientX - startX) > 4 || Math.abs(me.clientY - startY) > 4)) {
            dragging = true;
            btn.releasePointerCapture(ev.pointerId);
            btn.removeEventListener('pointermove', onMove);
            btn.removeEventListener('pointerup', onUp);
            this._startBorderTabDrag(ev, tab, border, btn, me);
          }
        };
        const onUp = () => {
          btn.removeEventListener('pointermove', onMove);
          btn.removeEventListener('pointerup', onUp);
          try { btn.releasePointerCapture(ev.pointerId); } catch(e) {}
          if (!dragging) {
            this._act(Actions.selectBorderTab(side, tab.id));
          }
        };
        try { btn.setPointerCapture(ev.pointerId); } catch(e) {}
        btn.addEventListener('pointermove', onMove);
        btn.addEventListener('pointerup', onUp);
      });
      return btn;
    }

    _renderBorder(border) {
      const side = border.side;
      const isV = side === 'left' || side === 'right';
      const isOpen = border.isOpen();
      const size = border.size;
      const stripW = 30;

      const wrapper = document.createElement('div');
      wrapper.className = `fl-sidebar fl-sidebar-${side}${isOpen ? ' fl-sidebar-open' : ''}`;
      wrapper.dataset.flBorder = side;
      this._nodeEls.set(border.id, wrapper);

      // Position absolutely
      if (side === 'left') {
        const hasBot = this.model.getBorders().some(b => !b._hidden && b.side === 'bottom');
        wrapper.style.cssText = `position:absolute;top:0;left:0;bottom:${hasBot ? stripW : 0}px;display:flex;flex-direction:row;z-index:2;`;
      } else if (side === 'right') {
        const hasBot = this.model.getBorders().some(b => !b._hidden && b.side === 'bottom');
        wrapper.style.cssText = `position:absolute;top:0;right:0;bottom:${hasBot ? stripW : 0}px;display:flex;flex-direction:row;z-index:2;`;
      } else {
        const hasL = this.model.getBorders().some(b => !b._hidden && (b.side === 'left' || b.side.startsWith('left-')));
        const hasR = this.model.getBorders().some(b => !b._hidden && (b.side === 'right' || b.side.startsWith('right-')));
        wrapper.style.cssText = `position:absolute;bottom:0;left:${hasL ? stripW : 0}px;right:${hasR ? stripW : 0}px;display:flex;flex-direction:column;z-index:2;`;
      }

      // Tab strip
      const strip = document.createElement('div');
      strip.className = `fl-sidebar-strip fl-sidebar-strip-${side}`;
      if (isV) {
        strip.style.cssText = `display:flex;flex-direction:column;width:${stripW}px;overflow:auto;flex-shrink:0;background:var(--fl-strip,#11111b);border-${side === 'left' ? 'right' : 'left'}:1px solid var(--fl-border,#313244);`;
      } else {
        strip.style.cssText = `display:flex;flex-direction:row;height:${stripW}px;overflow:auto;flex-shrink:0;background:var(--fl-strip,#11111b);border-top:1px solid var(--fl-border,#313244);`;
      }
      border.children.forEach((tab, i) => strip.appendChild(this._mkBorderTabBtn(tab, border, i)));

      // Content panel
      const panel = this._mkBorderPanel([border].filter(b => b.isOpen()), side, isV, size);

      if (side === 'right' || side === 'bottom') { wrapper.appendChild(panel); wrapper.appendChild(strip); }
      else { wrapper.appendChild(strip); wrapper.appendChild(panel); }
      return wrapper;
    }

    _mkBorderPanel(openBorders, side, isV, size) {
      const panel = document.createElement('div');
      panel.className = 'fl-sidebar-panel';
      const hasOpen = openBorders.length > 0;
      if (isV) {
        panel.style.cssText = `width:${hasOpen ? size : 0}px;overflow:hidden;transition:width 150ms ease;background:var(--fl-panel,#181825);display:flex;flex-direction:column;`;
      } else {
        panel.style.cssText = `height:${hasOpen ? size : 0}px;overflow:hidden;transition:height 150ms ease;background:var(--fl-panel,#181825);display:flex;flex-direction:column;`;
      }
      if (hasOpen) {
        openBorders.forEach((ob, oIdx) => {
          const selTab = ob.getSelectedNode();
          if (!selTab) return;
          const pane = document.createElement('div');
          pane.className = 'fl-sidebar-pane';
          pane.style.cssText = `flex:1;display:flex;flex-direction:column;overflow:hidden;${oIdx > 0 ? 'border-top:1px solid var(--fl-border,#313244);' : ''}`;
          // Header
          const header = document.createElement('div');
          header.className = 'fl-sidebar-header';
          const htxt = document.createElement('span');
          htxt.textContent = selTab.getName();
          header.appendChild(htxt);
          const closeBtn = document.createElement('button');
          closeBtn.type = 'button'; closeBtn.className = 'fl-sidebar-header-x'; closeBtn.innerHTML = '×';
          closeBtn.addEventListener('click', () => this._act(Actions.selectBorderTab(ob.side, selTab.id)));
          header.appendChild(closeBtn);
          pane.appendChild(header);
          // Content
          const c = this._getOrMakeContent(selTab);
          c.style.display = 'flex';
          const ph = document.createElement('div');
          ph.dataset.flPh = selTab.id;
          ph.style.cssText = 'flex:1;overflow:auto;';
          pane.appendChild(ph);
          // Add splitter between panes
          if (oIdx < openBorders.length - 1) {
            const splitter = document.createElement('div');
            splitter.className = 'fl-sidebar-splitter';
            splitter.style.touchAction = 'none';
            splitter.addEventListener('pointerdown', ev => {
              if (ev.button !== 0) return;
              ev.preventDefault();
              try { splitter.setPointerCapture(ev.pointerId); } catch(e) {}
              const prevPane = pane;
              const nextPane = panel.querySelectorAll('.fl-sidebar-pane')[oIdx + 1];
              if (!nextPane) return;
              const startY = ev.clientY;
              const startH1 = prevPane.offsetHeight, startH2 = nextPane.offsetHeight;
              const move = me => {
                const dy = me.clientY - startY;
                const h1 = Math.max(40, startH1 + dy), h2 = Math.max(40, startH2 - dy);
                prevPane.style.flex = `${h1} 0 0px`;
                nextPane.style.flex = `${h2} 0 0px`;
              };
              const up = () => {
                splitter.removeEventListener('pointermove', move);
                splitter.removeEventListener('pointerup', up);
                splitter.removeEventListener('pointercancel', up);
                try { splitter.releasePointerCapture(ev.pointerId); } catch(e) {}
              };
              splitter.addEventListener('pointermove', move);
              splitter.addEventListener('pointerup', up);
              splitter.addEventListener('pointercancel', up);
            });
            panel.appendChild(pane);
            panel.appendChild(splitter);
          } else {
            panel.appendChild(pane);
          }
        });
        // Resize handle
        const handle = document.createElement('div');
        handle.className = `fl-sidebar-resize fl-sidebar-resize-${side}`;
        handle.style.touchAction = 'none';
        handle.addEventListener('pointerdown', e => {
          if (e.button !== 0) return;
          e.preventDefault();
          try { handle.setPointerCapture(e.pointerId); } catch(err) {}
          const startPos = isV ? e.clientX : e.clientY;
          const startSize = size;
          const onMove = me => {
            const delta = isV ? (me.clientX - startPos) : (me.clientY - startPos);
            const dir = side === 'right' ? -1 : side === 'bottom' ? -1 : 1;
            const newSize = Math.max(50, startSize + delta * dir);
            if (isV) panel.style.width = newSize + 'px';
            else panel.style.height = newSize + 'px';
          };
          const onUp = () => {
            handle.removeEventListener('pointermove', onMove);
            handle.removeEventListener('pointerup', onUp);
            try { handle.releasePointerCapture(e.pointerId); } catch(err) {}
            const finalSize = isV ? panel.offsetWidth : panel.offsetHeight;
            openBorders.forEach(ob => this._act(Actions.resizeBorder(ob.side, finalSize)));
          };
          handle.addEventListener('pointermove', onMove);
          handle.addEventListener('pointerup', onUp);
        });
        panel.appendChild(handle);
      }
      return panel;
    }

    _updateBorderSelection(side) {
      // Partial re-render: only update the affected sidebar + main layout insets
      const baseSide = side.split('-')[0];
      const sidebarEl = this.container.querySelector(`.fl-sidebar-${baseSide}`);
      if (!sidebarEl) { this._render(); return; }
      // Replace the sidebar entirely (cheaper than full layout re-render)
      const borders = this.model.getBorders();
      const sideBorders = borders.filter(b => !b._hidden && (b.side === baseSide || b.side.startsWith(baseSide + '-')));
      const bottomBorder = borders.find(b => !b._hidden && b.side === 'bottom');
      let newSidebar;
      if (sideBorders.length > 1) {
        newSidebar = this._renderBorderGroup(sideBorders, baseSide);
      } else if (sideBorders.length === 1) {
        newSidebar = this._renderBorder(sideBorders[0]);
      }
      if (newSidebar) {
        sidebarEl.replaceWith(newSidebar);
      } else {
        sidebarEl.remove();
      }
      // Update main layout insets
      const leftBorders = borders.filter(b => !b._hidden && (b.side === 'left' || b.side.startsWith('left-')));
      const rightBorders = borders.filter(b => !b._hidden && (b.side === 'right' || b.side.startsWith('right-')));
      const leftOpen = leftBorders.find(b => b.isOpen());
      const rightOpen = rightBorders.find(b => b.isOpen());
      const stripW = 30;
      const leftSize = leftOpen ? Math.max(...leftBorders.map(b => b.size)) : 0;
      const rightSize = rightOpen ? Math.max(...rightBorders.map(b => b.size)) : 0;
      const bottomSize = bottomBorder && bottomBorder.isOpen() ? bottomBorder.size : 0;
      const rowEl = this.container.querySelector('.fl-row');
      if (rowEl) {
        rowEl.style.left = leftBorders.length > 0 ? (stripW + leftSize) + 'px' : '0';
        rowEl.style.right = rightBorders.length > 0 ? (stripW + rightSize) + 'px' : '0';
        rowEl.style.bottom = bottomBorder ? (stripW + bottomSize) + 'px' : '0';
      }
      // Replace placeholders with content elements
      this.container.querySelectorAll('[data-fl-ph]').forEach(ph => {
        const c = this._contentEls.get(ph.dataset.flPh);
        if (c) ph.replaceWith(c);
      });
    }

    _startBorderTabDrag(origEv, tab, border, btnEl, moveEv) {
      // Initiate a standard drag from a border tab
      const rect = btnEl.getBoundingClientRect();
      const ghost = document.createElement('div');
      ghost.className = 'fl-tab fl-drag-ghost';
      ghost.innerHTML = `<span class="fl-tab-label">${tab.getName()}</span>`;
      ghost.style.cssText = `position:fixed;left:${rect.left}px;top:${rect.top}px;min-width:${rect.width}px;pointer-events:none;z-index:9997;white-space:nowrap;`;
      document.body.appendChild(ghost);
      this._ghost = ghost;
      document.body.style.cursor = 'grabbing';

      const offX = origEv.clientX - rect.left, offY = origEv.clientY - rect.top;
      this._drag = {
        tab, tabset: border, idx: border.children.indexOf(tab),
        isNew: false, isStarted: true, isBorder: true, borderSide: border.side,
        drop: null, x0: origEv.clientX, y0: origEv.clientY, rect,
        offX, offY, captureEl: document.documentElement, pointerId: origEv.pointerId
      };
      document.documentElement.addEventListener('pointermove', this._pmDrag);
      document.documentElement.addEventListener('pointerup', this._puDrag);
      try { document.documentElement.setPointerCapture(origEv.pointerId); } catch(e) {}
    }

    _renderNode(node) {
      let el;
      if (node.type === NT.ROW)    el = this._renderRow(node);
      if (node.type === NT.TABSET) el = this._renderTabSet(node);
      if (!el) return document.createTextNode('');
      this._nodeEls.set(node.id, el);
      el.dataset.flNode = node.id;
      return el;
    }

    _renderRow(node) {
      const el = document.createElement('div');
      el.className = `fl-row fl-${node.direction}`;
      node.children.forEach((child, i) => {
        if (i > 0) el.appendChild(this._mkSplitter(node, i - 1));
        const ce = this._renderNode(child);
        ce.style.flex = `${child.getWeight()} ${child.getWeight()} 0px`;
        el.appendChild(ce);
      });
      return el;
    }

    _mkSplitter(row, afterIdx) {
      const sp = document.createElement('div');
      const isH = row.direction === 'row';
      sp.className = `fl-splitter ${isH ? 'fl-sp-h' : 'fl-sp-v'}`;
      sp.style.touchAction = 'none';
      sp.addEventListener('pointerdown', e => {
        if (e.button !== 0) return;
        e.preventDefault();
        const A = row.children[afterIdx], B = row.children[afterIdx + 1];
        try { sp.setPointerCapture(e.pointerId); } catch (err) {}
        this._resize = { A, B, isH, sp, row, x0: e.clientX, y0: e.clientY,
                         wa: A.getWeight(), wb: B.getWeight(),
                         rowEl: this._nodeEls.get(row.id), pointerId: e.pointerId };
        sp.classList.add('fl-sp-active');
        document.body.style.cursor     = isH ? 'col-resize' : 'row-resize';
        document.body.style.userSelect = 'none';
        sp.addEventListener('pointermove',   this._pmRes);
        sp.addEventListener('pointerup',     this._puRes);
        sp.addEventListener('pointercancel', this._puRes);
      });
      return sp;
    }

    _renderTabSet(node) {
      const el = document.createElement('div');
      el.className = 'fl-tabset';
      el.dataset.flTabset = node.id;
      el.addEventListener('pointerdown', () => { this._activeTabsetId = node.id; }, true);

      // ── tab strip ──
      const strip = document.createElement('div'); strip.className = 'fl-strip';
      const tabs  = document.createElement('div'); tabs.className  = 'fl-tabs';
      node.children.forEach((tab, i) => tabs.appendChild(this._mkTabBtn(tab, node, i)));
      strip.appendChild(tabs);

      // ── toolbar ──
      const tb = document.createElement('div'); tb.className = 'fl-toolbar';

      // custom tabset buttons (from constructor option)
      if (this.tabSetButtons) {
        const btns = this.tabSetButtons(node);
        if (btns) btns.forEach(b => tb.appendChild(b));
      }

      // maximize
      if (node.enableMaximize && node.children.length > 0) {
        const b = document.createElement('button');
        b.type = 'button'; b.className = 'fl-tbtn fl-tbtn-max';
        b.title   = node.isMaximized() ? 'Restore' : 'Maximize';
        b.innerHTML = node.isMaximized() ? SVG_RESTORE : SVG_MAX;
        b.addEventListener('click', () => this._act(Actions.maximizeToggle(node.id)));
        tb.appendChild(b);
      }
      strip.appendChild(tb);
      el.appendChild(strip);

      // ── content ──
      const area = document.createElement('div'); area.className = 'fl-content';
      node.children.forEach((tab, i) => {
        const c = this._getOrMakeContent(tab);
        c.style.display = i === node.getSelected() ? 'flex' : 'none';
        const ph = document.createElement('div');
        ph.dataset.flPh = tab.id;
        area.appendChild(ph);
      });
      el.appendChild(area);
      return el;
    }

    // Fast tab select — updates model + DOM directly, NO full re-render
    _selectTabFast(tsNode, tabIdx) {
      tsNode.setSelected(tabIdx);
      this._activeTabsetId = tsNode.id;
      const tsEl = this._nodeEls.get(tsNode.id);
      if (!tsEl) return;
      tsEl.querySelectorAll('.fl-tabs .fl-tab').forEach((b, i) =>
        b.classList.toggle('fl-tab-active', i === tabIdx));
      tsNode.children.forEach((t, i) => {
        const c = this._contentEls.get(t.id);
        if (c) c.style.display = i === tabIdx ? 'flex' : 'none';
      });
    }

    _mkTabBtn(tab, tabset, idx) {
      const btn = document.createElement('div');
      btn.className = `fl-tab${idx === tabset.getSelected() ? ' fl-tab-active' : ''}`;
      btn.dataset.flTab = tab.id;

      if (tab.getIcon()) {
        const ic = document.createElement('span');
        ic.className = 'fl-tab-icon'; ic.textContent = tab.getIcon();
        btn.appendChild(ic);
      }
      const lbl = document.createElement('span');
      lbl.className = 'fl-tab-label'; lbl.textContent = tab.getName();
      btn.appendChild(lbl);

      if (tab.isEnableClose()) {
        const x = document.createElement('button');
        x.type = 'button'; x.className = 'fl-tab-x'; x.innerHTML = '×'; x.title = 'Close';
        x.addEventListener('click', ev => {
          ev.stopPropagation();
          if (this.onTabClose && this.onTabClose(tab) === false) return;
          this._act(Actions.closeTab(tab.id));
        });
        btn.appendChild(x);
      }

      btn.draggable = false;
      btn.style.touchAction = 'none';
      btn.addEventListener('pointerdown', ev => {
        if (ev.button !== 0 || ev.target.closest('.fl-tab-x')) return;
        ev.preventDefault();
        // Fast select: no DOM wipe, no capture break
        this._selectTabFast(tabset, idx);
        if (!tab.isEnableDrag()) return;
        // Capture the pointer so we receive ALL moves/ups, even over canvas/tables
        try { btn.setPointerCapture(ev.pointerId); } catch (e) {}
        this._drag = {
          tab, tabset, idx, isNew: false, isStarted: false, drop: null,
          x0: ev.clientX, y0: ev.clientY, rect: btn.getBoundingClientRect(),
          captureEl: btn, pointerId: ev.pointerId
        };
        btn.addEventListener('pointermove',   this._pmDrag);
        btn.addEventListener('pointerup',     this._puDrag);
        btn.addEventListener('pointercancel', this._puDrag);
      });

      return btn;
    }

    _getOrMakeContent(tab) {
      let el = this._contentEls.get(tab.id);
      if (!el) {
        el = document.createElement('div');
        el.className = 'fl-tab-content';
        const inner = this.factory(tab);
        if (inner instanceof HTMLElement) el.appendChild(inner);
        else if (typeof inner === 'string') el.innerHTML = inner;
        this._contentEls.set(tab.id, el);
      }
      return el;
    }

    _mkDropInd() {
      const el = document.createElement('div');
      el.className = 'fl-drop-ind'; el.style.display = 'none';
      el.style.pointerEvents = 'none';
      document.body.appendChild(el);
      return el;
    }

    // ── Drag (pointer-capture based) ─────────────────────────────────
    _onDragMove(ev) {
      const d = this._drag;
      if (!d) return;
      const dx = ev.clientX - d.x0, dy = ev.clientY - d.y0;

      // Lazily create ghost once threshold (4px) is exceeded
      if (!d.isStarted) {
        if (dx * dx + dy * dy < 16) return;
        d.isStarted = true;
        d.offX = d.x0 - d.rect.left;
        d.offY = d.y0 - d.rect.top;
        const ghost = document.createElement('div');
        ghost.className = 'fl-tab fl-drag-ghost';
        if (!d.isNew && d.tab.getIcon()) ghost.innerHTML = `<span class="fl-tab-icon">${d.tab.getIcon()}</span>`;
        ghost.innerHTML += `<span class="fl-tab-label">${d.isNew ? (d.tabDef.name||'New') : d.tab.getName()}</span>`;
        ghost.style.cssText = `position:fixed;left:${d.rect.left}px;top:${d.rect.top}px;`
          + `min-width:${d.rect.width}px;pointer-events:none;z-index:9997;white-space:nowrap;`;
        document.body.appendChild(ghost);
        this._ghost = ghost;
        document.body.style.cursor = 'grabbing';
      }

      if (this._ghost) {
        this._ghost.style.left = (ev.clientX - d.offX) + 'px';
        this._ghost.style.top  = (ev.clientY - d.offY) + 'px';
      }
      this._updateDrop(ev);
    }

    _updateDrop(ev) {
      const d = this._drag; const ind = this._dropInd;
      const hit = this._findHit(ev.clientX, ev.clientY);
      if (!hit) { ind.style.display = 'none'; d.drop = null; return; }
      d.drop = hit;

      // Insertion bar (reorder / insert into a tab strip)
      if (hit.location === 'insert') {
        const b = hit.bar;
        ind.style.cssText = `display:block;position:fixed;left:${b.x - 1.5}px;top:${b.top}px;`
          + `width:3px;height:${b.height}px;border:none;border-radius:2px;`
          + `background:var(--fl-drop-border,#89b4fa);box-shadow:0 0 5px var(--fl-drop-border,#89b4fa);z-index:9998;`;
        return;
      }
      // Edge split / center zone
      const R = hit.rect;
      let L = R.left, T = R.top, W = R.width, H = R.height;
      if (hit.location === DL.LEFT)         { W *= 0.5; }
      else if (hit.location === DL.RIGHT)   { L += W*0.5; W *= 0.5; }
      else if (hit.location === DL.TOP)     { H *= 0.5; }
      else if (hit.location === DL.BOTTOM)  { T += H*0.5; H *= 0.5; }
      ind.style.cssText = `display:block;position:fixed;left:${L}px;top:${T}px;width:${W}px;height:${H}px;z-index:9998;`;
    }

    // Rect-based hit detection. Priority: border strip > tab strip (insertion bar) > edges (split) > center.
    _findHit(x, y) {
      const d = this._drag;

      // Check border tab strips first (enlarged drop zone during drag)
      for (const border of this.model.getBorders()) {
        if (border._hidden) continue;
        const stripEl = this.container.querySelector(`[data-fl-border="${border.side}"] .fl-sidebar-strip`);
        const sectionEl = this.container.querySelector(`.fl-sidebar-section[data-fl-border="${border.side}"]`);
        const checkEl = sectionEl || stripEl;
        if (!checkEl) continue;
        const sr = checkEl.getBoundingClientRect();
        // Expand hit area by 15px toward layout center for easier drops
        const expand = 15;
        const baseSide = border.side.split('-')[0];
        let hit = false;
        if (baseSide === 'left') hit = x >= sr.left && x <= sr.right + expand && y >= sr.top && y <= sr.bottom;
        else if (baseSide === 'right') hit = x >= sr.left - expand && x <= sr.right && y >= sr.top && y <= sr.bottom;
        else hit = x >= sr.left && x <= sr.right && y >= sr.top - expand && y <= sr.bottom;
        if (hit) return { tabsetId: border.id, location: 'border', borderSide: border.side, insertIndex: border.children.length };
      }

      let best = null, bestArea = Infinity;
      this.container.querySelectorAll('[data-fl-tabset]').forEach(tsEl => {
        const rect = tsEl.getBoundingClientRect();
        if (x < rect.left || x > rect.right || y < rect.top || y > rect.bottom) return;
        const area = rect.width * rect.height;
        if (area >= bestArea) return;                       // keep the smallest (innermost) tabset
        const tsNode = this.model.getRoot().findById(tsEl.dataset.flTabset);
        if (!tsNode || !tsNode.enableDrop) return;
        bestArea = area;
        const tabsetId = tsEl.dataset.flTabset;

        // 1) Over the tab strip → insertion bar with a precise index
        const stripEl = tsEl.querySelector('.fl-strip');
        const tabsEl  = tsEl.querySelector('.fl-tabs');
        if (stripEl) {
          const sr = stripEl.getBoundingClientRect();
          if (y >= sr.top && y <= sr.bottom && x >= sr.left && x <= sr.right) {
            const btns = tabsEl ? Array.from(tabsEl.querySelectorAll('[data-fl-tab]')) : [];
            let idx = btns.length, barX = null;
            for (let i = 0; i < btns.length; i++) {
              const br = btns[i].getBoundingClientRect();
              if (x < br.left + br.width / 2) { idx = i; barX = br.left; break; }
            }
            if (barX === null)                                       // past the last tab → append
              barX = btns.length ? btns[btns.length-1].getBoundingClientRect().right : sr.left + 2;
            best = { tabsetId, location: 'insert', insertIndex: idx, bar: { x: barX, top: sr.top, height: sr.height } };
            return;
          }
        }

        // 2) Body: nearest edge → split, else center
        const rx = x-rect.left, ry = y-rect.top, rw = rect.width, rh = rect.height;
        const dirs = {[DL.LEFT]:rx/rw,[DL.RIGHT]:(rw-rx)/rw,[DL.TOP]:ry/rh,[DL.BOTTOM]:(rh-ry)/rh};
        let location = DL.CENTER, minR = EDGE;
        for (const [loc, r] of Object.entries(dirs)) if (r < minR) { minR=r; location=loc; }

        // Dropping a tab onto the center of its OWN tabset body is a no-op
        if (!d.isNew && tabsetId === d.tabset?.id && location === DL.CENTER) { best = null; return; }
        best = { tabsetId, location, rect };
      });
      return best;
    }

    _onDragUp(ev) {
      const d = this._drag;
      if (!d) { this._endDragCleanup(); return; }

      const started = d.isStarted, drop = d.drop;
      this._endDragCleanup();

      if (!started) {
        // Plain click (no actual drag) — notify listeners
        if (this.onModelChange) this.onModelChange(this.model);
        return;
      }
      if (!drop) return; // dragged but released over no valid target

      const { tabsetId, location, insertIndex, borderSide } = drop;

      // Dropping into a border
      if (location === 'border' && borderSide) {
        if (d.isNew) {
          this._act(Actions.addBorderTab(borderSide, d.tabDef, insertIndex));
        } else if (d.isBorder) {
          // Already in a border — reorder or move between borders
          this._act(Actions.moveToBorder(d.tab.id, borderSide, insertIndex));
        } else {
          this._act(Actions.moveToBorder(d.tab.id, borderSide, insertIndex));
        }
        return;
      }

      // Dragging FROM a border INTO the layout
      if (d.isBorder && !borderSide) {
        if (location === 'insert')       this._act(Actions.moveFromBorder(d.borderSide, d.tab.id, tabsetId, DL.CENTER, insertIndex));
        else if (location === DL.CENTER) this._act(Actions.moveFromBorder(d.borderSide, d.tab.id, tabsetId, DL.CENTER));
        else                             this._act(Actions.moveFromBorder(d.borderSide, d.tab.id, tabsetId, location));
        return;
      }

      if (d.isNew) {
        if (location === 'insert')        this._act(Actions.addTab(tabsetId, d.tabDef, insertIndex));
        else if (location === DL.CENTER)  this._act(Actions.addTab(tabsetId, d.tabDef));
        else                              this._act(Actions.addTabSplit(d.tabDef, tabsetId, location));
      } else {
        if (location === 'insert')        this._act(Actions.moveTab(d.tab.id, tabsetId, DL.CENTER, insertIndex));
        else                              this._act(Actions.moveTab(d.tab.id, tabsetId, location));
      }
    }

    // Detach listeners, release capture, remove ghost — safe to call multiple times
    _endDragCleanup() {
      const d = this._drag;
      this._drag = null;
      if (d && d.captureEl) {
        d.captureEl.removeEventListener('pointermove',   this._pmDrag);
        d.captureEl.removeEventListener('pointerup',     this._puDrag);
        d.captureEl.removeEventListener('pointercancel', this._puDrag);
        try { d.captureEl.releasePointerCapture(d.pointerId); } catch (e) {}
      }
      if (this._ghost) { this._ghost.remove(); this._ghost = null; }
      this._dropInd.style.display = 'none';
      document.body.style.cursor = '';
    }

    _findTabIdx(tsId, tabId) {
      const ts = this.model.getRoot().findById(tsId);
      if (!ts) return undefined;
      const i = ts.children.findIndex(t => t.id === tabId);
      return i >= 0 ? i : undefined;
    }

    // ── Resize (pointer-capture based) ───────────────────────────────
    _onResMove(ev) {
      const r = this._resize;
      if (!r) return;
      const { A, B, isH, x0, y0, wa, wb, rowEl } = r;
      const rect = rowEl.getBoundingClientRect();
      const total = isH ? rect.width : rect.height;
      const delta = isH ? ev.clientX - x0 : ev.clientY - y0;
      const tw = wa + wb;
      const nA = clamp(wa + (delta / total) * tw, tw * 0.05, tw * 0.95), nB = tw - nA;
      A.setWeight(nA); B.setWeight(nB);
      const eA = this._nodeEls.get(A.id), eB = this._nodeEls.get(B.id);
      if (eA) eA.style.flex = `${nA} ${nA} 0px`;
      if (eB) eB.style.flex = `${nB} ${nB} 0px`;
    }
    _onResUp() {
      const r = this._resize;
      if (!r) return;
      this._resize = null;
      r.sp.classList.remove('fl-sp-active');
      r.sp.removeEventListener('pointermove',   this._pmRes);
      r.sp.removeEventListener('pointerup',     this._puRes);
      r.sp.removeEventListener('pointercancel', this._puRes);
      try { r.sp.releasePointerCapture(r.pointerId); } catch (e) {}
      document.body.style.cursor = document.body.style.userSelect = '';
      this.model.emit('change', this.model);
    }
  }

  const SVG_MAX     = `<svg viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="1" y="1" width="10" height="10" rx="1"/></svg>`;
  const SVG_RESTORE = `<svg viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="1" width="8" height="8" rx="1"/><path d="M1 4v7h7"/></svg>`;

  const FlexLayout = { Model, Layout, Actions, NodeType: NT, DropLocation: DL, BorderNode, BORDER_SIDES };
  if (typeof module !== 'undefined' && module.exports) module.exports = FlexLayout;
  else global.FlexLayout = FlexLayout;

}(typeof window !== 'undefined' ? window : globalThis, document));
