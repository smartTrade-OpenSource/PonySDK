
package com.ponysdk.core.stm;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ponysdk.core.instruction.Instruction;

public class TxnHashMap<K, V> extends TxnObjectImpl<Map<K, V>> implements Map<K, V> {

    private final Map<K, Instruction> pendingInstructions = new HashMap<K, Instruction>();

    public TxnHashMap() {
        super(new HashMap<K, V>());
    }

    @Override
    public void clear() {
        get4Write().clear();
    }

    @Override
    public boolean containsKey(final Object arg0) {
        return get().containsKey(arg0);
    }

    @Override
    public boolean containsValue(final Object arg0) {
        return get().containsValue(arg0);
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return get().entrySet();
    }

    @Override
    public V get(final Object arg0) {
        return get().get(arg0);
    }

    @Override
    public boolean isEmpty() {
        return get().isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return get().keySet();
    }

    @Override
    public V put(final K arg0, final V arg1) {
        return get4Write().put(arg0, arg1);
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> arg0) {
        get4Write().putAll(arg0);
    }

    @Override
    public V remove(final Object arg0) {
        return get4Write().remove(arg0);
    }

    @Override
    public int size() {
        return get().size();
    }

    @Override
    public Collection<V> values() {
        return get().values();
    }

    private Map<K, V> get4Write() {
        if (transaction == null) {
            transaction = Txn.get();
            if (transaction == null) {
                return initialValue;
            } else {
                transaction.addTnxListener(this);
            }
        }

        if (!set) {
            set = true;
            workingValue = new HashMap<K, V>(initialValue);
        }
        return workingValue;
    }

    public void addPendingInstruction(final K key, final Instruction instruction) {
        pendingInstructions.put(key, instruction);
    }

    public Map<K, Instruction> getPendingInstructions() {
        return pendingInstructions;
    }

    @Override
    public void beforeRollback() {
        super.beforeRollback();
        pendingInstructions.clear();
    }

    @Override
    public void afterFlush(final TxnContext txnContext) {
        super.afterFlush(txnContext);
        pendingInstructions.clear();
    }

}
