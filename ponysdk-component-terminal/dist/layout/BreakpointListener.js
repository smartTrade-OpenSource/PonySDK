/**
 * Listens for viewport breakpoint changes using window.matchMedia
 * and notifies the server via EventBridge.
 *
 * Requirements: 4.3 - WHEN the screen width crosses a Breakpoint,
 *   THE Layout_Manager SHALL reorganize child components
 * Requirements: 4.6 - WHEN hideOnMobile=true, THE Layout_Manager SHALL
 *   hide the component at the mobile breakpoint
 */
/**
 * Detects viewport breakpoint changes via matchMedia and dispatches
 * breakpoint-change events to the server through EventBridge.
 */
export class BreakpointListener {
    constructor(eventBridge, objectId) {
        this.eventBridge = eventBridge;
        this.objectId = objectId;
        this.mobileQuery = window.matchMedia('(max-width: 599px)');
        this.tabletQuery = window.matchMedia('(min-width: 600px) and (max-width: 1023px)');
        this.desktopQuery = window.matchMedia('(min-width: 1024px)');
        this.currentBreakpoint = this.detectCurrentBreakpoint();
        this.mobileHandler = (e) => { if (e.matches)
            this.onBreakpointChange('mobile'); };
        this.tabletHandler = (e) => { if (e.matches)
            this.onBreakpointChange('tablet'); };
        this.desktopHandler = (e) => { if (e.matches)
            this.onBreakpointChange('desktop'); };
        this.setupMediaQueries();
    }
    /**
     * Returns the currently active breakpoint.
     */
    getCurrentBreakpoint() {
        return this.currentBreakpoint;
    }
    /**
     * Removes all media query listeners. Call when disposing.
     */
    destroy() {
        this.mobileQuery.removeEventListener('change', this.mobileHandler);
        this.tabletQuery.removeEventListener('change', this.tabletHandler);
        this.desktopQuery.removeEventListener('change', this.desktopHandler);
    }
    setupMediaQueries() {
        this.mobileQuery.addEventListener('change', this.mobileHandler);
        this.tabletQuery.addEventListener('change', this.tabletHandler);
        this.desktopQuery.addEventListener('change', this.desktopHandler);
    }
    detectCurrentBreakpoint() {
        if (this.mobileQuery.matches)
            return 'mobile';
        if (this.tabletQuery.matches)
            return 'tablet';
        return 'desktop';
    }
    onBreakpointChange(newBreakpoint) {
        if (newBreakpoint === this.currentBreakpoint)
            return;
        this.currentBreakpoint = newBreakpoint;
        this.eventBridge.dispatch(this.objectId, 'breakpoint-change', {
            breakpoint: newBreakpoint,
        });
    }
}
//# sourceMappingURL=BreakpointListener.js.map