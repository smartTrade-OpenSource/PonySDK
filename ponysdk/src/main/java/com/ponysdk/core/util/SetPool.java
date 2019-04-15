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

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A pool of sets that uses caching to prevent having redundant sets.<br/>
 * The <code>ImmutableSet</code>s retrieved from this pool are immutable. Any kind of modification of an
 * <code>ImmutableSet</code> will throw an <code>UnsupportedOperationException</code>.<br/>
 * Within the pool, instances of <code>ImmutableSet</code> are unique and weakly referenced.<br/>
 * There is a single, strongly referenced, instance of an empty set in this pool. It can be retrieved using
 * <code>SetPool::emptyImmutableSet</code> method.<br/>
 * In order to get an <code>ImmutableSet</code> derived from another <code>ImmutableSet</code>, you can use either
 * <code>ImmutableSet::getXxx</code> or <code>SetPool::xxx</code> methods where <code>xxx</code> is the name of the
 * equivalent unsupported <code>ImmutableSet::xxx</code> method (e.g. instead of using the unsupported
 * <code>ImmutableSet::add</code> method, use <code>ImmutableSet::getAdd</code> or <code>SetPool::add</code> to get a
 * copy, of the original set, that contains the newly added element).<br/>
 * <b>IMPORTANT:</b> Objects contained in the sets of this pool must be immutable.
 *
 * @param <E> The type of objects contained in the sets of this pool, MUST be immutable
 */
public class SetPool<E> {

    /*
     * Implementation notes.
     *
     * SetPool is based on a SynchronizedWeakCache that weakly caches values.
     * The values in this cache are unique instances of ImmutableSet.
     *
     * The Set instances that are used, internally, as keys to retrieve ImmutableSets are of type MutableSet.
     * They represent the combination of the original ImmutableSet with the desired operation (the hash code of the
     * MutableSet is calculated accordingly). For example, when SetPool::add method is called, with arguments:
     * ImmutableSet of value {A,B,C} and new element D, a subclass of MutableSet (AddElementSet) of value
     * {original:{A,B,C}, newElement:D, hashCode:hashCode({A,B,C,D})} is used to find, or create if absent, an
     * equivalent ImmutableSet of value {A,B,C,D} in the cache.
     * The instances of MutableSet are recycled in ThreadLocals to reduce allocation.
     */

    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private final ImmutableSet emptySet;
    private final ThreadLocal<AddElementSet> addElementSetLocals = ThreadLocal.withInitial(AddElementSet::new);
    private final ThreadLocal<RemoveElementSet> removeElementSetLocals = ThreadLocal.withInitial(RemoveElementSet::new);
    private final ThreadLocal<AddAllElementsSet> addAllElementsSetLocals = ThreadLocal.withInitial(AddAllElementsSet::new);
    private final ThreadLocal<RemoveAllElementsSet> removeAllElementsSetLocals = ThreadLocal
            .withInitial(RemoveAllElementsSet::new);
    private final ThreadLocal<RetainAllElementsSet> retainAllElementsSetLocals = ThreadLocal
            .withInitial(RetainAllElementsSet::new);

    private final SynchronizedWeakCache<ImmutableSet> cache;
    private final Function<Integer, Set<E>> setFactory;

    /**
     * Constructs an empty {@code SetPool} with the default Set factory (an {@code ArraySet} factory) and the default
     * initial capacity (16)
     */
    public SetPool() {
        this(SetUtils::newArraySet, DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Constructs an empty {@code SetPool} with the specified Set factory and the default initial capacity (16)
     *
     * @param setFactory the factory used to create new Sets that are used internally in the {@code ImmutableSet}s, the
     *                   factory's integer parameter is a hint about the expected size of the Set to create
     */
    public SetPool(final Function<Integer, Set<E>> setFactory) {
        this(setFactory, DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Constructs an empty {@code SetPool} with the specified Set factory and initial capacity
     *
     * @param setFactory      the factory used to create new Sets that are used internally in the {@code ImmutableSet}s, the
     *                        factory's integer parameter is a hint about the expected size of the Set to create
     * @param initialCapacity the initial capacity of the internal hash map
     */
    public SetPool(final Function<Integer, Set<E>> setFactory, final int initialCapacity) {
        emptySet = new ImmutableSet(setFactory.apply(0), 0);
        cache = new SynchronizedWeakCache<>(initialCapacity);
        cache.getOrCompute(emptySet, e -> e);
        this.setFactory = setFactory;
    }

    /**
     * @return the unique empty instance of {@code ImmutableSet} within this pool
     */
    public ImmutableSet emptyImmutableSet() {
        return emptySet;
    }

    /**
     * Wrap {@code original} set and {@code delta} in {@code mutableSet}, and use the latter to find or create an
     * equivalent ImmutableSet from the {@code cache}
     */
    private <D> ImmutableSet find(final ImmutableSet original, final D delta, final MutableSet<D> mutableSet) {
        try {
            if (!mutableSet.wrap(original, delta)) return original;
            return cache.getOrCompute(mutableSet, MutableSet::toImmutableSet);
        } finally {
            mutableSet.reset();
        }
    }

    /**
     * @return an {@code ImmutableSet} that is equivalent to <code>(original UNION {e})</code>, or {@code original}
     * itself if {@code e} already belongs to it
     */
    public ImmutableSet add(final ImmutableSet original, final E e) {
        return find(original, e, addElementSetLocals.get());
    }

    /**
     * @return an {@code ImmutableSet} that is equivalent to <code>(original MINUS {o})</code>, or {@code original}
     * itself if {@code o} doesn't belong to it
     */
    public ImmutableSet remove(final ImmutableSet original, final Object o) {
        return find(original, o, removeElementSetLocals.get());
    }

    /**
     * @return an {@code ImmutableSet} that is equivalent to <code>(original UNION c)</code>, or {@code original}
     * itself if {@code c} is a subset of it
     */
    public ImmutableSet addAll(final ImmutableSet original, final Set<? extends E> c) {
        return find(original, c, addAllElementsSetLocals.get());
    }

    /**
     * @return an {@code ImmutableSet} that is equivalent to <code>(original INTERSECT c)</code>, or {@code original}
     * itself if it is a subset of {@code c}
     */
    public ImmutableSet retainAll(final ImmutableSet original, final Set<?> c) {
        return find(original, c, retainAllElementsSetLocals.get());
    }

    /**
     * @return an {@code ImmutableSet} that is equivalent to <code>(original MINUS c)</code>, or {@code original}
     * itself if {@code c} has no intersection with it
     */
    public ImmutableSet removeAll(final ImmutableSet original, final Set<?> c) {
        return find(original, c, removeAllElementsSetLocals.get());
    }

    public class ImmutableSet implements Set<E> {

        private final Set<E> s;
        private final int hashCode;

        private ImmutableSet(final Set<E> s, final int hashCode) {
            super();
            this.s = s;
            this.hashCode = hashCode;
        }

        @Override
        public void forEach(final Consumer<? super E> action) {
            s.forEach(action);
        }

        @Override
        public int size() {
            return s.size();
        }

        @Override
        public boolean isEmpty() {
            return s.isEmpty();
        }

        @Override
        public boolean contains(final Object o) {
            return s.contains(o);
        }

        /**
         * {@inheritDoc}<br/>
         * <b>IMPORTANT: </b><code>Iterator::remove</code> is unsupported in the returned iterator
         */
        @Override
        public Iterator<E> iterator() {
            return new Iterator<>() {

                private final Iterator<? extends E> i = s.iterator();

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
            return s.toArray();
        }

        @Override
        public <T> T[] toArray(final T[] a) {
            return s.toArray(a);
        }

        /**
         * UNSUPPORTED, use <code>getAdd(E)</code> instead
         */
        @Override
        public boolean add(final E e) {
            throw new UnsupportedOperationException("Use getAdd(E) instead");
        }

        /**
         * @return from the {@code SetPool}, an {@code ImmutableSet} that is equivalent to
         * <code>(this UNION {e})</code>, or {@code this} if {@code e} already belongs to it
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
         * @return from the {@code SetPool}, an {@code ImmutableSet} that is equivalent to
         * <code>(this MINUS {e})</code>, or {@code this} if {@code o} doesn't belong to it
         */
        public ImmutableSet getRemove(final Object o) {
            return SetPool.this.remove(this, o);
        }

        @Override
        public boolean containsAll(final Collection<?> c) {
            return this.s.containsAll(c);
        }

        /**
         * @return from the {@code SetPool}, an {@code ImmutableSet} that is equivalent to <code>(this UNION c)</code>,
         * or {@code this} if {@code c} is a subset of it
         */
        public ImmutableSet getAddAll(final Set<? extends E> c) {
            return SetPool.this.addAll(this, c);
        }

        /**
         * UNSUPPORTED, use <code>getAddAll(Set)</code> instead
         */
        @Override
        public boolean addAll(final Collection<? extends E> c) {
            throw new UnsupportedOperationException("Use getAddAll(Collection) instead");
        }

        /**
         * @return from the {@code SetPool}, an {@code ImmutableSet} that is equivalent to
         * <code>(this INTERSECT c)</code>, or {@code this} if it is a subset of {@code c}
         */
        public ImmutableSet getRetainAll(final Set<?> c) {
            return SetPool.this.retainAll(this, c);
        }

        /**
         * UNSUPPORTED, use <code>getRetainAll(Set)</code> instead
         */
        @Override
        public boolean retainAll(final Collection<?> c) {
            throw new UnsupportedOperationException("Use getRetainAll(Collection) instead");
        }

        /**
         * @return from the {@code SetPool}, an {@code ImmutableSet} that is equivalent to <code>(this MINUS c)</code>,
         * or {@code this} if {@code c} has no intersection with it
         */
        public ImmutableSet getRemoveAll(final Set<?> c) {
            return SetPool.this.removeAll(this, c);
        }

        /**
         * UNSUPPORTED, use <code>getRemoveAll(Set)</code> instead
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
         * @return the unique empty instance of {@code ImmutableSet} within the {@code SetPool}
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
            /*
             * MUST use c.containsAll(this) and not this.containsAll(c) since the latter may call c.toArray(),
             * c.iterator() or any other method that is not implemented in MutableSet
             */
            return c.containsAll(this);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public Stream<E> stream() {
            return s.stream();
        }

        @Override
        public Stream<E> parallelStream() {
            return s.parallelStream();
        }

        @Override
        public Spliterator<E> spliterator() {
            return s.spliterator();
        }

        @Override
        public String toString() {
            return s.toString();
        }

        /**
         * <b>UNSUPPORTED</b>
         */
        @Override
        public boolean removeIf(final Predicate<? super E> filter) {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Used exclusively as a key to retrieve an equivalent ImmutableSet from a Map
     */
    private abstract class MutableSet<D> implements Set<E> {

        private ImmutableSet originalSet;
        private D delta;
        private int hashCode;
        private int size;

        /**
         * @return whether the result set is different than the original set
         */
        public final boolean wrap(final ImmutableSet originalSet, final D delta) {
            if (!updateSizeAndHashCode(originalSet, delta)) return false;
            this.delta = delta;
            this.originalSet = originalSet;
            return true;
        }

        /**
         * @return whether the size is different than that of the original set
         */
        protected abstract boolean updateSizeAndHashCode(final ImmutableSet originalSet, final D delta);

        protected abstract boolean containsAll(final ImmutableSet originalSet, final D delta, final Collection<?> c);

        protected void setHashCode(final int hashCode) {
            this.hashCode = hashCode;
        }

        protected void setSize(final int size) {
            this.size = size;
        }

        public final void reset() {
            originalSet = null;
            delta = null;
        }

        @Override
        public final boolean containsAll(final Collection<?> c) {
            return containsAll(originalSet, delta, c);
        }

        @Override
        public final int hashCode() {
            return hashCode;
        }

        @Override
        public final int size() {
            return size;
        }

        @Override
        public final boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public final boolean contains(final Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final Iterator<E> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public final Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public final <T> T[] toArray(final T[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final boolean add(final Object e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final boolean remove(final Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final boolean addAll(final Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final boolean retainAll(final Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final boolean removeAll(final Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final void clear() {
            throw new UnsupportedOperationException();
        }

        public final ImmutableSet toImmutableSet() {
            final Set<E> newSet = setFactory.apply(Math.max(size, originalSet.size()));
            newSet.addAll(originalSet);
            applyDelta(newSet, delta);
            return new ImmutableSet(newSet, hashCode);
        }

        protected abstract void applyDelta(Set<E> set, D delta);

        /**
         * MUST be exclusively called with an ImmutableSet argument, since it is used only internally and for the
         * specific purpose of retrieving an equivalent ImmutableSet from a Map. Any other use may give unexpected
         * results
         */
        @Override
        public boolean equals(final Object o) {
            if (o == this) return true;
            if (o == null) return false;

            if (!(o instanceof SetPool<?>.ImmutableSet)) throw new IllegalArgumentException();
            final Collection<?> c = (Collection<?>) o;
            if (c.size() != size()) return false;
            /*
             * MUST use this.containsAll(c) and not c.containsAll(this) since the latter may call this.toArray(),
             * this.iterator() or any other method that is not implemented in MutableSet
             */
            return this.containsAll(c);
        }

    }

    private class AddElementSet extends MutableSet<E> {

        @Override
        protected boolean updateSizeAndHashCode(final SetPool<E>.ImmutableSet originalSet, final E delta) {
            if (originalSet.contains(delta)) return false;
            setSize(originalSet.size() + 1);
            setHashCode(originalSet.hashCode() + Objects.hashCode(delta));
            return true;
        }

        @Override
        protected boolean containsAll(final ImmutableSet originalSet, final E delta, final Collection<?> c) {
            for (final Object o : c) {
                if (!Objects.equals(o, delta) && !originalSet.contains(o)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void applyDelta(final Set<E> set, final E delta) {
            set.add(delta);
        }

    }

    private class RemoveElementSet extends MutableSet<Object> {

        @Override
        protected boolean updateSizeAndHashCode(final SetPool<E>.ImmutableSet originalSet, final Object delta) {
            if (!originalSet.contains(delta)) return false;
            setSize(originalSet.size() - 1);
            setHashCode(originalSet.hashCode() - Objects.hashCode(delta));
            return true;
        }

        @Override
        protected boolean containsAll(final ImmutableSet originalSet, final Object delta, final Collection<?> c) {
            for (final Object o : c) {
                if (Objects.equals(delta, o) || !originalSet.contains(o)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void applyDelta(final Set<E> set, final Object delta) {
            set.remove(delta);
        }

    }

    private class AddAllElementsSet extends MutableSet<Set<? extends E>> {

        @Override
        protected boolean updateSizeAndHashCode(final SetPool<E>.ImmutableSet originalSet, final Set<? extends E> delta) {
            int hashCode = originalSet.hashCode();
            int nbElements = originalSet.size();
            for (final E e : delta) {
                if (!originalSet.contains(e)) {
                    hashCode += Objects.hashCode(e);
                    nbElements++;
                }
            }
            if (nbElements == originalSet.size()) return false;
            setSize(nbElements);
            setHashCode(hashCode);
            return true;
        }

        @Override
        protected boolean containsAll(final ImmutableSet originalSet, final Set<? extends E> delta, final Collection<?> c) {
            for (final Object o : c) {
                if (!delta.contains(o) || !originalSet.contains(o)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void applyDelta(final Set<E> set, final Set<? extends E> delta) {
            set.addAll(delta);
        }

    }

    private class RetainAllElementsSet extends MutableSet<Set<?>> {

        @Override
        protected boolean updateSizeAndHashCode(final SetPool<E>.ImmutableSet originalSet, final Set<?> delta) {
            int hashCode = originalSet.hashCode();
            int nbElements = originalSet.size();
            for (final E e : originalSet) {
                if (!delta.contains(e)) {
                    hashCode -= Objects.hashCode(e);
                    nbElements--;
                }
            }
            if (nbElements == originalSet.size()) return false;
            setSize(nbElements);
            setHashCode(hashCode);
            return true;
        }

        @Override
        protected boolean containsAll(final ImmutableSet originalSet, final Set<?> delta, final Collection<?> c) {
            for (final Object o : c) {
                if (!delta.contains(o) && !originalSet.contains(o)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void applyDelta(final Set<E> set, final Set<?> delta) {
            set.retainAll(delta);
        }

    }

    private class RemoveAllElementsSet extends MutableSet<Set<?>> {

        @Override
        protected boolean updateSizeAndHashCode(final SetPool<E>.ImmutableSet originalSet, final Set<?> delta) {
            int hashCode = originalSet.hashCode();
            int nbElements = originalSet.size();
            for (final Object o : delta) {
                if (originalSet.contains(o)) {
                    hashCode -= Objects.hashCode(o);
                    nbElements--;
                }
            }
            if (nbElements == originalSet.size()) return false;
            setSize(nbElements);
            setHashCode(hashCode);
            return true;
        }

        @Override
        protected boolean containsAll(final ImmutableSet originalSet, final Set<?> delta, final Collection<?> c) {
            for (final Object o : c) {
                if (delta.contains(o) || !originalSet.contains(o)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void applyDelta(final Set<E> set, final Set<?> delta) {
            set.removeAll(delta);
        }

    }

}
