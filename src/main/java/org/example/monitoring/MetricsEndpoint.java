package org.example.monitoring;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

/**
 * HTTP endpoint для метрик Prometheus.
 * 
 * ПРОБЛЕМА КОТОРУЮ РЕШАЕМ:
 * - Нет способа получить метрики извне
 * - Нет интеграции с системами мониторинга
 * 
 * РЕШЕНИЕ:
 * - HTTP endpoint для Prometheus
 * - JSON API для метрик
 * - Health check endpoint
 */
public class MetricsEndpoint {
    
    private static final int METRICS_PORT = 8080;
    private static HttpServer server;
    private static final MetricsService metricsService = MetricsService.getInstance();
    
    /**
     * Запуск HTTP сервера для метрик
     */
    public static void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(METRICS_PORT), 0);
            
            // Endpoint для Prometheus метрик
            server.createContext("/metrics", new PrometheusHandler());
            
            // Endpoint для health check
            server.createContext("/health", new HealthHandler());
            
            // Endpoint для JSON метрик
            server.createContext("/api/metrics", new JsonMetricsHandler());
            
            // Endpoint для статистики
            server.createContext("/api/stats", new StatsHandler());
            
            server.setExecutor(null);
            server.start();
            
            System.out.println("🚀 HTTP metrics server started on port " + METRICS_PORT);
            System.out.println("📊 Prometheus metrics: http://localhost:" + METRICS_PORT + "/metrics");
            System.out.println("❤️ Health check: http://localhost:" + METRICS_PORT + "/health");
            System.out.println("📈 JSON metrics: http://localhost:" + METRICS_PORT + "/api/metrics");
            System.out.println("📊 Statistics: http://localhost:" + METRICS_PORT + "/api/stats");
            
        } catch (IOException e) {
            System.err.println("❌ HTTP metrics server startup error: " + e.getMessage());
        }
    }
    
    /**
     * Остановка HTTP сервера
     */
    public static void stop() {
        if (server != null) {
            System.out.println("🔄 Stopping HTTP metrics server...");
            server.stop(5); // 5 секунд на graceful shutdown
            server = null;
        }
    }
    
    /**
     * Handler для Prometheus метрик
     */
    static class PrometheusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String metrics = metricsService.getPrometheusMetrics();
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                exchange.sendResponseHeaders(200, metrics.getBytes(StandardCharsets.UTF_8).length);
                exchange.getResponseBody().write(metrics.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                String error = "Error generating metrics: " + e.getMessage();
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(500, error.getBytes().length);
                exchange.getResponseBody().write(error.getBytes());
            } finally {
                exchange.close();
            }
        }
    }
    
    /**
     * Handler для health check
     */
    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Обновляем системные метрики
                metricsService.updateSystemMetrics();
                
                String healthJson = String.format(
                    "{\n" +
                    "  \"status\": \"UP\",\n" +
                    "  \"timestamp\": %d,\n" +
                    "  \"uptime\": %.1f,\n" +
                    "  \"database\": \"%s\",\n" +
                    "  \"redis\": \"%s\"\n" +
                    "}",
                    System.currentTimeMillis(),
                    metricsService.getUptime(),
                    org.example.database.DatabaseManager.getInstance().isHealthy() ? "UP" : "DOWN",
                    org.example.redis.RedisManager.getInstance().isHealthy() ? "UP" : "DOWN"
                );
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, healthJson.getBytes().length);
                exchange.getResponseBody().write(healthJson.getBytes());
            } catch (Exception e) {
                String error = "{\"status\": \"DOWN\", \"error\": \"" + e.getMessage() + "\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, error.getBytes().length);
                exchange.getResponseBody().write(error.getBytes());
            } finally {
                exchange.close();
            }
        }
    }
    
    /**
     * Handler для JSON метрик
     */
    static class JsonMetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (metricsService.userMessagesCounter == null) {
                    String error = "{\"error\": \"Метрики не инициализированы\"}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(500, error.getBytes().length);
                    exchange.getResponseBody().write(error.getBytes());
                    return;
                }
                
                String metricsJson = String.format(
                    "{\n" +
                    "  \"user_messages\": %d,\n" +
                    "  \"admin_actions\": %d,\n" +
                    "  \"products_created\": %d,\n" +
                    "  \"purchase_requests\": %d,\n" +
                    "  \"photo_uploads\": %d,\n" +
                    "  \"errors\": %d,\n" +
                    "  \"active_users\": %d,\n" +
                    "  \"active_sessions\": %d,\n" +
                    "  \"uptime_seconds\": %.1f\n" +
                    "}",
                    (long) metricsService.userMessagesCounter.count(),
                    (long) metricsService.adminActionsCounter.count(),
                    (long) metricsService.productCreationsCounter.count(),
                    (long) metricsService.purchaseRequestsCounter.count(),
                    (long) metricsService.photoUploadsCounter.count(),
                    (long) metricsService.errorsCounter.count(),
                    metricsService.activeUsers.get(),
                    metricsService.activeSessions.get(),
                    metricsService.getUptime()
                );
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, metricsJson.getBytes().length);
                exchange.getResponseBody().write(metricsJson.getBytes());
            } catch (Exception e) {
                String error = "{\"error\": \"" + e.getMessage() + "\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, error.getBytes().length);
                exchange.getResponseBody().write(error.getBytes());
            } finally {
                exchange.close();
            }
        }
    }
    
    /**
     * Handler для статистики
     */
    static class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String stats = metricsService.getMetricsSummary();
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                exchange.sendResponseHeaders(200, stats.getBytes(StandardCharsets.UTF_8).length);
                exchange.getResponseBody().write(stats.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                String error = "Error generating stats: " + e.getMessage();
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(500, error.getBytes().length);
                exchange.getResponseBody().write(error.getBytes());
            } finally {
                exchange.close();
            }
        }
    }
}
