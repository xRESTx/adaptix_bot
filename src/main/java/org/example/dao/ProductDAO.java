package org.example.dao;

import org.example.database.DatabaseManager;
import org.example.table.Product;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.hibernate.LockOptions;

import java.util.List;

public class ProductDAO {

    private final SessionFactory sessionFactory;

    public ProductDAO() {
        // Используем централизованный DatabaseManager
        this.sessionFactory = DatabaseManager.getInstance().getSessionFactory();
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
            List<Product> products = session.createQuery("FROM Product", Product.class).list();
            System.out.println("🔍 findAll: Found " + products.size() + " total products in database");
            // Убираем детальное логирование для каждого товара - это замедляет работу
            return products;
        }
    }
    public List<Product> findAllVisible() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Product where visible = true", Product.class).list();
        }
    }

    /**
     * Получить все товары, доступные для покупки пользователями
     * (видимые и с доступными местами в акции)
     */
    public List<Product> findAllAvailableForUsers() {
        try (Session session = sessionFactory.openSession()) {
            List<Product> products = session.createQuery(
                "FROM Product WHERE visible = true AND numberOfParticipants < numberParticipants", 
                Product.class
            ).list();
            
            System.out.println("🔍 findAllAvailableForUsers: Found " + products.size() + " available products");
            // Убираем детальное логирование для каждого товара - это замедляет работу
            return products;
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

    /**
     * Пессимистичная блокировка: атомарное увеличение числа участников, если есть место.
     */
    public boolean incrementParticipantsIfAvailablePessimistic(int productId) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                // Загружаем с блокировкой на запись
                Product product = session.get(Product.class, productId, LockOptions.UPGRADE);
                if (product == null) {
                    tx.rollback();
                    return false;
                }
                // Проверяем и увеличиваем внутри одной транзакции под блокировкой
                if (product.getNumberOfParticipants() < product.getNumberParticipants()) {
                    product.setNumberOfParticipants(product.getNumberOfParticipants() + 1);
                    session.merge(product);
                    tx.commit();
                    return true;
                } else {
                    tx.rollback();
                    return false;
                }
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
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
