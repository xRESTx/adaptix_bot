package org.example.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.redis.RedisManager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Управление сессиями с использованием Redis для персистентности.
 * 
 * ПРОБЛЕМА КОТОРУЮ РЕШАЕМ:
 * - Старый SessionStore хранил сессии в памяти
 * - При перезапуске сервера все состояния терялись
 * - Нет масштабируемости для нескольких инстансов
 * 
 * РЕШЕНИЕ:
 * - Redis для хранения сессий
 * - JSON сериализация для сложных объектов
 * - Автоматическое истечение сессий
 * - Fallback на память если Redis недоступен
 */
public class RedisSessionStore {
    
    private static final RedisManager redisManager = RedisManager.getInstance();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Fallback на память если Redis недоступен
    private static final ConcurrentHashMap<Long, String> memoryFallback = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, ProductCreationSession> memoryProducts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, ReviewRequestSession> memoryReviews = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, ReviewSubmissionSession> memoryReviewSubmissions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, ReviewRejectionSession> memoryReviewRejections = new ConcurrentHashMap<>();
    
    // Префиксы для Redis ключей
    private static final String STATE_PREFIX = "session:state:";
    private static final String PRODUCT_PREFIX = "session:product:";
    private static final String REVIEW_PREFIX = "session:review:";
    private static final String REVIEW_SUBMISSION_PREFIX = "session:review_submission:";
    private static final String REVIEW_REJECTION_PREFIX = "session:review_rejection:";
    
    /**
     * Получение состояния сессии
     */
    public static String getState(Long chatId) {
        try {
            if (!redisManager.isHealthy()) {
                return memoryFallback.get(chatId);
            }
            
            String key = STATE_PREFIX + chatId;
            String state = redisManager.getSession(key);
            
            if (state != null) {
                return state;
            }
            
            // Fallback на память
            return memoryFallback.get(chatId);
            
        } catch (Exception e) {
            // Silent fallback to memory
            return memoryFallback.get(chatId);
        }
    }
    
    /**
     * Установка состояния сессии
     */
    public static void setState(Long chatId, String state) {
        try {
            // Всегда сохраняем в память
            memoryFallback.put(chatId, state);
            
            if (redisManager.isHealthy()) {
                String key = STATE_PREFIX + chatId;
                redisManager.setSession(key, state);
            }
            
        } catch (Exception e) {
            // Silent fallback to memory
        }
    }
    
    /**
     * Удаление состояния сессии
     */
    public static void removeState(Long chatId) {
        try {
            String key = STATE_PREFIX + chatId;
            redisManager.deleteSession(key);
        } catch (Exception e) {
            // Silent fallback
        } finally {
            // Всегда удаляем из памяти
            memoryFallback.remove(chatId);
        }
    }
    
    /**
     * Получение сессии создания продукта
     */
    public static ProductCreationSession getProductSession(Long chatId) {
        try {
            String key = PRODUCT_PREFIX + chatId;
            String sessionJson = redisManager.getSession(key);
            
            if (sessionJson != null) {
                return objectMapper.readValue(sessionJson, ProductCreationSession.class);
            }
            
            // Fallback на память
            return memoryProducts.get(chatId);
            
        } catch (Exception e) {
            // Silent fallback to memory
            return memoryProducts.get(chatId);
        }
    }
    
    /**
     * Сохранение сессии создания продукта
     */
    public static void setProductSession(Long chatId, ProductCreationSession session) {
        try {
            String key = PRODUCT_PREFIX + chatId;
            String sessionJson = objectMapper.writeValueAsString(session);
            redisManager.setSession(key, sessionJson);
            
            // Также сохраняем в память как fallback
            memoryProducts.put(chatId, session);
            
        } catch (JsonProcessingException e) {
            // Silent fallback to memory
            memoryProducts.put(chatId, session);
        } catch (Exception e) {
            // Silent fallback to memory
            memoryProducts.put(chatId, session);
        }
    }
    
    /**
     * Удаление сессии создания продукта
     */
    public static void removeProductSession(Long chatId) {
        try {
            String key = PRODUCT_PREFIX + chatId;
            redisManager.deleteSession(key);
        } catch (Exception e) {
            // Silent fallback
        } finally {
            memoryProducts.remove(chatId);
        }
    }
    
    /**
     * Получение сессии запроса отзыва
     */
    public static ReviewRequestSession getReviewSession(Long chatId) {
        try {
            String key = REVIEW_PREFIX + chatId;
            String sessionJson = redisManager.getSession(key);
            
            if (sessionJson != null) {
                return objectMapper.readValue(sessionJson, ReviewRequestSession.class);
            }
            
            // Fallback на память
            return memoryReviews.get(chatId);
            
        } catch (Exception e) {
            // Silent fallback to memory
            return memoryReviews.get(chatId);
        }
    }
    
    /**
     * Сохранение сессии запроса отзыва
     */
    public static void setReviewSession(Long chatId, ReviewRequestSession session) {
        try {
            String key = REVIEW_PREFIX + chatId;
            String sessionJson = objectMapper.writeValueAsString(session);
            redisManager.setSession(key, sessionJson);
            
            // Также сохраняем в память как fallback
            memoryReviews.put(chatId, session);
            
        } catch (JsonProcessingException e) {
            // Silent fallback to memory
            memoryReviews.put(chatId, session);
        } catch (Exception e) {
            // Silent fallback to memory
            memoryReviews.put(chatId, session);
        }
    }
    
    /**
     * Удаление сессии запроса отзыва
     */
    public static void removeReviewSession(Long chatId) {
        try {
            String key = REVIEW_PREFIX + chatId;
            redisManager.deleteSession(key);
        } catch (Exception e) {
            // Silent fallback
        } finally {
            memoryReviews.remove(chatId);
        }
    }
    
    /**
     * Очистка всех сессий пользователя
     */
    public static void clearAll(Long chatId) {
        removeState(chatId);
        removeProductSession(chatId);
        removeReviewSession(chatId);
        removeReviewSubmissionSession(chatId);
    }
    
    /**
     * Проверка состояния Redis
     */
    public static boolean isRedisHealthy() {
        return redisManager.isHealthy();
    }
    
    /**
     * Получение статистики сессий
     */
    public static String getSessionStats() {
        try {
            return "Redis: " + redisManager.getRedisStats() + 
                   ", Memory fallback: states=" + memoryFallback.size() + 
                   ", products=" + memoryProducts.size() + 
                   ", reviews=" + memoryReviews.size() +
                   ", review_submissions=" + memoryReviewSubmissions.size();
        } catch (Exception e) {
            return "Ошибка получения статистики сессий: " + e.getMessage();
        }
    }
    
    /**
     * Сохранение сессии подачи отзыва
     */
    public static void setReviewSubmissionSession(Long chatId, ReviewSubmissionSession session) {
        try {
            String key = REVIEW_SUBMISSION_PREFIX + chatId;
            String sessionJson = objectMapper.writeValueAsString(session);
            redisManager.setSession(key, sessionJson);
            
            // Также сохраняем в память как fallback
            memoryReviewSubmissions.put(chatId, session);
            
        } catch (JsonProcessingException e) {
            // Silent fallback to memory
            memoryReviewSubmissions.put(chatId, session);
        } catch (Exception e) {
            // Silent fallback to memory
            memoryReviewSubmissions.put(chatId, session);
        }
    }
    
    /**
     * Получение сессии подачи отзыва
     */
    public static ReviewSubmissionSession getReviewSubmissionSession(Long chatId) {
        try {
            if (!redisManager.isHealthy()) {
                return memoryReviewSubmissions.get(chatId);
            }
            
            String key = REVIEW_SUBMISSION_PREFIX + chatId;
            String sessionJson = redisManager.getSession(key);
            
            if (sessionJson != null) {
                return objectMapper.readValue(sessionJson, ReviewSubmissionSession.class);
            }
            
            return null;
            
        } catch (Exception e) {
            // Fallback to memory
            return memoryReviewSubmissions.get(chatId);
        }
    }
    
    /**
     * Удаление сессии подачи отзыва
     */
    public static void removeReviewSubmissionSession(Long chatId) {
        try {
            String key = REVIEW_SUBMISSION_PREFIX + chatId;
            redisManager.deleteSession(key);
        } catch (Exception e) {
            // Silent fallback
        } finally {
            memoryReviewSubmissions.remove(chatId);
        }
    }
    
    /**
     * Сохранение сессии отклонения отзыва
     */
    public static void setReviewRejectionSession(Long chatId, ReviewRejectionSession session) {
        try {
            String key = REVIEW_REJECTION_PREFIX + chatId;
            String sessionJson = objectMapper.writeValueAsString(session);
            redisManager.setSession(key, sessionJson);
            
            // Также сохраняем в память как fallback
            memoryReviewRejections.put(chatId, session);
            
        } catch (JsonProcessingException e) {
            // Silent fallback to memory
            memoryReviewRejections.put(chatId, session);
        } catch (Exception e) {
            // Silent fallback to memory
            memoryReviewRejections.put(chatId, session);
        }
    }
    
    /**
     * Получение сессии отклонения отзыва
     */
    public static ReviewRejectionSession getReviewRejectionSession(Long chatId) {
        try {
            if (!redisManager.isHealthy()) {
                return memoryReviewRejections.get(chatId);
            }
            
            String key = REVIEW_REJECTION_PREFIX + chatId;
            String sessionJson = redisManager.getSession(key);
            
            if (sessionJson != null) {
                return objectMapper.readValue(sessionJson, ReviewRejectionSession.class);
            }
            
            return null;
            
        } catch (Exception e) {
            // Fallback to memory
            return memoryReviewRejections.get(chatId);
        }
    }
    
    /**
     * Удаление сессии отклонения отзыва
     */
    public static void removeReviewRejectionSession(Long chatId) {
        try {
            String key = REVIEW_REJECTION_PREFIX + chatId;
            redisManager.deleteSession(key);
        } catch (Exception e) {
            // Silent fallback
        } finally {
            memoryReviewRejections.remove(chatId);
        }
    }
}
