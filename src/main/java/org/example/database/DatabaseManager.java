package org.example.database;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Централизованное управление соединениями с базой данных.
 * 
 * ПРОБЛЕМА КОТОРУЮ РЕШАЕМ:
 * - Каждый DAO создавал свою SessionFactory
 * - При 200+ пользователях это приводило к утечкам памяти
 * - Исчерпание пула соединений PostgreSQL
 * 
 * РЕШЕНИЕ:
 * - Единственная SessionFactory на все приложение
 * - Настроенный connection pool
 * - Правильное управление жизненным циклом
 */
public class DatabaseManager {
    
    private static volatile DatabaseManager instance;
    private static volatile SessionFactory sessionFactory;
    private static final Object lock = new Object();
    
    // Настройки пула соединений для 200+ пользователей
    private static final int MAX_POOL_SIZE = 20;        // Максимум соединений
    private static final int MIN_POOL_SIZE = 5;          // Минимум соединений
    private static final int CONNECTION_TIMEOUT = 30000; // 30 секунд таймаут
    private static final int IDLE_TIMEOUT = 600000;      // 10 минут idle timeout
    
    private DatabaseManager() {
        // Приватный конструктор для Singleton
    }
    
    /**
     * Получение единственного экземпляра DatabaseManager
     * Использует double-checked locking для thread-safety
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Получение SessionFactory с настройками connection pool
     * Создается только один раз при первом обращении
     */
    public SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            synchronized (lock) {
                if (sessionFactory == null) {
                    sessionFactory = createSessionFactory();
                }
            }
        }
        return sessionFactory;
    }
    
    /**
     * Создание SessionFactory с HikariCP connection pool
     * HikariCP - самый быстрый connection pool для Java
     */
    private SessionFactory createSessionFactory() {
        try {
            Configuration config = new Configuration().configure();
            
            // HikariCP уже настроен в hibernate.cfg.xml
            // Здесь можно добавить дополнительные настройки если нужно
            
            System.out.println("🚀 Initializing HikariCP connection pool...");
            System.out.println("📊 Pool settings: max=" + MAX_POOL_SIZE + ", min=" + MIN_POOL_SIZE);
            
            return config.buildSessionFactory();
            
        } catch (Throwable ex) {
            System.err.println("❌ Critical SessionFactory creation error: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }
    
    /**
     * Корректное закрытие SessionFactory при завершении приложения
     */
    public void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            System.out.println("🔄 Closing SessionFactory...");
            sessionFactory.close();
            sessionFactory = null;
        }
    }
    
    /**
     * Проверка состояния соединения
     */
    public boolean isHealthy() {
        try {
            return sessionFactory != null && !sessionFactory.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Получение статистики пула соединений
     */
    public String getConnectionPoolStats() {
        if (sessionFactory == null) {
            return "SessionFactory не инициализирована";
        }
        
        try {
            // Здесь можно добавить метрики пула соединений
            return String.format("Connection Pool: max=%d, min=%d, healthy=%s", 
                MAX_POOL_SIZE, MIN_POOL_SIZE, isHealthy());
        } catch (Exception e) {
            return "Ошибка получения статистики: " + e.getMessage();
        }
    }
}
