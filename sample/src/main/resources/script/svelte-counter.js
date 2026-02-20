/**
 * Svelte Counter Component - Pre-compiled
 * 
 * Source: ./Counter.svelte
 * 
 * Simplified Svelte-style component with fine-grained reactivity.
 */

class SvelteCounter {
    constructor(options) {
        this.target = options.target;
        this.props = { label: '', count: 0, color: '#ff3e00', ...options.props };
        this.callbacks = {};
        this.mounted = false;
        this.refs = {};
    }
    
    $on(event, callback) {
        if (!this.callbacks[event]) this.callbacks[event] = [];
        this.callbacks[event].push(callback);
    }
    
    $set(newProps) {
        const oldProps = { ...this.props };
        Object.assign(this.props, newProps);
        
        if (this.mounted) {
            // Fine-grained updates (Svelte style)
            if (oldProps.label !== this.props.label) {
                this.refs.label.textContent = this.props.label;
            }
            if (oldProps.count !== this.props.count) {
                this.refs.count.textContent = this.props.count;
                // Animate
                this.refs.count.style.transform = 'scale(1.2)';
                setTimeout(() => this.refs.count.style.transform = 'scale(1)', 150);
            }
            if (oldProps.color !== this.props.color) {
                this.updateColor();
            }
        }
    }
    
    updateColor() {
        const { color } = this.props;
        this.refs.container.style.borderColor = color;
        this.refs.container.style.background = `linear-gradient(135deg, ${color}22, ${color}11)`;
        this.refs.count.style.color = color;
    }
    
    dispatch(event) {
        (this.callbacks[event] || []).forEach(cb => cb());
    }
    
    mount() {
        const { label, count, color } = this.props;
        
        this.target.innerHTML = `
            <div class="svelte-container" style="
                display: inline-flex;
                flex-direction: column;
                align-items: center;
                padding: 1.5rem;
                border: 3px solid ${color};
                border-radius: 12px;
                min-width: 180px;
                background: linear-gradient(135deg, ${color}22, ${color}11);
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                transition: all 0.3s ease;
            ">
                <div style="
                    background: linear-gradient(135deg, #ff3e00, #ff6b35);
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
                ">🔥 SVELTE</div>
                <div class="svelte-label" style="
                    font-size: 0.875rem;
                    font-weight: 600;
                    text-transform: uppercase;
                    color: #666;
                    margin-bottom: 0.5rem;
                ">${label}</div>
                <div class="svelte-count" style="
                    font-size: 3rem;
                    font-weight: 700;
                    color: ${color};
                    margin-bottom: 1rem;
                    line-height: 1;
                    transition: transform 0.15s ease;
                ">${count}</div>
                <div style="display: flex; gap: 0.5rem;">
                    <button class="svelte-dec" style="
                        width: 44px; height: 44px; border: none; border-radius: 10px;
                        font-size: 1.5rem; background: linear-gradient(135deg, #e74c3c, #c0392b);
                        color: white; cursor: pointer; box-shadow: 0 2px 4px rgba(0,0,0,0.2);
                    ">−</button>
                    <button class="svelte-inc" style="
                        width: 44px; height: 44px; border: none; border-radius: 10px;
                        font-size: 1.5rem; background: linear-gradient(135deg, #2ecc71, #27ae60);
                        color: white; cursor: pointer; box-shadow: 0 2px 4px rgba(0,0,0,0.2);
                    ">+</button>
                </div>
            </div>
        `;
        
        // Store refs for fine-grained updates
        this.refs.container = this.target.querySelector('.svelte-container');
        this.refs.label = this.target.querySelector('.svelte-label');
        this.refs.count = this.target.querySelector('.svelte-count');
        
        // Bind events
        this.target.querySelector('.svelte-dec').onclick = () => this.dispatch('decrement');
        this.target.querySelector('.svelte-inc').onclick = () => this.dispatch('increment');
        
        this.mounted = true;
    }
    
    $destroy() {
        this.target.innerHTML = '';
        this.mounted = false;
    }
}

// Register with PonySDK
window.registerSvelteComponent('svelte-counter', function(container) {
    let component = null;
    let eventCallback = null;

    return {
        getSvelteComponent: function() {
            return {
                setEventCallback: function(cb) { 
                    eventCallback = cb;
                },
                updateProps: function(newProps) {
                    if (component) {
                        component.$set(newProps);
                    }
                },
                mount: function() {
                    component = new SvelteCounter({
                        target: container,
                        props: { label: '', count: 0, color: '#ff3e00' }
                    });
                    component.$on('increment', () => eventCallback && eventCallback('increment', {}));
                    component.$on('decrement', () => eventCallback && eventCallback('decrement', {}));
                    component.mount();
                },
                unmount: function() {
                    if (component) {
                        component.$destroy();
                        component = null;
                    }
                }
            };
        }
    };
});

console.log('[PComponent] 🔥 Svelte counter registered');
