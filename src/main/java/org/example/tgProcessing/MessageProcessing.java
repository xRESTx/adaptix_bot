package org.example.tgProcessing;

import org.example.dao.PhotoDAO;
import org.example.dao.ProductDAO;
import org.example.dao.PurchaseDAO;
import org.example.dao.UserDAO;
import org.example.session.ProductCreationSession;
import org.example.session.ReviewRequestSession;
import org.example.session.SessionStore;
import org.example.table.Photo;
import org.example.table.Product;
import org.example.table.Purchase;
import org.example.table.User;
import org.example.telegramBots.TelegramBot;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

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
//        if (user != null) {
//            createTelegramBot.sendMessageUser(groupID, user.getId_message(), "Пользователь: " + msg);
//        } else {
//            logicUI.sendStart(chatId, update);
//            return;
//        }
        if(user == null){
            logicUI.sendStart(chatId, update);
            return;
        }
        if(msg!=null){
            switch (msg) {
                case "/start" -> logicUI.sendStart(chatId, update);
                case "Админ меню" -> logicUI.sendAdminMenu(user,null);
                case "Каталог товаров" -> logicUI.sendProducts(user);
                case "Отмена добавления товара" -> {
                    SessionStore.removeState(chatId);
                    logicUI.sendMenu(user, null);
                }
                case "Отмена покупки товара" -> {
                    SessionStore.removeState(chatId);
                    logicUI.sendProducts(user);
                }
            }
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
            if ("REVIEW_REQUEST".equals(state)) {
                ReviewRequestSession session = SessionStore.getReviewSession(chatId);
                if (session != null) {
                    switch (session.getStep()) {
                        case ARTICUL_CHECK:
                            if(Objects.equals(msg, String.valueOf(session.getProduct().getArticul()))){
                                session.getRequest().setArticul(msg.trim());
                                session.setStep(ReviewRequestSession.Step.FULL_NAME);
                                createTelegramBot.sendMessage(user,
                                        "Введите, пожалуйста, ваше полное ФИО без сокращений:");
                            }else {
                                createTelegramBot.sendMessage(user,"Введен неправильный артикль, повторите попытку");
                            }
                            break;

                        case FULL_NAME:
                            session.getRequest().setFullName(msg.trim());
                            session.setStep(ReviewRequestSession.Step.PHONE_NUMBER);

                            logicUI.sendNumberPhone(user);
                            break;

                        case PHONE_NUMBER:
                            String phoneNumber;
                            if(update.getMessage().getContact() != null){
                                Contact contact = update.getMessage().getContact();
                                phoneNumber = contact.getPhoneNumber();
                            }else{
                                phoneNumber = msg;
                            }


                            session.getRequest().setPhoneNumber(phoneNumber.trim());
                            session.setStep(ReviewRequestSession.Step.CARD_NUMBER);

                            logicUI.sentBack(user, "Введите номер карты для получения кешбэка \n" +
                                    "(<strong>Т-Банк</strong> или <strong>Сбер</strong>, другие не поддерживаем):", "Отмена покупки товара");

                            break;

                        case CARD_NUMBER:
                            // можно добавить лёгкую валидацию, если нужно
                            session.getRequest().setCardNumber(msg);
                            session.setStep(ReviewRequestSession.Step.PURCHASE_AMOUNT);
                            createTelegramBot.sendMessage(user,
                                    "Укажите сумму покупки товара на Wildberries:");
                            break;

                        case PURCHASE_AMOUNT:
                            try {
                                Integer sum = Integer.parseInt(msg.trim());

                                session.getRequest().setPurchaseAmount(String.valueOf(sum));
                                session.setStep(ReviewRequestSession.Step.BANK_NAME);
                                logicUI.sendMessageBank(user,
                                        "Укажите название банка, выпустившего карту:");
                                break;
                            } catch (NumberFormatException e) {
                                createTelegramBot.sendMessage(user, "Некорректная сумма. Пожалуйста, введите число.");
                                break;
                            }


                        case BANK_NAME:
                            session.getRequest().setBankName(msg.trim());
                            session.setStep(ReviewRequestSession.Step.ORDER_SCREENSHOT);
                            logicUI.sentBack(user, "Прикрепите скриншот заказа с Wildberries с товаром:", "Отмена покупки товара");
                            break;

                        case ORDER_SCREENSHOT:
                            handleScreenshot(update,user);
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
    }

    public void handleScreenshot(Update update, User user) {
        Sent createTelegramBot = new Sent();
        long chatId = update.getMessage().getChatId();
        Message message = update.getMessage();
        TelegramBot telegramBot = new TelegramBot();
        ReviewRequestSession session = SessionStore.getReviewSession(chatId);
        if (session == null || session.getStep() != ReviewRequestSession.Step.ORDER_SCREENSHOT) {
            return;
        }
        if (message.getPhoto() == null || message.getPhoto().isEmpty()) {
            createTelegramBot.sendMessage(user, "Пожалуйста, приложите скриншот заказа картинкой.");
            return;
        }
        PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
        String fileId   = photo.getFileId();
        try {
            File reviewsDir = new File("reviews/");
            if (!reviewsDir.exists()) reviewsDir.mkdirs();
            String fileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".jpg";
            Path filePath   = Paths.get("reviews/", fileName);

            telegramBot.downloadFile(fileId, filePath.toString());
            Purchase purchase = new Purchase();
            purchase.setProduct(session.getProduct());
            purchase.setUser(user);
            purchase.setDate(LocalDate.now());
            purchase.setPurchaseStage(0);
            PurchaseDAO purchaseDAO = new PurchaseDAO();
            purchaseDAO.save(purchase);

            Photo photoEntity = new Photo();
            photoEntity.setPurchase(purchase);
            photoEntity.setUser(user);
            photoEntity.setIdPhoto(fileName);
            PhotoDAO photoDAO = new PhotoDAO();
            photoDAO.save(photoEntity);

            String finishText =
                    "Спасибо за участие!\n\n" +
                            "После получения товара (на следующий день после забора с ПВЗ):\n" +
                            "1️⃣ Перейдите в главное меню → «📝 Оставить отзыв»\n" +
                            "2️⃣ Заполните форму по инструкции\n" +
                            "3️⃣ После утверждения отзыва администратором, перейдите в раздел " +
                            "→ «💸 Получить кешбек» и отправьте скриншот вашего отзыва";

            LogicUI logicUI = new LogicUI();
            logicUI.sendMenu(user, finishText);
            SessionStore.removeReviewSession(chatId);
            String text =
                    "Пользователь купил товар \"" + session.getProduct().getProductName() + "\"\n"
                    + "ФИО: " + session.getRequest().getFullName() + "\n"
                    + "Номер телефона: <code>" + session.getRequest().getPhoneNumber() + "</code>\n"
                    + "Банк: " + session.getRequest().getBankName() + "\n"
                    + "Реквизиты: <code>" + session.getRequest().getCardNumber() + "</code>\n"
                    + "Стоимость для пользователя: <code>" + session.getRequest().getPurchaseAmount() + "</code>\n";
            createTelegramBot.sendMessageGroup(user,text, filePath.toString());
        } catch (TelegramApiException | IOException e) {
            e.printStackTrace();
            createTelegramBot.sendMessage(user, "Не удалось загрузить скриншот. Попробуйте ещё раз.");
        }
    }

    public void handlePhoto(Update update, User user) {
        Sent createTelegramBot = new Sent();
        long chatId = update.getMessage().getChatId();
        Message message = update.getMessage();
        TelegramBot telegramBot = new TelegramBot();
        ProductCreationSession session = SessionStore.getProductSession(chatId);

        if (session != null && session.getStep() == ProductCreationSession.Step.PHOTO) {
            if (message.getPhoto() == null || message.getPhoto().isEmpty()) {
                createTelegramBot.sendMessage(user, "Пожалуйста, отправьте фотографию товара, а не текст.");
                return;
            }
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
                LogicUI logicUI = new LogicUI();
                logicUI.sendMenu(user,null);
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
        SessionStore.clearAll(chatId);

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
                        logicUI.sentBack(user, "Отправьте артикуль товара:", "Отмена добавления товара");
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
                    logicUI.sendMenuAgain(user,Integer.parseInt(messageID));
                    break;
                }
                case "Exit_Product":{
                    logicUI.sendProducts(user);
                    break;
                }
                case "buy_product":{
                    ProductDAO productDAO = new ProductDAO();
                    Product product = productDAO.findById(Integer.parseInt(messageID));
                    ReviewRequestSession session = new ReviewRequestSession();
                    session.setProduct(product);
                    session.setStep(ReviewRequestSession.Step.ARTICUL_CHECK);
                    SessionStore.setReviewSession(chatId,session);

                    SessionStore.setState(chatId, "REVIEW_REQUEST");

                    logicUI.sentBack(user, "Пожалуйста, введите артикул товара Wildberries для проверки.", "Отмена покупки товара");

                    break;
                }
            }
        }
    }
}