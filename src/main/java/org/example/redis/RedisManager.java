package org.example.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * Централизованное управление Redis соединениями.
 * 
 * ПРОБЛЕМА КОТОРУЮ РЕШАЕМ:
 * - Сессии хранились в памяти (ConcurrentHashMap)
 * - При перезапуске сервера все состояния терялись
 * - Нет персистентности между сессиями
 * 
 * РЕШЕНИЕ:
 * - Redis для хранения сессий
 * - Connection pool для Redis
 * - Автоматическое истечение сессий
 */
public class RedisManager {
    
    private static volatile RedisManager instance;
    private static volatile JedisPool jedisPool;
    private static final Object lock = new Object();
    
    // Настройки Redis connection pool
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    private static final int MAX_TOTAL = 20;           // Максимум соединений
    private static final int MAX_IDLE = 10;            // Максимум idle соединений
    private static final int MIN_IDLE = 5;              // Минимум idle соединений
    private static final Duration MAX_WAIT = Duration.ofSeconds(30); // Таймаут ожидания
    
    // Настройки TTL для сессий
    private static final int SESSION_TTL_SECONDS = 24 * 60 * 60; // 24 часа
    private static final int CACHE_TTL_SECONDS = 60 * 60;        // 1 час для кэша
    
    private RedisManager() {
        // Приватный конструктор для Singleton
    }
    
    /**
     * Получение единственного экземпляра RedisManager
     */
    public static RedisManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new RedisManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Получение JedisPool с оптимизированными настройками
     */
    public JedisPool getJedisPool() {
        if (jedisPool == null) {
            synchronized (lock) {
                if (jedisPool == null) {
                    jedisPool = createJedisPool();
                }
            }
        }
        return jedisPool;
    }
    
    /**
     * Создание JedisPool с настройками для высокой нагрузки
     */
    private JedisPool createJedisPool() {
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            
            // Настройки connection pool
            poolConfig.setMaxTotal(MAX_TOTAL);
            poolConfig.setMaxIdle(MAX_IDLE);
            poolConfig.setMinIdle(MIN_IDLE);
            poolConfig.setMaxWaitMillis(MAX_WAIT.toMillis());
            
            // Настройки для высокой производительности
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            poolConfig.setTimeBetweenEvictionRunsMillis(30000);
            poolConfig.setNumTestsPerEvictionRun(3);
            poolConfig.setMinEvictableIdleTimeMillis(60000);
            
            jedisPool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT);
            
            // Silent initialization
            
            // Test connection silently
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
            }
            
            return jedisPool;
            
        } catch (Exception ex) {
            // Silent fallback - system will work without Redis
            return null;
        }
    }
    
    /**
     * Get Jedis resource from pool
     */
    public Jedis getResource() {
        JedisPool pool = getJedisPool();
        if (pool == null) {
            throw new RuntimeException("Redis unavailable");
        }
        return pool.getResource();
    }
    
    /**
     * Сохранение сессии в Redis с TTL
     */
    public void setSession(String key, String value) {
        try (Jedis jedis = getResource()) {
            jedis.setex(key, SESSION_TTL_SECONDS, value);
        }
    }
    
    /**
     * Получение сессии из Redis
     */
    public String getSession(String key) {
        try (Jedis jedis = getResource()) {
            return jedis.get(key);
        }
    }
    
    /**
     * Удаление сессии из Redis
     */
    public void deleteSession(String key) {
        try (Jedis jedis = getResource()) {
            jedis.del(key);
        }
    }
    
    /**
     * Проверка существования сессии
     */
    public boolean existsSession(String key) {
        try (Jedis jedis = getResource()) {
            return jedis.exists(key);
        }
    }
    
    /**
     * Установка TTL для существующего ключа
     */
    public void expireSession(String key, int seconds) {
        try (Jedis jedis = getResource()) {
            jedis.expire(key, seconds);
        }
    }
    
    /**
     * Получение TTL ключа
     */
    public long getSessionTTL(String key) {
        try (Jedis jedis = getResource()) {
            return jedis.ttl(key);
        }
    }
    
    /**
     * Graceful shutdown of JedisPool
     */
    public void shutdown() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            System.out.println("🔄 Closing Redis connection pool...");
            jedisPool.close();
            jedisPool = null;
        }
    }
    
    /**
     * Проверка состояния Redis соединения
     */
    public boolean isHealthy() {
        try {
            JedisPool pool = getJedisPool();
            if (pool == null) {
                return false;
            }
            try (Jedis jedis = pool.getResource()) {
                String pong = jedis.ping();
                return "PONG".equals(pong);
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get Redis statistics
     */
    public String getRedisStats() {
        try {
            JedisPool pool = getJedisPool();
            if (pool == null) {
                return "Redis unavailable - using memory fallback";
            }
            return String.format("Redis Pool: active=%d, idle=%d, healthy=%s", 
                pool.getNumActive(), pool.getNumIdle(), isHealthy());
        } catch (Exception e) {
            return "Redis stats error: " + e.getMessage();
        }
    }
}
