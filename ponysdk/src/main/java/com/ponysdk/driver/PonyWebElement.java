/*
 * Copyright (c) 2018 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.driver;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.json.Json;
import javax.json.JsonValue;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.model.WidgetType;

public class PonyWebElement implements WebElement {

    private final PonySDKWebDriver ponySDKWebDriver;

    final Map<String, String> attributes = new ConcurrentHashMap<>();
    final Collection<String> styles = new CopyOnWriteArrayList<>();
    final List<PonyWebElement> children = new CopyOnWriteArrayList<>();
    final PonySearchContext context = new PonySearchContext(children, true);
    final int objectID;
    final WidgetType widgetType;
    volatile PonyWebElement parent;
    volatile String text;
    volatile boolean displayed = true;
    volatile boolean enabled = true;

    PonyWebElement(final PonySDKWebDriver ponySDKWebDriver, final int objectID, final WidgetType widgetType) {
        this.ponySDKWebDriver = ponySDKWebDriver;
        this.objectID = objectID;
        this.widgetType = widgetType;
    }

    public void printTree(final Writer writer) throws IOException {
        printTree(0, writer);
    }

    void printTree(final int level, final Writer writer) throws IOException {
        for (int i = 0; i < level; i++) {
            writer.write('\t');
        }
        writer.write(this.toString());
        writer.write('\n');
        for (final PonyWebElement e : children) {
            e.printTree(level + 1, writer);
        }
        for (int i = 0; i < level; i++) {
            writer.write('\t');
        }
        writer.write("</" + widgetType.name() + '>');
        writer.write('\n');
    }

    @Override
    public void click() {
        sendApplicationInstruction(ClientToServerModel.DOM_HANDLER_TYPE, DomHandlerType.CLICK.getValue());
    }

    @Override
    public void submit() {
        throw new UnsupportedOperationException();
    }

    public void sendApplicationInstruction(final ClientToServerModel model, final JsonValue value) {
        this.ponySDKWebDriver.sendApplicationInstruction(Json.createObjectBuilder() //
            .add(ClientToServerModel.OBJECT_ID.toStringValue(), objectID).add(model.toStringValue(), value).build());
    }

    public void sendApplicationInstruction(final ClientToServerModel model, final int value) {
        this.ponySDKWebDriver.sendApplicationInstruction(Json.createObjectBuilder() //
            .add(ClientToServerModel.OBJECT_ID.toStringValue(), objectID).add(model.toStringValue(), value).build());
    }

    public void sendApplicationInstruction(final ClientToServerModel model, final long value) {
        this.ponySDKWebDriver.sendApplicationInstruction(Json.createObjectBuilder() //
            .add(ClientToServerModel.OBJECT_ID.toStringValue(), objectID).add(model.toStringValue(), value).build());
    }

    public void sendApplicationInstruction(final ClientToServerModel model, final double value) {
        this.ponySDKWebDriver.sendApplicationInstruction(Json.createObjectBuilder() //
            .add(ClientToServerModel.OBJECT_ID.toStringValue(), objectID).add(model.toStringValue(), value).build());
    }

    public void sendApplicationInstruction(final ClientToServerModel model, final boolean value) {
        this.ponySDKWebDriver.sendApplicationInstruction(Json.createObjectBuilder() //
            .add(ClientToServerModel.OBJECT_ID.toStringValue(), objectID).add(model.toStringValue(), value).build());
    }

    public void sendApplicationInstruction(final ClientToServerModel model, final String value) {
        this.ponySDKWebDriver.sendApplicationInstruction(Json.createObjectBuilder() //
            .add(ClientToServerModel.OBJECT_ID.toStringValue(), objectID).add(model.toStringValue(), value).build());
    }

    @Override
    public void sendKeys(final CharSequence... keysToSend) {
        for (final CharSequence seq : keysToSend) {
            sendApplicationInstruction(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE, text = seq.toString());
        }
    }

    @Override
    public void clear() {
        this.text = null;
    }

    @Override
    public String getTagName() {
        return widgetType == null ? null : widgetType.name();
    }

    @Override
    public String getAttribute(final String name) {
        return attributes.get(name);
    }

    @Override
    public boolean isSelected() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public List<WebElement> findElements(final By by) {
        return context.findElements(by);
    }

    public List<PonyWebElement> findElementsAsPony(final By by) {
        return (List<PonyWebElement>) (Object) findElements(by);
    }

    @Override
    public PonyWebElement findElement(final By by) {
        return (PonyWebElement) context.findElement(by);
    }

    @Override
    public boolean isDisplayed() {
        return displayed;
    }

    @Override
    public Point getLocation() {
        return null;
    }

    @Override
    public Dimension getSize() {
        return null;
    }

    @Override
    public Rectangle getRect() {
        return null;
    }

    @Override
    public String getCssValue(final String propertyName) {
        return null;
    }

    @Override
    public <X> X getScreenshotAs(final OutputType<X> target) throws WebDriverException {
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('<').append(widgetType).append(" id='").append(objectID).append('\'');
        if (!styles.isEmpty()) sb.append(" class='").append(styles).append('\'');
        if (!attributes.isEmpty()) sb.append(" attributes='").append(attributes).append('\'');
        sb.append(" >");
        if (text != null) sb.append(text);

        return sb.toString();
    }

    public PonyWebElement getParent() {
        return parent;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Collection<String> getStyles() {
        return styles;
    }

    public Collection<PonyWebElement> getChildren() {
        return children;
    }

    public int getObjectID() {
        return objectID;
    }

    public WidgetType getWidgetType() {
        return widgetType;
    }

    @Override
    public int hashCode() {
        return objectID;
    }

}
