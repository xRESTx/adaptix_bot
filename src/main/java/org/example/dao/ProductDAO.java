package org.example.dao;

import org.example.table.Product;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.util.List;

public class ProductDAO {

    private final SessionFactory sessionFactory;

    public ProductDAO() {
        try {
            sessionFactory = new Configuration().configure().buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError("Ошибка создания SessionFactory: " + ex);
        }
    }

    public void save(Product product) {
        executeInsideTransaction(session -> session.persist(product));
    }

    public void update(Product product) {
        executeInsideTransaction(session -> session.merge(product));
    }

    public Product findById(int id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Product.class, id);
        }
    }

    public List<Product> findAll() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Product", Product.class).list();
        }
    }
    public List<Product> findAllVisible() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Product where visible = true", Product.class).list();
        }
    }

    public void deleteById(int id) {
        executeInsideTransaction(session -> {
            Product product = session.get(Product.class, id);
            if (product != null) {
                session.remove(product);
            }
        });
    }

    public Product findFirst() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Product", Product.class)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }
    public void updateVisibleById(int id, boolean visible) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            Query<?> query = session.createQuery("update Product set visible = :visible where idProduct = :idProduct");
            query.setParameter("visible", visible);
            query.setParameter("idProduct", id);
            query.executeUpdate();
            tx.commit();
        }
    }

    public void close() {
        sessionFactory.close();
    }

    // Вспомогательный метод для управления транзакциями
    private void executeInsideTransaction(SessionAction action) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                action.execute(session);
                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }

    @FunctionalInterface
    private interface SessionAction {
        void execute(Session session);
    }
}
