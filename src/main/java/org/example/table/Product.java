package org.example.table;

import jakarta.persistence.*;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idProduct")
    private int idProduct;

    @Column(name = "articul", nullable = false)
    private int articul;

    @Column(name = "productName", nullable = false, length = 255)
    private String productName;

    @Column(name = "cashbackPercentage", nullable = false)
    private int cashbackPercentage;

    @Column(name = "keyQuery", nullable = false, length = 255)
    private String keyQuery;

    @Column(name = "numberParticipants", nullable = false)
    private int numberParticipants;

    @Column(name = "numberOfParticipants", nullable = false)
    private int numberOfParticipants;

    @Column(name = "additionalСonditions", nullable = false, columnDefinition = "text")
    private String additionalСonditions;

    @Column(name = "photo", nullable = false)
    private String photo;

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public int getIdProduct() {
        return idProduct;
    }

    public void setIdProduct(int idProduct) {
        this.idProduct = idProduct;
    }

    public int getArticul() {
        return articul;
    }

    public void setArticul(int articul) {
        this.articul = articul;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getCashbackPercentage() {
        return cashbackPercentage;
    }

    public void setCashbackPercentage(int cashbackPercentage) {
        this.cashbackPercentage = cashbackPercentage;
    }

    public String getKeyQuery() {
        return keyQuery;
    }

    public void setKeyQuery(String keyQuery) {
        this.keyQuery = keyQuery;
    }

    public int getNumberParticipants() {
        return numberParticipants;
    }

    public void setNumberParticipants(int numberParticipants) {
        this.numberParticipants = numberParticipants;
    }

    public int getNumberOfParticipants() {
        return numberOfParticipants;
    }

    public void setNumberOfParticipants(int numberOfParticipants) {
        this.numberOfParticipants = numberOfParticipants;
    }

    public String getAdditionalСonditions() {
        return additionalСonditions;
    }

    public void setAdditionalСonditions(String additionalСonditions) {
        this.additionalСonditions = additionalСonditions;
    }
}