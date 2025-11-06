package org.example.settings;

import org.example.dao.TechnicalSupportDAO;
import org.example.table.TechnicalSupport;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Настройки администратора
 */
public class AdminSettings {
    private static final AdminSettings instance = new AdminSettings();
    private final Map<String, String> settings = new ConcurrentHashMap<>();
    
    private AdminSettings() {
        TechnicalSupportDAO technicalSupportDAO = new TechnicalSupportDAO();
        TechnicalSupport technicalSupport = technicalSupportDAO.findByKey("support_mention");

        settings.put("support_mention", technicalSupport.getUsername());
    }
    
    public static AdminSettings getInstance() {
        return instance;
    }
    
    /**
     * Получить значение настройки
     */
    public String getSetting(String key) {
        return settings.getOrDefault(key, "");
    }
    
    /**
     * Установить значение настройки
     */
    public void setSetting(String key, String value) {
        TechnicalSupportDAO technicalSupportDAO = new TechnicalSupportDAO();
        TechnicalSupport technicalSupport = technicalSupportDAO.findByKey("support_mention");

        technicalSupport.setUsername(value);
        technicalSupportDAO.update(technicalSupport);

        settings.put(key, value);
    }
    
    /**
     * Получить упоминание техподдержки
     */
    public String getSupportMention() {
        return getSetting("support_mention");
    }
    
    /**
     * Установить упоминание техподдержки
     */
    public void setSupportMention(String mention) {
        setSetting("support_mention", mention);
    }
    
    /**
     * Получить все настройки
     */
    public Map<String, String> getAllSettings() {
        return new ConcurrentHashMap<>(settings);
    }
}
