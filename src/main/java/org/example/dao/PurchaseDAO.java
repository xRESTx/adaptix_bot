package org.example.dao;

import org.example.database.DatabaseManager;
import org.example.table.Purchase;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

public class PurchaseDAO {

    private final SessionFactory sessionFactory;

    public PurchaseDAO() {
        // Используем централизованный DatabaseManager
        this.sessionFactory = DatabaseManager.getInstance().getSessionFactory();
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

    public List<Purchase> findByProductId(int productId) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Purchase p WHERE p.product.idProduct = :productId", Purchase.class)
                    .setParameter("productId", productId)
                    .list();
        }
    }
    
    public Purchase findByUserAndProduct(org.example.table.User user, org.example.table.Product product) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Purchase p WHERE p.user = :user AND p.product = :product ORDER BY p.date DESC", Purchase.class)
                    .setParameter("user", user)
                    .setParameter("product", product)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }
    
    public List<Purchase> findByUserId(Long userId) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Purchase p WHERE p.user.idUser = :userId ORDER BY p.date DESC", Purchase.class)
                    .setParameter("userId", userId)
                    .list();
        }
    }

    // Удаляем метод close() - управление жизненным циклом в DatabaseManager

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
