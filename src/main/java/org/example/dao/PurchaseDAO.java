package org.example.dao;

import org.example.table.Purchase;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.util.List;

public class PurchaseDAO {

    private final SessionFactory sessionFactory;

    public PurchaseDAO() {
        try {
            sessionFactory = new Configuration().configure().buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError("Ошибка создания SessionFactory: " + ex);
        }
    }

    public void save(Purchase purchase) {
        executeInsideTransaction(session -> session.persist(purchase));
    }

    public void update(Purchase purchase) {
        executeInsideTransaction(session -> session.merge(purchase));
    }

    public Purchase findById(int id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Purchase.class, id);
        }
    }

    public List<Purchase> findAll() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Purchase", Purchase.class).list();
        }
    }

    public void deleteById(int id) {
        executeInsideTransaction(session -> {
            Purchase purchase = session.get(Purchase.class, id);
            if (purchase != null) {
                session.remove(purchase);
            }
        });
    }

    public Purchase findFirst() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Purchase", Purchase.class)
                    .setMaxResults(1)
                    .uniqueResult();
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
