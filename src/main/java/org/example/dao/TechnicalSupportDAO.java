package org.example.dao;

import org.example.database.DatabaseManager;
import org.example.table.Photo;
import org.example.table.TechnicalSupport;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

public class TechnicalSupportDAO {
    private final SessionFactory sessionFactory;

    public TechnicalSupportDAO() {
        this.sessionFactory = DatabaseManager.getInstance().getSessionFactory();
    }

    public void update(TechnicalSupport technicalSupport) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            session.merge(technicalSupport);
            tx.commit();
        }
    }

    public TechnicalSupport findByKey(String key) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM TechnicalSupport WHERE key = :key", TechnicalSupport.class)
                    .setParameter("key", key)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }
}
