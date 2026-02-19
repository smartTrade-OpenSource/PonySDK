/**
 * Registration script for TradingGrid component.
 * This file is bundled separately and loaded in the HTML.
 */

import { SIGNATURE, createFactory } from './index';

// Register with ComponentTerminal via the bridge
declare global {
    interface Window {
        registerReactComponent: (signature: string, factory: any) => void;
    }
}

// Register the component
if (typeof window !== 'undefined' && window.registerReactComponent) {
    window.registerReactComponent(SIGNATURE, createFactory);
    console.log(`Registered ${SIGNATURE} component`);
} else {
    console.error('registerReactComponent not available - ensure component-bridge.js is loaded first');
}