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

package com.ponysdk.core.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * A pool of sets that uses caching to reduce the number of redundant sets.<br/>
 * The <code>ImmutableSet</code>s retrieved from this pool are immutable. Any kind of modification of an ImmutableSet
 * will throw an <code>UnsupportedOperationException</code>.<br/>
 * Within the pool, instances of <code>ImmutableSet</code> are unique and weakly referenced.<br/>
 * There is a single, strongly referenced, instance of an empty set in this pool. It can be retrieved using
 * <code>SetPool::emptyImmutableSet</code> method.<br/>
 * In order to get an <code>ImmutableSet</code> derived from another <code>ImmutableSet</code>, you can use either
 * <code>ImmutableSet::getXxx</code> or <code>SetPool::xxx</code> where <code>xxx</code> is the name of the
 * equivalent unsupported <code>ImmutableSet::xxx</code> method (e.g. instead of using the unsupported
 * <code>ImmutableSet::add</code> method, use <code>ImmutableSet::getAdd</code> or <code>SetPool::add</code> to get a
 * copy, of the original set, that contains the newly added element).<br/>
 * <b>IMPORTANT:</b> Objects contained in the sets of this pool must be immutable.
 *
 * @param <E>
 *            The type of objects contained in the sets of this pool, MUST be immutable
 */
public class SetPool<E> {

    /*
     * Implementation notes.
     *
     * SetPool is based on a ConcurrentReferenceHashMap whose keys and values are weak references.
     * The keys and values of this map are unique instances of ImmutableSet.
     * To be able to retrieve an ImmutableSet using an equivalent Set instance key, for each key/value pair in the map,
     * both the key and value point to the same ImmutableSet instance.
     *
     * The Set instances that are used, internally, as keys to retrieve ImmutableSets are of type MutableSet.
     * They represent the product of the original ImmutableSet with the desired operation (the hash code of the
     * MutableSet is calculated accordingly). They are recycled in ThreadLocals to reduce allocation.
     * If no ImmutableSet instance is found, a new one will be constructed based on the MutableSet and put in the pool.
     * For example, when SetPool::add method is called, with arguments: ImmutableSet of value {A,B,C} and new element D,
     * a subclass of MutableSet: AddElementSet of value {original:{A,B,C}, newElement:D, hashCode:hashCode({A,B,C,D})}
     * is used to find, or create if absent, an equivalent ImmutableSet of value {A,B,C,D} in the pool.
     */

    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private final ImmutableSet emptySet;
    private final ThreadLocal<AddElementSet> addElementSetLocals = ThreadLocal.withInitial(() -> new AddElementSet());
    private final ThreadLocal<RemoveElementSet> removeElementSetLocals = ThreadLocal.withInitial(() -> new RemoveElementSet());
    private final ThreadLocal<AddAllElementsSet> addAllElementsSetLocals = ThreadLocal.withInitial(() -> new AddAllElementsSet());
    private final ThreadLocal<RemoveAllElementsSet> removeAllElementsSetLocals = ThreadLocal
        .withInitial(() -> new RemoveAllElementsSet());
    private final ThreadLocal<RetainAllElementsSet> retainAllElementsSetLocals = ThreadLocal
        .withInitial(() -> new RetainAllElementsSet());

    private final ConcurrentReferenceHashMap<Set<E>, ImmutableSet> pool;
    private final Function<Integer, Set<E>> setFactory;

    /**
     * Constructs an empty {@code SetPool} with the default Set factory (an {@code ArraySet} factory) and the default
     * initial capacity (16)
     */
    public SetPool() {
        this((capacity) -> new ArraySet<>(capacity), DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Constructs an empty {@code SetPool} with the specified Set factory and the default initial capacity (16)
     *
     * @param setFactory
     *            the factory used to create new Sets that are used internally in the {@code ImmutableSet}s, the
     *            factory's integer parameter is a hint about the expected size of the Set to create
     */
    public SetPool(final Function<Integer, Set<E>> setFactory) {
        this(setFactory, DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Constructs an empty {@code SetPool} with the specified Set factory and initial capacity
     *
     * @param setFactory
     *            the factory used to create new Sets that are used internally in the {@code ImmutableSet}s, the
     *            factory's integer parameter is a hint about the expected size of the Set to create
     * @param initialCapacity
     *            the initial capacity of the internal hash map
     */
    public SetPool(final Function<Integer, Set<E>> setFactory, final int initialCapacity) {
        emptySet = new ImmutableSet(setFactory.apply(0), 0);
        pool = new ConcurrentReferenceHashMap<>(initialCapacity, ConcurrentReferenceHashMap.ReferenceType.WEAK);
        pool.put(emptySet, emptySet);
        this.setFactory = setFactory;
    }

    /**
     * @return the unique empty instance of {@code ImmutableSet} within this pool
     */
    public ImmutableSet emptyImmutableSet() {
        return emptySet;
    }

    /**
     * Get an {@code ImmutableSet} that is equivalent to {@code key} from the pool. If not found, construct a new
     * {@code ImmutableSet} from {@code key} using {@code MutableSet::toImmutableSet}, put it in the pool and return it.
     */
    private ImmutableSet putIfAbsent(final MutableSet key) {
        ImmutableSet value = pool.get(key);
        if (value == null) {
            final ImmutableSet newSet = key.toImmutableSet();
            value = pool.putIfAbsent(newSet, newSet);
            if (value == null) value = newSet;
        }
        return value;
    }

    /**
     * Get an {@code ImmutableSet} that is equivalent to <code>(original UNION {e})</code>
     */
    public ImmutableSet add(final ImmutableSet original, final E e) {
        if (original.contains(e)) return original;
        final int hashCode = original.hashCode() + Objects.hashCode(e);
        final AddElementSet key = addElementSetLocals.get();
        try {
            key.wrap(original, hashCode, e);
            return putIfAbsent(key);
        } finally {
            key.reset();
        }
    }

    /**
     * Get an {@code ImmutableSet} that is equivalent to <code>(original MINUS {o})</code>
     */
    public ImmutableSet remove(final ImmutableSet original, final Object o) {
        if (!original.contains(o)) return original;
        final int hashCode = original.hashCode() - Objects.hashCode(o);
        final RemoveElementSet key = removeElementSetLocals.get();
        try {
            key.wrap(original, hashCode, o);
            return putIfAbsent(key);
        } finally {
            key.reset();
        }
    }

    /**
     * Get an {@code ImmutableSet} that is equivalent to <code>(original UNION c)</code>
     */
    public ImmutableSet addAll(final ImmutableSet original, final Collection<? extends E> c) {
        int hashCode = original.hashCode();
        int nbElements = original.size();
        for (final E e : c) {
            if (!original.contains(e)) {
                nbElements++;
                hashCode += Objects.hashCode(e);
            }
        }
        if (nbElements == original.size()) return original;

        final AddAllElementsSet key = addAllElementsSetLocals.get();
        try {
            key.wrap(original, hashCode, c, nbElements);
            return putIfAbsent(key);
        } finally {
            key.reset();
        }
    }

    /**
     * Get an {@code ImmutableSet} that is equivalent to <code>(original INTERSECT c)</code>
     */
    public ImmutableSet retainAll(final ImmutableSet original, final Collection<? extends E> c) {
        int hashCode = original.hashCode();
        int nbElements = original.size();
        for (final E e : original) {
            if (!c.contains(e)) {
                nbElements--;
                hashCode -= Objects.hashCode(e);
            }
        }
        if (nbElements == original.size()) return original;

        final RetainAllElementsSet key = retainAllElementsSetLocals.get();
        try {
            key.wrap(original, hashCode, c, nbElements);
            return putIfAbsent(key);
        } finally {
            key.reset();
        }
    }

    /**
     * Get an {@code ImmutableSet} that is equivalent to <code>(original MINUS c)</code>
     */
    public ImmutableSet removeAll(final ImmutableSet original, final Collection<? extends E> c) {
        int hashCode = original.hashCode();
        int nbElements = original.size();
        for (final Object o : c) {
            if (original.contains(o)) {
                nbElements--;
                hashCode -= Objects.hashCode(o);
            }
        }
        if (nbElements == original.size()) return original;

        final RemoveAllElementsSet key = removeAllElementsSetLocals.get();
        try {
            key.wrap(original, hashCode, c, nbElements);
            return putIfAbsent(key);
        } finally {
            key.reset();
        }
    }

    public class ImmutableSet implements Set<E> {

        private final Set<E> c;
        private final int hashCode;

        private ImmutableSet(final Set<E> c, final int hashCode) {
            super();
            this.c = c;
            this.hashCode = hashCode;
        }

        @Override
        public void forEach(final Consumer<? super E> action) {
            c.forEach(action);
        }

        @Override
        public int size() {
            return c.size();
        }

        @Override
        public boolean isEmpty() {
            return c.isEmpty();
        }

        @Override
        public boolean contains(final Object o) {
            return c.contains(o);
        }

        /**
         * {@inheritDoc}<br/>
         * <b>IMPORTANT: </b><code>Iterator::remove</code> is unsupported in the returned iterator
         */
        @Override
        public Iterator<E> iterator() {
            return new Iterator<>() {

                private final Iterator<? extends E> i = c.iterator();

                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }

                @Override
                public E next() {
                    return i.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void forEachRemaining(final Consumer<? super E> action) {
                    // Use backing collection version
                    i.forEachRemaining(action);
                }
            };
        }

        @Override
        public Object[] toArray() {
            return c.toArray();
        }

        @Override
        public <T> T[] toArray(final T[] a) {
            return c.toArray(a);
        }

        /**
         * UNSUPPORTED, use <code>getAdd(E)</code> instead
         */
        @Override
        public boolean add(final E e) {
            throw new UnsupportedOperationException("Use getAdd(E) instead");
        }

        /**
         * Get, from the {@code SetPool}, an {@code ImmutableSet} that is equivalent to <code>(this UNION {e})</code>
         */
        public ImmutableSet getAdd(final E e) {
            return SetPool.this.add(this, e);
        }

        /**
         * UNSUPPORTED, use <code>getRemove(Object)</code> instead
         */
        @Override
        public boolean remove(final Object o) {
            throw new UnsupportedOperationException("Use getRemove(Object) instead");
        }

        /**
         * Get, from the {@code SetPool}, an {@code ImmutableSet} that is equivalent to <code>(this MINUS {e})</code>
         */
        public ImmutableSet getRemove(final Object o) {
            return SetPool.this.remove(this, o);
        }

        @Override
        public boolean containsAll(final Collection<?> c) {
            return this.c.containsAll(c);
        }

        /**
         * Get, from the {@code SetPool}, an {@code ImmutableSet} that is equivalent to <code>(this UNION c)</code>
         */
        public ImmutableSet getAddAll(final Collection<? extends E> c) {
            return SetPool.this.addAll(this, c);
        }

        /**
         * UNSUPPORTED, use <code>getAddAll(Collection)</code> instead
         */
        @Override
        public boolean addAll(final Collection<? extends E> c) {
            throw new UnsupportedOperationException("Use getAddAll(Collection) instead");
        }

        /**
         * Get, from the {@code SetPool}, an {@code ImmutableSet} that is equivalent to <code>(this INTERSECT c)</code>
         */
        public ImmutableSet getRetainAll(final Collection<? extends E> c) {
            return SetPool.this.retainAll(this, c);
        }

        /**
         * UNSUPPORTED, use <code>getRetainAll(Collection)</code> instead
         */
        @Override
        public boolean retainAll(final Collection<?> c) {
            throw new UnsupportedOperationException("Use getRetainAll(Collection) instead");
        }

        /**
         * Get, from the {@code SetPool}, an {@code ImmutableSet} that is equivalent to <code>(this MINUS c)</code>
         */
        public ImmutableSet getRemoveAll(final Collection<? extends E> c) {
            return SetPool.this.removeAll(this, c);
        }

        /**
         * UNSUPPORTED, use <code>getRemoveAll(Collection)</code> instead
         */
        @Override
        public boolean removeAll(final Collection<?> c) {
            throw new UnsupportedOperationException("Use getRemoveAll(Collection) instead");
        }

        /**
         * UNSUPPORTED, use <code>getClear()</code> instead
         */
        @Override
        public void clear() {
            throw new UnsupportedOperationException("Use getClear() instead");
        }

        /**
         * Get the unique empty instance of {@code ImmutableSet} within the {@code SetPool}
         */
        public ImmutableSet getClear() {
            return emptySet;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this) return true;

            if (!(o instanceof Set)) return false;
            final Collection<?> c = (Collection<?>) o;
            if (c.size() != size()) return false;
            try {
                return c.containsAll(this);
            } catch (final ClassCastException unused) {
                return false;
            } catch (final NullPointerException unused) {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        /**
         * <b>UNSUPPORTED</b>
         */
        @Override
        public boolean removeIf(final Predicate<? super E> filter) {
            throw new UnsupportedOperationException();
        }

    }

    private class AddElementSet extends MutableSet {

        private E newElement;

        private void wrap(final ImmutableSet originalSet, final int hashCode, final E newElement) {
            wrap(originalSet, hashCode);
            this.newElement = newElement;
        }

        @Override
        protected void reset() {
            super.reset();
            newElement = null;
        }

        @Override
        public int size() {
            return originalSet.size() + 1;
        }

        @Override
        public boolean containsAll(final Collection<?> c) {
            for (final Object o : c) {
                if (!Objects.equals(o, newElement) && !originalSet.contains(o)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected ImmutableSet toImmutableSet() {
            final Set<E> newSet = setFactory.apply(originalSet.size() + 1);
            newSet.addAll(originalSet);
            newSet.add(newElement);
            return new ImmutableSet(newSet, hashCode);
        }

    }

    private class RemoveElementSet extends MutableSet {

        private Object removeElement;

        private void wrap(final ImmutableSet originalSet, final int hashCode, final Object removeElement) {
            wrap(originalSet, hashCode);
            this.removeElement = removeElement;
        }

        @Override
        protected void reset() {
            super.reset();
            removeElement = null;
        }

        @Override
        public int size() {
            return originalSet.size() - 1;
        }

        @Override
        public boolean containsAll(final Collection<?> c) {
            for (final Object o : c) {
                if (Objects.equals(removeElement, o) || !originalSet.contains(o)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected ImmutableSet toImmutableSet() {
            final Set<E> newSet = setFactory.apply(originalSet.size());
            newSet.addAll(originalSet);
            newSet.remove(removeElement);
            return new ImmutableSet(newSet, hashCode);
        }

    }

    private class RemoveAllElementsSet extends MutableSet {

        private Collection<? extends E> c;
        private int nbElements;

        private void wrap(final ImmutableSet originalSet, final int hashCode, final Collection<? extends E> c, final int nbElements) {
            super.wrap(originalSet, hashCode);
            this.c = c;
            this.nbElements = nbElements;
        }

        @Override
        protected void reset() {
            super.reset();
            c = null;
        }

        @Override
        public int size() {
            return nbElements;
        }

        @Override
        public boolean containsAll(final Collection<?> c) {
            for (final Object o : c) {
                if (this.c.contains(o) || !originalSet.contains(o)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected ImmutableSet toImmutableSet() {
            final Set<E> newSet = setFactory.apply(originalSet.size());
            newSet.addAll(originalSet);
            newSet.removeAll(c);
            return new ImmutableSet(newSet, hashCode);
        }

    }

    private class AddAllElementsSet extends MutableSet {

        private Collection<? extends E> c;
        private int nbElements;

        private void wrap(final ImmutableSet originalSet, final int hashCode, final Collection<? extends E> c, final int nbElements) {
            super.wrap(originalSet, hashCode);
            this.c = c;
            this.nbElements = nbElements;
        }

        @Override
        protected void reset() {
            super.reset();
            c = null;
        }

        @Override
        public int size() {
            return nbElements;
        }

        @Override
        public boolean containsAll(final Collection<?> c) {
            for (final Object o : c) {
                if (!this.c.contains(o) || !originalSet.contains(o)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected ImmutableSet toImmutableSet() {
            final Set<E> newSet = setFactory.apply(nbElements);
            newSet.addAll(originalSet);
            newSet.addAll(c);
            return new ImmutableSet(newSet, hashCode);
        }

    }

    private class RetainAllElementsSet extends MutableSet {

        private Collection<? extends E> c;
        private int nbElements;

        private void wrap(final ImmutableSet originalSet, final int hashCode, final Collection<? extends E> c, final int nbElements) {
            super.wrap(originalSet, hashCode);
            this.c = c;
            this.nbElements = nbElements;
        }

        @Override
        protected void reset() {
            super.reset();
            c = null;
        }

        @Override
        public int size() {
            return nbElements;
        }

        @Override
        public boolean containsAll(final Collection<?> c) {
            for (final Object o : c) {
                if (!this.c.contains(o) && !originalSet.contains(o)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected ImmutableSet toImmutableSet() {
            final Set<E> newSet = setFactory.apply(originalSet.size());
            newSet.addAll(originalSet);
            newSet.retainAll(c);
            return new ImmutableSet(newSet, hashCode);
        }

    }

    private abstract class MutableSet implements Set<E> {

        protected ImmutableSet originalSet;
        protected int hashCode;

        protected final void wrap(final ImmutableSet originalSet, final int hashCode) {
            this.originalSet = originalSet;
            this.hashCode = hashCode;
        }

        protected void reset() {
            originalSet = null;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(final Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<E> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(final T[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(final Object e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(final Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(final Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(final Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(final Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        protected abstract ImmutableSet toImmutableSet();
    }

}
