package org.example.session;

import org.example.table.User;
import org.example.table.Purchase;

/**
 * Сессия для ввода причины отмены покупки администратором
 */
public class PurchaseCancellationSession {
    private User admin;
    private Purchase purchase;
    private String reason;
    private boolean reasonEntered = false;
    
    public PurchaseCancellationSession(User admin, Purchase purchase) {
        this.admin = admin;
        this.purchase = purchase;
    }
    
    public User getAdmin() {
        return admin;
    }
    
    public void setAdmin(User admin) {
        this.admin = admin;
    }
    
    public Purchase getPurchase() {
        return purchase;
    }
    
    public void setPurchase(Purchase purchase) {
        this.purchase = purchase;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public boolean isReasonEntered() {
        return reasonEntered;
    }
    
    public void setReasonEntered(boolean reasonEntered) {
        this.reasonEntered = reasonEntered;
    }
}
