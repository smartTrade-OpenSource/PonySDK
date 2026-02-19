/**
 * TradingGrid component registration for PonySDK ComponentTerminal.
 */
import { TradingGrid } from './TradingGrid';
import type { ComponentFactory } from '../../../../../../../ponysdk-component-terminal/src/types';

// Component signature - must match PTradingGrid.getComponentSignature()
export const SIGNATURE = 'trading-grid';

/**
 * Factory function that creates a ComponentFactory for TradingGrid.
 * This matches the ComponentTerminal registration pattern.
 * 
 * Note: React is expected to be available as window.React from CDN
 */
export function createFactory(container: HTMLElement): ComponentFactory {
    return {
        getContainer: () => container,
        getReactComponent: () => TradingGrid,
        initialProps: { stocks: [] }
    };
}
