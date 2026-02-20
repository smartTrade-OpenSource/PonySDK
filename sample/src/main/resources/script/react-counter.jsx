/**
 * React Counter Component
 * Real React implementation using JSX
 */

// React Counter Component
const ReactCounter = ({ label, count, color, onIncrement, onDecrement }) => {
    return React.createElement('div', {
        style: {
            display: 'inline-flex',
            flexDirection: 'column',
            alignItems: 'center',
            padding: '1.5rem',
            border: `3px solid ${color}`,
            borderRadius: '12px',
            minWidth: '180px',
            background: `linear-gradient(135deg, ${color}22, ${color}11)`,
            fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
        }
    }, [
        // Badge
        React.createElement('div', {
            key: 'badge',
            style: {
                background: '#61dafb',
                color: '#282c34',
                padding: '4px 12px',
                borderRadius: '20px',
                fontSize: '11px',
                fontWeight: 'bold',
                marginBottom: '12px',
                display: 'flex',
                alignItems: 'center',
                gap: '6px'
            }
        }, [
            React.createElement('span', { key: 'icon' }, '⚛️'),
            'REACT'
        ]),
        // Label
        React.createElement('div', {
            key: 'label',
            style: {
                fontSize: '0.875rem',
                fontWeight: '600',
                textTransform: 'uppercase',
                color: '#666',
                marginBottom: '0.5rem'
            }
        }, label),
        // Count
        React.createElement('div', {
            key: 'count',
            style: {
                fontSize: '3rem',
                fontWeight: '700',
                color: color,
                marginBottom: '1rem',
                lineHeight: 1
            }
        }, count),
        // Buttons
        React.createElement('div', {
            key: 'buttons',
            style: { display: 'flex', gap: '0.5rem' }
        }, [
            React.createElement('button', {
                key: 'dec',
                onClick: onDecrement,
                style: {
                    width: '44px',
                    height: '44px',
                    border: 'none',
                    borderRadius: '10px',
                    fontSize: '1.5rem',
                    background: '#e74c3c',
                    color: 'white',
                    cursor: 'pointer',
                    transition: 'transform 0.1s',
                    boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
                },
                onMouseDown: (e) => e.target.style.transform = 'scale(0.95)',
                onMouseUp: (e) => e.target.style.transform = 'scale(1)'
            }, '−'),
            React.createElement('button', {
                key: 'inc',
                onClick: onIncrement,
                style: {
                    width: '44px',
                    height: '44px',
                    border: 'none',
                    borderRadius: '10px',
                    fontSize: '1.5rem',
                    background: '#2ecc71',
                    color: 'white',
                    cursor: 'pointer',
                    transition: 'transform 0.1s',
                    boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
                },
                onMouseDown: (e) => e.target.style.transform = 'scale(0.95)',
                onMouseUp: (e) => e.target.style.transform = 'scale(1)'
            }, '+')
        ])
    ]);
};

// Register with PonySDK
window.registerReactComponent('sample-counter', function(container) {
    let currentProps = { label: '', count: 0, color: '#61dafb' };
    let eventCallback = null;
    let root = null;

    function render() {
        const element = React.createElement(ReactCounter, {
            ...currentProps,
            onIncrement: () => eventCallback && eventCallback('increment', {}),
            onDecrement: () => eventCallback && eventCallback('decrement', {})
        });
        
        if (!root) {
            root = ReactDOM.createRoot(container);
        }
        root.render(element);
    }

    return {
        getReactComponent: function() {
            return {
                setEventCallback: function(cb) { eventCallback = cb; },
                updateProps: function(newProps) { 
                    currentProps = { ...currentProps, ...newProps }; 
                    render(); 
                },
                mount: render,
                unmount: function() { 
                    if (root) {
                        root.unmount();
                        root = null;
                    }
                }
            };
        }
    };
});

console.log('[PComponent] ⚛️ React counter registered');
