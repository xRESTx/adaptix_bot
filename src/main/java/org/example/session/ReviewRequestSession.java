package org.example.session;

import org.example.table.Product;

public class ReviewRequestSession {
    private ReviewRequest request = new ReviewRequest();
    private Step step = Step.SEARCH_SCREENSHOT;
    private Product product;
    private Long groupMessageId;
    private String searchScreenshotPath;  // Путь к скриншоту поиска
    private String deliveryScreenshotPath; // Путь к скриншоту доставки

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

    public String getSearchScreenshotPath() {
        return searchScreenshotPath;
    }

    public void setSearchScreenshotPath(String searchScreenshotPath) {
        this.searchScreenshotPath = searchScreenshotPath;
    }

    public String getDeliveryScreenshotPath() {
        return deliveryScreenshotPath;
    }

    public void setDeliveryScreenshotPath(String deliveryScreenshotPath) {
        this.deliveryScreenshotPath = deliveryScreenshotPath;
    }
}