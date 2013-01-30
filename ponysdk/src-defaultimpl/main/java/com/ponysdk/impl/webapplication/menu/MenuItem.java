
package com.ponysdk.impl.webapplication.menu;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class MenuItem {

    private String name;
    private Collection<String> categories;

    public MenuItem(final String name, final Collection<String> categories) {
        this.name = name;
        this.categories = categories;
    }

    public MenuItem(final String name, final String category) {
        this.name = name;
        if (category == null) this.categories = Collections.emptyList();
        else this.categories = Arrays.asList(category);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Collection<String> getCategories() {
        return categories;
    }

    public void setCategories(final Collection<String> categories) {
        this.categories = categories;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((categories == null) ? 0 : categories.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final MenuItem other = (MenuItem) obj;
        if (categories == null) {
            if (other.categories != null) return false;
        } else if (!categories.equals(other.categories)) return false;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        return true;
    }

}
