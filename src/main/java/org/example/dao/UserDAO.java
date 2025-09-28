package org.example.dao;

import org.example.table.User;
import jakarta.persistence.*;
import java.util.List;

public class UserDAO {

    private final EntityManager em;

    public UserDAO(EntityManager em) {
        this.em = em;
    }

    public void save(User user) {
        em.getTransaction().begin();
        em.persist(user);
        em.getTransaction().commit();
    }

    public User findById(int id) {
        return em.find(User.class, id);
    }

    public List<User> findAll() {
        return em.createQuery("SELECT u FROM User u", User.class).getResultList();
    }

    public void update(User user) {
        em.getTransaction().begin();
        em.merge(user);
        em.getTransaction().commit();
    }

    public void delete(int id) {
        em.getTransaction().begin();
        User u = em.find(User.class, id);
        if (u != null) em.remove(u);
        em.getTransaction().commit();
    }
}