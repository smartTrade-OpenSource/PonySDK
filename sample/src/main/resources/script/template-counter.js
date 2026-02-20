/**
 * Template Counter Component
 * 
 * Uses native HTML Template API with Shadow DOM.
 * 
 * Source: ./counter.template.html
 * 
 * This demonstrates PonySDK's PTemplateComponent which allows
 * defining UI with pure HTML templates - no framework needed.
 */

console.log('[PComponent] 📄 Loading template-counter.js...');

// Inline the template (in production, could be loaded from counter.template.html)
const TEMPLATE_HTML = `
<template id="pony-counter-template">
    <style>
        :host {
            display: inline-block;
        }
        
        .container {
            display: inline-flex;
            flex-direction: column;
            align-items: center;
            padding: 1.5rem;
            border: 3px solid var(--color, #9b59b6);
            border-radius: 12px;
            min-width: 180px;
            background: linear-gradient(135deg, 
                color-mix(in srgb, var(--color, #9b59b6) 13%, transparent), 
                color-mix(in srgb, var(--color, #9b59b6) 7%, transparent)
            );
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            transition: all 0.3s ease;
        }
        
        .badge {
            background: linear-gradient(135deg, #9b59b6, #8e44ad);
            color: white;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 11px;
            font-weight: bold;
            margin-bottom: 12px;
            display: flex;
            align-items: center;
            gap: 6px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.2);
        }
        
        .label {
            font-size: 0.875rem;
            font-weight: 600;
            text-transform: uppercase;
            color: #666;
            margin-bottom: 0.5rem;
        }
        
        .count {
            font-size: 3rem;
            font-weight: 700;
            color: var(--color, #9b59b6);
            margin-bottom: 1rem;
            line-height: 1;
            transition: transform 0.15s ease;
        }
        
        .count.bump {
            transform: scale(1.2);
        }
        
        .buttons {
            display: flex;
            gap: 0.5rem;
        }
        
        button {
            width: 44px;
            height: 44px;
            border: none;
            border-radius: 10px;
            font-size: 1.5rem;
            color: white;
            cursor: pointer;
            box-shadow: 0 2px 4px rgba(0,0,0,0.2);
            transition: transform 0.1s;
        }
        
        button:active {
            transform: scale(0.95);
        }
        
        .btn-dec {
            background: linear-gradient(135deg, #e74c3c, #c0392b);
        }
        
        .btn-inc {
            background: linear-gradient(135deg, #2ecc71, #27ae60);
        }
    </style>
    
    <div class="container">
        <div class="badge">
            <span>📄</span>
            TEMPLATE
        </div>
        <div class="label" data-bind="label"></div>
        <div class="count" data-bind="count"></div>
        <div class="buttons">
            <button class="btn-dec" data-event="decrement">−</button>
            <button class="btn-inc" data-event="increment">+</button>
        </div>
    </div>
</template>
`;

// Inject template into document (wait for DOM ready)
function injectTemplate() {
    if (!document.getElementById('pony-counter-template')) {
        const container = document.createElement('div');
        container.innerHTML = TEMPLATE_HTML;
        document.body.appendChild(container.firstElementChild);
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', injectTemplate);
} else {
    injectTemplate();
}

/**
 * Template Renderer Class
 * Handles mounting, updating, and event binding for template-based components
 */
class TemplateCounterRenderer {
    constructor(container) {
        this.container = container;
        this.shadowRoot = null;
        this.props = { label: '', count: 0, color: '#9b59b6' };
        this.eventCallback = null;
        this.prevCount = null;
    }
    
    mount() {
        // Create host element with Shadow DOM
        const host = document.createElement('div');
        this.shadowRoot = host.attachShadow({ mode: 'open' });
        
        // Clone template content
        const template = document.getElementById('pony-counter-template');
        const content = template.content.cloneNode(true);
        this.shadowRoot.appendChild(content);
        
        // Bind events using data-event attributes
        this.shadowRoot.querySelectorAll('[data-event]').forEach(el => {
            el.addEventListener('click', () => {
                const eventType = el.getAttribute('data-event');
                this.eventCallback && this.eventCallback(eventType, {});
            });
        });
        
        this.container.appendChild(host);
        this.update();
    }
    
    update() {
        if (!this.shadowRoot) return;
        
        const { label, count, color } = this.props;
        
        // Update bound elements
        const labelEl = this.shadowRoot.querySelector('[data-bind="label"]');
        if (labelEl) labelEl.textContent = label;
        
        const countEl = this.shadowRoot.querySelector('[data-bind="count"]');
        if (countEl) {
            countEl.textContent = count;
            
            // Animate on change
            if (this.prevCount !== null && this.prevCount !== count) {
                countEl.classList.add('bump');
                setTimeout(() => countEl.classList.remove('bump'), 150);
            }
            this.prevCount = count;
        }
        
        // Update CSS custom property for color
        const containerEl = this.shadowRoot.querySelector('.container');
        if (containerEl) {
            containerEl.style.setProperty('--color', color);
            containerEl.style.borderColor = color;
            containerEl.style.background = `linear-gradient(135deg, ${color}22, ${color}11)`;
        }
        
        if (countEl) {
            countEl.style.color = color;
        }
    }
    
    setProps(newProps) {
        Object.assign(this.props, newProps);
        this.update();
    }
    
    unmount() {
        this.container.innerHTML = '';
        this.shadowRoot = null;
    }
}

// Register with PonySDK
window.registerTemplateComponent('template-counter', {
    renderer: TemplateCounterRenderer
});

console.log('[PComponent] 📄 Template counter registered (HTML Template + Shadow DOM)');
