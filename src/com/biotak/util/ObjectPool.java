package com.biotak.util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * Memory-optimized object pool to reduce allocation overhead
 * @param <T> Type of objects to pool
 */
public class ObjectPool<T> {
    private final ConcurrentLinkedQueue<T> pool = new ConcurrentLinkedQueue<>();
    private final Supplier<T> factory;
    private final int maxSize;
    private final java.util.concurrent.atomic.AtomicInteger currentSize = new java.util.concurrent.atomic.AtomicInteger(0); // Fix race condition
    
    public ObjectPool(Supplier<T> factory, int maxSize) {
        this.factory = factory;
        this.maxSize = Math.min(maxSize, 200); // Cap maximum size
    }
    
    public ObjectPool(Supplier<T> factory) {
        this(factory, 50); // Reduced default max size for memory efficiency
    }
    
    /**
     * Get an object from the pool or create a new one
     */
    public T acquire() {
        T object = pool.poll();
        if (object != null) {
            currentSize.decrementAndGet();
            return object;
        }
        return factory.get();
    }
    
    /**
     * Return an object to the pool
     */
    public void release(T object) {
        if (object != null && currentSize.get() < maxSize) {
            if (pool.offer(object)) {
                currentSize.incrementAndGet();
            }
        }
    }
    
    /**
     * Clear the pool
     */
    public void clear() {
        pool.clear();
    }
    
    /**
     * Get current pool size
     */
    public int size() {
        return pool.size();
    }
}