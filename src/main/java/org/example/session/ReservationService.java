package org.example.session;

import org.example.dao.ProductDAO;
import org.example.table.Product;
import org.example.table.User;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Сервис для управления бронированием товаров
 */
public class ReservationService {
    
    private static final ReservationService instance = new ReservationService();
    private final ConcurrentHashMap<String, Reservation> reservations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    private ReservationService() {
        // Запускаем задачу очистки просроченных броней каждые 5 минут
        scheduler.scheduleAtFixedRate(this::cleanupExpiredReservations, 5, 5, TimeUnit.MINUTES);
    }
    
    public static ReservationService getInstance() {
        return instance;
    }
    
    /**
     * Забронировать товар за пользователем
     */
    public boolean reserveProduct(User user, Product product) {
        String key = user.getIdUser() + "_" + product.getIdProduct();
        
        // Проверяем, не забронирован ли уже товар этим пользователем
        if (reservations.containsKey(key)) {
            return false; // Уже забронирован
        }
        
        // Проверяем доступность товара
        if (!product.hasAvailableSlots()) {
            return false; // Нет свободных мест
        }
        
        // Увеличиваем количество участников
        boolean success = ReservationManager.incrementProductParticipants(product.getIdProduct());
        
        if (success) {
            // Создаем бронь
            Reservation reservation = new Reservation(user, product, LocalDateTime.now());
            reservations.put(key, reservation);
            
            System.out.println("✅ Product " + product.getIdProduct() + " reserved for user " + user.getIdUser());
            return true;
        }
        
        return false;
    }
    
    /**
     * Отменить бронь товара
     */
    public boolean cancelReservation(User user, Product product) {
        String key = user.getIdUser() + "_" + product.getIdProduct();
        Reservation reservation = reservations.remove(key);
        
        if (reservation != null) {
            // Уменьшаем количество участников
            ReservationManager.decrementProductParticipants(product.getIdProduct());
            
            System.out.println("❌ Reservation cancelled for user " + user.getIdUser() + ", product " + product.getIdProduct());
            return true;
        }
        
        return false;
    }
    
    /**
     * Проверить, забронирован ли товар пользователем
     */
    public boolean isReservedByUser(User user, Product product) {
        String key = user.getIdUser() + "_" + product.getIdProduct();
        return reservations.containsKey(key);
    }
    
    /**
     * Получить время бронирования
     */
    public LocalDateTime getReservationTime(User user, Product product) {
        String key = user.getIdUser() + "_" + product.getIdProduct();
        Reservation reservation = reservations.get(key);
        return reservation != null ? reservation.getReservedAt() : null;
    }
    
    /**
     * Очистка просроченных броней (старше 30 минут)
     */
    private void cleanupExpiredReservations() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);
        
        reservations.entrySet().removeIf(entry -> {
            Reservation reservation = entry.getValue();
            if (reservation.getReservedAt().isBefore(cutoffTime)) {
                // Уменьшаем количество участников
                ReservationManager.decrementProductParticipants(reservation.getProduct().getIdProduct());
                
                System.out.println("🕐 Auto-cancelled expired reservation for user " + 
                    reservation.getUser().getIdUser() + ", product " + reservation.getProduct().getIdProduct());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Получить количество активных броней
     */
    public int getActiveReservationsCount() {
        return reservations.size();
    }
    
    /**
     * Остановить сервис
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Класс для хранения информации о бронировании
     */
    private static class Reservation {
        private final User user;
        private final Product product;
        private final LocalDateTime reservedAt;
        
        public Reservation(User user, Product product, LocalDateTime reservedAt) {
            this.user = user;
            this.product = product;
            this.reservedAt = reservedAt;
        }
        
        public User getUser() { return user; }
        public Product getProduct() { return product; }
        public LocalDateTime getReservedAt() { return reservedAt; }
    }
}
