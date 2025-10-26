package org.example.tgProcessing;

import org.example.dao.ProductDAO;
import org.example.dao.PurchaseDAO;
import org.example.dao.UserDAO;
import org.example.table.Product;
import org.example.table.Purchase;
import org.example.table.User;
import org.example.settings.AdminSettings;
import org.example.session.ReviewSubmissionSession;
import org.example.session.RedisSessionStore;
import org.example.telegramBots.TelegramBot;
import java.util.ResourceBundle;
import org.example.telegramBots.TelegramBotLogs;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.File;

public class LogicUI {

    public void sendStart(long chatId,Update update) {
        Sent sent = new Sent();
        TelegramBotLogs telegramBotLogs = new TelegramBotLogs();
        UserDAO userDAO = new UserDAO();

        User user = userDAO.findById(chatId);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Каталог товаров");
        row1.add("Оставить отзыв");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Техподдержка");
        row2.add("Получить кешбек");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Личный кабинет");
        if(user!=null && user.isAdmin()) {
            row3.add("Админ меню");
        }
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(List.of(row1,row2,row3));
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        SendMessage sendMessage = new SendMessage();

        sendMessage.setReplyMarkup(keyboardMarkup);

        if(user==null){
            user = new User();
            user.setIdUser(chatId);
            user.setAdmin(false);
            user.setBlock(false);
            user.setUserFlag(true);
            user.setUsername(update.getMessage().getFrom().getUserName());
            List<Long> messageIdAndGroup = telegramBotLogs.createTopic(update);
            user.setId_message(Math.toIntExact(messageIdAndGroup.getLast()));

            sent.sendMessageStart(user, user.getUsername() + ", Вас приветствует AdaptixBot", sendMessage);
            userDAO.save(user);
        }else{
            sent.sendMessage(user,user.getUsername() + ", с возвращением!", sendMessage);
        }
    }

    public void sendAdminMenu(User user, Integer messageId){
        Sent sent = new Sent();
        if(user.isAdmin()){
            if(messageId==null){
                SendMessage sendMessage = new SendMessage();
                Message sentMessage = sent.sendMessage(user,"Выберите действие Меню",sendMessage);
                messageId = sentMessage.getMessageId();
            }
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            List<InlineKeyboardButton> row3 = new ArrayList<>();
            
            InlineKeyboardButton btnAddAdmin = new InlineKeyboardButton();
            btnAddAdmin.setText("Добавить админа");
            btnAddAdmin.setCallbackData("addAdmin:" + messageId);
            row1.add(btnAddAdmin);

            InlineKeyboardButton btnAddProduct = new InlineKeyboardButton();
            btnAddProduct.setText("Добавить товар");
            btnAddProduct.setCallbackData("addProduct:" + messageId);
            row1.add(btnAddProduct);

            InlineKeyboardButton btnIsAdmin= new InlineKeyboardButton();
            if(user.isUserFlag()){
                btnIsAdmin.setText("Пользователь");
                btnIsAdmin.setCallbackData("isUser:" + messageId);
                row2.add(btnIsAdmin);
            }else {
                btnIsAdmin.setText("Админ");
                btnIsAdmin.setCallbackData("isUser:" + messageId);
                row2.add(btnIsAdmin);
            }
            
            // Кнопка для админ-интерфейса
            InlineKeyboardButton btnAdminInterface = new InlineKeyboardButton();
            btnAdminInterface.setText("🔧 Админ-интерфейс");
            btnAdminInterface.setCallbackData("admin_menu");
            row3.add(btnAdminInterface);
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            keyboard.add(row1);
            keyboard.add(row2);
            keyboard.add(row3);

            InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
            inlineKeyboard.setKeyboard(keyboard);

            EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
            editMarkup.setChatId(user.getIdUser());
            editMarkup.setMessageId(messageId);
            editMarkup.setReplyMarkup(inlineKeyboard);

            sent.editMessageMarkup(user,messageId,"Выберите действие: Админ меню",editMarkup);
        }
    }

    public void sendNumberPhone(User user){
        Sent createTelegramBot = new Sent();
        KeyboardButton contactButton = new KeyboardButton("Отправить номер");
        contactButton.setRequestContact(true);

        KeyboardRow row = new KeyboardRow();
        row.add(contactButton);

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);
        createTelegramBot.sendReplyKeyboardMarkup(user,keyboardMarkup,"Отлично. Теперь введите ваш номер телефона:");
    }
    public void sendMenu(User user, String text){
        System.out.println("🏠 sendMenu called for user: " + (user != null ? user.getUsername() : "null"));
        Sent sent = new Sent();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Каталог товаров");
        row1.add("Оставить отзыв");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Техподдержка");
        row2.add("Получить кешбек");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Личный кабинет");
        if(user!=null && user.isAdmin()) {
            row3.add("Админ меню");
        }

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(List.of(row1,row2,row3));
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        SendMessage sendMessage = new SendMessage();

        sendMessage.setReplyMarkup(keyboardMarkup);
        
        String menuText = "🏠 <b>Главное меню</b>";
        
        System.out.println("🏠 Sending menu text: " + (text != null ? text : menuText));
        
        if(text == null){
            sent.sendMessageStart(user, menuText, sendMessage);
        }else{
            sent.sendMessageStart(user, text, sendMessage);
        }
        
        System.out.println("🏠 Menu sent successfully");
    }
    public void sendMenuAgain(User user, Integer messageID){
        TelegramBot telegramBot = new TelegramBot();
        telegramBot.deleteMessage(user.getIdUser(),messageID);
        Sent sent = new Sent();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Каталог товаров");
        row1.add("Оставить отзыв");
        KeyboardRow row2 = new KeyboardRow();
        row2.add("Техподдержка");
        row2.add("Получить кешбек");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Личный кабинет");
        if(user!=null && user.isAdmin()) {
            row3.add("Админ меню");
        }
        
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(List.of(row1,row2,row3));
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyMarkup(keyboardMarkup);
        sent.sendMessageStart(user, "Выберите действие Меню", sendMessage);
    }
    public void sendProducts(User user){
        Sent sent = new Sent();
        SendMessage sendMessage = new SendMessage();
        ProductDAO productDAO = new ProductDAO();

        // Каталог товаров всегда показывает только видимые товары (пользовательский режим)
        List<Product> products = productDAO.findAllVisible();
        if(products.isEmpty()){
            sent.sendMessage(user,"К сожалению товаров на выкуп нет",sendMessage);
            return;
        }
        TelegramBot telegramBot = new TelegramBot();
        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
        keyboardRemove.setRemoveKeyboard(true);
        sendMessage.setReplyMarkup(keyboardRemove);

        int messageId = sent.sendMessage(user,"📦 Выберите товар:",sendMessage).getMessageId();
        telegramBot.deleteMessage(user.getIdUser(),messageId);

        SendMessage sendAgain = new SendMessage();
        messageId = sent.sendMessage(user,"📦 Выберите товар:",sendAgain).getMessageId();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for(Product product : products){
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(product.getProductName() + "  " + product.getCashbackPercentage() + "% кешбек");
            button.setCallbackData("product_:" + product.getIdProduct() + ":" + messageId);
            rows.add(List.of(button));
        }
        InlineKeyboardButton back = new InlineKeyboardButton("⬅️ Назад");
        back.setCallbackData("Exit:" + messageId);
        rows.add(List.of(back));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);

        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(user.getIdUser());
        editMarkup.setMessageId(messageId);
        editMarkup.setReplyMarkup(markup);

        sent.editMessageMarkup(user, messageId, "📦 Выберите товар:", editMarkup);
    }
    
    /**
     * Показать товары админа (все товары) для выбора
     */
    public void sendAdminProducts(User user){
        Sent sent = new Sent();
        SendMessage sendMessage = new SendMessage();
        ProductDAO productDAO = new ProductDAO();

        List<Product> products = productDAO.findAll();  // Всегда показываем все товары админа
        if(products.isEmpty()){
            sent.sendMessage(user,"К сожалению товаров на выкуп нет",sendMessage);
            return;
        }
        TelegramBot telegramBot = new TelegramBot();
        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
        keyboardRemove.setRemoveKeyboard(true);
        sendMessage.setReplyMarkup(keyboardRemove);

        int messageId = sent.sendMessage(user,"📦 Выберите товар:",sendMessage).getMessageId();
        telegramBot.deleteMessage(user.getIdUser(),messageId);

        SendMessage sendAgain = new SendMessage();
        messageId = sent.sendMessage(user,"📦 Выберите товар:",sendAgain).getMessageId();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for(Product product : products){
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(product.getProductName() + "  " + product.getCashbackPercentage() + "% кешбек");
            button.setCallbackData("product_:" + product.getIdProduct() + ":" + messageId);
            rows.add(List.of(button));
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);

        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(user.getIdUser());
        editMarkup.setMessageId(messageId);
        editMarkup.setReplyMarkup(markup);

        sent.editMessageMarkup(user, messageId, "📦 Выберите товар:", editMarkup);
    }
    
    /**
     * Показать покупки пользователя для получения кешбека
     */
    public void showUserPurchases(User user) {
        Sent sent = new Sent();
        SendMessage sendMessage = new SendMessage();
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        
        // Получаем покупки пользователя, где отзыв уже оставлен (этап 2)
        List<Purchase> purchases = purchaseDAO.findByUserId(user.getIdUser());
        
        // Отладочная информация
        System.out.println("🔍 Debug: Total purchases for user " + user.getIdUser() + ": " + purchases.size());
        for (Purchase purchase : purchases) {
            System.out.println("🔍 Debug: Purchase ID " + purchase.getIdPurchase() + ", Stage: " + purchase.getPurchaseStage() + ", Product: " + purchase.getProduct().getProductName());
        }
        
        // Фильтруем только те покупки, где отзыв уже оставлен, но кешбек еще не выплачен (purchaseStage >= 2 и < 4)
        List<Purchase> eligiblePurchases = purchases.stream()
                .filter(purchase -> {
                    int stage = purchase.getPurchaseStage();
                    boolean eligible = stage >= 2 && stage < 4;
                    System.out.println("🔍 Debug: Purchase " + purchase.getIdPurchase() + " stage " + stage + " eligible: " + eligible);
                    return eligible;
                })
                .collect(java.util.stream.Collectors.toList());
        
        System.out.println("🔍 Debug: Eligible purchases for cashback: " + eligiblePurchases.size());
        
        if (eligiblePurchases.isEmpty()) {
            sent.sendMessage(user, "💸 У вас нет покупок, готовых для получения кешбека.\n\n" +
                    "Для получения кешбека необходимо:\n" +
                    "1️⃣ Заказать товар через «Каталог товаров»\n" +
                    "2️⃣ Оставить отзыв через «Оставить отзыв»\n" +
                    "3️⃣ Дождаться одобрения отзыва администратором\n\n" +
                    "После этого здесь появится возможность получить кешбек!\n\n" +
                    "💡 Если кешбек уже выплачен, он не будет отображаться в этом списке.", sendMessage);
            return;
        }
        
        // Создаем клавиатуру с кнопкой "Назад"
        KeyboardRow backRow = new KeyboardRow();
        backRow.add("⬅️ Назад");
        
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(List.of(backRow));
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        sendMessage.setReplyMarkup(keyboardMarkup);
        
        // Отправляем первое сообщение с заголовком и кнопкой "Назад"
        sent.sendMessage(user, "💸 <b>Раздел кешбека</b>", sendMessage);
        
        // Создаем второе сообщение с inline кнопками покупок
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        for (Purchase purchase : eligiblePurchases) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            String stageText = getPurchaseStageText(purchase.getPurchaseStage());
            button.setText(purchase.getProduct().getProductName() + " - " + stageText);
            button.setCallbackData("cashback_purchase:" + purchase.getIdPurchase());
            rows.add(List.of(button));
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        
        SendMessage inlineMessage = new SendMessage();
        inlineMessage.setReplyMarkup(markup);
        
        // Отправляем второе сообщение с inline кнопками
        sent.sendMessage(user, "Выберите покупку для получения кешбека:", inlineMessage);
    }
    
    /**
     * Показать личный кабинет пользователя
     */
    public void showUserCabinet(User user) {
        Sent sent = new Sent();
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        
        // Получаем все покупки пользователя
        List<Purchase> purchases = purchaseDAO.findByUserId(user.getIdUser());
        
        if (purchases.isEmpty()) {
            sent.sendMessage(user, "👤 Личный кабинет\n\n" +
                    "📦 У вас пока нет покупок.\n" +
                    "Перейдите в «Каталог товаров» чтобы сделать первую покупку!");
            return;
        }
        
        // Формируем информацию о покупках
        StringBuilder cabinetInfo = new StringBuilder();
        cabinetInfo.append("👤 Личный кабинет\n\n");
        
        for (Purchase purchase : purchases) {
            cabinetInfo.append("🗓️ Дата заказа: ")
                    .append(formatDateTime(purchase.getDate(), purchase.getOrderTime()))
                    .append("\n");
            
            cabinetInfo.append("📦 Товар: ")
                    .append(purchase.getProduct().getProductName())
                    .append("\n");
            
            cabinetInfo.append("🔄 Статус отзыва: ")
                    .append(getReviewStatusText(purchase.getPurchaseStage()))
                    .append("\n");
            
            cabinetInfo.append("📸 Статус скриншота отзыва: ")
                    .append(getScreenshotStatusText(purchase.getPurchaseStage()))
                    .append("\n");
            
            cabinetInfo.append("💰 Статус выплаты: ")
                    .append(getPaymentStatusText(purchase.getPurchaseStage()))
                    .append("\n\n");
        }
        
        sent.sendMessage(user, cabinetInfo.toString());
    }
    
    /**
     * Показать товары пользователя для выбора отзыва
     */
    public void showUserProductsForReview(User user) {
        Sent sent = new Sent();
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        
        // Получаем все покупки пользователя
        List<Purchase> purchases = purchaseDAO.findByUserId(user.getIdUser());
        
        // Фильтруем покупки, где товар заказан и отзыв еще не оставлен
        List<Purchase> eligiblePurchases = purchases.stream()
                .filter(purchase -> purchase.getPurchaseStage() >= 0 && purchase.getPurchaseStage() < 2)
                .collect(java.util.stream.Collectors.toList());
        
        if (eligiblePurchases.isEmpty()) {
            sent.sendMessage(user, "📝 Оставить отзыв\n\n" +
                    "❌ У вас нет товаров готовых для отзыва.\n\n" +
                    "Для оставления отзыва необходимо:\n" +
                    "1️⃣ Заказать товар через «Каталог товаров»\n" +
                    "2️⃣ Дождаться подтверждения заказа\n\n" +
                    "После этого здесь появится возможность оставить отзыв!");
            return;
        }
        
        // Если есть только один товар, сразу переходим к вводу текста отзыва
        if (eligiblePurchases.size() == 1) {
            Purchase purchase = eligiblePurchases.get(0);
            
            // Создаем клавиатуру с кнопкой "Назад"
            KeyboardRow backRow = new KeyboardRow();
            backRow.add("⬅️ Назад");
            
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setKeyboard(List.of(backRow));
            keyboardMarkup.setResizeKeyboard(true);
            keyboardMarkup.setOneTimeKeyboard(false);
            
            SendMessage sendMessage = new SendMessage();
            sendMessage.setReplyMarkup(keyboardMarkup);
            
            // Отправляем сообщение о том, что пользователь оставляет отзыв на конкретный товар
            String message = "📝 Вы оставляли заявку на товар: \"" + purchase.getProduct().getProductName() + "\"\n\n" +
                    "Пожалуйста, напишите текст вашего отзыва о товаре 🖊";
            
            sent.sendMessage(user, message, sendMessage);
            
            // Создаем сессию подачи отзыва и устанавливаем состояние
            ReviewSubmissionSession session = new ReviewSubmissionSession(purchase);
            session.setStep(ReviewSubmissionSession.Step.TEXT); // Устанавливаем шаг для ввода текста
            RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
            RedisSessionStore.setState(user.getIdUser(), "REVIEW_SUBMISSION_TEXT"); // Специальное состояние для одного товара
            
            return;
        }
        
        // Если товаров больше одного, показываем список для выбора
        // Создаем клавиатуру с кнопкой "Назад"
        KeyboardRow backRow = new KeyboardRow();
        backRow.add("⬅️ Назад");
        
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(List.of(backRow));
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyMarkup(keyboardMarkup);
        
        // Отправляем первое сообщение с заголовком и кнопкой "Назад"
        sent.sendMessage(user, "📝 <b>Раздел отзывов</b>", sendMessage);
        
        // Создаем второе сообщение с inline кнопками товаров
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        for (Purchase purchase : eligiblePurchases) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(purchase.getProduct().getProductName());
            button.setCallbackData("review_product:" + purchase.getIdPurchase());
            rows.add(List.of(button));
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        
        SendMessage inlineMessage = new SendMessage();
        inlineMessage.setReplyMarkup(markup);
        
        // Отправляем второе сообщение с inline кнопками
        sent.sendMessage(user, "Выберите товар для оставления отзыва:", inlineMessage);
    }
    
    /**
     * Форматировать дату и время
     */
    private String formatDateTime(java.time.LocalDate date, java.time.LocalTime time) {
        if (date == null) {
            return "Не указано";
        }
        
        String dateStr = String.format("%02d.%02d.%02d", 
                date.getDayOfMonth(), date.getMonthValue(), date.getYear() % 100);
        
        if (time != null) {
            String timeStr = String.format("%02d:%02d:%02d", 
                    time.getHour(), time.getMinute(), time.getSecond());
            return dateStr + " " + timeStr;
        }
        
        return dateStr;
    }
    
    /**
     * Получить текст статуса отзыва
     */
    private String getReviewStatusText(int purchaseStage) {
        switch (purchaseStage) {
            case 0: return "Отзыв не оставлен";
            case 1: return "Отзыв оставлен";
            case 2: return "Отзыв утвержден";
            case 3: return "Отзыв утвержден";
            default: return "Неизвестный статус";
        }
    }
    
    /**
     * Получить текст статуса скриншота отзыва
     */
    private String getScreenshotStatusText(int purchaseStage) {
        switch (purchaseStage) {
            case 0: return "Не требуется";
            case 1: return "Не отправлен";
            case 2: return "Отправлен";
            case 3: return "Отправлен";
            default: return "Неизвестный статус";
        }
    }
    
    /**
     * Получить текст статуса выплаты
     */
    private String getPaymentStatusText(int purchaseStage) {
        switch (purchaseStage) {
            case 0: return "Не требуется";
            case 1: return "Ожидание отзыва";
            case 2: return "Ожидание выплаты";
            case 3: return "Выплачено";
            default: return "Неизвестный статус";
        }
    }
    
    public void sendMessageBank(User user, String text){
        Sent sent = new Sent();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Сбер");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(List.of(row1));
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        SendMessage sendMessage = new SendMessage();

        sendMessage.setReplyMarkup(keyboardMarkup);
        sent.sendMessageStart(user, text, sendMessage);
    }
    public void sentOneProduct(User user, Product selected, int messageId){
        Sent sent = new Sent();
        String textProduct =
                "Вы выбрали товар: "+ selected.getProductName() + " \n" +
                        "\n" +
                        "Кешбек " + selected.getCashbackPercentage() +  "% после публикации отзыва \uD83D\uDE4F\n" +
                        "Принимаем только карты Сбера (Россия)\n" +
                        "\n" +
                        "Условия участия:\n" +
                        "- Подпишитесь на наш канал @adaptix_focus \uD83D\uDE09\n" +
                        "- Включите запись экрана (мы её можем запросить)\n" +
                        "- Найдите наш товар по запросу \""+ selected.getKeyQuery() +"\" \uD83D\uDD0E\n" +
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
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton back = new InlineKeyboardButton("⬅️ Назад");
        back.setCallbackData("Exit_Product:" + messageId);
        keyboard.add(List.of(back));

        if(false){ // Каталог товаров всегда показывает пользовательский интерфейс
            InlineKeyboardButton name = new InlineKeyboardButton("Наименование");
            name.setCallbackData("update_tariffs_name_" + selected.getIdProduct());

            InlineKeyboardButton description = new InlineKeyboardButton("Описание");
            description.setCallbackData("update_tariffs_description_" + selected.getIdProduct());

            InlineKeyboardButton price = new InlineKeyboardButton("Стоимость");
            price.setCallbackData("update_tariffs_price_" + selected.getIdProduct());

            InlineKeyboardButton term = new InlineKeyboardButton("Продолжительность");
            term.setCallbackData("update_tariffs_term_" + selected.getIdProduct());

            InlineKeyboardButton discount = new InlineKeyboardButton("Скидка");
            discount.setCallbackData("update_tariffs_discount_" + selected.getIdProduct());

            InlineKeyboardButton visible;
            if(selected.isVisible()){
                visible = new InlineKeyboardButton("Видим ✅");
            }else{
                visible = new InlineKeyboardButton("Невидим ️⚠️");
            }
            visible.setCallbackData("changeVisible_" + selected.getIdProduct());
            keyboard.add(Arrays.asList(name,description,price,term,discount,visible));
        } else {
            // Каталог товаров всегда показывает пользовательский интерфейс
            InlineKeyboardButton buy = new InlineKeyboardButton("✅ Купить");
            buy.setCallbackData("buy_product:" + selected.getIdProduct());
            keyboard.add(List.of(buy));
        }

        InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();
        inlineMarkup.setKeyboard(keyboard);

        TelegramBot telegramBot = new TelegramBot();
        telegramBot.deleteMessage(user.getIdUser(),messageId);

        // Проверяем существование файла фотографии
        File photoFile = new File(selected.getPhoto());
        if (!photoFile.exists()) {
            System.err.println("❌ Product photo file does not exist: " + selected.getPhoto());
            // Отправляем только текст с кнопками без фотографии
            SendMessage textMessage = new SendMessage();
            textMessage.setChatId(String.valueOf(user.getIdUser()));
            textMessage.setText(textProduct);
            textMessage.setReplyMarkup(inlineMarkup);
            textMessage.setParseMode("HTML");
            
            sent.sendMessageWithMarkup(user, textMessage);
            return;
        }

        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(String.valueOf(user.getIdUser()));
        sendPhoto.setReplyMarkup(inlineMarkup);

        sent.sendPhotoWithButton(user.getIdUser(), selected.getPhoto(), textProduct, sendPhoto);
    }

    public void sentBack(User user, String text, String buttonText){
        Sent sent = new Sent();
        KeyboardRow row1 = new KeyboardRow();
        row1.add(buttonText);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(List.of(row1));
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        SendMessage sendMessage = new SendMessage();

        sendMessage.setReplyMarkup(keyboardMarkup);

        sent.sendMessage(user,text, sendMessage);
    }

    /**
     * Обновить обычное меню (edit message)
     */
    public void updateMenu(User user, int messageId, String text) {
        Sent sent = new Sent();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Каталог товаров");
        row1.add("Оставить отзыв");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Техподдержка");
        row2.add("Получить кешбек");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Личный кабинет");
        if(user!=null && user.isAdmin()) {
            row3.add("Админ меню");
        }

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(List.of(row1,row2,row3));
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        
        String messageText = text != null ? text : "Выберите действие Меню";
        sent.editMessageMarkup(user, messageId, messageText, null);
    }

    // ==================== АДМИН ФУНКЦИИ ====================

    /**
     * Показать главное меню админа
     */
    public void showAdminMenu(User admin) {
        System.out.println("🔧 Showing admin menu for user: " + admin.getUsername());
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Кнопка "Просмотр товаров"
        InlineKeyboardButton productsButton = new InlineKeyboardButton();
        productsButton.setText("📦 Просмотр товаров");
        productsButton.setCallbackData("admin_products");
        
        // Кнопка "Добавить товар"
        InlineKeyboardButton addProductButton = new InlineKeyboardButton();
        addProductButton.setText("➕ Добавить товар");
        addProductButton.setCallbackData("admin_add_product");
        
        // Кнопка "Статистика"
        InlineKeyboardButton statsButton = new InlineKeyboardButton();
        statsButton.setText("📊 Статистика");
        statsButton.setCallbackData("admin_stats");
        
        // Кнопка "Управление пользователями"
        InlineKeyboardButton userManagementButton = new InlineKeyboardButton();
        userManagementButton.setText("👥 Админы");
        userManagementButton.setCallbackData("admin_user_management");
        
        // Кнопка "Настройки"
        InlineKeyboardButton settingsButton = new InlineKeyboardButton();
        settingsButton.setText("⚙️ Настройки");
        settingsButton.setCallbackData("admin_settings");
        
        // Кнопка "Назад в обычное меню"
        InlineKeyboardButton backToMenuButton = new InlineKeyboardButton();
        backToMenuButton.setText("🏠 Назад в меню");
        backToMenuButton.setCallbackData("admin_back_to_main_menu");
        
        rows.add(List.of(productsButton));
        rows.add(List.of(addProductButton));
        rows.add(List.of(statsButton));
        rows.add(List.of(userManagementButton));
        rows.add(List.of(settingsButton));
        rows.add(List.of(backToMenuButton));
        
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setReplyMarkup(keyboard);
        
        Sent sent = new Sent();
        System.out.println("📤 Sending admin menu message");
        sent.sendMessage(admin, "🔧 Панель администратора", message);
    }

    /**
     * Обновить главное меню админа (edit message)
     */
    public void updateAdminMenu(User admin, int messageId) {
        System.out.println("🔧 Updating admin menu for user: " + admin.getUsername());
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Кнопка "Просмотр товаров"
        InlineKeyboardButton productsButton = new InlineKeyboardButton();
        productsButton.setText("📦 Просмотр товаров");
        productsButton.setCallbackData("admin_products");
        
        // Кнопка "Добавить товар"
        InlineKeyboardButton addProductButton = new InlineKeyboardButton();
        addProductButton.setText("➕ Добавить товар");
        addProductButton.setCallbackData("admin_add_product");
        
        // Кнопка "Статистика"
        InlineKeyboardButton statsButton = new InlineKeyboardButton();
        statsButton.setText("📊 Статистика");
        statsButton.setCallbackData("admin_stats");
        
        // Кнопка "Управление пользователями"
        InlineKeyboardButton userManagementButton = new InlineKeyboardButton();
        userManagementButton.setText("👥 Админы");
        userManagementButton.setCallbackData("admin_user_management");
        
        // Кнопка "Настройки"
        InlineKeyboardButton settingsButton = new InlineKeyboardButton();
        settingsButton.setText("⚙️ Настройки");
        settingsButton.setCallbackData("admin_settings");
        
        // Кнопка "Назад в обычное меню"
        InlineKeyboardButton backToMenuButton = new InlineKeyboardButton();
        backToMenuButton.setText("🏠 Назад в меню");
        backToMenuButton.setCallbackData("admin_back_to_main_menu");
        
        rows.add(List.of(productsButton));
        rows.add(List.of(addProductButton));
        rows.add(List.of(statsButton));
        rows.add(List.of(userManagementButton));
        rows.add(List.of(settingsButton));
        rows.add(List.of(backToMenuButton));
        
        keyboard.setKeyboard(rows);
        
        Sent sent = new Sent();
        System.out.println("📤 Updating admin menu message");
        sent.editMessageMarkup(admin, messageId, "🔧 Панель администратора", null);
    }

    /**
     * Показать список товаров для админа
     */
    public void showProductsList(User admin) {
        showProductsListWithEditButtons(admin);
    }
    
    /**
     * Показать список товаров с кнопками редактирования
     */
    public void showProductsListWithEditButtons(User admin) {
        ProductDAO productDAO = new ProductDAO();
        List<Product> products = productDAO.findAll();
        
        System.out.println("🔍 Found " + products.size() + " products in database");
        
        if (products.isEmpty()) {
            // Показываем сообщение с кнопкой "Назад" даже если товаров нет
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
            
            // Кнопка "Назад"
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("⬅️ Назад в админ-меню");
            backButton.setCallbackData("admin_back_to_menu");
            
            rows.add(List.of(backButton));
            keyboard.setKeyboard(rows);
            
            SendMessage message = new SendMessage();
            message.setReplyMarkup(keyboard);
            
            Sent sent = new Sent();
            sent.sendMessage(admin, "📦 Список товаров пуст", message);
            return;
        }
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        for (Product product : products) {
            // Кнопка товара с индикатором видимости
            InlineKeyboardButton productButton = new InlineKeyboardButton();
            String visibilityIcon = product.isVisible() ? "👁️" : "🙈";
            productButton.setText(visibilityIcon + " " + product.getProductName() + " (ID: " + product.getIdProduct() + ")");
            productButton.setCallbackData("admin_product_" + product.getIdProduct());
            
            // Кнопка редактирования товара (меньше)
            InlineKeyboardButton editButton = new InlineKeyboardButton();
            editButton.setText("✏️");
            editButton.setCallbackData("admin_edit_product_" + product.getIdProduct());
            
            // Размещаем кнопки в одной строке
            rows.add(List.of(productButton, editButton));
        }
        
        // Кнопка "Назад в админ-меню"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад в админ-меню");
        backButton.setCallbackData("admin_back_to_menu");
        rows.add(List.of(backButton));
        
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setReplyMarkup(keyboard);
        
        Sent sent = new Sent();
        sent.sendMessage(admin, "📦 Выберите товар для просмотра покупок или редактирования:", message);
    }
    
    /**
     * Показать список товаров с возможностью редактирования
     */
    public void showProductsListWithEdit(User admin) {
        ProductDAO productDAO = new ProductDAO();
        List<Product> products = productDAO.findAll();
        
        System.out.println("🔍 Found " + products.size() + " products in database");
        
        if (products.isEmpty()) {
            // Показываем сообщение с кнопкой "Назад" даже если товаров нет
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
            
            // Кнопка "Назад"
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("⬅️ Назад в админ-меню");
            backButton.setCallbackData("admin_back_to_menu");
            
            rows.add(List.of(backButton));
            keyboard.setKeyboard(rows);
            
            SendMessage message = new SendMessage();
            message.setReplyMarkup(keyboard);
            
            Sent sent = new Sent();
            sent.sendMessage(admin, "📦 Список товаров пуст", message);
            return;
        }
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        for (Product product : products) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(product.getProductName() + " (ID: " + product.getIdProduct() + ")");
            button.setCallbackData("admin_product_" + product.getIdProduct());
            
            rows.add(List.of(button));
        }
        
        // Кнопка "Назад в админ-меню"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад в админ-меню");
        backButton.setCallbackData("admin_back_to_menu");
        rows.add(List.of(backButton));
        
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setReplyMarkup(keyboard);
        
        Sent sent = new Sent();
        sent.sendMessage(admin, "📦 Выберите товар для просмотра покупок:", message);
    }

    /**
     * Обновить список товаров (edit message)
     */
    public void updateProductsList(User admin, int messageId) {
        ProductDAO productDAO = new ProductDAO();
        List<Product> products = productDAO.findAll();
        
        System.out.println("🔍 Found " + products.size() + " products in database (update)");
        
        if (products.isEmpty()) {
            Sent sent = new Sent();
            sent.editMessageMarkup(admin, messageId, "📦 Список товаров пуст", null);
            return;
        }
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        for (Product product : products) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(product.getProductName() + " (ID: " + product.getIdProduct() + ")");
            button.setCallbackData("admin_product_" + product.getIdProduct());
            
            rows.add(List.of(button));
        }
        
        // Кнопка "Назад в админ-меню"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад в админ-меню");
        backButton.setCallbackData("admin_back_to_menu");
        rows.add(List.of(backButton));
        
        keyboard.setKeyboard(rows);
        
        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(admin.getIdUser());
        editMarkup.setMessageId(messageId);
        editMarkup.setReplyMarkup(keyboard);
        
        Sent sent = new Sent();
        sent.editMessageMarkup(admin, messageId, "📦 Выберите товар для просмотра покупок:", editMarkup);
    }

    /**
     * Показать список пользователей, купивших товар
     */
    public void showProductPurchases(User admin, int productId) {
        ProductDAO productDAO = new ProductDAO();
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        
        Product product = productDAO.findById(productId);
        if (product == null) {
            // Показываем сообщение с кнопкой "Назад" если товар не найден
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
            
            // Кнопка "Назад"
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("⬅️ Назад к товарам");
            backButton.setCallbackData("admin_back_to_products");
            
            rows.add(List.of(backButton));
            keyboard.setKeyboard(rows);
            
            SendMessage message = new SendMessage();
            message.setReplyMarkup(keyboard);
            
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Товар не найден", message);
            return;
        }
        
        List<Purchase> purchases = purchaseDAO.findByProductId(productId);
        
        if (purchases.isEmpty()) {
            // Показываем сообщение с кнопкой "Назад" даже если покупок нет
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
            
            // Кнопка "Назад"
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("⬅️ Назад к товарам");
            backButton.setCallbackData("admin_back_to_products");
            
            rows.add(List.of(backButton));
            keyboard.setKeyboard(rows);
            
            SendMessage message = new SendMessage();
            message.setReplyMarkup(keyboard);
            
            Sent sent = new Sent();
            sent.sendMessage(admin, "📦 Покупок по товару \"" + product.getProductName() + "\" не найдено", message);
            return;
        }
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        for (Purchase purchase : purchases) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(purchase.getUser().getUsername() + " - " + getPurchaseStageText(purchase.getPurchaseStage()));
            button.setCallbackData("admin_user_" + purchase.getIdPurchase());
            
            rows.add(List.of(button));
        }
        
        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад к товарам");
        backButton.setCallbackData("admin_back_to_products");
        
        rows.add(List.of(backButton));
        
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setReplyMarkup(keyboard);
        
        Sent sent = new Sent();
        sent.sendMessage(admin, "🛒 Покупатели товара \"" + product.getProductName() + "\":", message);
    }

    /**
     * Показать детали покупки
     */
    public void showPurchaseDetails(User admin, int purchaseId) {
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        Purchase purchase = purchaseDAO.findById(purchaseId);
        
        if (purchase == null) {
            // Показываем сообщение с кнопкой "Назад" если покупка не найдена
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
            
            // Кнопка "Назад"
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("⬅️ Назад к товарам");
            backButton.setCallbackData("admin_back_to_products");
            
            rows.add(List.of(backButton));
            keyboard.setKeyboard(rows);
            
            SendMessage message = new SendMessage();
            message.setReplyMarkup(keyboard);
            
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Покупка не найдена", message);
            return;
        }
        
        String timeText = "";
        if (purchase.getOrderTime() != null) {
            timeText = "\n🕐 Время заказа: " + purchase.getOrderTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
        
        // Добавляем информацию об отзыве если он оставлен
        String reviewInfo = "";
        if (purchase.getPurchaseStage() >= 2) {
            reviewInfo = "\n⭐ Отзыв: Оставлен";
            if (purchase.getReviewMessageId() != null) {
                reviewInfo += " ✅";
            }
        }
        
        String text = "🛒 Детали покупки:\n\n" +
                     "👤 Пользователь: @" + purchase.getUser().getUsername() + "\n" +
                     "📦 Товар: " + purchase.getProduct().getProductName() + "\n" +
                     "📅 Дата: " + purchase.getDate() + timeText + "\n" +
                     "📊 Статус: " + getPurchaseStageText(purchase.getPurchaseStage()) + reviewInfo + "\n\n" +
                     "📋 Этапы покупки:";
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Кнопки для переходов к сообщениям этапов (показываем все выполненные этапы)
        System.out.println("🔍 Debug: PurchaseStage = " + purchase.getPurchaseStage());
        System.out.println("🔍 Debug: OrderMessageId = " + purchase.getOrderMessageId());
        System.out.println("🔍 Debug: ReviewMessageId = " + purchase.getReviewMessageId());
        System.out.println("🔍 Debug: CashbackMessageId = " + purchase.getCashbackMessageId());
        
        // Этап 1: Товар заказан (всегда показываем как выполненный)
        if (purchase.getOrderMessageId() != null) {
            try {
                ResourceBundle rb = ResourceBundle.getBundle("app");
                String groupIdStr = rb.getString("tg.group");
                
                // Убираем префикс "-100" если он есть
                String cleanGroupId = groupIdStr;
                if (groupIdStr.startsWith("-100")) {
                    cleanGroupId = groupIdStr.substring(4);
                } else if (groupIdStr.startsWith("100")) {
                    cleanGroupId = groupIdStr.substring(3);
                }
                
                // Формируем ссылку на сообщение в подгруппе пользователя
                String orderUrl = "https://t.me/c/" + cleanGroupId + "/" + purchase.getOrderMessageId();
                
                InlineKeyboardButton orderButton = new InlineKeyboardButton();
                orderButton.setText("1️⃣ Товар заказан ✅");
                orderButton.setUrl(orderUrl);
                rows.add(List.of(orderButton));
                
                System.out.println("🔗 Created order link: " + orderUrl + " for purchase ID: " + purchase.getIdPurchase());
            } catch (Exception e) {
                System.err.println("Ошибка при создании ссылки на заказ: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Показываем этап без ссылки если нет messageId
            InlineKeyboardButton orderButton = new InlineKeyboardButton();
            orderButton.setText("1️⃣ Товар заказан ✅");
            orderButton.setCallbackData("no_message_available");
            rows.add(List.of(orderButton));
            
            System.out.println("⚠️ No orderMessageId for purchase ID: " + purchase.getIdPurchase());
        }
        
        // Этап 2: Оставить отзыв (показываем как выполненный только если отзыв действительно оставлен)
        if (purchase.getPurchaseStage() >= 2) {
            if (purchase.getReviewMessageId() != null) {
                try {
                    ResourceBundle rb = ResourceBundle.getBundle("app");
                    String groupIdStr = rb.getString("tg.group");
                    
                    // Убираем префикс "-100" если он есть
                    String cleanGroupId = groupIdStr;
                    if (groupIdStr.startsWith("-100")) {
                        cleanGroupId = groupIdStr.substring(4);
                    } else if (groupIdStr.startsWith("100")) {
                        cleanGroupId = groupIdStr.substring(3);
                    }
                    
                    String reviewUrl = "https://t.me/c/" + cleanGroupId + "/" + purchase.getReviewMessageId();
                    
                    InlineKeyboardButton reviewButton = new InlineKeyboardButton();
                    reviewButton.setText("2️⃣ Оставить отзыв ✅");
                    reviewButton.setUrl(reviewUrl);
                    rows.add(List.of(reviewButton));
                } catch (Exception e) {
                    System.err.println("Ошибка при создании ссылки на отзыв: " + e.getMessage());
                }
            } else {
                // Показываем этап без ссылки если нет messageId
                InlineKeyboardButton reviewButton = new InlineKeyboardButton();
                reviewButton.setText("2️⃣ Оставить отзыв ✅");
                reviewButton.setCallbackData("no_message_available");
                rows.add(List.of(reviewButton));
            }
        }
        
        // Этап 3: Получить кешбек (показываем как выполненный только если кешбек действительно запрошен)
        if (purchase.getPurchaseStage() >= 3) {
            if (purchase.getCashbackMessageId() != null) {
                try {
                    ResourceBundle rb = ResourceBundle.getBundle("app");
                    String groupIdStr = rb.getString("tg.group");
                    
                    // Убираем префикс "-100" если он есть
                    String cleanGroupId = groupIdStr;
                    if (groupIdStr.startsWith("-100")) {
                        cleanGroupId = groupIdStr.substring(4);
                    } else if (groupIdStr.startsWith("100")) {
                        cleanGroupId = groupIdStr.substring(3);
                    }
                    
                    String cashbackUrl = "https://t.me/c/" + cleanGroupId + "/" + purchase.getCashbackMessageId();
                    
                    InlineKeyboardButton cashbackButton = new InlineKeyboardButton();
                    cashbackButton.setText("3️⃣ Получить кешбек ✅");
                    cashbackButton.setUrl(cashbackUrl);
                    rows.add(List.of(cashbackButton));
                } catch (Exception e) {
                    System.err.println("Ошибка при создании ссылки на кешбек: " + e.getMessage());
                }
            } else {
                // Показываем этап без ссылки если нет messageId
                InlineKeyboardButton cashbackButton = new InlineKeyboardButton();
                cashbackButton.setText("3️⃣ Получить кешбек ✅");
                cashbackButton.setCallbackData("no_message_available");
                rows.add(List.of(cashbackButton));
            }
        }
        
        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад к покупателям");
        backButton.setCallbackData("admin_back_to_purchases_" + purchase.getProduct().getIdProduct());
        rows.add(List.of(backButton));
        
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setReplyMarkup(keyboard);
        
        Sent sent = new Sent();
        sent.sendMessage(admin, text, message);
    }

    /**
     * Показать статистику
     */
    public void showStats(User admin) {
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        ProductDAO productDAO = new ProductDAO();
        List<Purchase> allPurchases = purchaseDAO.findAll();
        List<Product> allProducts = productDAO.findAll();
        
        int totalPurchases = allPurchases.size();
        int totalProducts = allProducts.size();
        
        String text = "📊 Статистика системы:\n\n" +
                     "📦 Всего товаров: " + totalProducts + "\n" +
                     "🛒 Всего покупок: " + totalPurchases + "\n" +
                     "👥 Уникальных покупателей: " + 
                     allPurchases.stream().map(Purchase::getUser).distinct().count();
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад в админ-меню");
        backButton.setCallbackData("admin_back_to_menu");
        
        rows.add(List.of(backButton));
        
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setReplyMarkup(keyboard);
        
        Sent sent = new Sent();
        sent.sendMessage(admin, text, message);
    }

    /**
     * Обновить статистику (edit message)
     */
    public void updateStats(User admin, int messageId) {
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        ProductDAO productDAO = new ProductDAO();
        List<Purchase> allPurchases = purchaseDAO.findAll();
        List<Product> allProducts = productDAO.findAll();
        
        int totalPurchases = allPurchases.size();
        int totalProducts = allProducts.size();
        
        String text = "📊 Статистика системы:\n\n" +
                     "📦 Всего товаров: " + totalProducts + "\n" +
                     "🛒 Всего покупок: " + totalPurchases + "\n" +
                     "👥 Уникальных покупателей: " + 
                     allPurchases.stream().map(Purchase::getUser).distinct().count();
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад в админ-меню");
        backButton.setCallbackData("admin_back_to_menu");
        
        rows.add(List.of(backButton));
        
        keyboard.setKeyboard(rows);
        
        Sent sent = new Sent();
        sent.editMessageMarkup(admin, messageId, text, null);
    }

    /**
     * Показать настройки админа
     */
    public void showSettings(User admin) {
        AdminSettings settings = AdminSettings.getInstance();
        String supportMention = settings.getSupportMention();
        
        String text = "⚙️ Настройки администратора:\n\n" +
                     "🆘 Техподдержка: " + supportMention + "\n\n" +
                     "Выберите настройку для изменения:";
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Кнопка "Изменить техподдержку"
        InlineKeyboardButton supportButton = new InlineKeyboardButton();
        supportButton.setText("🆘 Изменить техподдержку");
        supportButton.setCallbackData("admin_change_support");
        
        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад в админ-меню");
        backButton.setCallbackData("admin_back_to_menu");
        
        rows.add(List.of(supportButton));
        rows.add(List.of(backButton));
        
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setReplyMarkup(keyboard);
        
        Sent sent = new Sent();
        sent.sendMessage(admin, text, message);
    }

    /**
     * Обновить настройки админа (edit message)
     */
    public void updateSettings(User admin, int messageId) {
        AdminSettings settings = AdminSettings.getInstance();
        String supportMention = settings.getSupportMention();
        
        String text = "⚙️ Настройки администратора:\n\n" +
                     "🆘 Техподдержка: " + supportMention + "\n\n" +
                     "Выберите настройку для изменения:";
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Кнопка "Изменить техподдержку"
        InlineKeyboardButton supportButton = new InlineKeyboardButton();
        supportButton.setText("🆘 Изменить техподдержку");
        supportButton.setCallbackData("admin_change_support");
        
        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад в админ-меню");
        backButton.setCallbackData("admin_back_to_menu");
        
        rows.add(List.of(supportButton));
        rows.add(List.of(backButton));
        
        keyboard.setKeyboard(rows);
        
        Sent sent = new Sent();
        sent.editMessageMarkup(admin, messageId, text, null);
    }

    /**
     * Получить текст статуса покупки
     */
    private String getPurchaseStageText(int stage) {
        return switch (stage) {
            case 0: yield "🛒 Заказан";
            case 1: yield "📦 В пути";
            case 2: yield "🏠 Доставлен";
            case 3: yield "🏠 Доставлена";
            default: yield "❓ Неизвестно";
        };
    }

    /**
     * Показать меню добавления товара
     */
    public void showAddProductMenu(User admin) {
        String text = "➕ Добавление нового товара:\n\n" +
                     "Введите артикул товара:";
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад в админ-меню");
        backButton.setCallbackData("admin_back_to_menu");
        
        rows.add(List.of(backButton));
        
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setReplyMarkup(keyboard);
        
        Sent sent = new Sent();
        sent.sendMessage(admin, text, message);
    }

    /**
     * Показать меню управления пользователями
     */
    public void showUserManagementMenu(User admin) {
        // Получаем список всех админов
        UserDAO userDAO = new UserDAO();
        List<User> admins = userDAO.findAllAdmins();
        
        String text = "👥 Управление админами:\n\n";
        
        if (admins.isEmpty()) {
            text += "❌ Админы не найдены";
        } else {
            text += "👑 Список админов:\n";
            for (int i = 0; i < admins.size(); i++) {
                User adminUser = admins.get(i);
                String username = adminUser.getUsername() != null ? "@" + adminUser.getUsername() : "Без username";
                text += (i + 1) + ". " + username + "\n";
            }
        }
        
        text += "\nВыберите действие:";
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Кнопка "Добавить админа"
        InlineKeyboardButton addAdminButton = new InlineKeyboardButton();
        addAdminButton.setText("➕ Добавить админа");
        addAdminButton.setCallbackData("admin_add_admin");
        
        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад в админ-меню");
        backButton.setCallbackData("admin_back_to_admin_menu");
        
        rows.add(List.of(addAdminButton));
        rows.add(List.of(backButton));
        
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setReplyMarkup(keyboard);
        
        Sent sent = new Sent();
        sent.sendMessage(admin, text, message);
    }
    
    /**
     * Показать меню редактирования товара
     */
    public void showEditProductMenu(User admin, int productId) {
        ProductDAO productDAO = new ProductDAO();
        Product product = productDAO.findById(productId);
        
        if (product == null) {
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Товар не найден");
            return;
        }
        
        String text = "✏️ Редактирование товара:\n\n" +
                     "📦 Название: " + product.getProductName() + "\n" +
                     "🔢 Артикул: " + product.getArticul() + "\n" +
                     "💰 Кэшбэк: " + product.getCashbackPercentage() + "%\n" +
                     "🔍 Ключевой запрос: " + product.getKeyQuery() + "\n" +
                     "📝 Условия: " + product.getAdditionalСonditions() + "\n" +
                     "📷 Фотография: " + (product.getPhoto() != null ? "Загружена" : "Не загружена") + "\n" +
                     "👁️ Видимость: " + (product.isVisible() ? "Видимый" : "Скрытый") + "\n\n" +
                     "Выберите что редактировать:";
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Кнопки редактирования
        InlineKeyboardButton nameButton = new InlineKeyboardButton();
        nameButton.setText("📦 Название");
        nameButton.setCallbackData("admin_edit_product_name_" + productId);
        
        InlineKeyboardButton articulButton = new InlineKeyboardButton();
        articulButton.setText("🔢 Артикул");
        articulButton.setCallbackData("admin_edit_product_articul_" + productId);
        
        InlineKeyboardButton cashbackButton = new InlineKeyboardButton();
        cashbackButton.setText("💰 Кэшбэк");
        cashbackButton.setCallbackData("admin_edit_product_cashback_" + productId);
        
        InlineKeyboardButton queryButton = new InlineKeyboardButton();
        queryButton.setText("🔍 Ключевой запрос");
        queryButton.setCallbackData("admin_edit_product_query_" + productId);
        
        InlineKeyboardButton conditionsButton = new InlineKeyboardButton();
        conditionsButton.setText("📝 Условия");
        conditionsButton.setCallbackData("admin_edit_product_conditions_" + productId);
        
        InlineKeyboardButton photoButton = new InlineKeyboardButton();
        photoButton.setText("📷 Фотография");
        photoButton.setCallbackData("admin_edit_product_photo_" + productId);
        
        InlineKeyboardButton visibilityButton = new InlineKeyboardButton();
        visibilityButton.setText("👁️ Видимость");
        visibilityButton.setCallbackData("admin_edit_product_visibility_" + productId);
        
        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад к товарам");
        backButton.setCallbackData("admin_back_to_products");
        
        rows.add(List.of(nameButton));
        rows.add(List.of(articulButton));
        rows.add(List.of(cashbackButton));
        rows.add(List.of(queryButton));
        rows.add(List.of(conditionsButton));
        rows.add(List.of(photoButton));
        rows.add(List.of(visibilityButton));
        rows.add(List.of(backButton));
        
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setReplyMarkup(keyboard);
        
        Sent sent = new Sent();
        sent.sendMessage(admin, text, message);
    }
}
