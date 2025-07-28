package com.biotak.util;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import com.biotak.debug.AdvancedLogger;

/**
 * Advanced memory management utilities for optimal performance
 */
public final class AdvancedMemoryManager {
    
    // Memory usage tracking
    private static final AtomicLong totalAllocatedBytes = new AtomicLong(0);
    private static final AtomicLong totalDeallocatedBytes = new AtomicLong(0);
    
    // Weak reference cache for expensive objects
    private static final ConcurrentHashMap<String, WeakReference<Object>> weakCache = 
        new ConcurrentHashMap<>();
    
    // Off-heap storage simulation using byte arrays
    private static final ConcurrentHashMap<String, byte[]> offHeapStorage = 
        new ConcurrentHashMap<>();
    
    // Memory pool for different object sizes
    private static final ObjectPool<byte[]> SMALL_BUFFER_POOL = 
        new ObjectPool<>(() -> new byte[1024], 50);  // 1KB buffers
    
    private static final ObjectPool<byte[]> MEDIUM_BUFFER_POOL = 
        new ObjectPool<>(() -> new byte[8192], 20);  // 8KB buffers
    
    private static final ObjectPool<byte[]> LARGE_BUFFER_POOL = 
        new ObjectPool<>(() -> new byte[65536], 5);  // 64KB buffers
    
    private AdvancedMemoryManager() {}
    
    /**
     * Get a buffer from the appropriate pool
     */
    public static byte[] getBuffer(int size) {
        if (size <= 1024) {
            return SMALL_BUFFER_POOL.acquire();
        } else if (size <= 8192) {
            return MEDIUM_BUFFER_POOL.acquire();
        } else if (size <= 65536) {
            return LARGE_BUFFER_POOL.acquire();
        } else {
            // For very large buffers, create new one
            trackAllocation(size);
            return new byte[size];
        }
    }
    
    /**
     * Return a buffer to the appropriate pool
     */
    public static void releaseBuffer(byte[] buffer) {
        if (buffer == null) return;
        
        int size = buffer.length;
        if (size == 1024) {
            SMALL_BUFFER_POOL.release(buffer);
        } else if (size == 8192) {
            MEDIUM_BUFFER_POOL.release(buffer);
        } else if (size == 65536) {
            LARGE_BUFFER_POOL.release(buffer);
        } else {
            // Large buffers are not pooled
            trackDeallocation(size);
        }
    }
    
    /**
     * Store object in weak reference cache
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFromWeakCache(String key, java.util.function.Supplier<T> supplier) {
        WeakReference<Object> ref = weakCache.get(key);
        if (ref != null) {
            T cached = (T) ref.get();
            if (cached != null) {
                return cached;
            }
        }
        
        // Create new object and cache it
        T newObject = supplier.get();
        weakCache.put(key, new WeakReference<>(newObject));
        return newObject;
    }
    
    /**
     * Store data in off-heap simulation
     */
    public static void storeOffHeap(String key, byte[] data) {
        offHeapStorage.put(key, data.clone()); // Clone to avoid external modifications
    }
    
    /**
     * Retrieve data from off-heap simulation
     */
    public static byte[] getFromOffHeap(String key) {
        byte[] data = offHeapStorage.get(key);
        return data != null ? data.clone() : null; // Clone to avoid external modifications
    }
    
    /**
     * Remove data from off-heap simulation
     */
    public static void removeFromOffHeap(String key) {
        offHeapStorage.remove(key);
    }
    
    /**
     * Track memory allocation
     */
    private static void trackAllocation(long bytes) {
        totalAllocatedBytes.addAndGet(bytes);
    }
    
    /**
     * Track memory deallocation
     */
    private static void trackDeallocation(long bytes) {
        totalDeallocatedBytes.addAndGet(bytes);
    }
    
    /**
     * Get current memory usage statistics
     */
    public static String getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        long allocated = totalAllocatedBytes.get();
        long deallocated = totalDeallocatedBytes.get();
        long netAllocated = allocated - deallocated;
        
        return String.format(
            "JVM Memory: %d/%d MB (%.1f%%), Custom Tracking: %d KB net, Weak Cache: %d, Off-heap: %d",
            usedMemory / (1024 * 1024),
            maxMemory / (1024 * 1024),
            (double) usedMemory / maxMemory * 100.0,
            netAllocated / 1024,
            weakCache.size(),
            offHeapStorage.size()
        );
    }
    
    /**
     * Force garbage collection and cleanup
     */
    public static void forceCleanup() {
        // Clean up weak references
        weakCache.entrySet().removeIf(entry -> entry.getValue().get() == null);
        
        // Suggest garbage collection
        System.gc();
        
        AdvancedLogger.info("AdvancedMemoryManager", "forceCleanup", "Memory cleanup completed: %s", getMemoryStats());
    }
    
    /**
     * Memory-efficient string interning for frequently used strings
     */
    private static final ConcurrentHashMap<String, WeakReference<String>> stringInternCache = 
        new ConcurrentHashMap<>();
    
    public static String internString(String str) {
        if (str == null) return null;
        
        WeakReference<String> ref = stringInternCache.get(str);
        if (ref != null) {
            String cached = ref.get();
            if (cached != null) {
                return cached;
            }
        }
        
        // Cache the string
        stringInternCache.put(str, new WeakReference<>(str));
        return str;
    }
    
    /**
     * Compact representation for boolean arrays (bit packing)
     */
    public static class CompactBooleanArray {
        private final long[] data;
        private final int size;
        
        public CompactBooleanArray(int size) {
            this.size = size;
            this.data = new long[(size + 63) / 64]; // 64 bits per long
        }
        
        public void set(int index, boolean value) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException();
            }
            
            int longIndex = index / 64;
            int bitIndex = index % 64;
            
            if (value) {
                data[longIndex] |= (1L << bitIndex);
            } else {
                data[longIndex] &= ~(1L << bitIndex);
            }
        }
        
        public boolean get(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException();
            }
            
            int longIndex = index / 64;
            int bitIndex = index % 64;
            
            return (data[longIndex] & (1L << bitIndex)) != 0;
        }
        
        public int size() {
            return size;
        }
        
        public long getMemoryUsage() {
            return data.length * 8L; // 8 bytes per long
        }
    }
    
    /**
     * Clear all caches and pools
     */
    public static void clearAll() {
        weakCache.clear();
        offHeapStorage.clear();
        stringInternCache.clear();
        SMALL_BUFFER_POOL.clear();
        MEDIUM_BUFFER_POOL.clear();
        LARGE_BUFFER_POOL.clear();
    }
}