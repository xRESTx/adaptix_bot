package org.example.dao;

import org.example.table.Product;
import jakarta.persistence.*;
import java.util.List;

public class ProductDAO {

    private final EntityManager em;

    public ProductDAO(EntityManager em) {
        this.em = em;
    }

    public void save(Product product) {
        em.getTransaction().begin();
        em.persist(product);
        em.getTransaction().commit();
    }

    public Product findById(int id) {
        return em.find(Product.class, id);
    }

    public List<Product> findAll() {
        return em.createQuery("SELECT p FROM Product p", Product.class).getResultList();
    }

    public void update(Product product) {
        em.getTransaction().begin();
        em.merge(product);
        em.getTransaction().commit();
    }

    public void delete(int id) {
        em.getTransaction().begin();
        Product p = em.find(Product.class, id);
        if (p != null) em.remove(p);
        em.getTransaction().commit();
    }
}