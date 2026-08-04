package com.gitclone.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LruCacheTest {

    @Test
    public void testLruCacheEviction() {
        LruCache<String, Integer> cache = new LruCache<>(3);
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);

        assertEquals(3, cache.size());
        assertEquals(1, cache.get("one")); // Access "one", moves to head

        cache.put("four", 4); // "two" should be evicted because "one" was recently accessed

        assertEquals(3, cache.size());
        assertNull(cache.get("two"));
        assertEquals(1, cache.get("one"));
        assertEquals(3, cache.get("three"));
        assertEquals(4, cache.get("four"));
    }

    @Test
    public void testClearAndRemove() {
        LruCache<String, String> cache = new LruCache<>(2);
        cache.put("A", "Apple");
        cache.put("B", "Banana");

        cache.remove("A");
        assertNull(cache.get("A"));
        assertEquals(1, cache.size());

        cache.clear();
        assertEquals(0, cache.size());
        assertNull(cache.get("B"));
    }
}
