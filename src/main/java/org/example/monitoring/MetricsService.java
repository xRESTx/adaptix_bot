package org.example.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.example.async.AsyncService;
import org.example.database.DatabaseManager;
import org.example.redis.RedisManager;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Сервис метрик и мониторинга для AdaptixBot.
 * 
 * ПРОБЛЕМА КОТОРУЮ РЕШАЕМ:
 * - Нет видимости в производительность системы
 * - Нет метрик для анализа узких мест
 * - Нет мониторинга состояния соединений
 * 
 * РЕШЕНИЕ:
 * - Micrometer + Prometheus для метрик
 * - Метрики производительности
 * - Мониторинг состояния системы
 * - Алерты и дашборды
 */
public class MetricsService {
    
    private static volatile MetricsService instance;
    private static volatile MeterRegistry meterRegistry;
    private static final Object lock = new Object();
    
    // Счетчики событий
    public Counter userMessagesCounter;
    public Counter adminActionsCounter;
    public Counter productCreationsCounter;
    public Counter purchaseRequestsCounter;
    public Counter photoUploadsCounter;
    public Counter errorsCounter;
    
    // Таймеры производительности
    public Timer messageProcessingTimer;
    public Timer photoProcessingTimer;
    public Timer databaseOperationTimer;
    public Timer redisOperationTimer;
    
    // Gauges для состояния системы
    public AtomicLong activeUsers = new AtomicLong(0);
    public AtomicLong activeSessions = new AtomicLong(0);
    public AtomicLong databaseConnections = new AtomicLong(0);
    public AtomicLong redisConnections = new AtomicLong(0);
    
    private MetricsService() {
        // Приватный конструктор для Singleton
    }
    
    /**
     * Получение единственного экземпляра MetricsService
     */
    public static MetricsService getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new MetricsService();
                }
            }
        }
        return instance;
    }
    
    /**
     * Получение MeterRegistry с Prometheus
     */
    public MeterRegistry getMeterRegistry() {
        if (meterRegistry == null) {
            synchronized (lock) {
                if (meterRegistry == null) {
                    meterRegistry = createMeterRegistry();
                    initializeMetrics();
                }
            }
        }
        return meterRegistry;
    }
    
    /**
     * Создание MeterRegistry с Prometheus конфигурацией
     */
    private MeterRegistry createMeterRegistry() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        
        System.out.println("🚀 Initializing Prometheus metrics...");
        System.out.println("📊 Metrics endpoint: /actuator/prometheus");
        
        return registry;
    }
    
    /**
     * Инициализация всех метрик
     */
    private void initializeMetrics() {
        // Счетчики событий
        userMessagesCounter = Counter.builder("adaptix.user.messages")
            .description("Number of user messages")
            .register(meterRegistry);
            
        adminActionsCounter = Counter.builder("adaptix.admin.actions")
            .description("Number of admin actions")
            .register(meterRegistry);
            
        productCreationsCounter = Counter.builder("adaptix.products.created")
            .description("Number of products created")
            .register(meterRegistry);
            
        purchaseRequestsCounter = Counter.builder("adaptix.purchases.requests")
            .description("Number of purchase requests")
            .register(meterRegistry);
            
        photoUploadsCounter = Counter.builder("adaptix.photos.uploads")
            .description("Number of photos uploaded")
            .register(meterRegistry);
            
        errorsCounter = Counter.builder("adaptix.errors.total")
            .description("Total number of errors")
            .register(meterRegistry);
        
        // Таймеры производительности
        messageProcessingTimer = Timer.builder("adaptix.processing.messages")
            .description("Message processing time")
            .register(meterRegistry);
            
        photoProcessingTimer = Timer.builder("adaptix.processing.photos")
            .description("Photo processing time")
            .register(meterRegistry);
            
        databaseOperationTimer = Timer.builder("adaptix.database.operations")
            .description("Database operations time")
            .register(meterRegistry);
            
        redisOperationTimer = Timer.builder("adaptix.redis.operations")
            .description("Redis operations time")
            .register(meterRegistry);
        
        // Gauges для состояния системы
        Gauge.builder("adaptix.users.active", activeUsers, AtomicLong::get)
            .description("Number of active users")
            .register(meterRegistry);
            
        Gauge.builder("adaptix.sessions.active", activeSessions, AtomicLong::get)
            .description("Number of active sessions")
            .register(meterRegistry);
            
        Gauge.builder("adaptix.database.connections", databaseConnections, AtomicLong::get)
            .description("Number of database connections")
            .register(meterRegistry);
            
        Gauge.builder("adaptix.redis.connections", redisConnections, AtomicLong::get)
            .description("Number of Redis connections")
            .register(meterRegistry);
        
        // Системные метрики
        Gauge.builder("adaptix.system.uptime", this, MetricsService::getUptime)
            .description("System uptime in seconds")
            .register(meterRegistry);
        
        System.out.println("✅ Metrics initialized");
    }
    
    /**
     * Регистрация сообщения от пользователя
     */
    public void recordUserMessage() {
        userMessagesCounter.increment();
    }
    
    /**
     * Регистрация действия администратора
     */
    public void recordAdminAction() {
        adminActionsCounter.increment();
    }
    
    /**
     * Регистрация создания товара
     */
    public void recordProductCreation() {
        productCreationsCounter.increment();
    }
    
    /**
     * Регистрация запроса на покупку
     */
    public void recordPurchaseRequest() {
        purchaseRequestsCounter.increment();
    }
    
    /**
     * Регистрация загрузки фото
     */
    public void recordPhotoUpload() {
        photoUploadsCounter.increment();
    }
    
    /**
     * Регистрация ошибки
     */
    public void recordError(String errorType) {
        errorsCounter.increment();
        Counter.builder("adaptix.errors.by_type")
            .tag("type", errorType)
            .description("Errors by type")
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Измерение времени обработки сообщения
     */
    public Timer.Sample startMessageProcessing() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * Завершение измерения времени обработки сообщения
     */
    public void stopMessageProcessing(Timer.Sample sample) {
        sample.stop(messageProcessingTimer);
    }
    
    /**
     * Измерение времени обработки фото
     */
    public Timer.Sample startPhotoProcessing() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * Завершение измерения времени обработки фото
     */
    public void stopPhotoProcessing(Timer.Sample sample) {
        sample.stop(photoProcessingTimer);
    }
    
    /**
     * Измерение времени операции с БД
     */
    public Timer.Sample startDatabaseOperation() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * Завершение измерения времени операции с БД
     */
    public void stopDatabaseOperation(Timer.Sample sample) {
        sample.stop(databaseOperationTimer);
    }
    
    /**
     * Измерение времени операции с Redis
     */
    public Timer.Sample startRedisOperation() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * Завершение измерения времени операции с Redis
     */
    public void stopRedisOperation(Timer.Sample sample) {
        sample.stop(redisOperationTimer);
    }
    
    /**
     * Обновление количества активных пользователей
     */
    public void updateActiveUsers(long count) {
        activeUsers.set(count);
    }
    
    /**
     * Обновление количества активных сессий
     */
    public void updateActiveSessions(long count) {
        activeSessions.set(count);
    }
    
    /**
     * Обновление количества соединений с БД
     */
    public void updateDatabaseConnections(long count) {
        databaseConnections.set(count);
    }
    
    /**
     * Обновление количества соединений с Redis
     */
    public void updateRedisConnections(long count) {
        redisConnections.set(count);
    }
    
    /**
     * Получение времени работы системы
     */
    public double getUptime() {
        return (System.currentTimeMillis() - startTime) / 1000.0;
    }
    
    private static final long startTime = System.currentTimeMillis();
    
    /**
     * Получение Prometheus метрик в формате текста
     */
    public String getPrometheusMetrics() {
        if (meterRegistry == null) {
            return "# Метрики не инициализированы\n";
        }
        return ((PrometheusMeterRegistry) meterRegistry).scrape();
    }
    
    /**
     * Получение сводки метрик
     */
    public String getMetricsSummary() {
        if (userMessagesCounter == null) {
            return "📊 Метрики не инициализированы";
        }
        
        return String.format(
            "📊 Метрики AdaptixBot:\n" +
            "• Сообщения пользователей: %d\n" +
            "• Действия админов: %d\n" +
            "• Создано товаров: %d\n" +
            "• Запросы покупок: %d\n" +
            "• Загружено фото: %d\n" +
            "• Ошибки: %d\n" +
            "• Активные пользователи: %d\n" +
            "• Активные сессии: %d\n" +
            "• Время работы: %.1f сек",
            (long) userMessagesCounter.count(),
            (long) adminActionsCounter.count(),
            (long) productCreationsCounter.count(),
            (long) purchaseRequestsCounter.count(),
            (long) photoUploadsCounter.count(),
            (long) errorsCounter.count(),
            activeUsers.get(),
            activeSessions.get(),
            getUptime()
        );
    }
    
    /**
     * Обновление системных метрик
     */
    public void updateSystemMetrics() {
        // Обновляем метрики соединений
        updateDatabaseConnections(DatabaseManager.getInstance().isHealthy() ? 1 : 0);
        updateRedisConnections(RedisManager.getInstance().isHealthy() ? 1 : 0);
        
        // Здесь можно добавить логику подсчета активных пользователей и сессий
        // updateActiveUsers(getActiveUsersCount());
        // updateActiveSessions(getActiveSessionsCount());
    }
}
