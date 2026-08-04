package com.gitclone.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * An LRU Cache implementation backed by a custom Doubly Linked List and a HashMap
 * to achieve O(1) read and write operations.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public class LruCache<K, V> {

    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    private final Map<K, Node<K, V>> map;
    private Node<K, V> head;
    private Node<K, V> tail;

    public LruCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.map = new HashMap<>();
    }

    /**
     * Retrieves a value from the cache. Moves accessed node to head.
     */
    public synchronized V get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) {
            return null;
        }
        moveToHead(node);
        return node.value;
    }

    /**
     * Puts a value into the cache. If capacity is exceeded, evicts the LRU item (tail).
     */
    public synchronized void put(K key, V value) {
        Node<K, V> node = map.get(key);
        if (node != null) {
            node.value = value;
            moveToHead(node);
        } else {
            if (map.size() >= capacity) {
                evictLru();
            }
            Node<K, V> newNode = new Node<>(key, value);
            addToHead(newNode);
            map.put(key, newNode);
        }
    }

    /**
     * Removes an entry from the cache.
     */
    public synchronized void remove(K key) {
        Node<K, V> node = map.remove(key);
        if (node != null) {
            removeNode(node);
        }
    }

    /**
     * Gets the current number of items in the cache.
     */
    public synchronized int size() {
        return map.size();
    }

    /**
     * Clears the cache.
     */
    public synchronized void clear() {
        map.clear();
        head = null;
        tail = null;
    }

    private void moveToHead(Node<K, V> node) {
        removeNode(node);
        addToHead(node);
    }

    private void addToHead(Node<K, V> node) {
        node.next = head;
        node.prev = null;
        if (head != null) {
            head.prev = node;
        }
        head = node;
        if (tail == null) {
            tail = node;
        }
    }

    private void removeNode(Node<K, V> node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            tail = node.prev;
        }
        node.prev = null;
        node.next = null;
    }

    private void evictLru() {
        if (tail != null) {
            map.remove(tail.key);
            removeNode(tail);
        }
    }
}
