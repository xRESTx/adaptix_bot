package org.example.dao;

import org.example.table.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.util.List;

public class UserDAO {

    private final SessionFactory sessionFactory;

    public UserDAO() {
        try {
            sessionFactory = new Configuration().configure().buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError("Ошибка создания SessionFactory: " + ex);
        }
    }

    public void save(User user) {
        executeInsideTransaction(session -> session.persist(user));
    }

    public void update(User user) {
        executeInsideTransaction(session -> session.merge(user));
    }

    public User findById(long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(User.class, id);
        }
    }
    public User findByIdMessage(int idMessage) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM Users WHERE id_message = :id_message";
            return session.createQuery(hql, User.class)
                    .setParameter("id_message", idMessage)
                    .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public List<User> findAll() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Users", User.class).list();
        }
    }

    public void deleteById(int id) {
        executeInsideTransaction(session -> {
            User user = session.get(User.class, id);
            if (user != null) {
                session.remove(user);
            }
        });
    }

    public User findFirst() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Users", User.class)
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
