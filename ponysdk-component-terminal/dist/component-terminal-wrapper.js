/**
 * Wrapper for component-terminal.browser.js that exposes it to window.ComponentTerminal
 * 
 * This file should be loaded AFTER component-terminal.browser.js
 */
(function() {
    'use strict';
    
    // The component-terminal.browser.js bundle is an IIFE that returns the exports
    // We need to capture that return value and expose it to window
    
    // Check if ComponentTerminal was already loaded by the IIFE
    if (typeof ComponentTerminal !== 'undefined') {
        window.ComponentTerminal = ComponentTerminal;
        console.log('ComponentTerminal exposed to window');
    } else {
        console.error('ComponentTerminal not found - ensure component-terminal.browser.js loaded first');
    }
})();
