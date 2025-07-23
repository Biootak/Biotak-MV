package com.biotak.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized I/O operations for better performance
 */
public final class IOOptimizer {
    
    // Buffer pool for I/O operations
    private static final ObjectPool<ByteBuffer> BUFFER_POOL = 
        new ObjectPool<>(() -> ByteBuffer.allocateDirect(8192), 10);
    
    // Cache for file channels to avoid repeated opening
    private static final ConcurrentHashMap<String, FileChannel> channelCache = 
        new ConcurrentHashMap<>();
    
    // Thread-local buffers for serialization
    private static final ThreadLocal<ByteArrayOutputStream> BYTE_ARRAY_OUTPUT_STREAM = 
        ThreadLocal.withInitial(() -> new ByteArrayOutputStream(1024));
    
    private static final ThreadLocal<ByteArrayInputStream> BYTE_ARRAY_INPUT_STREAM = 
        ThreadLocal.withInitial(() -> new ByteArrayInputStream(new byte[0]));
    
    private IOOptimizer() {}
    
    /**
     * Fast file writing using NIO
     */
    public static void fastWriteFile(String filePath, byte[] data) throws IOException {
        Path path = java.nio.file.Paths.get(filePath);
        
        // Ensure parent directory exists
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        
        // Use NIO for better performance
        try (FileChannel channel = FileChannel.open(path, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.WRITE, 
                StandardOpenOption.TRUNCATE_EXISTING)) {
            
            ByteBuffer buffer = ByteBuffer.wrap(data);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(false); // Force write to disk
        }
    }
    
    /**
     * Fast file reading using NIO
     */
    public static byte[] fastReadFile(String filePath) throws IOException {
        Path path = java.nio.file.Paths.get(filePath);
        
        if (!Files.exists(path)) {
            return new byte[0];
        }
        
        long fileSize = Files.size(path);
        if (fileSize > Integer.MAX_VALUE) {
            throw new IOException("File too large: " + fileSize);
        }
        
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
            
            while (buffer.hasRemaining()) {
                if (channel.read(buffer) == -1) {
                    break;
                }
            }
            
            return buffer.array();
        }
    }
    
    /**
     * Optimized object serialization
     */
    public static byte[] fastSerialize(Serializable object) throws IOException {
        ByteArrayOutputStream baos = BYTE_ARRAY_OUTPUT_STREAM.get();
        baos.reset(); // Clear previous data
        
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            oos.flush();
            return baos.toByteArray();
        }
    }
    
    /**
     * Optimized object deserialization
     */
    @SuppressWarnings("unchecked")
    public static <T> T fastDeserialize(byte[] data, Class<T> clazz) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (T) ois.readObject();
        }
    }
    
    /**
     * Memory-mapped file reading for large files
     */
    public static ByteBuffer mapFile(String filePath) throws IOException {
        Path path = java.nio.file.Paths.get(filePath);
        
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        }
    }
    
    /**
     * Buffered writing with automatic flushing
     */
    public static class OptimizedWriter implements AutoCloseable {
        private final BufferedWriter writer;
        private final int flushThreshold;
        private int writeCount = 0;
        
        public OptimizedWriter(String filePath, int bufferSize, int flushThreshold) throws IOException {
            this.writer = Files.newBufferedWriter(
                java.nio.file.Paths.get(filePath), 
                java.nio.charset.StandardCharsets.UTF_8
            );
            this.flushThreshold = flushThreshold;
        }
        
        public void writeLine(String line) throws IOException {
            writer.write(line);
            writer.newLine();
            
            if (++writeCount >= flushThreshold) {
                writer.flush();
                writeCount = 0;
            }
        }
        
        @Override
        public void close() throws IOException {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
    }
    
    /**
     * Compressed data storage
     */
    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (java.util.zip.GZIPOutputStream gzos = new java.util.zip.GZIPOutputStream(baos)) {
            gzos.write(data);
            gzos.finish();
            return baos.toByteArray();
        }
    }
    
    /**
     * Compressed data retrieval
     */
    public static byte[] decompress(byte[] compressedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (java.util.zip.GZIPInputStream gzis = new java.util.zip.GZIPInputStream(
                new ByteArrayInputStream(compressedData))) {
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            
            return baos.toByteArray();
        }
    }
    
    /**
     * Async file writing
     */
    public static java.util.concurrent.CompletableFuture<Void> asyncWriteFile(String filePath, byte[] data) {
        return java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                fastWriteFile(filePath, data);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write file: " + filePath, e);
            }
        });
    }
    
    /**
     * Async file reading
     */
    public static java.util.concurrent.CompletableFuture<byte[]> asyncReadFile(String filePath) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return fastReadFile(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file: " + filePath, e);
            }
        });
    }
    
    /**
     * Get I/O statistics
     */
    public static String getIOStats() {
        return String.format(
            "Buffer Pool: %d, Channel Cache: %d",
            BUFFER_POOL.size(),
            channelCache.size()
        );
    }
    
    /**
     * Cleanup resources
     */
    public static void cleanup() {
        // Close all cached channels
        channelCache.values().forEach(channel -> {
            try {
                if (channel.isOpen()) {
                    channel.close();
                }
            } catch (IOException e) {
                Logger.warn("Failed to close file channel: " + e.getMessage());
            }
        });
        channelCache.clear();
        
        // Clear buffer pool
        BUFFER_POOL.clear();
    }
}