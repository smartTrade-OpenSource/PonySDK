/**
 * Vue 3 Counter Component
 * Real Vue 3 implementation using Composition API and render functions
 */

const { createApp, ref, computed, h } = Vue;

// Vue Counter using Composition API
const VueCounterComponent = {
    props: {
        label: { type: String, default: '' },
        count: { type: Number, default: 0 },
        color: { type: String, default: '#42b883' }
    },
    
    emits: ['increment', 'decrement'],
    
    setup(props, { emit }) {
        // Reactive refs for animations
        const isAnimating = ref(false);
        
        // Computed styles
        const containerStyle = computed(() => ({
            display: 'inline-flex',
            flexDirection: 'column',
            alignItems: 'center',
            padding: '1.5rem',
            border: `3px solid ${props.color}`,
            borderRadius: '12px',
            minWidth: '180px',
            background: `linear-gradient(135deg, ${props.color}22, ${props.color}11)`,
            fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
            transition: 'all 0.3s ease'
        }));
        
        const countStyle = computed(() => ({
            fontSize: '3rem',
            fontWeight: '700',
            color: props.color,
            marginBottom: '1rem',
            lineHeight: 1,
            transform: isAnimating.value ? 'scale(1.2)' : 'scale(1)',
            transition: 'transform 0.15s ease'
        }));
        
        // Methods
        const handleIncrement = () => {
            isAnimating.value = true;
            setTimeout(() => isAnimating.value = false, 150);
            emit('increment');
        };
        
        const handleDecrement = () => {
            isAnimating.value = true;
            setTimeout(() => isAnimating.value = false, 150);
            emit('decrement');
        };
        
        return { containerStyle, countStyle, handleIncrement, handleDecrement };
    },
    
    render() {
        return h('div', { style: this.containerStyle }, [
            // Badge
            h('div', {
                style: {
                    background: 'linear-gradient(135deg, #42b883, #35495e)',
                    color: 'white',
                    padding: '4px 12px',
                    borderRadius: '20px',
                    fontSize: '11px',
                    fontWeight: 'bold',
                    marginBottom: '12px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
                }
            }, [h('span', '💚'), ' VUE 3']),
            
            // Label
            h('div', {
                style: {
                    fontSize: '0.875rem',
                    fontWeight: '600',
                    textTransform: 'uppercase',
                    color: '#666',
                    marginBottom: '0.5rem'
                }
            }, this.label),
            
            // Count with animation
            h('div', { style: this.countStyle }, this.count),
            
            // Buttons
            h('div', { style: { display: 'flex', gap: '0.5rem' } }, [
                h('button', {
                    onClick: this.handleDecrement,
                    style: {
                        width: '44px',
                        height: '44px',
                        border: 'none',
                        borderRadius: '10px',
                        fontSize: '1.5rem',
                        background: 'linear-gradient(135deg, #e74c3c, #c0392b)',
                        color: 'white',
                        cursor: 'pointer',
                        boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
                    }
                }, '−'),
                h('button', {
                    onClick: this.handleIncrement,
                    style: {
                        width: '44px',
                        height: '44px',
                        border: 'none',
                        borderRadius: '10px',
                        fontSize: '1.5rem',
                        background: 'linear-gradient(135deg, #2ecc71, #27ae60)',
                        color: 'white',
                        cursor: 'pointer',
                        boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
                    }
                }, '+')
            ])
        ]);
    }
};

// Register with PonySDK
window.registerVueComponent('vue-counter', function(container) {
    let app = null;
    let eventCallback = null;
    let currentProps = { label: '', count: 0, color: '#42b883' };

    return {
        getVueComponent: function() {
            return {
                setEventCallback: function(cb) { 
                    eventCallback = cb; 
                },
                
                updateProps: function(newProps) {
                    currentProps = { ...currentProps, ...newProps };
                    
                    if (app) {
                        // Unmount and remount with new props (Vue 3 way for external props)
                        app.unmount();
                        mountApp();
                    }
                },
                
                mount: function() {
                    mountApp();
                },
                
                unmount: function() {
                    if (app) {
                        app.unmount();
                        app = null;
                    }
                }
            };
        }
    };
    
    function mountApp() {
        app = createApp({
            render() {
                return h(VueCounterComponent, {
                    ...currentProps,
                    onIncrement: () => eventCallback && eventCallback('increment', {}),
                    onDecrement: () => eventCallback && eventCallback('decrement', {})
                });
            }
        });
        app.mount(container);
    }
});

console.log('[PComponent] 💚 Vue 3 counter registered (Composition API)');
