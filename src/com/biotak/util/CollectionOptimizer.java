package com.biotak.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Optimized collection operations to replace expensive stream().collect() calls
 * مشکل: در خط 701 BiotakTrigger استفاده مکرر از stream operations
 */
public class CollectionOptimizer {
    
    /**
     * Optimized alternative to stream().collect() for Map truncation
     * جایگزین برای خط 701: entries.stream().collect(...)
     */
    public static <K, V> Map<K, V> truncateMap(Map<K, V> source, int maxSize) {
        if (source.size() <= maxSize) {
            return source;
        }
        
        Map<K, V> result = new LinkedHashMap<>(maxSize);
        int count = 0;
        
        for (Map.Entry<K, V> entry : source.entrySet()) {
            if (count >= maxSize) break;
            result.put(entry.getKey(), entry.getValue());
            count++;
        }
        
        return result;
    }
    
    /**
     * Optimized map copying without stream overhead
     */
    public static <K, V> Map<K, V> copyMapEfficiently(Map<K, V> source) {
        if (source.isEmpty()) return new HashMap<>();
        
        Map<K, V> result = new HashMap<>(source.size());
        result.putAll(source);
        return result;
    }
    
    /**
     * Fast map filtering without streams
     */
    public static <K, V> Map<K, V> filterMap(Map<K, V> source, 
                                           java.util.function.Predicate<Map.Entry<K, V>> predicate) {
        Map<K, V> result = new HashMap<>();
        
        for (Map.Entry<K, V> entry : source.entrySet()) {
            if (predicate.test(entry)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        
        return result;
    }
    
    /**
     * Optimized list truncation without subList() overhead
     */
    public static <T> List<T> truncateList(List<T> source, int maxSize) {
        if (source.size() <= maxSize) {
            return source;
        }
        
        List<T> result = new ArrayList<>(maxSize);
        for (int i = 0; i < maxSize && i < source.size(); i++) {
            result.add(source.get(i));
        }
        
        return result;
    }
    
    /**
     * Fast string joining without stream collectors
     */
    public static String joinStrings(Collection<String> strings, String delimiter) {
        if (strings.isEmpty()) return "";
        if (strings.size() == 1) return strings.iterator().next();
        
        StringBuilder sb = PoolManager.getStringBuilder();
        try {
            boolean first = true;
            for (String str : strings) {
                if (!first) {
                    sb.append(delimiter);
                }
                sb.append(str);
                first = false;
            }
            return sb.toString();
        } finally {
            PoolManager.releaseStringBuilder(sb);
        }
    }
    
    /**
     * Optimized map transformation without streams
     */
    public static <K, V, R> Map<K, R> transformMapValues(Map<K, V> source, 
                                                        java.util.function.Function<V, R> transformer) {
        Map<K, R> result = new HashMap<>(source.size());
        
        for (Map.Entry<K, V> entry : source.entrySet()) {
            result.put(entry.getKey(), transformer.apply(entry.getValue()));
        }
        
        return result;
    }
    
    /**
     * Fast list to array conversion
     */
    public static <T> T[] listToArray(List<T> list, Class<T> clazz) {
        @SuppressWarnings("unchecked")
        T[] array = (T[]) java.lang.reflect.Array.newInstance(clazz, list.size());
        return list.toArray(array);
    }
    
    /**
     * Batch operation optimizer for large collections
     */
    public static <T> void processBatches(Collection<T> collection, int batchSize, 
                                        java.util.function.Consumer<List<T>> processor) {
        List<T> batch = new ArrayList<>(batchSize);
        
        for (T item : collection) {
            batch.add(item);
            
            if (batch.size() >= batchSize) {
                processor.accept(batch);
                batch.clear();
            }
        }
        
        // Process remaining items
        if (!batch.isEmpty()) {
            processor.accept(batch);
        }
    }
    
    /**
     * Memory-efficient distinct operation
     */
    public static <T> List<T> getDistinct(List<T> source) {
        if (source.size() <= 1) return source;
        
        Set<T> seen = new HashSet<>(source.size());
        List<T> result = new ArrayList<>();
        
        for (T item : source) {
            if (seen.add(item)) {
                result.add(item);
            }
        }
        
        return result;
    }
    
    /**
     * Fast collection size check without creating collections
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
    
    /**
     * Optimized intersection of two sets
     */
    public static <T> Set<T> intersect(Set<T> set1, Set<T> set2) {
        if (isEmpty(set1) || isEmpty(set2)) {
            return Collections.emptySet();
        }
        
        // Use smaller set for iteration
        Set<T> smaller = set1.size() <= set2.size() ? set1 : set2;
        Set<T> larger = set1.size() > set2.size() ? set1 : set2;
        
        Set<T> result = new HashSet<>();
        for (T item : smaller) {
            if (larger.contains(item)) {
                result.add(item);
            }
        }
        
        return result;
    }
    
    /**
     * Performance test method
     */
    public static void benchmarkOperations() {
        Map<String, Double> testMap = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            testMap.put("key" + i, (double) i);
        }
        
        // Benchmark stream vs optimized
        long startTime = System.nanoTime();
        
        // Stream operation
        Map<String, Double> streamResult = testMap.entrySet().stream()
            .limit(5000)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        long streamTime = System.nanoTime() - startTime;
        
        startTime = System.nanoTime();
        
        // Optimized operation
        Map<String, Double> optimizedResult = truncateMap(testMap, 5000);
        
        long optimizedTime = System.nanoTime() - startTime;
        
        System.out.printf("Stream time: %d ns, Optimized time: %d ns, Speedup: %.2fx%n",
                         streamTime, optimizedTime, (double) streamTime / optimizedTime);
    }
}
