package org.example.session;

import org.example.table.Product;
import org.example.table.User;
import org.example.tgProcessing.LogicUI;
import org.example.tgProcessing.Sent;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Сервис для управления бронированием товаров
 */
public class ReservationService {
    
    private static final long INACTIVITY_WARNING_MINUTES = Long.getLong("reservation.warning.minutes", 30L);
    private static final long CANCELLATION_GRACE_MINUTES = Long.getLong("reservation.cancellation.grace.minutes", 2L);

    private static final ReservationService instance = new ReservationService();
    private final ConcurrentHashMap<String, Reservation> reservations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    private ReservationService() {
        // Запускаем задачу проверки неактивных броней каждую минуту
        scheduler.scheduleAtFixedRate(this::checkInactiveReservations, 0, 1, TimeUnit.MINUTES);
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
            // Создаем бронь с текущим временем активности
            LocalDateTime now = LocalDateTime.now();
            Reservation reservation = new Reservation(user, product, now, now);
            reservations.put(key, reservation);
            
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
     * Обновить время последней активности пользователя в бронировании
     */
    public void updateLastActivity(User user, Product product) {
        String key = user.getIdUser() + "_" + product.getIdProduct();
        Reservation reservation = reservations.get(key);
        if (reservation != null) {
            reservation.updateLastActivity();
        }
    }
    
    /**
     * Проверка неактивных броней и отправка уведомлений
     * Для тестов: 1 минута неактивности
     * В продакшене: 30 минут неактивности
     */
    private void checkInactiveReservations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime inactiveThreshold = now.minusMinutes(INACTIVITY_WARNING_MINUTES);
        LocalDateTime cancelThreshold = now.minusMinutes(INACTIVITY_WARNING_MINUTES + CANCELLATION_GRACE_MINUTES);
        
        reservations.entrySet().removeIf(entry -> {
            Reservation reservation = entry.getValue();
            LocalDateTime lastActivity = reservation.getLastActivityTime();
            
            // Если прошло более (INACTIVITY_WARNING + GRACE) минут с последней активности - снимаем бронь
            if (lastActivity.isBefore(cancelThreshold)) {
                // Отправляем уведомление об отмене и очищаем сессию
                cancelReservationWithNotification(reservation);
                
                // Уменьшаем количество участников
                ReservationManager.decrementProductParticipants(reservation.getProduct().getIdProduct());
                
                System.out.println("🕐 Auto-cancelled inactive reservation for user " + 
                    reservation.getUser().getIdUser() + ", product " + reservation.getProduct().getIdProduct() +
                    " (inactive for more than " + (INACTIVITY_WARNING_MINUTES + CANCELLATION_GRACE_MINUTES) + " minutes)");
                return true;
            }
            
            // Если прошло более INACTIVITY_WARNING_MINUTES с последней активности и уведомление еще не отправлено
            if (lastActivity.isBefore(inactiveThreshold) && !reservation.isNotificationSent()) {
                // Отправляем уведомление пользователю
                sendInactivityNotification(reservation);
                reservation.markNotificationSent();
                System.out.println("📢 Sent inactivity notification to user " + 
                    reservation.getUser().getIdUser() + " for product " + reservation.getProduct().getIdProduct());
            }
            
            return false;
        });
    }
    
    /**
     * Отправить уведомление пользователю о неактивности
     */
    private void sendInactivityNotification(Reservation reservation) {
        try {
            User user = reservation.getUser();
            Product product = reservation.getProduct();
            
            String message = "⏰ <b>Напоминание о бронировании</b>\n\n" +
                           "Вы начали оформление покупки товара <b>\"" + product.getProductName() + "\"</b>, " +
                           "но не завершили процесс.\n\n" +
                           "⚠️ Если вы не продолжите оформление в течение " + CANCELLATION_GRACE_MINUTES + " минут после этого сообщения, " +
                           "бронирование будет автоматически отменено, и товар станет доступен другим пользователям.\n\n" +
                           "Продолжите оформление заказа, чтобы не потерять место в акции!";
            
            Sent sent = new Sent();
            sent.sendMessage(user, message);
        } catch (Exception e) {
            System.err.println("❌ Error sending inactivity notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Отменить бронь с уведомлением и очисткой сессии
     */
    private void cancelReservationWithNotification(Reservation reservation) {
        try {
            User user = reservation.getUser();
            Product product = reservation.getProduct();
            long chatId = user.getIdUser();
            
            // Отправляем уведомление об отмене
            long minutesWithoutActivity = INACTIVITY_WARNING_MINUTES + CANCELLATION_GRACE_MINUTES;
            String message = "❌ <b>Бронирование отменено</b>\n\n" +
                           "Ваше бронирование товара <b>\"" + product.getProductName() + "\"</b> было автоматически отменено " +
                           "из-за неактивности более " + minutesWithoutActivity + " минут.\n\n" +
                           "Товар снова доступен для бронирования другими пользователями.\n\n" +
                           "Если вы хотите приобрести этот товар, пожалуйста, начните процесс заново.";

            LogicUI logicUI = new LogicUI();
            logicUI.sendMenu(user,message);
            
            // Очищаем сессию пользователя
            RedisSessionStore.removeReviewSession(chatId);
            RedisSessionStore.removeState(chatId);
            
        } catch (Exception e) {
            System.err.println("❌ Error cancelling reservation with notification: " + e.getMessage());
            e.printStackTrace();
        }
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
     * Завершить бронирование без изменения количества участников (успешное завершение покупки)
     */
    public void completeReservation(User user, Product product) {
        if (user == null || product == null) {
            return;
        }
        String key = user.getIdUser() + "_" + product.getIdProduct();
        reservations.remove(key);
    }

    /**
     * Класс для хранения информации о бронировании
     */
    private static class Reservation {
        private final User user;
        private final Product product;
        private final LocalDateTime reservedAt;
        private LocalDateTime lastActivityTime;
        private boolean notificationSent;
        
        public Reservation(User user, Product product, LocalDateTime reservedAt, LocalDateTime lastActivityTime) {
            this.user = user;
            this.product = product;
            this.reservedAt = reservedAt;
            this.lastActivityTime = lastActivityTime;
            this.notificationSent = false;
        }
        
        public User getUser() { return user; }
        public Product getProduct() { return product; }
        public LocalDateTime getReservedAt() { return reservedAt; }
        public LocalDateTime getLastActivityTime() { return lastActivityTime; }
        public boolean isNotificationSent() { return notificationSent; }
        
        public void updateLastActivity() {
            this.lastActivityTime = LocalDateTime.now();
            // Сбрасываем флаг уведомления, если пользователь снова активен
            // Это позволит отправить уведомление снова, если пользователь снова станет неактивным
            this.notificationSent = false;
        }
        
        public void markNotificationSent() {
            this.notificationSent = true;
        }
    }
}
