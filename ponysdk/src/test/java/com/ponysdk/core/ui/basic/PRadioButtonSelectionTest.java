package com.ponysdk.core.ui.basic;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.test.PSuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PRadioButtonSelectionTest extends PSuite {

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyListThrowsException() {
        Element.newPRadioButtonSelection(Collections.emptyList());
    }

    @Test
    public void testInitWithTwoButtons() {
        PRadioButton radio1 = Element.newPRadioButton("Option 1");
        PRadioButton radio2 = Element.newPRadioButton("Option 2");
        
        PWindow.getMain().add(radio1);
        PWindow.getMain().add(radio2);
        
        PRadioButtonSelection selection = Element.newPRadioButtonSelection(Arrays.asList(radio1, radio2));
        
        assertNotNull(selection.getValue());
        assertEquals(radio1, selection.getValue());
        assertEquals(PCheckBoxState.CHECKED, radio1.getState());
        assertEquals(PCheckBoxState.UNCHECKED, radio2.getState());
    }

    @Test
    public void testSetValue() {
        PRadioButton radio1 = Element.newPRadioButton("Option 1");
        PRadioButton radio2 = Element.newPRadioButton("Option 2");
        
        PWindow.getMain().add(radio1);
        PWindow.getMain().add(radio2);
        
        PRadioButtonSelection selection = Element.newPRadioButtonSelection(Arrays.asList(radio1, radio2));
        
        selection.setValue(radio2);
        
        assertEquals(radio2, selection.getValue());
        assertEquals(PCheckBoxState.UNCHECKED, radio1.getState());
        assertEquals(PCheckBoxState.CHECKED, radio2.getState());
    }

    @Test
    public void testSetValueDoesNotFireHandler() {
        PRadioButton radio1 = Element.newPRadioButton("Option 1");
        PRadioButton radio2 = Element.newPRadioButton("Option 2");
        
        PWindow.getMain().add(radio1);
        PWindow.getMain().add(radio2);
        
        PRadioButtonSelection selection = Element.newPRadioButtonSelection(Arrays.asList(radio1, radio2));
        
        AtomicInteger handlerFiredCount = new AtomicInteger(0);
        
        PValueChangeHandler<PRadioButton> handler = event -> handlerFiredCount.incrementAndGet();
        
        selection.addValueChangeHandler(handler);
        assertEquals(1, selection.getValueChangeHandlers().size());
        
        selection.setValue(radio2);
        
        assertEquals(0, handlerFiredCount.get());
        assertEquals(radio2, selection.getValue());
    }

    @Test
    public void testRemoveValueChangeHandler() {
        PRadioButton radio1 = Element.newPRadioButton("Option 1");
        PRadioButton radio2 = Element.newPRadioButton("Option 2");
        
        PWindow.getMain().add(radio1);
        PWindow.getMain().add(radio2);
        
        PRadioButtonSelection selection = Element.newPRadioButtonSelection(Arrays.asList(radio1, radio2));
        
        PValueChangeHandler<PRadioButton> handler = event -> {};
        
        selection.addValueChangeHandler(handler);
        assertEquals(1, selection.getValueChangeHandlers().size());
        
        assertTrue(selection.removeValueChangeHandler(handler));
        assertEquals(0, selection.getValueChangeHandlers().size());
        
        assertFalse(selection.removeValueChangeHandler(handler));
    }

    @Test
    public void testMultipleButtons() {
        PRadioButton radio1 = Element.newPRadioButton("Option 1");
        PRadioButton radio2 = Element.newPRadioButton("Option 2");
        PRadioButton radio3 = Element.newPRadioButton("Option 3");
        
        PWindow.getMain().add(radio1);
        PWindow.getMain().add(radio2);
        PWindow.getMain().add(radio3);
        
        PRadioButtonSelection selection = Element.newPRadioButtonSelection(Arrays.asList(radio1, radio2, radio3));
        
        assertEquals(radio1, selection.getValue());
        
        selection.setValue(radio3);
        assertEquals(radio3, selection.getValue());
        assertEquals(PCheckBoxState.UNCHECKED, radio1.getState());
        assertEquals(PCheckBoxState.UNCHECKED, radio2.getState());
        assertEquals(PCheckBoxState.CHECKED, radio3.getState());
        
        selection.setValue(radio2);
        assertEquals(radio2, selection.getValue());
        assertEquals(PCheckBoxState.UNCHECKED, radio1.getState());
        assertEquals(PCheckBoxState.CHECKED, radio2.getState());
        assertEquals(PCheckBoxState.UNCHECKED, radio3.getState());
    }

    @Test
    public void testAddAndRemoveHandlers() {
        PRadioButton radio1 = Element.newPRadioButton("Option 1");
        PRadioButton radio2 = Element.newPRadioButton("Option 2");
        
        PWindow.getMain().add(radio1);
        PWindow.getMain().add(radio2);
        
        PRadioButtonSelection selection = Element.newPRadioButtonSelection(Arrays.asList(radio1, radio2));
        
        PValueChangeHandler<PRadioButton> handler1 = event -> {};
        PValueChangeHandler<PRadioButton> handler2 = event -> {};
        
        selection.addValueChangeHandler(handler1);
        selection.addValueChangeHandler(handler2);
        assertEquals(2, selection.getValueChangeHandlers().size());
        
        assertTrue(selection.removeValueChangeHandler(handler1));
        assertEquals(1, selection.getValueChangeHandlers().size());
        
        assertFalse(selection.removeValueChangeHandler(handler1));
        assertEquals(1, selection.getValueChangeHandlers().size());
    }

    @Test
    public void testGetValueChangeHandlersWhenEmpty() {
        PRadioButton radio1 = Element.newPRadioButton("Option 1");
        PRadioButton radio2 = Element.newPRadioButton("Option 2");
        
        PWindow.getMain().add(radio1);
        PWindow.getMain().add(radio2);
        
        PRadioButtonSelection selection = Element.newPRadioButtonSelection(Arrays.asList(radio1, radio2));
        
        assertNotNull(selection.getValueChangeHandlers());
        assertEquals(0, selection.getValueChangeHandlers().size());
    }

    @Test
    public void testSetValueBeforeInitialization() {
        PRadioButton radio1 = Element.newPRadioButton("Option 1");
        PRadioButton radio2 = Element.newPRadioButton("Option 2");
        
        PRadioButtonSelection selection = Element.newPRadioButtonSelection(Arrays.asList(radio1, radio2));
        
        selection.setValue(radio2);
        assertEquals(radio2, selection.getValue());
        
        PWindow.getMain().add(radio1);
        PWindow.getMain().add(radio2);
        
        assertEquals(radio2, selection.getValue());
        assertEquals(PCheckBoxState.UNCHECKED, radio1.getState());
        assertEquals(PCheckBoxState.CHECKED, radio2.getState());
    }

    @Test
    public void testDefaultSelectionAfterInitialization() {
        PRadioButton radio1 = Element.newPRadioButton("Option 1");
        PRadioButton radio2 = Element.newPRadioButton("Option 2");
        
        PRadioButtonSelection selection = Element.newPRadioButtonSelection(Arrays.asList(radio1, radio2));
        
        PWindow.getMain().add(radio1);
        PWindow.getMain().add(radio2);
        
        assertEquals(radio1, selection.getValue());
        assertEquals(PCheckBoxState.CHECKED, radio1.getState());
        assertEquals(PCheckBoxState.UNCHECKED, radio2.getState());
    }

    @Test
    public void testPartialInitialization() {
        PRadioButton radio1 = Element.newPRadioButton("Option 1");
        PRadioButton radio2 = Element.newPRadioButton("Option 2");
        
        PWindow.getMain().add(radio1);
        
        PRadioButtonSelection selection = Element.newPRadioButtonSelection(Arrays.asList(radio1, radio2));
        
        selection.setValue(radio2);
        assertEquals(radio2, selection.getValue());
        
        PWindow.getMain().add(radio2);
        
        assertEquals(radio2, selection.getValue());
        assertEquals(PCheckBoxState.UNCHECKED, radio1.getState());
        assertEquals(PCheckBoxState.CHECKED, radio2.getState());
    }

    @Test
    public void testRemoveHandlerWithoutAdd() {
        PRadioButton radio1 = Element.newPRadioButton("Option 1");
        PRadioButton radio2 = Element.newPRadioButton("Option 2");
        
        PWindow.getMain().add(radio1);
        PWindow.getMain().add(radio2);
        
        PRadioButtonSelection selection = Element.newPRadioButtonSelection(Arrays.asList(radio1, radio2));
        
        PValueChangeHandler<PRadioButton> handler = event -> {};
        
        assertFalse(selection.removeValueChangeHandler(handler));
        assertEquals(0, selection.getValueChangeHandlers().size());
    }

    @Test
    public void testCreationWithUninitializedRadios() {
        PRadioButton radio1 = Element.newPRadioButton("Option 1");
        PRadioButton radio2 = Element.newPRadioButton("Option 2");
        
        PRadioButtonSelection selection = Element.newPRadioButtonSelection(Arrays.asList(radio1, radio2));
        
        assertNull(selection.getValue());

        PWindow.getMain().add(radio1);
        PWindow.getMain().add(radio2);
        
        assertEquals(radio1, selection.getValue());
        assertEquals(PCheckBoxState.CHECKED, radio1.getState());
        assertEquals(PCheckBoxState.UNCHECKED, radio2.getState());
    }

    @Test
    public void testUserInteractionBeforeInitialization() {
        PRadioButton radio1 = Element.newPRadioButton("Option 1");
        PRadioButton radio2 = Element.newPRadioButton("Option 2");
        
        PRadioButtonSelection selection = Element.newPRadioButtonSelection(Arrays.asList(radio1, radio2));
        
        AtomicInteger handlerFiredCount = new AtomicInteger(0);
        selection.addValueChangeHandler(event -> handlerFiredCount.incrementAndGet());
        
        radio2.getValueChangeHandlers().forEach(handler -> 
            handler.onValueChange(new com.ponysdk.core.ui.basic.event.PValueChangeEvent<>(radio2, true)));
        
        assertEquals(0, handlerFiredCount.get());
        
        PWindow.getMain().add(radio1);
        PWindow.getMain().add(radio2);
        
        assertEquals(radio1, selection.getValue());
    }

    @Test
    public void testUserInteractionAfterInitialization() {
        PRadioButton radio1 = Element.newPRadioButton("Option 1");
        PRadioButton radio2 = Element.newPRadioButton("Option 2");
        
        PWindow.getMain().add(radio1);
        PWindow.getMain().add(radio2);
        
        PRadioButtonSelection selection = Element.newPRadioButtonSelection(Arrays.asList(radio1, radio2));
        
        AtomicInteger handlerFiredCount = new AtomicInteger(0);
        AtomicReference<PRadioButton> selectedValue = new AtomicReference<>();
        
        selection.addValueChangeHandler(event -> {
            handlerFiredCount.incrementAndGet();
            selectedValue.set(event.getData());
        });
        
        radio2.getValueChangeHandlers().forEach(handler -> 
            handler.onValueChange(new com.ponysdk.core.ui.basic.event.PValueChangeEvent<>(radio2, true)));
        
        assertEquals(1, handlerFiredCount.get());
        assertEquals(radio2, selectedValue.get());
        assertEquals(radio2, selection.getValue());
    }

    @Test
    public void testUserInteractionDoesNotFireIfSameRadio() {
        PRadioButton radio1 = Element.newPRadioButton("Option 1");
        PRadioButton radio2 = Element.newPRadioButton("Option 2");
        
        PWindow.getMain().add(radio1);
        PWindow.getMain().add(radio2);
        
        PRadioButtonSelection selection = Element.newPRadioButtonSelection(Arrays.asList(radio1, radio2));
        
        AtomicInteger handlerFiredCount = new AtomicInteger(0);
        selection.addValueChangeHandler(event -> handlerFiredCount.incrementAndGet());
        
        radio1.getValueChangeHandlers().forEach(handler -> 
            handler.onValueChange(new com.ponysdk.core.ui.basic.event.PValueChangeEvent<>(radio1, true)));
        
        assertEquals(0, handlerFiredCount.get());
    }
}
