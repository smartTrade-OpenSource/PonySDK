import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class UIContextImplTest {

    private UIContextImpl uiContext;
    private String sessionId;

    @Mock
    private UIComponent mockComponent;
    
    @Before
    public void setUp() {
        sessionId = UUID.randomUUID().toString();
        uiContext = new UIContextImpl(sessionId);
    }

    @Test
    public void testGetSessionId() {
        assertEquals("Session ID should match the one provided in constructor", 
            sessionId, uiContext.getSessionID());
    }

    @Test
    public void testRegisterAndGetComponent() {
        String componentId = "test-component-1";
        uiContext.registerComponent(componentId, mockComponent);
        
        assertEquals("Should return the registered component",
            mockComponent, uiContext.getComponent(componentId));
    }

    @Test
    public void testGetNonExistentComponent() {
        assertNull("Should return null for non-existent component", 
            uiContext.getComponent("non-existent"));
    }

    @Test
    public void testUpdateUIState() {
        UIStateUpdate mockUpdate = mock(UIStateUpdate.class);
        uiContext.updateUIState(mockUpdate);
        
        // Verify the state was updated correctly
        // This will depend on your actual implementation
        // You might need to add getter methods to verify the state
    }

    @Test
    public void testMultipleComponentRegistration() {
        String componentId = "test-component-1";
        UIComponent newMockComponent = mock(UIComponent.class);
        
        // Register first component
        uiContext.registerComponent(componentId, mockComponent);
        
        // Register second component with same ID
        uiContext.registerComponent(componentId, newMockComponent);
        
        // Should return the most recently registered component
        assertEquals("Should return the most recent component",
            newMockComponent, uiContext.getComponent(componentId));
    }

    @Test
    public void testRegisterNullComponent() {
        String componentId = "test-component-1";
        
        // Test registering null component
        uiContext.registerComponent(componentId, null);
        assertNull("Should allow null component registration",
            uiContext.getComponent(componentId));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterWithNullId() {
        uiContext.registerComponent(null, mockComponent);
    }

    @Test
    public void testContextDestruction() {
        // Assuming there's a destroy method
        String componentId = "test-component-1";
        uiContext.registerComponent(componentId, mockComponent);
        
        uiContext.destroy();
        
        assertNull("Should return null after destruction",
            uiContext.getComponent(componentId));
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicBoolean failed = new AtomicBoolean(false);
        
        for (int i = 0; i < threadCount; i++) {
            final String componentId = "component-" + i;
            final UIComponent component = mock(UIComponent.class);
            
            new Thread(() -> {
                try {
                    uiContext.registerComponent(componentId, component);
                    assertEquals(component, uiContext.getComponent(componentId));
                } catch (Exception e) {
                    failed.set(true);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await(5, TimeUnit.SECONDS);
        assertFalse("Concurrent access test failed", failed.get());
    }
}
