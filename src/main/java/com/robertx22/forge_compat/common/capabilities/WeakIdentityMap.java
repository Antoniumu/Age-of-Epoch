package com.robertx22.forge_compat.common.capabilities;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Weak keys compared by identity, not equals(). Entity equality uses numeric IDs;
 * an integrated server and its client can have different objects with the same ID.
 * Callers provide synchronization (CapabilityProvider already does).
 */
final class WeakIdentityMap<V> {
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private final HashMap<Key, V> entries = new HashMap<>();
    V get(Object owner) {
        clean();
        return entries.get(new Key(owner, null));
    }
    void put(Object owner, V value) {
        clean();
        entries.put(new Key(owner, queue), value);
    }
    private void clean() {
        Key key;
        while ((key=(Key)queue.poll()) != null) entries.remove(key);
    }
    private static final class Key extends WeakReference<Object> {
        private final int hash;
        Key(Object owner, ReferenceQueue<Object> queue) { super(owner,queue); hash=System.identityHashCode(owner); }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            if(this==other) return true;
            Object owner=get();
            return owner!=null && other instanceof Key key && owner==key.get();
        }
    }
}
