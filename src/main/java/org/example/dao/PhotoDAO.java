package org.example.dao;

import org.example.table.Photo;
import jakarta.persistence.*;
import java.util.List;

public class PhotoDAO {

    private final EntityManager em;

    public PhotoDAO(EntityManager em) {
        this.em = em;
    }

    public void save(Photo photo) {
        em.getTransaction().begin();
        em.persist(photo);
        em.getTransaction().commit();
    }

    public Photo findById(int id) {
        return em.find(Photo.class, id);
    }

    public List<Photo> findAll() {
        return em.createQuery("SELECT ph FROM Photo ph", Photo.class).getResultList();
    }

    public void update(Photo photo) {
        em.getTransaction().begin();
        em.merge(photo);
        em.getTransaction().commit();
    }

    public void delete(int id) {
        em.getTransaction().begin();
        Photo ph = em.find(Photo.class, id);
        if (ph != null) em.remove(ph);
        em.getTransaction().commit();
    }
}