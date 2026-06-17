/**
 * Sample Web Components for testing PWebComponent integration.
 *
 * CRITICAL RULE: Web Components created by PonySDK via document.createElement()
 * must NOT do any DOM work in their constructor. Use connectedCallback() instead.
 * See: https://html.spec.whatwg.org/multipage/custom-elements.html#custom-element-conformance
 *
 * Slot convention: native <slot name="..."> in shadow DOM.
 * PonySDK sets slot="name" on child elements — the browser handles projection.
 *
 * Components that need shared DS styles extend pony.wc.PonyElement (registered inside
 * onPonyLoaded so pony.wc is available). Components that don't need shared styles
 * extend HTMLElement directly and can be registered immediately.
 */

// ============================================================
// <my-counter> — Simple counter with increment button.
// No shadow DOM, no shared styles needed — extends HTMLElement directly.
// Properties: count (Number), label (String via attribute)
// Events: count-changed { count, action }
// ============================================================
class MyCounter extends HTMLElement {
    constructor() {
        super();
        this._count = 0;
        this._label = 'Count';
    }

    static get observedAttributes() { return ['label']; }

    connectedCallback() { this._render(); }

    attributeChangedCallback(name, _oldVal, newVal) {
        if (name === 'label') { this._label = newVal; this._render(); }
    }

    set count(val) {
        this._count = typeof val === 'number' ? val : parseInt(val, 10) || 0;
        if (this.isConnected) this._render();
    }
    get count() { return this._count; }

    reset() {
        this._count = 0;
        if (this.isConnected) this._render();
        this.dispatchEvent(new CustomEvent('count-changed', { detail: { count: 0, action: 'reset' } }));
    }

    _render() {
        this.innerHTML =
            '<div style="display:inline-flex;align-items:center;gap:10px;padding:12px 16px;' +
            'border:2px solid #4a90d9;border-radius:8px;background:#f0f6ff;font-family:sans-serif;">' +
                '<span style="font-weight:600;color:#555;">' + this._label + ':</span>' +
                '<span style="font-size:28px;font-weight:bold;color:#4a90d9;min-width:50px;text-align:center;">' +
                    this._count + '</span>' +
                '<button style="padding:6px 14px;border:none;border-radius:4px;background:#4a90d9;' +
                'color:#fff;cursor:pointer;font-size:16px;font-weight:bold;">+1</button>' +
            '</div>';
        var self = this;
        this.querySelector('button').addEventListener('click', function () {
            self._count++;
            self._render();
            self.dispatchEvent(new CustomEvent('count-changed', { detail: { count: self._count, action: 'increment' } }));
        });
    }
}
customElements.define('my-counter', MyCounter);

// ============================================================
// Components below extend PonyElement to inherit shared DS styles.
// They MUST be registered inside onPonyLoaded so pony.wc is available.
// ============================================================
document.onPonyLoaded(function () {
    var PonyElement = pony.wc.PonyElement;

    // ============================================================
    // <my-dashboard> — Dashboard with named slots.
    // Slots: "toolbar", "content", "footer"
    // Attributes: title (String), theme ("dark" | "light")
    // ============================================================
    class MyDashboard extends PonyElement {
        // No constructor — GWT creates elements via document.createElement(),
        // which calls the constructor. Any DOM work here crashes.
        // All init goes in connectedCallback / _render.

        static get observedAttributes() { return ['title', 'theme']; }

        connectedCallback() {
            super.connectedCallback(); // attaches shadow + injects shared sheets + calls _render
        }
        attributeChangedCallback() { if (this.isConnected) this._render(); }

        _render() {
            var title = this.getAttribute('title') || 'Dashboard';
            var isDark = this.getAttribute('theme') === 'dark';

            var bg       = isDark ? '#1e1e2e' : '#f8f9fa';
            var fg       = isDark ? '#cdd6f4' : '#333';
            var border   = isDark ? '#45475a' : '#dee2e6';
            var headerBg = isDark ? '#181825' : '#4a90d9';
            var toolbarBg= isDark ? '#313244' : '#e9ecef';
            var contentBg= isDark ? '#1e1e2e' : '#ffffff';
            var footerBg = isDark ? '#181825' : '#f1f3f5';

            this.shadowRoot.innerHTML =
                '<style>' +
                '  :host { display:block; }' +
                '  .wrapper { border:1px solid ' + border + ';border-radius:10px;overflow:hidden;' +
                '    background:' + bg + ';color:' + fg + ';font-family:sans-serif;box-shadow:0 2px 12px rgba(0,0,0,0.12); }' +
                '  .header { padding:12px 18px;background:' + headerBg + ';color:#fff;' +
                '    font-size:16px;font-weight:bold;display:flex;align-items:center;gap:8px; }' +
                '  .toolbar { padding:8px 12px;background:' + toolbarBg + ';border-bottom:1px solid ' + border + ';' +
                '    display:flex;align-items:center;gap:8px;flex-wrap:wrap;min-height:40px; }' +
                '  .content { padding:16px;background:' + contentBg + ';min-height:80px; }' +
                '  .footer { padding:8px 16px;background:' + footerBg + ';border-top:1px solid ' + border + ';' +
                '    font-size:12px;color:#888;min-height:32px;display:flex;align-items:center; }' +
                '</style>' +
                '<div class="wrapper">' +
                    '<div class="header"><span>◆</span><span>' + title + '</span></div>' +
                    '<div class="toolbar"><slot name="toolbar"></slot></div>' +
                    '<div class="content"><slot name="content"></slot></div>' +
                    '<div class="footer"><slot name="footer"></slot></div>' +
                '</div>';
        }
    }
    customElements.define('my-dashboard', MyDashboard);

    // ============================================================
    // <my-card> — Card with named slots.
    // Slots: default (main content), "actions" (button bar)
    // Attributes: title (String), theme ("dark" | "light")
    // Properties: content (String — rendered inside .body)
    // ============================================================
    class MyCard extends PonyElement {
        // No constructor — see MyDashboard comment above.
        static get observedAttributes() { return ['title', 'theme']; }

        connectedCallback() {
            if (!this._content) this._content = '';
            super.connectedCallback(); // attaches shadow + injects shared sheets + calls _render
        }
        attributeChangedCallback() { if (this.isConnected) this._render(); }

        set content(val) {
            this._content = typeof val === 'string' ? val : String(val);
            if (this.isConnected) this._render();
        }
        get content() { return this._content; }

        _render() {
            var title = this.getAttribute('title') || 'Card';
            var isDark = this.getAttribute('theme') === 'dark';

            var bg      = isDark ? '#2d2d2d' : '#ffffff';
            var fg      = isDark ? '#e0e0e0' : '#333';
            var border  = isDark ? '#555'    : '#ddd';
            var headerBg= isDark ? '#3a3a3a' : '#f7f7f7';
            var accent  = isDark ? '#6ab0ff' : '#4a90d9';

            this.shadowRoot.innerHTML =
                '<style>' +
                '  :host { display:block; }' +
                '  .wrapper { border:2px solid ' + border + ';border-radius:10px;overflow:hidden;' +
                '    background:' + bg + ';color:' + fg + ';font-family:sans-serif;box-shadow:0 2px 8px rgba(0,0,0,0.1); }' +
                '  .header { padding:12px 16px;background:' + headerBg + ';border-bottom:2px solid ' + border + ';' +
                '    font-weight:bold;font-size:16px;color:' + accent + '; }' +
                '  .body { padding:16px; }' +
                '  .actions { padding:10px 16px;border-top:1px solid ' + border + ';' +
                '    display:flex;gap:8px;flex-wrap:wrap;background:' + headerBg + '; }' +
                '</style>' +
                '<div class="wrapper">' +
                    '<div class="header">◆ ' + title + '</div>' +
                    '<div class="body">' +
                        (this._content ? '<div>' + this._content + '</div>' : '') +
                        '<slot></slot>' +
                    '</div>' +
                    '<div class="actions"><slot name="actions"></slot></div>' +
                '</div>';
        }
    }
    customElements.define('my-card', MyCard);

});
