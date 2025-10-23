package org.example.session;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Менеджер бронирования товаров с таймаутом
 */
public class ReservationManager {
    private static final ReservationManager instance = new ReservationManager();
    private final Map<Long, LocalDateTime> reservations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final int RESERVATION_TIMEOUT_MINUTES = 15;
    
    private ReservationManager() {
        // Запускаем проверку таймаутов каждую минуту
        scheduler.scheduleAtFixedRate(this::checkTimeouts, 1, 1, TimeUnit.MINUTES);
    }
    
    public static ReservationManager getInstance() {
        return instance;
    }
    
    /**
     * Создать бронь для пользователя
     */
    public void createReservation(Long chatId) {
        reservations.put(chatId, LocalDateTime.now());
        System.out.println("🕐 Reservation created for user: " + chatId);
    }
    
    /**
     * Проверить, есть ли активная бронь
     */
    public boolean hasActiveReservation(Long chatId) {
        LocalDateTime reservationTime = reservations.get(chatId);
        if (reservationTime == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutesPassed = java.time.Duration.between(reservationTime, now).toMinutes();
        
        if (minutesPassed >= RESERVATION_TIMEOUT_MINUTES) {
            reservations.remove(chatId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Отменить бронь
     */
    public void cancelReservation(Long chatId) {
        reservations.remove(chatId);
        System.out.println("❌ Reservation cancelled for user: " + chatId);
    }
    
    /**
     * Получить оставшееся время брони в минутах
     */
    public long getRemainingTime(Long chatId) {
        LocalDateTime reservationTime = reservations.get(chatId);
        if (reservationTime == null) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutesPassed = java.time.Duration.between(reservationTime, now).toMinutes();
        return Math.max(0, RESERVATION_TIMEOUT_MINUTES - minutesPassed);
    }
    
    /**
     * Проверка таймаутов
     */
    private void checkTimeouts() {
        LocalDateTime now = LocalDateTime.now();
        reservations.entrySet().removeIf(entry -> {
            long minutesPassed = java.time.Duration.between(entry.getValue(), now).toMinutes();
            if (minutesPassed >= RESERVATION_TIMEOUT_MINUTES) {
                System.out.println("⏰ Reservation timeout for user: " + entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Завершение работы
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
