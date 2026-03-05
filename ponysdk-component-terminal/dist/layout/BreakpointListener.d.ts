/**
 * Listens for viewport breakpoint changes using window.matchMedia
 * and notifies the server via EventBridge.
 *
 * Requirements: 4.3 - WHEN the screen width crosses a Breakpoint,
 *   THE Layout_Manager SHALL reorganize child components
 * Requirements: 4.6 - WHEN hideOnMobile=true, THE Layout_Manager SHALL
 *   hide the component at the mobile breakpoint
 */
import type { EventBridge } from '../EventBridge.js';
export type BreakpointName = 'mobile' | 'tablet' | 'desktop';
/**
 * Detects viewport breakpoint changes via matchMedia and dispatches
 * breakpoint-change events to the server through EventBridge.
 */
export declare class BreakpointListener {
    private eventBridge;
    private objectId;
    private currentBreakpoint;
    private mobileQuery;
    private tabletQuery;
    private desktopQuery;
    private mobileHandler;
    private tabletHandler;
    private desktopHandler;
    constructor(eventBridge: EventBridge, objectId: number);
    /**
     * Returns the currently active breakpoint.
     */
    getCurrentBreakpoint(): BreakpointName;
    /**
     * Removes all media query listeners. Call when disposing.
     */
    destroy(): void;
    private setupMediaQueries;
    private detectCurrentBreakpoint;
    private onBreakpointChange;
}
//# sourceMappingURL=BreakpointListener.d.ts.map