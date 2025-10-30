package org.example.session;

import org.example.table.Purchase;

public class CashbackSession {
    private Purchase purchase;
    private Step step = Step.CARD_INPUT;
    private String cardNumber;
    
    // Для пересылки скриншота кешбека
    private String screenshotFileId; // Telegram file_id скриншота кешбека
    private Integer screenshotMessageId; // ID сообщения со скриншотом кешбека
    
    public enum Step {
        CARD_INPUT,     // Ввод номера карты
        SCREENSHOT,     // Отправка скриншота отзыва
        COMPLETE        // Завершение
    }
    
    public CashbackSession(Purchase purchase) {
        this.purchase = purchase;
    }
    
    public Purchase getPurchase() {
        return purchase;
    }
    
    public void setPurchase(Purchase purchase) {
        this.purchase = purchase;
    }
    
    public Step getStep() {
        return step;
    }
    
    public void setStep(Step step) {
        this.step = step;
    }
    
    public String getCardNumber() {
        return cardNumber;
    }
    
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    public boolean isComplete() {
        return step == Step.COMPLETE;
    }
    
    public String getScreenshotFileId() {
        return screenshotFileId;
    }
    
    public void setScreenshotFileId(String screenshotFileId) {
        this.screenshotFileId = screenshotFileId;
    }
    
    public Integer getScreenshotMessageId() {
        return screenshotMessageId;
    }
    
    public void setScreenshotMessageId(Integer screenshotMessageId) {
        this.screenshotMessageId = screenshotMessageId;
    }
}
