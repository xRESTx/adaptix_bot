package org.example.session;

import org.example.table.Product;

public class ProductCreationSession {
    private Product product = new Product();
    private Step step = Step.ARTICUL;

    public ProductCreationSession() {
        // Устанавливаем товар как видимый по умолчанию
        product.setVisible(false);
        // Устанавливаем начальное количество участников в 0
        product.setNumberOfParticipants(0);
    }

    public enum Step {
        ARTICUL,
        PRODUCT_NAME,
        CASHBACK_PERCENTAGE,
        KEY_QUERY,
        NUMBER_PARTICIPANTS,
        ADDITIONAL_CONDITIONS,
        PHOTO
    }

    // Геттеры и сеттеры
    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        this.step = step;
    }
}