package com.gitclone.utils;

import java.util.NoSuchElementException;

/**
 * A custom FIFO Queue implementation backed by a singly linked list.
 *
 * @param <E> Element type
 */
public class Queue<E> {

    private static class Node<E> {
        E data;
        Node<E> next;

        Node(E data) {
            this.data = data;
        }
    }

    private Node<E> head;
    private Node<E> tail;
    private int size;

    public Queue() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    /**
     * Enqueues an element to the tail of the queue.
     */
    public synchronized void enqueue(E item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot enqueue null elements");
        }
        Node<E> newNode = new Node<>(item);
        if (tail != null) {
            tail.next = newNode;
        }
        tail = newNode;
        if (head == null) {
            head = newNode;
        }
        size++;
    }

    /**
     * Dequeues an element from the head of the queue.
     */
    public synchronized E dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        E data = head.data;
        head = head.next;
        if (head == null) {
            tail = null;
        }
        size--;
        return data;
    }

    /**
     * Inspects the element at the head of the queue without removing it.
     */
    public synchronized E peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        return head.data;
    }

    /**
     * Checks if the queue is empty.
     */
    public synchronized boolean isEmpty() {
        return size == 0;
    }

    /**
     * Gets the number of elements in the queue.
     */
    public synchronized int size() {
        return size;
    }
}
