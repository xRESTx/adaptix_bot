package org.example;

import org.example.async.AsyncService;
import org.example.database.DatabaseManager;
import org.example.monitoring.MetricsEndpoint;
import org.example.monitoring.MetricsService;
import org.example.redis.RedisManager;
import org.example.service.ReservationCleanupService;
import org.example.telegramBots.TelegramBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            // Инициализация ботов
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TelegramBot());
            // Initialize metrics
            MetricsService metricsService = MetricsService.getInstance();
            metricsService.getMeterRegistry(); // Initialize metrics
            
            // Start HTTP server for metrics
            MetricsEndpoint.start();
            
            // Start reservation cleanup service
            ReservationCleanupService.start();
            
            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("🔄 Shutting down AdaptixBot...");
                MetricsEndpoint.stop();
                ReservationCleanupService.stop();
                AsyncService.shutdown();
                DatabaseManager.getInstance().shutdown();
                RedisManager.getInstance().shutdown();
                System.out.println("✅ Graceful shutdown completed");
            }));
            
        } catch (TelegramApiException e) {
            System.err.println("❌ Bot startup error: " + e.getMessage());
            
            // Проверяем, является ли это проблемой подключения
            if (e.getCause() instanceof java.net.ConnectException || 
                e.getCause() instanceof java.net.SocketTimeoutException ||
                e.getMessage().contains("Connection timed out") ||
                e.getMessage().contains("Unable to execute")) {
                
                System.err.println("⚠️ Network connectivity issue detected. This might be due to:");
                System.err.println("   - Internet connection problems");
                System.err.println("   - Firewall blocking Telegram API");
                System.err.println("   - Proxy settings");
                System.err.println("   - Telegram API temporary unavailability");
                System.err.println("");
                System.err.println("🔧 Troubleshooting steps:");
                System.err.println("   1. Check internet connection");
                System.err.println("   2. Try: ping api.telegram.org");
                System.err.println("   3. Check firewall/proxy settings");
                System.err.println("   4. Wait a few minutes and try again");
                System.err.println("");
                System.err.println("💡 The application will continue running in background mode.");
                System.err.println("   All database operations and business logic will work normally.");
                System.err.println("   Only Telegram bot functionality will be unavailable.");
                
                // Не завершаем приложение, позволяем ему работать в фоновом режиме
                try {
                    // Ждем в фоновом режиме
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } else {
                // Для других ошибок завершаем приложение
                System.err.println("❌ Critical error occurred. Shutting down application.");
                MetricsEndpoint.stop();
                ReservationCleanupService.stop();
                AsyncService.shutdown();
                DatabaseManager.getInstance().shutdown();
                RedisManager.getInstance().shutdown();
                System.exit(1);
            }
        }
    }
}