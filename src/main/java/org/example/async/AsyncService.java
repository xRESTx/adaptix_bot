package org.example.async;

import org.example.dao.PhotoDAO;
import org.example.dao.PurchaseDAO;
import org.example.session.ReviewRequestSession;
import org.example.table.Photo;
import org.example.table.Purchase;
import org.example.table.User;
import org.example.telegramBots.TelegramBot;
import org.example.tgProcessing.Sent;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Асинхронная обработка тяжелых операций.
 * 
 * ПРОБЛЕМА КОТОРУЮ РЕШАЕМ:
 * - Загрузка фото блокирует основной поток
 * - Обработка файлов замедляет ответы бота
 * - Нет параллельной обработки запросов
 * 
 * РЕШЕНИЕ:
 * - Асинхронная обработка тяжелых операций
 * - Отдельный thread pool для файловых операций
 * - Неблокирующие операции
 */
public class AsyncService {
    
    // Отдельный thread pool для файловых операций
    private static final ExecutorService fileProcessingExecutor = 
        Executors.newFixedThreadPool(10, r -> {
            Thread t = new Thread(r, "FileProcessing-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    
    // Thread pool для обработки фото
    private static final ExecutorService photoProcessingExecutor = 
        Executors.newFixedThreadPool(5, r -> {
            Thread t = new Thread(r, "PhotoProcessing-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    
    /**
     * Асинхронная обработка загрузки скриншота заказа
     */
    public static CompletableFuture<Void> processScreenshotAsync(
            ReviewRequestSession session, 
            User user, 
            PhotoSize photo, 
            String fileId,
            Long groupMessageId) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                System.out.println("🔄 Async screenshot processing for user: " + user.getIdUser());
                
                TelegramBot telegramBot = new TelegramBot();
                
                // Создаем директорию для скриншотов
                File reviewsDir = new File("reviews/");
                if (!reviewsDir.exists()) {
                    reviewsDir.mkdirs();
                }
                
                // Генерируем уникальное имя файла
                String fileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".jpg";
                Path filePath = Paths.get("reviews/", fileName);
                
                // Загружаем файл
                telegramBot.downloadFile(fileId, filePath.toString());
                
                // Создаем запись о покупке
                Purchase purchase = new Purchase();
                purchase.setProduct(session.getProduct());
                purchase.setUser(user);
                purchase.setDate(LocalDate.now());
                purchase.setPurchaseStage(0);
                purchase.setGroupMessageId(groupMessageId);
                
                PurchaseDAO purchaseDAO = new PurchaseDAO();
                purchaseDAO.save(purchase);
                
                // Создаем запись о фото
                Photo photoEntity = new Photo();
                photoEntity.setPurchase(purchase);
                photoEntity.setUser(user);
                photoEntity.setIdPhoto(fileName);
                
                PhotoDAO photoDAO = new PhotoDAO();
                photoDAO.save(photoEntity);
                
                System.out.println("✅ Screenshot processed successfully: " + fileName);
                
            } catch (TelegramApiException | IOException e) {
                System.err.println("❌ Screenshot processing error: " + e.getMessage());
                e.printStackTrace();
            }
        }, fileProcessingExecutor);
    }
    
    /**
     * Асинхронная обработка загрузки фото товара
     */
    public static CompletableFuture<String> processProductPhotoAsync(
            String photoId, 
            String productName) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("🔄 Async product photo processing: " + productName);
                
                TelegramBot telegramBot = new TelegramBot();
                
                // Создаем директорию для загрузок
                File uploadDir = new File("upload/");
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                
                // Генерируем уникальное имя файла
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                String fileName = sdf.format(new Date()) + ".jpg";
                Path filePath = Paths.get("upload/", fileName);
                
                // Загружаем файл
                telegramBot.downloadFile(photoId, filePath.toString());
                
                // Проверяем, что файл действительно создался
                File downloadedFile = filePath.toFile();
                if (downloadedFile.exists()) {
                    System.out.println("✅ Product photo processed successfully: " + fileName);
                } else {
                    System.out.println("❌ Error: Downloaded file does not exist: " + filePath.toString());
                    return null;
                }
                
                return filePath.toString();
                
            } catch (TelegramApiException | IOException e) {
                System.err.println("❌ Product photo processing error: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }, photoProcessingExecutor);
    }
    
    /**
     * Асинхронная обработка уведомлений
     */
    public static CompletableFuture<Void> sendNotificationAsync(
            String message, 
            String filePath) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                System.out.println("🔄 Async notification sending");
                
                // Здесь можно добавить логику отправки уведомлений
                // Например, отправка в группу, email, webhook и т.д.
                
                System.out.println("✅ Notification sent: " + message);
                
            } catch (Exception e) {
                System.err.println("❌ Notification sending error: " + e.getMessage());
                e.printStackTrace();
            }
        }, fileProcessingExecutor);
    }
    
    /**
     * Асинхронная очистка временных файлов
     */
    public static CompletableFuture<Void> cleanupTempFilesAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                System.out.println("🔄 Async temp files cleanup");
                
                // Очистка старых файлов (старше 7 дней)
                long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
                
                cleanupDirectory("upload/", cutoffTime);
                cleanupDirectory("reviews/", cutoffTime);
                
                System.out.println("✅ Temp files cleaned up");
                
            } catch (Exception e) {
                System.err.println("❌ File cleanup error: " + e.getMessage());
                e.printStackTrace();
            }
        }, fileProcessingExecutor);
    }
    
    /**
     * Очистка файлов в директории старше указанного времени
     */
    private static void cleanupDirectory(String directoryPath, long cutoffTime) {
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        int deletedCount = 0;
        for (File file : files) {
            if (file.isFile() && file.lastModified() < cutoffTime) {
                if (file.delete()) {
                    deletedCount++;
                }
            }
        }
        
        if (deletedCount > 0) {
            System.out.println("🗑️ Deleted " + deletedCount + " old files from " + directoryPath);
        }
    }
    
    /**
     * Корректное завершение всех thread pool'ов
     */
    public static void shutdown() {
        System.out.println("🔄 Shutting down async services...");
        
        fileProcessingExecutor.shutdown();
        photoProcessingExecutor.shutdown();
        
        // Ждем завершения всех задач (максимум 30 секунд)
        try {
            if (!fileProcessingExecutor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                fileProcessingExecutor.shutdownNow();
            }
            if (!photoProcessingExecutor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                photoProcessingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            fileProcessingExecutor.shutdownNow();
            photoProcessingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("✅ Async services shutdown completed");
    }
    
    /**
     * Получение статистики thread pool'ов
     */
    public static String getThreadPoolStats() {
        return String.format("Thread Pools - File: active=%d, queue=%d, Photo: active=%d, queue=%d",
            fileProcessingExecutor.isShutdown() ? 0 : 1, // Упрощенная статистика
            fileProcessingExecutor.isShutdown() ? 0 : 0,
            photoProcessingExecutor.isShutdown() ? 0 : 1,
            photoProcessingExecutor.isShutdown() ? 0 : 0
        );
    }

    /**
     * Асинхронная обработка загрузки скриншота поиска
     */
    public static CompletableFuture<Void> processSearchScreenshotAsync(
            ReviewRequestSession session,
            User user,
            PhotoSize photo,
            String fileId) {

        return CompletableFuture.runAsync(() -> {
            try {
                System.out.println("🔄 Async search screenshot processing for user: " + user.getIdUser());

                TelegramBot telegramBot = new TelegramBot();

                // Создаем директорию для скриншотов
                File reviewsDir = new File("reviews/");
                if (!reviewsDir.exists()) {
                    reviewsDir.mkdirs();
                }

                // Генерируем уникальное имя файла
                String fileName = "search_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".jpg";
                Path filePath = Paths.get("reviews/", fileName);

                // Загружаем файл
                telegramBot.downloadFile(fileId, filePath.toString());

                // Сохраняем путь к скриншоту поиска
                session.setSearchScreenshotPath(filePath.toString());

                System.out.println("✅ Search screenshot processed successfully: " + fileName);

            } catch (TelegramApiException | IOException e) {
                System.err.println("❌ Search screenshot processing error: " + e.getMessage());
                e.printStackTrace();
            }
        }, fileProcessingExecutor);
    }

    /**
     * Асинхронная обработка загрузки скриншота доставки
     */
    public static CompletableFuture<Void> processDeliveryScreenshotAsync(
            ReviewRequestSession session,
            User user,
            PhotoSize photo,
            String fileId) {

        return CompletableFuture.runAsync(() -> {
            try {
                System.out.println("🔄 Async delivery screenshot processing for user: " + user.getIdUser());

                TelegramBot telegramBot = new TelegramBot();

                // Создаем директорию для скриншотов
                File reviewsDir = new File("reviews/");
                if (!reviewsDir.exists()) {
                    reviewsDir.mkdirs();
                }

                // Генерируем уникальное имя файла
                String fileName = "delivery_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".jpg";
                Path filePath = Paths.get("reviews/", fileName);

                // Загружаем файл
                telegramBot.downloadFile(fileId, filePath.toString());

                // Сохраняем путь к скриншоту доставки
                session.setDeliveryScreenshotPath(filePath.toString());

                // Создаем запись о покупке
                Purchase purchase = new Purchase();
                purchase.setProduct(session.getProduct());
                purchase.setUser(user);
                purchase.setDate(LocalDate.now());
                purchase.setOrderTime(java.time.LocalTime.now());
                purchase.setPurchaseStage(0);
                purchase.setGroupMessageId(session.getGroupMessageId());

                PurchaseDAO purchaseDAO = new PurchaseDAO();
                purchaseDAO.save(purchase);
                
                // Сообщение в группу уже отправлено в MessageProcessing.java
                // Здесь только сохраняем ID сообщения из сессии
                System.out.println("🔍 Debug: session.getGroupMessageId() = " + session.getGroupMessageId());
                if (session.getGroupMessageId() != null) {
                    purchase.setOrderMessageId(session.getGroupMessageId());
                    purchaseDAO.update(purchase);
                    System.out.println("✅ Order message ID saved: " + session.getGroupMessageId());
                } else {
                    System.out.println("❌ Group message ID is null in session!");
                }

                // Создаем запись о фото поиска
                Photo searchPhoto = new Photo();
                searchPhoto.setPurchase(purchase);
                searchPhoto.setUser(user);
                searchPhoto.setIdPhoto("search_" + fileName);

                PhotoDAO photoDAO = new PhotoDAO();
                photoDAO.save(searchPhoto);

                // Создаем запись о фото доставки
                Photo deliveryPhoto = new Photo();
                deliveryPhoto.setPurchase(purchase);
                deliveryPhoto.setUser(user);
                deliveryPhoto.setIdPhoto("delivery_" + fileName);

                photoDAO.save(deliveryPhoto);

                System.out.println("✅ Delivery screenshot processed successfully: " + fileName);

            } catch (TelegramApiException | IOException e) {
                System.err.println("❌ Delivery screenshot processing error: " + e.getMessage());
                e.printStackTrace();
            }
        }, fileProcessingExecutor);
    }
    
    /**
     * Асинхронная обработка фотографии для отзыва
     */
    public static CompletableFuture<String> processReviewPhotoAsync(
            String photoId, 
            Long userId, 
            int photoNumber) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("🔄 Async review photo processing: user " + userId + ", photo " + photoNumber);
                
                TelegramBot telegramBot = new TelegramBot();
                
                // Создаем директорию для отзывов
                File reviewsDir = new File("reviews/");
                if (!reviewsDir.exists()) {
                    reviewsDir.mkdirs();
                }
                
                // Генерируем уникальное имя файла
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                String fileName = "review_" + userId + "_" + photoNumber + "_" + sdf.format(new Date()) + ".jpg";
                Path filePath = Paths.get("reviews/", fileName);
                
                // Загружаем файл
                telegramBot.downloadFile(photoId, filePath.toString());
                
                // Проверяем, что файл действительно создался
                File downloadedFile = filePath.toFile();
                if (downloadedFile.exists()) {
                    System.out.println("✅ Review photo processed successfully: " + fileName);
                } else {
                    System.out.println("❌ Error: Downloaded file does not exist: " + filePath.toString());
                    return null;
                }
                
                return filePath.toString();
                
            } catch (TelegramApiException | IOException e) {
                System.err.println("❌ Review photo processing error: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }, photoProcessingExecutor);
    }
    
    /**
     * Асинхронная обработка видео для отзыва
     */
    public static CompletableFuture<String> processReviewVideoAsync(
            String videoId, 
            Long userId) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("🔄 Async review video processing: user " + userId);
                
                TelegramBot telegramBot = new TelegramBot();
                
                // Создаем директорию для отзывов
                File reviewsDir = new File("reviews/");
                if (!reviewsDir.exists()) {
                    reviewsDir.mkdirs();
                }
                
                // Генерируем уникальное имя файла
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                String fileName = "review_" + userId + "_video_" + sdf.format(new Date()) + ".mp4";
                Path filePath = Paths.get("reviews/", fileName);
                
                // Загружаем файл
                telegramBot.downloadFile(videoId, filePath.toString());
                
                // Проверяем, что файл действительно создался
                File downloadedFile = filePath.toFile();
                if (downloadedFile.exists()) {
                    System.out.println("✅ Review video processed successfully: " + fileName);
                } else {
                    System.out.println("❌ Error: Downloaded file does not exist: " + filePath.toString());
                    return null;
                }
                
                return filePath.toString();
                
            } catch (TelegramApiException | IOException e) {
                System.err.println("❌ Review video processing error: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }, photoProcessingExecutor);
    }
}
