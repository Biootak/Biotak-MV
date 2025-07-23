package com.biotak.util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * Simple object pool to reduce memory allocation overhead
 * @param <T> Type of objects to pool
 */
public class ObjectPool<T> {
    private final ConcurrentLinkedQueue<T> pool = new ConcurrentLinkedQueue<>();
    private final Supplier<T> factory;
    private final int maxSize;
    
    public ObjectPool(Supplier<T> factory, int maxSize) {
        this.factory = factory;
        this.maxSize = maxSize;
    }
    
    public ObjectPool(Supplier<T> factory) {
        this(factory, 100); // Default max size
    }
    
    /**
     * Get an object from the pool or create a new one
     */
    public T acquire() {
        T object = pool.poll();
        return object != null ? object : factory.get();
    }
    
    /**
     * Return an object to the pool
     */
    public void release(T object) {
        if (object != null && pool.size() < maxSize) {
            pool.offer(object);
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