package org.example;

import org.example.async.AsyncService;
import org.example.database.DatabaseManager;
import org.example.monitoring.MetricsEndpoint;
import org.example.monitoring.MetricsService;
import org.example.redis.RedisManager;
import org.example.telegramBots.TelegramBot;
import org.example.telegramBots.TelegramBotLogs;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            // Инициализация ботов
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TelegramBot());
            botsApi.registerBot(new TelegramBotLogs());
            
            System.out.println("✅ AdaptixBot started successfully!");
            
            // Initialize metrics
            MetricsService metricsService = MetricsService.getInstance();
            metricsService.getMeterRegistry(); // Initialize metrics
            
            // Start HTTP server for metrics
            MetricsEndpoint.start();
            
            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("🔄 Shutting down AdaptixBot...");
                MetricsEndpoint.stop();
                AsyncService.shutdown();
                DatabaseManager.getInstance().shutdown();
                RedisManager.getInstance().shutdown();
                System.out.println("✅ Graceful shutdown completed");
            }));
            
        } catch (TelegramApiException e) {
            System.err.println("❌ Bot startup error: " + e.getMessage());
            MetricsEndpoint.stop();
            AsyncService.shutdown();
            DatabaseManager.getInstance().shutdown();
            RedisManager.getInstance().shutdown();
            throw new RuntimeException(e);
        }
    }
}