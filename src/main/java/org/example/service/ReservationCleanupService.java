package org.example.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Сервис для периодической очистки истекших бронирований
 * Теперь упрощенный - только для демонстрации архитектуры
 */
public class ReservationCleanupService {
    
    private static ScheduledExecutorService scheduler;
    
    /**
     * Запустить сервис очистки
     */
    public static void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return; // Уже запущен
        }
        
        scheduler = Executors.newScheduledThreadPool(1);
        
        // Запускаем задачу каждые 5 минут
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Теперь просто логируем, что сервис работает
                System.out.println("🔄 Reservation cleanup service is running...");
            } catch (Exception e) {
                System.err.println("❌ Error in reservation cleanup service: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.MINUTES);
        
        System.out.println("✅ Reservation cleanup service started");
    }
    
    /**
     * Остановить сервис очистки
     */
    public static void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("🛑 Reservation cleanup service stopped");
        }
    }
}