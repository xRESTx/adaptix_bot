package org.example.session;

public class ReviewRequest {
    private String articul;
    private String fullName;
    private String phoneNumber;
    private String cardNumber;
    private String purchaseAmount;
    private String bankName;
    private String orderScreenshotFileId; // Telegram file_id прикреплённого скриншота

    // --- getters & setters ---

    public String getArticul() {
        return articul;
    }

    public void setArticul(String articul) {
        this.articul = articul;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getPurchaseAmount() {
        return purchaseAmount;
    }

    public void setPurchaseAmount(String purchaseAmount) {
        this.purchaseAmount = purchaseAmount;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getOrderScreenshotFileId() {
        return orderScreenshotFileId;
    }

    public void setOrderScreenshotFileId(String orderScreenshotFileId) {
        this.orderScreenshotFileId = orderScreenshotFileId;
    }
}