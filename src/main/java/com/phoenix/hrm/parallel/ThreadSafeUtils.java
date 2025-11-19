package com.phoenix.hrm.parallel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-Safe Utilities for Phoenix HRM Parallel Test Execution
 * 
 * Provides comprehensive thread-safe utilities including:
 * - Thread-safe data structures and collections
 * - Resource synchronization mechanisms
 * - Thread-local storage management
 * - Concurrent execution helpers
 * - Thread safety validation tools
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 3.0
 * @since Phase 3
 */
public class ThreadSafeUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ThreadSafeUtils.class);
    
    /**
     * Thread-safe counter with additional features
     */
    public static class SafeCounter {
        private final AtomicLong counter = new AtomicLong(0);
        private final String name;
        private final long maxValue;
        
        public SafeCounter(String name) {
            this(name, Long.MAX_VALUE);
        }
        
        public SafeCounter(String name, long maxValue) {
            this.name = name;
            this.maxValue = maxValue;
        }
        
        public long increment() {
            long newValue = counter.incrementAndGet();
            if (newValue > maxValue) {
                counter.set(maxValue);
                return maxValue;
            }
            return newValue;
        }
        
        public long decrement() {
            long newValue = counter.decrementAndGet();
            if (newValue < 0) {
                counter.set(0);
                return 0;
            }
            return newValue;
        }
        
        public long get() {
            return counter.get();
        }
        
        public void reset() {
            counter.set(0);
        }
        
        public boolean compareAndSet(long expected, long newValue) {
            return counter.compareAndSet(expected, newValue);
        }
        
        public String getName() {
            return name;
        }
    }
    
    /**
     * Thread-safe registry for storing and retrieving objects
     */
    public static class SafeRegistry<T> {
        private final ConcurrentHashMap<String, T> registry = new ConcurrentHashMap<>();
        private final String registryName;
        
        public SafeRegistry(String registryName) {
            this.registryName = registryName;
        }
        
        public void register(String key, T value) {
            registry.put(key, value);
            logger.debug("Registered {} in {}: {}", value.getClass().getSimpleName(), registryName, key);
        }
        
        public T get(String key) {
            return registry.get(key);
        }
        
        public T remove(String key) {
            T removed = registry.remove(key);
            if (removed != null) {
                logger.debug("Removed from {}: {}", registryName, key);
            }
            return removed;
        }
        
        public boolean contains(String key) {
            return registry.containsKey(key);
        }
        
        public Set<String> getKeys() {
            return new HashSet<>(registry.keySet());
        }
        
        public Collection<T> getValues() {
            return new ArrayList<>(registry.values());
        }
        
        public int size() {
            return registry.size();
        }
        
        public void clear() {
            int size = registry.size();
            registry.clear();
            logger.debug("Cleared {} with {} entries", registryName, size);
        }
        
        public Map<String, T> snapshot() {
            return new HashMap<>(registry);
        }
    }
    
    /**
     * Thread-safe barrier for synchronizing multiple threads
     */
    public static class SafeBarrier {
        private final CyclicBarrier barrier;
        private final String barrierName;
        private final AtomicInteger waitingThreads = new AtomicInteger(0);
        
        public SafeBarrier(int parties, String barrierName) {
            this.barrier = new CyclicBarrier(parties);
            this.barrierName = barrierName;
        }
        
        public SafeBarrier(int parties, String barrierName, Runnable barrierAction) {
            this.barrier = new CyclicBarrier(parties, barrierAction);
            this.barrierName = barrierName;
        }
        
        public void await() throws InterruptedException, BrokenBarrierException {
            int waiting = waitingThreads.incrementAndGet();
            logger.debug("Thread {} waiting at barrier {} ({} waiting)", 
                Thread.currentThread().getName(), barrierName, waiting);
            
            try {
                barrier.await();
            } finally {
                waitingThreads.decrementAndGet();
            }
        }
        
        public boolean await(long timeout, TimeUnit unit) 
                throws InterruptedException, BrokenBarrierException, TimeoutException {
            int waiting = waitingThreads.incrementAndGet();
            logger.debug("Thread {} waiting at barrier {} with timeout {} {} ({} waiting)", 
                Thread.currentThread().getName(), barrierName, timeout, unit, waiting);
            
            try {
                barrier.await(timeout, unit);
                return true;
            } catch (TimeoutException e) {
                logger.warn("Timeout waiting at barrier {}: {} {}", barrierName, timeout, unit);
                throw e;
            } finally {
                waitingThreads.decrementAndGet();
            }
        }
        
        public int getNumberWaiting() {
            return barrier.getNumberWaiting();
        }
        
        public boolean isBroken() {
            return barrier.isBroken();
        }
        
        public void reset() {
            barrier.reset();
            waitingThreads.set(0);
            logger.debug("Barrier {} reset", barrierName);
        }
    }
    
    /**
     * Thread-safe event bus for inter-thread communication
     */
    public static class SafeEventBus {
        private final Map<Class<?>, Set<EventListener<?>>> listeners = new ConcurrentHashMap<>();
        private final ExecutorService eventExecutor;
        private final String busName;
        
        public SafeEventBus(String busName) {
            this.busName = busName;
            this.eventExecutor = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, busName + "-EventHandler");
                t.setDaemon(true);
                return t;
            });
        }
        
        public <T> void subscribe(Class<T> eventType, EventListener<T> listener) {
            listeners.computeIfAbsent(eventType, k -> ConcurrentHashMap.newKeySet()).add(listener);
            logger.debug("Subscribed listener to {} events in {}", eventType.getSimpleName(), busName);
        }
        
        public <T> void unsubscribe(Class<T> eventType, EventListener<T> listener) {
            Set<EventListener<?>> eventListeners = listeners.get(eventType);
            if (eventListeners != null) {
                eventListeners.remove(listener);
                logger.debug("Unsubscribed listener from {} events in {}", eventType.getSimpleName(), busName);
            }
        }
        
        @SuppressWarnings("unchecked")
        public <T> void publish(T event) {
            Class<?> eventType = event.getClass();
            Set<EventListener<?>> eventListeners = listeners.get(eventType);
            
            if (eventListeners != null && !eventListeners.isEmpty()) {
                logger.debug("Publishing {} event to {} listeners in {}", 
                    eventType.getSimpleName(), eventListeners.size(), busName);
                
                for (EventListener<?> listener : eventListeners) {
                    eventExecutor.submit(() -> {
                        try {
                            ((EventListener<T>) listener).onEvent(event);
                        } catch (Exception e) {
                            logger.error("Error processing event {} in listener: {}", 
                                eventType.getSimpleName(), e.getMessage());
                        }
                    });
                }
            }
        }
        
        public void shutdown() {
            eventExecutor.shutdown();
            try {
                if (!eventExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    eventExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                eventExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.debug("Event bus {} shut down", busName);
        }
    }
    
    /**
     * Event listener interface
     */
    public interface EventListener<T> {
        void onEvent(T event);
    }
    
    /**
     * Thread-safe cache with expiration support
     */
    public static class SafeCache<K, V> {
        private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
        private final long defaultTtlMs;
        private final ScheduledExecutorService cleanupExecutor;
        private final String cacheName;
        
        private static class CacheEntry<V> {
            final V value;
            final long expiryTime;
            
            CacheEntry(V value, long ttlMs) {
                this.value = value;
                this.expiryTime = System.currentTimeMillis() + ttlMs;
            }
            
            boolean isExpired() {
                return System.currentTimeMillis() > expiryTime;
            }
        }
        
        public SafeCache(String cacheName, long defaultTtlMs) {
            this.cacheName = cacheName;
            this.defaultTtlMs = defaultTtlMs;
            this.cleanupExecutor = Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, cacheName + "-Cleanup");
                t.setDaemon(true);
                return t;
            });
            
            // Schedule periodic cleanup
            cleanupExecutor.scheduleWithFixedDelay(this::cleanup, defaultTtlMs, defaultTtlMs, TimeUnit.MILLISECONDS);
        }
        
        public void put(K key, V value) {
            put(key, value, defaultTtlMs);
        }
        
        public void put(K key, V value, long ttlMs) {
            cache.put(key, new CacheEntry<>(value, ttlMs));
            logger.debug("Cached entry in {}: {} (TTL: {}ms)", cacheName, key, ttlMs);
        }
        
        public V get(K key) {
            CacheEntry<V> entry = cache.get(key);
            if (entry != null) {
                if (!entry.isExpired()) {
                    return entry.value;
                } else {
                    cache.remove(key);
                    logger.debug("Expired entry removed from {}: {}", cacheName, key);
                }
            }
            return null;
        }
        
        public V getOrCompute(K key, java.util.function.Supplier<V> supplier) {
            V value = get(key);
            if (value == null) {
                value = supplier.get();
                if (value != null) {
                    put(key, value);
                }
            }
            return value;
        }
        
        public boolean containsKey(K key) {
            CacheEntry<V> entry = cache.get(key);
            return entry != null && !entry.isExpired();
        }
        
        public void remove(K key) {
            cache.remove(key);
            logger.debug("Manually removed from {}: {}", cacheName, key);
        }
        
        public void clear() {
            int size = cache.size();
            cache.clear();
            logger.debug("Cleared cache {} with {} entries", cacheName, size);
        }
        
        public int size() {
            cleanup(); // Remove expired entries first
            return cache.size();
        }
        
        private void cleanup() {
            int expiredCount = 0;
            for (Iterator<Map.Entry<K, CacheEntry<V>>> it = cache.entrySet().iterator(); it.hasNext();) {
                Map.Entry<K, CacheEntry<V>> entry = it.next();
                if (entry.getValue().isExpired()) {
                    it.remove();
                    expiredCount++;
                }
            }
            if (expiredCount > 0) {
                logger.debug("Cleaned up {} expired entries from {}", expiredCount, cacheName);
            }
        }
        
        public void shutdown() {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            clear();
            logger.debug("Cache {} shut down", cacheName);
        }
    }
    
    /**
     * Thread-safe rate limiter
     */
    public static class SafeRateLimiter {
        private final Semaphore semaphore;
        private final ScheduledExecutorService scheduler;
        private final int maxPermits;
        private final long refillIntervalMs;
        private final String limiterName;
        private final AtomicInteger currentPermits;
        
        public SafeRateLimiter(String limiterName, int maxPermits, long refillIntervalMs) {
            this.limiterName = limiterName;
            this.maxPermits = maxPermits;
            this.refillIntervalMs = refillIntervalMs;
            this.semaphore = new Semaphore(maxPermits, true);
            this.currentPermits = new AtomicInteger(maxPermits);
            this.scheduler = Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, limiterName + "-Refill");
                t.setDaemon(true);
                return t;
            });
            
            // Schedule permit refills
            scheduler.scheduleAtFixedRate(this::refill, refillIntervalMs, refillIntervalMs, TimeUnit.MILLISECONDS);
        }
        
        public boolean tryAcquire() {
            return tryAcquire(1);
        }
        
        public boolean tryAcquire(int permits) {
            boolean acquired = semaphore.tryAcquire(permits);
            if (acquired) {
                currentPermits.addAndGet(-permits);
                logger.debug("Acquired {} permits from {} (remaining: {})", 
                    permits, limiterName, currentPermits.get());
            } else {
                logger.debug("Failed to acquire {} permits from {} (available: {})", 
                    permits, limiterName, currentPermits.get());
            }
            return acquired;
        }
        
        public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
            return tryAcquire(1, timeout, unit);
        }
        
        public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException {
            boolean acquired = semaphore.tryAcquire(permits, timeout, unit);
            if (acquired) {
                currentPermits.addAndGet(-permits);
                logger.debug("Acquired {} permits from {} with timeout (remaining: {})", 
                    permits, limiterName, currentPermits.get());
            }
            return acquired;
        }
        
        public void acquire() throws InterruptedException {
            acquire(1);
        }
        
        public void acquire(int permits) throws InterruptedException {
            semaphore.acquire(permits);
            currentPermits.addAndGet(-permits);
            logger.debug("Acquired {} permits from {} (remaining: {})", 
                permits, limiterName, currentPermits.get());
        }
        
        private void refill() {
            int available = semaphore.availablePermits();
            int toRefill = maxPermits - available;
            if (toRefill > 0) {
                semaphore.release(toRefill);
                currentPermits.set(maxPermits);
                logger.debug("Refilled {} permits in {} (total: {})", toRefill, limiterName, maxPermits);
            }
        }
        
        public int availablePermits() {
            return semaphore.availablePermits();
        }
        
        public void shutdown() {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.debug("Rate limiter {} shut down", limiterName);
        }
    }
    
    /**
     * Thread-safe execution tracker
     */
    public static class ExecutionTracker {
        private final Map<String, ExecutionMetrics> metrics = new ConcurrentHashMap<>();
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        
        public static class ExecutionMetrics {
            private final AtomicLong executionCount = new AtomicLong(0);
            private final AtomicLong totalDuration = new AtomicLong(0);
            private final AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);
            private final AtomicLong maxDuration = new AtomicLong(0);
            private volatile long lastExecution = 0;
            
            public void recordExecution(long duration) {
                executionCount.incrementAndGet();
                totalDuration.addAndGet(duration);
                lastExecution = System.currentTimeMillis();
                
                // Update min duration
                long currentMin = minDuration.get();
                while (duration < currentMin && !minDuration.compareAndSet(currentMin, duration)) {
                    currentMin = minDuration.get();
                }
                
                // Update max duration
                long currentMax = maxDuration.get();
                while (duration > currentMax && !maxDuration.compareAndSet(currentMax, duration)) {
                    currentMax = maxDuration.get();
                }
            }
            
            public long getExecutionCount() { return executionCount.get(); }
            public long getTotalDuration() { return totalDuration.get(); }
            public long getAverageDuration() { 
                long count = executionCount.get();
                return count > 0 ? totalDuration.get() / count : 0;
            }
            public long getMinDuration() { 
                long min = minDuration.get();
                return min == Long.MAX_VALUE ? 0 : min;
            }
            public long getMaxDuration() { return maxDuration.get(); }
            public long getLastExecution() { return lastExecution; }
        }
        
        public void recordExecution(String operation, long duration) {
            lock.readLock().lock();
            try {
                metrics.computeIfAbsent(operation, k -> new ExecutionMetrics()).recordExecution(duration);
            } finally {
                lock.readLock().unlock();
            }
        }
        
        public ExecutionMetrics getMetrics(String operation) {
            lock.readLock().lock();
            try {
                return metrics.get(operation);
            } finally {
                lock.readLock().unlock();
            }
        }
        
        public Map<String, ExecutionMetrics> getAllMetrics() {
            lock.readLock().lock();
            try {
                return new HashMap<>(metrics);
            } finally {
                lock.readLock().unlock();
            }
        }
        
        public void reset() {
            lock.writeLock().lock();
            try {
                metrics.clear();
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    /**
     * Utility method to execute code with timing
     */
    public static <T> T executeWithTiming(String operation, ExecutionTracker tracker, java.util.concurrent.Callable<T> task) throws Exception {
        long startTime = System.currentTimeMillis();
        try {
            return task.call();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            if (tracker != null) {
                tracker.recordExecution(operation, duration);
            }
        }
    }
    
    /**
     * Create a thread-safe list
     */
    public static <T> List<T> createSafeList() {
        return Collections.synchronizedList(new ArrayList<>());
    }
    
    /**
     * Create a thread-safe set
     */
    public static <T> Set<T> createSafeSet() {
        return ConcurrentHashMap.newKeySet();
    }
    
    /**
     * Create a thread-safe map
     */
    public static <K, V> Map<K, V> createSafeMap() {
        return new ConcurrentHashMap<>();
    }
    
    /**
     * Validate thread safety of an object (basic check)
     */
    public static boolean isThreadSafe(Object obj) {
        if (obj == null) return true;
        
        String className = obj.getClass().getName();
        
        // Known thread-safe classes
        return className.startsWith("java.util.concurrent.") ||
               className.contains("Atomic") ||
               className.contains("ThreadSafe") ||
               className.contains("Concurrent") ||
               obj instanceof ConcurrentMap ||
               obj instanceof BlockingQueue;
    }
}