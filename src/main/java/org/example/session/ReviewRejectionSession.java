package org.example.session;

import org.example.table.Purchase;

public class ReviewRejectionSession {
    private Purchase purchase;
    private String rejectionReason;
    
    public ReviewRejectionSession(Purchase purchase) {
        this.purchase = purchase;
    }
    
    public Purchase getPurchase() {
        return purchase;
    }
    
    public void setPurchase(Purchase purchase) {
        this.purchase = purchase;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
