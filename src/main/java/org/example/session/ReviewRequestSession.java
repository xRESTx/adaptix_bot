package org.example.session;

import org.example.table.Product;

public class ReviewRequestSession {
    private ReviewRequest request = new ReviewRequest();
    private Step step = Step.ARTICUL_CHECK;
    private Product product;

    public enum Step {
        ARTICUL_CHECK,        // 1. артикул
        FULL_NAME,            // 2. ФИО
        PHONE_NUMBER,         // 3. телефон
        CARD_NUMBER,          // 4. номер карты
        PURCHASE_AMOUNT,      // 5. сумма покупки
        BANK_NAME,            // 6. банк-эмитент
        ORDER_SCREENSHOT,     // 7. скриншот заказа
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
}