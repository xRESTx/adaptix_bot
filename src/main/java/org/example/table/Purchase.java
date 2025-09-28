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

    @Column(name = "purchaseStage", nullable = false)
    private int purchaseStage;

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

    public int getPurchaseStage() {
        return purchaseStage;
    }

    public void setPurchaseStage(int purchaseStage) {
        this.purchaseStage = purchaseStage;
    }
}