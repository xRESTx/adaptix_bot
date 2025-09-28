package org.example.dao;

import org.example.table.Purchase;
import jakarta.persistence.*;
import java.util.List;

public class PurchaseDAO {

    private final EntityManager em;

    public PurchaseDAO(EntityManager em) {
        this.em = em;
    }

    public void save(Purchase purchase) {
        em.getTransaction().begin();
        em.persist(purchase);
        em.getTransaction().commit();
    }

    public Purchase findById(int id) {
        return em.find(Purchase.class, id);
    }

    public List<Purchase> findAll() {
        return em.createQuery("SELECT p FROM Purchase p", Purchase.class).getResultList();
    }

    public void update(Purchase purchase) {
        em.getTransaction().begin();
        em.merge(purchase);
        em.getTransaction().commit();
    }

    public void delete(int id) {
        em.getTransaction().begin();
        Purchase p = em.find(Purchase.class, id);
        if (p != null) em.remove(p);
        em.getTransaction().commit();
    }
}