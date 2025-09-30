//package org.example.session;
//
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//public class SessionStore {
//    private static final Map<Long, String> waitingForInput = new ConcurrentHashMap<>();
//
//    private static final Map<Long, TariffCreationSession> newTariffs = new ConcurrentHashMap<>();
//
//    public static String getState(Long chatId) {
//        return waitingForInput.get(chatId);
//    }
//    public static void setState(Long chatId, String state) {
//        waitingForInput.put(chatId, state);
//    }
//
//    public static void removeState(Long chatId) {
//        waitingForInput.remove(chatId);
//    }
//
//    public static TariffCreationSession getTariffSession(Long chatId) {
//        return newTariffs.get(chatId);
//    }
//
//    public static void setTariffSession(Long chatId, TariffCreationSession session) {
//        newTariffs.put(chatId, session);
//    }
//
//    public static void removeTariffSession(Long chatId) {
//        newTariffs.remove(chatId);
//    }
//
//    public static void clearAll(Long chatId) {
//        waitingForInput.remove(chatId);
//        newTariffs.remove(chatId);
//    }
//}
