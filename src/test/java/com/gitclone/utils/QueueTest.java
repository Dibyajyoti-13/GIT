package com.gitclone.utils;

import org.junit.jupiter.api.Test;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.*;

public class QueueTest {

    @Test
    public void testFifoQueueBehavior() {
        Queue<String> queue = new Queue<>();
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());

        queue.enqueue("First");
        queue.enqueue("Second");
        queue.enqueue("Third");

        assertFalse(queue.isEmpty());
        assertEquals(3, queue.size());
        assertEquals("First", queue.peek());

        assertEquals("First", queue.dequeue());
        assertEquals("Second", queue.dequeue());
        assertEquals(1, queue.size());

        assertEquals("Third", queue.peek());
        assertEquals("Third", queue.dequeue());
        assertTrue(queue.isEmpty());

        assertThrows(NoSuchElementException.class, queue::dequeue);
    }
}
