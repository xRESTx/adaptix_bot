package org.example.dao;

import org.example.database.DatabaseManager;
import org.example.table.Photo;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

public class PhotoDAO {

    private final SessionFactory sessionFactory;

    public PhotoDAO() {
        // Используем централизованный DatabaseManager
        this.sessionFactory = DatabaseManager.getInstance().getSessionFactory();
    }

    public void save(Photo photo) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            session.persist(photo);
            tx.commit();
        }
    }

    public void update(Photo photo) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            session.merge(photo);
            tx.commit();
        }
    }

    public Photo findById(int id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Photo.class, id);
        }
    }

    public List<Photo> findAll() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Photo", Photo.class).list();
        }
    }

    public void deleteById(int id) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            Photo photo = session.get(Photo.class, id);
            if (photo != null) {
                session.remove(photo);
            }
            tx.commit();
        }
    }

    public Photo findFirst() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Photo", Photo.class)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }

    // Удаляем метод close() - управление жизненным циклом в DatabaseManager
}
