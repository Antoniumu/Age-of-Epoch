package com.robertx22.forge_compat.common.capabilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeakIdentityMapTest {
    private record EntityKey(int numericId) {}
    @Test void integratedClientAndServerEntitiesMustNeverShareCapabilities() {
        EntityKey server=new EntityKey(42), client=new EntityKey(42);
        assertEquals(server,client);
        assertNotSame(server,client);
        WeakIdentityMap<String> caps=new WeakIdentityMap<>();
        caps.put(server,"server quest progress");
        assertNull(caps.get(client));
        caps.put(client,"client mirror");
        assertEquals("server quest progress",caps.get(server));
        assertEquals("client mirror",caps.get(client));
        caps.put(client,"older packet");
        assertEquals("server quest progress",caps.get(server));
        assertEquals("older packet",caps.get(client));
    }
    @Test void repeatedLookupOfSameOwnerReusesItsValue() {
        Object owner=new Object();
        WeakIdentityMap<Object> map=new WeakIdentityMap<>();
        Object value=new Object();
        map.put(owner,value);
        assertSame(value,map.get(owner));
        map.put(owner,owner);
        assertSame(owner,map.get(owner));
    }
}
