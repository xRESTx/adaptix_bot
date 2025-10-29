package org.example.session;

import org.example.table.Product;

public class ReviewRequestSession {
    private ReviewRequest request = new ReviewRequest();
    private Step step = Step.SEARCH_SCREENSHOT;
    private Product product;
    private Long groupMessageId;
    private Integer searchScreenshotMessageId; // ID сообщения со скриншотом поиска
    private Integer deliveryScreenshotMessageId; // ID сообщения со скриншотом доставки
    private String searchScreenshotFileId; // Telegram file_id скриншота поиска
    private String deliveryScreenshotFileId; // Telegram file_id скриншота доставки
    private Integer purchaseId; // ID созданной покупки

    public enum Step {
        SEARCH_SCREENSHOT,    // 1. скриншот поиска и товара
        ARTICUL_CHECK,        // 2. артикул
        FULL_NAME,            // 3. ФИО
        PHONE_NUMBER,         // 4. телефон
        CARD_NUMBER,          // 5. номер карты
        PURCHASE_AMOUNT,      // 6. сумма покупки
        BANK_NAME,            // 7. банк-эмитент
        DELIVERY_SCREENSHOT,  // 8. скриншот раздела доставки
        COMPLETE              // финальное сообщение
    }

    public Product getProduct() {
        return product;
    }

    public  void setProduct(Product product) {
        this.product = product;
    }

    public ReviewRequest getRequest() {
        return request;
    }

    public void setRequest(ReviewRequest request) {
        this.request = request;
    }

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        this.step = step;
    }

    public Long getGroupMessageId() {
        return groupMessageId;
    }

    public void setGroupMessageId(Long groupMessageId) {
        this.groupMessageId = groupMessageId;
    }
    
    public Integer getPurchaseId() {
        return purchaseId;
    }
    
    public void setPurchaseId(Integer purchaseId) {
        this.purchaseId = purchaseId;
    }
    
    public Integer getSearchScreenshotMessageId() {
        return searchScreenshotMessageId;
    }
    
    public void setSearchScreenshotMessageId(Integer searchScreenshotMessageId) {
        this.searchScreenshotMessageId = searchScreenshotMessageId;
    }
    
    public Integer getDeliveryScreenshotMessageId() {
        return deliveryScreenshotMessageId;
    }
    
    public void setDeliveryScreenshotMessageId(Integer deliveryScreenshotMessageId) {
        this.deliveryScreenshotMessageId = deliveryScreenshotMessageId;
    }
    
    public String getSearchScreenshotFileId() {
        return searchScreenshotFileId;
    }
    
    public void setSearchScreenshotFileId(String searchScreenshotFileId) {
        this.searchScreenshotFileId = searchScreenshotFileId;
    }
    
    public String getDeliveryScreenshotFileId() {
        return deliveryScreenshotFileId;
    }
    
    public void setDeliveryScreenshotFileId(String deliveryScreenshotFileId) {
        this.deliveryScreenshotFileId = deliveryScreenshotFileId;
    }
}