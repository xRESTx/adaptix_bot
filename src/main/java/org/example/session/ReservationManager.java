package org.example.session;

import org.example.dao.ProductDAO;
import org.example.table.Product;

/**
 * Менеджер для управления количеством участников товаров
 * (ранее ReservationManager, теперь упрощенный для управления только количеством)
 */
public class ReservationManager {
    
    /**
     * Увеличить количество участников товара
     */
    public static boolean incrementProductParticipants(int productId) {
        ProductDAO productDAO = new ProductDAO();
        return productDAO.incrementParticipantsIfAvailablePessimistic(productId);
    }
    
    /**
     * Уменьшить количество участников товара
     */
    public static boolean decrementProductParticipants(int productId) {
        ProductDAO productDAO = new ProductDAO();
        Product product = productDAO.findById(productId);
        
        if (product == null) {
            return false;
        }
        
        if (product.getNumberOfParticipants() <= 0) {
            return false;
        }
        
        product.decrementParticipants();
        productDAO.update(product);
        return true;
    }
}