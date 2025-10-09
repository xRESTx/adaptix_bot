package org.example.tgProcessing;

import org.example.dao.ProductDAO;
import org.example.dao.UserDAO;
import org.example.session.ProductCreationSession;
import org.example.session.SessionStore;
import org.example.table.Product;
import org.example.table.User;
import org.example.telegramBots.TelegramBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

public class MessageProcessing {

    public void sentPhotoUpdate(Update update) {
        Sent sent = new Sent();
        long chatId = update.getMessage().getChatId();
        UserDAO userDAO = new UserDAO();
        Integer threadID = update.getMessage().getMessageThreadId();
        if (threadID != null) {
            User user = userDAO.findByIdMessage(threadID);
            sent.sendPhoto(user.getIdUser(), null, chatId, update.getMessage().getMessageId());
        } else {
            User user = userDAO.findById(chatId);
            ResourceBundle rb = ResourceBundle.getBundle("app");
            long groupID = Long.parseLong(rb.getString("tg.group"));

            sent.sendPhoto(groupID, user.getId_message(), chatId, update.getMessage().getMessageId());
        }
    }

    public void handleUpdate(Update update) throws TelegramApiException, IOException {
        Sent createTelegramBot = new Sent();
        LogicUI logicUI = new LogicUI();

        ResourceBundle rb = ResourceBundle.getBundle("app");
        long groupID = Long.parseLong(rb.getString("tg.group"));

        String msg = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        if (String.valueOf(chatId).startsWith("-100")) {
            return;
        }
        UserDAO userDAO = new UserDAO();
        Integer threadID = update.getMessage().getMessageThreadId();
        if (threadID != null) {
            User user = userDAO.findByIdMessage(threadID);
            createTelegramBot.sendMessageFromBot(user.getIdUser(), msg);
            return;
        }

        User user = userDAO.findById(chatId);

        if (user != null) {
            createTelegramBot.sendMessageUser(groupID, user.getId_message(), "Пользователь: " + msg);
        } else {
            logicUI.sendStart(chatId, update);
            return;
        }

        String state = SessionStore.getState(chatId);
        if(state!= null) {
            if ("PRODUCT_CREATION".equals(state)) {
                ProductCreationSession session = SessionStore.getProductSession(chatId);
                if (session != null) {
                    switch (session.getStep()) {
                        case ARTICUL:
                            try {
                                int articul = Integer.parseInt(msg);
                                session.getProduct().setArticul(articul);
                                session.setStep(ProductCreationSession.Step.PRODUCT_NAME);
                                createTelegramBot.sendMessage(user, "Введите название товара:");
                            } catch (NumberFormatException e) {
                                createTelegramBot.sendMessage(user, "Некорректный артикуль. Пожалуйста, введите число.");
                                return;
                            }
                            break;
                        case PRODUCT_NAME:
                            session.getProduct().setProductName(msg);
                            session.setStep(ProductCreationSession.Step.CASHBACK_PERCENTAGE);
                            createTelegramBot.sendMessage(user, "Введите процент кэшбэка:");
                            break;
                        case CASHBACK_PERCENTAGE:
                            try {
                                int cashbackPercentage = Integer.parseInt(msg);
                                session.getProduct().setCashbackPercentage(cashbackPercentage);
                                session.setStep(ProductCreationSession.Step.KEY_QUERY);
                                createTelegramBot.sendMessage(user, "Введите ключевой запрос:");
                            } catch (NumberFormatException e) {
                                createTelegramBot.sendMessage(user, "Некорректный процент кэшбэка. Пожалуйста, введите число.");
                            }
                            break;
                        case KEY_QUERY:
                            session.getProduct().setKeyQuery(msg);
                            session.setStep(ProductCreationSession.Step.NUMBER_PARTICIPANTS);
                            createTelegramBot.sendMessage(user, "Введите количество участников:");
                            break;
                        case NUMBER_PARTICIPANTS:
                            try {
                                int numberParticipants = Integer.parseInt(msg);
                                session.getProduct().setNumberParticipants(numberParticipants);
                                session.setStep(ProductCreationSession.Step.ADDITIONAL_CONDITIONS);
                                createTelegramBot.sendMessage(user, "Введите дополнительные условия:");
                            } catch (NumberFormatException e) {
                                createTelegramBot.sendMessage(user, "Некорректное количество участников. Пожалуйста, введите число.");
                            }
                            break;
                        case ADDITIONAL_CONDITIONS:
                            session.getProduct().setAdditionalСonditions(msg);
                            session.setStep(ProductCreationSession.Step.PHOTO);
                            createTelegramBot.sendMessage(user, "Отправьте фотографию товара:");
                            break;
                        case PHOTO:
                            handlePhoto(update, user);
                            break;
                    }
                }
            }
            if(state.startsWith("addAdmin_")){
                String find = msg.replace("@","");
                User userFind = userDAO.findByUsername(find);
                if(userFind!=null){
                    if(userFind.getIdUser() == user.getIdUser()){
                        createTelegramBot.sendMessage(userFind,"Самого себя разжаловать нельзя(");
                    }else {
                        if(!userFind.isAdmin()){
                            userDAO.updateAdminByTgId(userFind.getIdUser(),true);
                            createTelegramBot.sendMessage(user,"Админ добавлен");
                            createTelegramBot.sendMessage(userFind,"Вы новый администратор");
                            logicUI.sendAdminMenu(userFind,null);

                        }else {
                            userDAO.updateAdminByTgId(userFind.getIdUser(),false);
                            createTelegramBot.sendMessage(user,"Админ удален");
                            createTelegramBot.sendMessage(userFind,"Вы разжалованы");
                            logicUI.sendStart(userFind.getIdUser(),update);
                        }
                    }
                }else {
                    createTelegramBot.sendMessage(user,"Человека с таким именем в БД не найдено");
                }

                SessionStore.removeState(chatId);
                return;
            }
        }
        switch (msg) {
            case "/start" -> logicUI.sendStart(chatId, update);
            case "Админ меню" -> logicUI.sendAdminMenu(user,null);
            case "Каталог товаров" -> logicUI.sendProducts(user);
        }
    }

    public void handlePhoto(Update update, User user) {
        Sent createTelegramBot = new Sent();
        long chatId = update.getMessage().getChatId();
        Message message = update.getMessage();
        TelegramBot telegramBot = new TelegramBot();
        ProductCreationSession session = SessionStore.getProductSession(chatId);

        if (session != null && session.getStep() == ProductCreationSession.Step.PHOTO) {
            PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
            String photoId = photo.getFileId();
            try {
                File uploadDir = new File("upload/");
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                String fileName = sdf.format(new Date()) + ".jpg";
                Path filePath = Paths.get("upload/", fileName);

                telegramBot.downloadFile(photoId, filePath.toString());

                session.getProduct().setPhoto(filePath.toString());
                ProductDAO productDAO = new ProductDAO();
                productDAO.save(session.getProduct());
                SessionStore.clearAll(chatId);
                String textProduct =
                        "Вы выбрали товар: "+ session.getProduct().getProductName() + " \n" +
                                "\n" +
                                "Кешбек " + session.getProduct().getCashbackPercentage() +  "% после публикации отзыва \uD83D\uDE4F\n" +
                                "Принимаем только карты Сбера (Россия)\n" +
                                "\n" +
                                "Условия участия:\n" +
                                "- Подпишитесь на наш канал @adaptix_focus \uD83D\uDE09\n" +
                                "- Включите запись экрана (мы её можем запросить)\n" +
                                "- Найдите наш товар по запросу \""+ session.getProduct().getKeyQuery() +"\" \uD83D\uDD0E\n" +
                                "- Закажите товар и заполните заявку\n" +
                                "- Заберите товар с ПВЗ в течении 3 дней\uD83D\uDC4D\n" +
                                "- Согласуйте свой отзыв с фотографиями в нашем боте\n" +
                                "- Оставьте свой отзыв и заполните форму получения кешбека (только когда отзыв опубликовали)\n" +
                                "- Кешбек ВЫПЛАЧИВАЕТСЯ В ПН И ПТ\uD83D\uDCB3\n" +
                                "\n" +
                                "Важно:\n" +
                                "- Участвовать можно только в одной раздаче на один аккаунт не чаще чем раз в две недели\n" +
                                "- ФИО в заказе должно совпадать с номером карты\uD83D\uDC64\n" +
                                "- Качественные фотографии в отзыве обязательны\uD83D\uDCF8\n" +
                                "- Отзыв нужно оставить не позднее 3 дней после забора товара с ПВЗ \uD83D\uDCC5\n" +
                                "- Желающие возвращать товар на ПВЗ не могут участвовать в акции \uD83D\uDEAB";
                createTelegramBot.sendPhoto(user.getIdUser(),filePath.toString(),textProduct);
            } catch (TelegramApiException | IOException e) {
                e.printStackTrace();
                createTelegramBot.sendMessage(user, "Произошла ошибка при загрузке фотографии.");
            }
        }
    }
    public void callBackQuery(Update update) {
        Sent createTelegramBot = new Sent();
        LogicUI logicUI = new LogicUI();
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        UserDAO userDAO = new UserDAO();
        User user = userDAO.findById(chatId);

        if(data.startsWith("product_")){
            ProductDAO productDAO = new ProductDAO();
            String[] parts = data.split(":");
            Product selected = productDAO.findById(Integer.parseInt(parts[1]));
            logicUI.sentOneProduct(user,selected, Integer.parseInt(parts[2]));
        } else if (data.startsWith("changeVisible_")) {
            int productId = Integer.parseInt(data.substring("changeVisible_".length()));
            ProductDAO productDAO = new ProductDAO();
            Product product = productDAO.findById(productId);
            boolean visible = product.isVisible();
            productDAO.updateVisibleById(productId,!visible);
            if(visible){
                createTelegramBot.sendMessage(user,"Товар теперь невидим");
            }else {
                createTelegramBot.sendMessage(user,"Товар теперь видим");
            }
        }
        if (data.contains(":")) {
            String[] parts = data.split(":", 2);
            String command = parts[0];
            String messageID = parts[1];
            switch (command) {
                case "addAdmin": {
                    if (user.isAdmin()) {
                        SessionStore.setState(chatId,"addAdmin_");
                        createTelegramBot.editMessageMarkup(user, Integer.parseInt(messageID), "Отправьте тег (Например @qwerty123)", null);
                    }
                    break;
                }
                case "addProduct": {
                    if (user.isAdmin()) {
                        ProductCreationSession session = new ProductCreationSession();
                        SessionStore.setProductSession(chatId, session);
                        SessionStore.setState(chatId, "PRODUCT_CREATION");
                        createTelegramBot.sendMessage(user, "Отправьте артикуль товара:");
                    } else {
                        createTelegramBot.sendMessage(user, "У вас нет прав для добавления товара.");
                    }
                    break;
                }
                case "isUser":{
                    if(user.isAdmin()){
                        userDAO.updateUserByTgId(chatId,!user.isUserFlag());
                        user.setUserFlag(!user.isUserFlag());
                        logicUI.sendAdminMenu(user, Integer.parseInt(messageID));
                    }
                    break;
                }
                case "Exit":{
                    if(user.isAdmin()){
                        logicUI.sendAdminMenu(user, Integer.parseInt(messageID));
                    }
                    break;
                }
            }
            switch (data) {
                default -> {
                }
            }
        }
    }
}