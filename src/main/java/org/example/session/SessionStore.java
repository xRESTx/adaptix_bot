package org.example.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStore {
    private static final Map<Long, String> waitingForInput = new ConcurrentHashMap<>();

    private static final Map<Long, ProductCreationSession> newProducts = new ConcurrentHashMap<>();

    private static final Map<Long, ReviewRequestSession> newReview = new ConcurrentHashMap<>();

    public static String getState(Long chatId) {
        return waitingForInput.get(chatId);
    }

    public static void setState(Long chatId, String state) {
        waitingForInput.put(chatId, state);
    }

    public static void removeState(Long chatId) {
        waitingForInput.remove(chatId);
    }

    public static ReviewRequestSession getReviewSession(Long chatId) {
        return newReview.get(chatId);
    }

    public static void setReviewSession(Long chatId, ReviewRequestSession session) {
        newReview.put(chatId, session);
    }

    public static ProductCreationSession getProductSession(Long chatId) {
        return newProducts.get(chatId);
    }

    public static void setProductSession(Long chatId, ProductCreationSession session) {
        newProducts.put(chatId, session);
    }

    public static void removeProductSession(Long chatId) {
        newProducts.remove(chatId);
    }
    public static void removeReviewSession(Long chatId) {
        newReview.remove(chatId);
    }

    public static void clearAll(Long chatId) {
        waitingForInput.remove(chatId);
        newProducts.remove(chatId);
        newReview.remove(chatId);
    }
}