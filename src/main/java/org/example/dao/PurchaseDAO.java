package org.example.dao;

import org.example.database.DatabaseManager;
import org.example.table.Purchase;
import org.example.table.Product;
import org.example.table.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

public class
PurchaseDAO {

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
            // Используем JOIN FETCH для загрузки связанных объектов User и Product
            return session.createQuery(
                "SELECT DISTINCT p FROM Purchase p " +
                "LEFT JOIN FETCH p.user " +
                "LEFT JOIN FETCH p.product " +
                "WHERE p.product.idProduct = :productId " +
                "ORDER BY p.date DESC", Purchase.class)
                .setParameter("productId", productId)
                .list();
        } catch (Exception e) {
            System.err.println("❌ Error loading purchases for product " + productId + ": " + e.getMessage());
            if (e.getMessage().contains("NanoOfSecond") || e.getMessage().contains("Invalid value")) {
                System.err.println("🔧 Detected orderTime parsing error, using fallback method");
            }
            // Fallback к запросу без orderTime для избежания ошибок DateTimeException
            try (Session session = sessionFactory.openSession()) {
                // Оптимизированный запрос с JOIN для избежания N+1 проблемы
                List<Object[]> results = session.createQuery(
                    "SELECT p.idPurchase, p.date, p.purchaseStage, p.groupMessageId, " +
                    "p.orderMessageId, p.reviewMessageId, p.cashbackMessageId, p.cardNumber, p.purchaseAmount, " +
                    "pr.idProduct, pr.productName, " +
                    "u.idUser, u.username " +
                    "FROM Purchase p " +
                    "LEFT JOIN p.product pr " +
                    "LEFT JOIN p.user u " +
                    "WHERE p.product.idProduct = :productId " +
                    "ORDER BY p.date DESC", Object[].class)
                    .setParameter("productId", productId)
                    .list();

                // Создаем объекты Purchase вручную
                List<Purchase> purchases = new java.util.ArrayList<>();
                for (Object[] row : results) {
                    try {
                        Purchase purchase = new Purchase();
                        purchase.setIdPurchase((Integer) row[0]);
                        purchase.setDate((java.time.LocalDate) row[1]);
                        purchase.setPurchaseStage((Integer) row[2]);
                        purchase.setGroupMessageId((Long) row[3]);
                        purchase.setOrderMessageId((Long) row[4]);
                        purchase.setReviewMessageId((Long) row[5]);
                        purchase.setCashbackMessageId((Long) row[6]);
                        purchase.setCardNumber((String) row[7]);
                        purchase.setPurchaseAmount((Integer) row[8]);

                        // Создаем объекты Product и User из JOIN результатов
                        if (row[9] != null) {
                            Product product = new Product();
                            product.setIdProduct((Integer) row[9]);
                            product.setProductName((String) row[10]);
                            purchase.setProduct(product);
                        }

                        if (row[11] != null) {
                            User user = new User();
                            user.setIdUser((Long) row[11]);
                            user.setUsername((String) row[12]);
                            purchase.setUser(user);
                        }

                        purchases.add(purchase);
                    } catch (Exception e2) {
                        System.err.println("⚠️ Skipping purchase due to error: " + e2.getMessage());
                        // Пропускаем проблемную запись
                    }
                }
                return purchases;
            } catch (Exception e2) {
                System.err.println("❌ Error loading purchases for product " + productId + " (fallback): " + e2.getMessage());
                return new java.util.ArrayList<>();
            }
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
            try {
                // Используем простой запрос без orderTime для избежания ошибок
                List<Object[]> results = session.createQuery(
                    "SELECT p.idPurchase, p.date, p.purchaseStage, p.groupMessageId, " +
                    "p.orderMessageId, p.reviewMessageId, p.cashbackMessageId, p.cardNumber, p.purchaseAmount, " +
                    "p.product.idProduct, p.user.idUser " +
                    "FROM Purchase p " +
                    "WHERE p.user.idUser = :userId " +
                    "ORDER BY p.date DESC", Object[].class)
                    .setParameter("userId", userId)
                    .list();
                
                // Создаем объекты Purchase вручную
                List<Purchase> purchases = new java.util.ArrayList<>();
                for (Object[] row : results) {
                    try {
                        Purchase purchase = new Purchase();
                        purchase.setIdPurchase((Integer) row[0]);
                        purchase.setDate((java.time.LocalDate) row[1]);
                        purchase.setPurchaseStage((Integer) row[2]);
                        purchase.setGroupMessageId((Long) row[3]);
                        purchase.setOrderMessageId((Long) row[4]);
                        purchase.setReviewMessageId((Long) row[5]);
                        purchase.setCashbackMessageId((Long) row[6]);
                        purchase.setCardNumber((String) row[7]);
                        purchase.setPurchaseAmount((Integer) row[8]);
                        
                        // Создаем объекты Product и User с минимальными данными
                        if (row[9] != null) {
                            // Загружаем полную информацию о товаре
                            ProductDAO productDAO = new ProductDAO();
                            Product product = productDAO.findById((Integer) row[9]);
                            if (product != null) {
                                System.out.println("🔍 Loaded product: " + product.getProductName() + " (ID: " + product.getIdProduct() + ")");
                                purchase.setProduct(product);
                            } else {
                                // Если товар не найден, создаем минимальный объект
                                System.out.println("⚠️ Product not found for ID: " + row[9] + ", creating minimal object");
                                Product minimalProduct = new Product();
                                minimalProduct.setIdProduct((Integer) row[9]);
                                minimalProduct.setProductName("Товар удален");
                                purchase.setProduct(minimalProduct);
                            }
                        }
                        
                        if (row[10] != null) {
                            // Загружаем полную информацию о пользователе
                            UserDAO userDAO = new UserDAO();
                            User user = userDAO.findById((Long) row[10]);
                            if (user != null) {
                                System.out.println("🔍 Loaded user: " + user.getUsername() + " (ID: " + user.getIdUser() + ")");
                                purchase.setUser(user);
                            } else {
                                // Если пользователь не найден, создаем минимальный объект
                                System.out.println("⚠️ User not found for ID: " + row[10] + ", creating minimal object");
                                User minimalUser = new User();
                                minimalUser.setIdUser((Long) row[10]);
                                minimalUser.setUsername("Пользователь удален");
                                purchase.setUser(minimalUser);
                            }
                        }
                        
                        purchases.add(purchase);
                    } catch (Exception e) {
                        System.err.println("⚠️ Skipping purchase due to error: " + e.getMessage());
                        // Пропускаем проблемную запись
                    }
                }
                return purchases;
            } catch (Exception e) {
                System.err.println("❌ Error loading purchases for user " + userId + ": " + e.getMessage());
                return new java.util.ArrayList<>();
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
