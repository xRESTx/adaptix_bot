package org.example.table;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "purchase")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPurchase")
    private int idPurchase;

    @ManyToOne
    @JoinColumn(name = "idProduct", nullable = false, foreignKey = @ForeignKey(name = "purchase_idProduct_product_idProduct_foreign"))
    private Product product;

    @ManyToOne
    @JoinColumn(name = "idUser", nullable = false, foreignKey = @ForeignKey(name = "purchase_idUser_user_idUser_foreign"))
    private User user;

    @Column(name = "date", nullable = false)
    private LocalDate date;
    
    @Column(name = "orderTime")
    private java.time.LocalTime orderTime;

    @Column(name = "purchaseStage", nullable = false)
    private int purchaseStage;

    @Column(name = "groupMessageId")
    private Long groupMessageId;
    
    @Column(name = "orderMessageId")
    private Long orderMessageId;  // Этап 1: товар заказан
    
    @Column(name = "reviewMessageId")
    private Long reviewMessageId;  // Этап 2: оставить отзыв
    
    @Column(name = "cashbackMessageId")
    private Long cashbackMessageId;  // Этап 3: получить кешбек
    
    @Column(name = "cardNumber")
    private String cardNumber;  // Номер карты для кешбека

    public int getIdPurchase() {
        return idPurchase;
    }

    public void setIdPurchase(int idPurchase) {
        this.idPurchase = idPurchase;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public java.time.LocalTime getOrderTime() {
        return orderTime;
    }
    
    public void setOrderTime(java.time.LocalTime orderTime) {
        this.orderTime = orderTime;
    }

    public int getPurchaseStage() {
        return purchaseStage;
    }

    public void setPurchaseStage(int purchaseStage) {
        this.purchaseStage = purchaseStage;
    }

    public Long getGroupMessageId() {
        return groupMessageId;
    }

    public void setGroupMessageId(Long groupMessageId) {
        this.groupMessageId = groupMessageId;
    }
    
    public Long getOrderMessageId() {
        return orderMessageId;
    }
    
    public void setOrderMessageId(Long orderMessageId) {
        this.orderMessageId = orderMessageId;
    }
    
    public Long getReviewMessageId() {
        return reviewMessageId;
    }
    
    public void setReviewMessageId(Long reviewMessageId) {
        this.reviewMessageId = reviewMessageId;
    }
    
    public Long getCashbackMessageId() {
        return cashbackMessageId;
    }
    
    public void setCashbackMessageId(Long cashbackMessageId) {
        this.cashbackMessageId = cashbackMessageId;
    }
    
    public String getCardNumber() {
        return cardNumber;
    }
    
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
}