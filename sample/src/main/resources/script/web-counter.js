/**
 * Web Component Counter
 * Native Custom Element implementation using Shadow DOM
 */

class PonyCounter extends HTMLElement {
    static get observedAttributes() {
        return ['label', 'count', 'color'];
    }
    
    constructor() {
        super();
        this.attachShadow({ mode: 'open' });
        this._eventCallback = null;
        this._props = { label: '', count: 0, color: '#f7df1e' };
    }
    
    connectedCallback() {
        this.render();
    }
    
    attributeChangedCallback(name, oldValue, newValue) {
        if (oldValue !== newValue) {
            this._props[name] = name === 'count' ? parseInt(newValue) : newValue;
            this.updateDisplay();
        }
    }
    
    set eventCallback(cb) {
        this._eventCallback = cb;
    }
    
    setProps(props) {
        Object.assign(this._props, props);
        this.updateDisplay();
    }
    
    updateDisplay() {
        const { label, count, color } = this._props;
        
        const countEl = this.shadowRoot.querySelector('.wc-count');
        if (countEl) {
            countEl.textContent = count;
            countEl.style.color = color;
        }
        
        const labelEl = this.shadowRoot.querySelector('.wc-label');
        if (labelEl) labelEl.textContent = label;
        
        const container = this.shadowRoot.querySelector('.wc-container');
        if (container) {
            container.style.borderColor = color;
            container.style.background = `linear-gradient(135deg, ${color}22, ${color}11)`;
        }
    }
    
    render() {
        const { label, count, color } = this._props;
        
        this.shadowRoot.innerHTML = `
            <style>
                :host {
                    display: inline-block;
                }
                
                .wc-container {
                    display: inline-flex;
                    flex-direction: column;
                    align-items: center;
                    padding: 1.5rem;
                    border: 3px solid ${color};
                    border-radius: 12px;
                    min-width: 180px;
                    background: linear-gradient(135deg, ${color}22, ${color}11);
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                }
                
                .wc-badge {
                    background: #f7df1e;
                    color: #333;
                    padding: 4px 12px;
                    border-radius: 20px;
                    font-size: 11px;
                    font-weight: bold;
                    margin-bottom: 12px;
                    display: flex;
                    align-items: center;
                    gap: 6px;
                }
                
                .wc-label {
                    font-size: 0.875rem;
                    font-weight: 600;
                    text-transform: uppercase;
                    color: #666;
                    margin-bottom: 0.5rem;
                }
                
                .wc-count {
                    font-size: 3rem;
                    font-weight: 700;
                    color: ${color};
                    margin-bottom: 1rem;
                    line-height: 1;
                }
                
                .wc-buttons {
                    display: flex;
                    gap: 0.5rem;
                }
                
                .wc-btn {
                    width: 44px;
                    height: 44px;
                    border: none;
                    border-radius: 10px;
                    font-size: 1.5rem;
                    color: white;
                    cursor: pointer;
                    transition: transform 0.1s;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.2);
                }
                
                .wc-btn:active {
                    transform: scale(0.95);
                }
                
                .wc-btn-dec { background: #e74c3c; }
                .wc-btn-inc { background: #2ecc71; }
            </style>
            
            <div class="wc-container">
                <div class="wc-badge">
                    <span>🌐</span>
                    WEB COMPONENT
                </div>
                <div class="wc-label">${label}</div>
                <div class="wc-count">${count}</div>
                <div class="wc-buttons">
                    <button class="wc-btn wc-btn-dec">−</button>
                    <button class="wc-btn wc-btn-inc">+</button>
                </div>
            </div>
        `;
        
        // Event listeners
        this.shadowRoot.querySelector('.wc-btn-dec').addEventListener('click', () => {
            this._eventCallback && this._eventCallback('decrement', {});
        });
        
        this.shadowRoot.querySelector('.wc-btn-inc').addEventListener('click', () => {
            this._eventCallback && this._eventCallback('increment', {});
        });
    }
}

// Register Custom Element
if (!customElements.get('pony-counter')) {
    customElements.define('pony-counter', PonyCounter);
}

// Register with PonySDK
window.registerWebComponent('web-counter', function(container) {
    let element = null;
    let eventCallback = null;

    return {
        getWebComponent: function() {
            return {
                setEventCallback: function(cb) { 
                    eventCallback = cb;
                    if (element) element.eventCallback = cb;
                },
                updateProps: function(newProps) {
                    if (element) {
                        element.setProps(newProps);
                    }
                },
                mount: function() {
                    element = document.createElement('pony-counter');
                    element.eventCallback = eventCallback;
                    container.appendChild(element);
                },
                unmount: function() {
                    if (element && element.parentNode) {
                        element.parentNode.removeChild(element);
                        element = null;
                    }
                }
            };
        }
    };
});

console.log('[PComponent] 🌐 Web Component counter registered');
