package org.example.table;

import jakarta.persistence.*;

@Entity
@Table(name = "photo")
public class Photo {

    @Id
    @Column(name = "idPhoto")
    private String idPhoto;

    @ManyToOne
    @JoinColumn(name = "idPurchase", nullable = false, foreignKey = @ForeignKey(name = "photo_idPurchase_purchase_idPurchase_foreign"))
    private Purchase purchase;

    @ManyToOne
    @JoinColumn(name = "idUser", nullable = false, foreignKey = @ForeignKey(name = "photo_idUser_user_idUser_foreign"))
    private User user;

    public String getIdPhoto() {
        return idPhoto;
    }

    public void setIdPhoto(String idPhoto) {
        this.idPhoto = idPhoto;
    }

    public Purchase getPurchase() {
        return purchase;
    }

    public void setPurchase(Purchase purchase) {
        this.purchase = purchase;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}