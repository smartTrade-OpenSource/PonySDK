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

import com.ponysdk.core.model.WidgetType;
import org.openqa.selenium.By;
import org.openqa.selenium.By.*;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.*;

import java.util.*;
import java.util.function.Predicate;

class PonySearchContext implements FindsById, FindsByName, FindsByClassName, FindsByCssSelector, FindsByTagName, SearchContext {

    private static final ThreadLocal<MultipleElementsConsumer> multipleElementsConsumers = ThreadLocal
        .withInitial(MultipleElementsConsumer::new);
    private static final ThreadLocal<SingleElementConsumer> singleElementConsumers = ThreadLocal
        .withInitial(SingleElementConsumer::new);
    private static final Comparator<PonyWebElement> elementsComparator = (e1, e2) -> e1.objectID - e2.objectID;

    private final Collection<PonyWebElement> elements;
    private final boolean diveable;

    private static class MultipleElementsConsumer implements Predicate<PonyWebElement> {

        private List<PonyWebElement> list = null;

        @Override
        public boolean test(final PonyWebElement t) {
            list.add(t);
            return true;
        }
    }

    private static class SingleElementConsumer implements Predicate<PonyWebElement> {

        private PonyWebElement e = null;

        @Override
        public boolean test(final PonyWebElement t) {
            e = t;
            return false;
        }

    }

    private enum FindBy {
        ID,
        NAME,
        CLASS_NAME,
        CSS_SELECTOR,
        TAG_NAME
    }

    PonySearchContext(final Collection<PonyWebElement> elements, final boolean diveable) {
        this.elements = elements;
        this.diveable = diveable;
    }

    @Override
    public WebElement findElementById(final String using) {
        return findElement(FindBy.ID, using);
    }

    @Override
    public List<WebElement> findElementsById(final String using) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WebElement findElementByName(final String using) {
        return findElement(FindBy.NAME, using);
    }

    @Override
    public List<WebElement> findElementsByName(final String using) {
        return (List<WebElement>) (Object) findElements(FindBy.NAME, using);
    }

    @Override
    public WebElement findElementByClassName(final String using) {
        return findElement(FindBy.CLASS_NAME, using);
    }

    @Override
    public List<WebElement> findElementsByClassName(final String using) {
        return (List<WebElement>) (Object) findElements(FindBy.CLASS_NAME, using);
    }

    @Override
    public WebElement findElementByCssSelector(final String using) {
        return findElement(FindBy.CSS_SELECTOR, using);
    }

    @Override
    public List<WebElement> findElementsByCssSelector(final String using) {
        return (List<WebElement>) (Object) findElements(FindBy.CSS_SELECTOR, using);
    }

    @Override
    public WebElement findElementByTagName(final String using) {
        return findElement(FindBy.TAG_NAME, using);
    }

    @Override
    public List<WebElement> findElementsByTagName(final String using) {
        return (List<WebElement>) (Object) findElements(FindBy.TAG_NAME, using);
    }

    private PonyWebElement findElement(final FindBy findBy, String using) {
        using = using.trim();
        if (using.isEmpty()) return null;
        final SingleElementConsumer consumer = singleElementConsumers.get();
        findElements(findBy, using, consumer);
        final PonyWebElement e = consumer.e;
        consumer.e = null;
        return e;
    }

    private List<PonyWebElement> findElements(final FindBy findBy, String using) {
        using = using.trim();
        if (using.isEmpty()) return null;
        final MultipleElementsConsumer consumer = multipleElementsConsumers.get();
        consumer.list = new ArrayList<>();
        findElements(findBy, using, consumer);
        final List<PonyWebElement> elements = consumer.list;
        consumer.list = null;
        elements.sort(elementsComparator);
        return elements;
    }

    private void findElements(final FindBy findBy, final String using, final Predicate<PonyWebElement> consumer) {
        switch (findBy) {
            case CLASS_NAME:
                findElementsByClassName(Arrays.asList(using.split(" ")), diveable, consumer);
                break;
            case NAME:
                findElementsByAttribute("name", using, diveable, consumer);
                break;
            case ID:
                findElementsByAttribute("id", using, diveable, consumer);
                break;
            case CSS_SELECTOR:
                findElementsByCssSelector(using, 0, diveable, consumer);
                break;
            case TAG_NAME:
                findElementsByTagName(WidgetType.valueOf(using), using, diveable, consumer);
        }
    }

    private boolean findElementsByClassName(final List<String> styles, final boolean dive, final Predicate<PonyWebElement> consumer) {
        for (final PonyWebElement e : elements) {
            if (e.styles.containsAll(styles) && !consumer.test(e)) return false;
        }

        if (dive) {
            for (final PonyWebElement e : elements) {
                if (!e.context.findElementsByClassName(styles, dive, consumer)) return false;
            }
        }

        return true;
    }

    private boolean findElementsByTagName(final WidgetType widgetType, final String using, final boolean dive,
                                          final Predicate<PonyWebElement> consumer) {
        for (final PonyWebElement e : elements) {
            if (e.getWidgetType() == widgetType && !consumer.test(e)) return false;
        }

        if (dive) {
            for (final PonyWebElement e : elements) {
                if (!e.context.findElementsByTagName(widgetType, using, dive, consumer)) return false;
            }
        }

        return true;
    }

    private boolean findElementsByAttribute(final String attribute, final String using, final boolean dive,
                                            final Predicate<PonyWebElement> consumer) {
        for (final PonyWebElement e : elements) {
            if (using.equals(e.attributes.get(attribute)) && !consumer.test(e)) return false;
        }

        if (dive) {
            for (final PonyWebElement e : elements) {
                if (!e.context.findElementsByAttribute(attribute, using, dive, consumer)) return false;
            }
        }

        return true;
    }

    private boolean findElementsByCssSelector(final String using, int usingIndex, final boolean dive,
                                              final Predicate<PonyWebElement> consumer) {
        WidgetType widgetType = null;
        if (using.charAt(usingIndex) != '.') {
            final int wordStart = usingIndex;
            final int wordEnd = wordEnd(using, usingIndex);
            if (wordEnd == wordStart) throw new IllegalArgumentException(
                "Invalid css selector '" + using + "' at " + usingIndex + ", a widget type or class name is expected");
            widgetType = WidgetType.valueOf(using.substring(wordStart, wordEnd));
            usingIndex = wordEnd;
        }

        final ArrayList<String> styles = new ArrayList<>();

        while (usingIndex < using.length() && using.charAt(usingIndex) == '.') {
            final int wordStart = usingIndex + 1;
            final int wordEnd = wordEnd(using, wordStart);
            if (wordEnd == wordStart) throw new IllegalArgumentException(
                "Invalid css selector '" + using + "' at " + usingIndex + ", a class name is expected after '.'");
            styles.add(using.substring(wordStart, wordEnd));
            usingIndex = wordEnd;
        }

        return findElementByCssSelector(widgetType, using, usingIndex, styles, dive, consumer);
    }

    private boolean findElementByCssSelector(final WidgetType widgetType, final String using, final int usingIndex,
                                             final List<String> styles, final boolean dive, final Predicate<PonyWebElement> consumer) {
        for (final PonyWebElement e : elements) {
            if ((widgetType == null || widgetType == e.widgetType) && e.styles.containsAll(styles)) {
                if (using.length() > usingIndex) {
                    if (!e.context.findElementByCssSelector(using, usingIndex, consumer)) return false;
                } else {
                    if (!consumer.test(e)) return false;
                }
            }
        }

        if (dive) {
            for (final PonyWebElement e : elements) {
                if (!e.context.findElementByCssSelector(widgetType, using, usingIndex, styles, true, consumer)) return false;
            }
        }

        return true;
    }

    private boolean findElementByCssSelector(final String using, final int usingIndex, final Predicate<PonyWebElement> consumer) {
        switch (using.charAt(usingIndex)) {
            case ' ':
                return findElementsByCssSelector(using, usingIndex + 1, true, consumer);
            case '>':
                return findElementsByCssSelector(using, usingIndex + 1, false, consumer);
            default:
                throw new IllegalArgumentException(
                    "Invalid css selector '" + using + "' at " + usingIndex + ", character ' ' or '>' is expected");
        }
    }

    private static int wordEnd(final String str, final int wordStart) {
        for (int i = wordStart; i < str.length(); i++) {
            final char c = str.charAt(i);
            if (c == '.' || c == ' ' || c == '>') return i;
        }
        return str.length();
    }

    @Override
    public List<WebElement> findElements(final By by) {
        if (by instanceof ById) {
            final ById byId = (ById) by;
            return byId.findElements(this);
        } else if (by instanceof ByName) {
            final ByName byName = (ByName) by;
            return byName.findElements(this);
        } else if (by instanceof ByClassName) {
            final ByClassName byClassName = (ByClassName) by;
            return byClassName.findElements(this);
        } else if (by instanceof ByCssSelector) {
            final ByCssSelector byCssSelector = (ByCssSelector) by;
            return byCssSelector.findElements(this);
        } else if (by instanceof ByTagName) {
            final ByTagName byTagName = (ByTagName) by;
            return byTagName.findElements(this);
        } else {
            throw new IllegalArgumentException(by.getClass().getSimpleName() + " is not supported");
        }
    }

    @Override
    public WebElement findElement(final By by) {
        if (by instanceof ById) {
            final ById byId = (ById) by;
            return byId.findElement(this);
        } else if (by instanceof ByName) {
            final ByName byName = (ByName) by;
            return byName.findElement(this);
        } else if (by instanceof ByClassName) {
            final ByClassName byClassName = (ByClassName) by;
            return byClassName.findElement(this);
        } else if (by instanceof ByCssSelector) {
            final ByCssSelector byCssSelector = (ByCssSelector) by;
            return byCssSelector.findElement(this);
        } else if (by instanceof ByTagName) {
            final ByTagName byTagName = (ByTagName) by;
            return byTagName.findElement(this);
        } else {
            throw new IllegalArgumentException(by.getClass().getSimpleName() + " is not supported");
        }
    }

}
