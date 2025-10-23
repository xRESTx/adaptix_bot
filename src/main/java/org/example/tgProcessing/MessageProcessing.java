package org.example.tgProcessing;

import org.example.async.AsyncService;
import org.example.dao.PhotoDAO;
import org.example.dao.ProductDAO;
import org.example.dao.PurchaseDAO;
import org.example.dao.UserDAO;
import org.example.monitoring.MetricsService;
import io.micrometer.core.instrument.Timer;
import org.example.session.ProductCreationSession;
import org.example.session.ReviewRequestSession;
import org.example.session.ReviewSubmissionSession;
import org.example.session.RedisSessionStore;
import org.example.session.ReservationManager;
import org.example.settings.AdminSettings;
import org.example.table.Photo;
import java.util.ResourceBundle;
import org.example.table.Product;
import org.example.table.Purchase;
import org.example.table.User;
import org.example.telegramBots.TelegramBot;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Video;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
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
        // Начинаем измерение времени обработки
        MetricsService metricsService = MetricsService.getInstance();
        Timer.Sample sample = metricsService.startMessageProcessing();
        
        // Отладочная информация
        if (update.hasCallbackQuery()) {
            System.out.println("🔍 Update has callback query: " + update.getCallbackQuery().getData());
        }
        
        try {
            Sent createTelegramBot = new Sent();
            LogicUI logicUI = new LogicUI();

            ResourceBundle rb = ResourceBundle.getBundle("app");
            long groupID = Long.parseLong(rb.getString("tg.group"));

            String msg = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            
            // Регистрируем сообщение от пользователя
            metricsService.recordUserMessage();
        if (String.valueOf(chatId).startsWith("-100")) {
            metricsService.stopMessageProcessing(sample);
            return;
        }
        UserDAO userDAO = new UserDAO();
        Integer threadID = update.getMessage().getMessageThreadId();
        if (threadID != null) {
            User user = userDAO.findByIdMessage(threadID);
            createTelegramBot.sendMessageFromBot(user.getIdUser(), msg);
            metricsService.stopMessageProcessing(sample);
            return;
        }

        User user = userDAO.findById(chatId);
//        if (user != null) {
//            createTelegramBot.sendMessageUser(groupID, user.getId_message(), "Пользователь: " + msg);
//        } else {
//            logicUI.sendStart(chatId, update);
//            return;
//        }
        // Обработка callback-запросов (до проверки user на null)
        if (update.hasCallbackQuery()) {
            if (user != null) {
                handleCallbackQuery(update, user);
            }
            metricsService.stopMessageProcessing(sample);
            return;
        }

        if(user == null){
            logicUI.sendStart(chatId, update);
            metricsService.stopMessageProcessing(sample);
            return;
        }

        // Проверяем, есть ли фотография и активная сессия покупки товара
        if (update.getMessage().hasPhoto() || update.getMessage().hasVideo()) {
            // Сначала проверяем, не редактируется ли фотография товара
            String state = RedisSessionStore.getState(chatId);
            if (state != null && state.startsWith("edit_product_photo_")) {
                handlePhoto(update, user);
                metricsService.stopMessageProcessing(sample);
                return;
            }
            
            // Проверяем, не идет ли процесс подачи отзыва
            if (state != null && state.equals("REVIEW_SUBMISSION")) {
                handleReviewMedia(update, user);
                metricsService.stopMessageProcessing(sample);
                return;
            }
            
            ReviewRequestSession reviewSession = RedisSessionStore.getReviewSession(chatId);
            if (reviewSession != null) {
                if (reviewSession.getStep() == ReviewRequestSession.Step.SEARCH_SCREENSHOT) {
                    handleSearchScreenshot(update, user);
                    metricsService.stopMessageProcessing(sample);
                    return;
                } else if (reviewSession.getStep() == ReviewRequestSession.Step.DELIVERY_SCREENSHOT) {
                    handleDeliveryScreenshot(update, user);
                    metricsService.stopMessageProcessing(sample);
                    return;
                }
            } else {
                // Проверяем, не идет ли процесс создания товара
                ProductCreationSession productSession = RedisSessionStore.getProductSession(chatId);
                if (productSession != null && productSession.getStep() == ProductCreationSession.Step.PHOTO) {
                    // Идет процесс создания товара - обрабатываем фотографию товара
                    handlePhoto(update, user);
                    metricsService.stopMessageProcessing(sample);
                    return;
                } else {
                    // Фотография без активной сессии - начинаем процесс с поиска товара
                    startSearchProcess(update, user);
                    metricsService.stopMessageProcessing(sample);
                    return;
                }
            }
        }

        if(msg!=null){
            switch (msg) {
                case "/start" -> logicUI.sendStart(chatId, update);
                           case "Админ меню" -> {
                               // Показываем админ-меню (обычное меню остается)
                               logicUI.showAdminMenu(user);
                               metricsService.recordAdminAction();
                           }
                case "Каталог товаров" -> logicUI.sendProducts(user);
                case "Оставить отзыв" -> {
                    // Показываем инструкции для оставления отзыва
                    Sent sent = new Sent();
                    sent.sendMessage(user, "📝 Для оставления отзыва:\n\n" +
                        "1️⃣ Перейдите в главное меню → «📝 Оставить отзыв»\n" +
                        "2️⃣ Выберите товар из ваших покупок\n" +
                        "3️⃣ Отправьте 3-4 фотографии и 1 видео\n" +
                        "4️⃣ Получите кешбек после публикации отзыва!\n\n" +
                        "💡 Сначала закажите товар через «Каталог товаров»");
                }
                case "Техподдержка" -> {
                    String supportMention = AdminSettings.getInstance().getSupportMention();
                    createTelegramBot.sendMessage(user, "🆘 Техподдержка: " + supportMention + "\n\nОпишите вашу проблему, и мы обязательно поможем!");
                }
                case "Отмена добавления товара" -> {
                    RedisSessionStore.removeState(chatId);
                    logicUI.sendMenu(user, null);
                }
                case "Отмена покупки товара" -> {
                    RedisSessionStore.removeState(chatId);
                    logicUI.sendProducts(user);
                }
            }
        }


        String state = RedisSessionStore.getState(chatId);
        if(state!= null) {
            if ("PRODUCT_CREATION".equals(state)) {
                System.out.println("🔧 Processing PRODUCT_CREATION state for user: " + chatId);
                ProductCreationSession session = RedisSessionStore.getProductSession(chatId);
                System.out.println("🔧 Session found: " + (session != null));
                if (session != null) {
                    System.out.println("🔧 Current step: " + session.getStep());
                    switch (session.getStep()) {
                        case ARTICUL:
                            System.out.println("🔧 Processing ARTICUL step with input: " + msg);
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
                } else {
                    System.out.println("🔧 Session is null for PRODUCT_CREATION state");
                }
            }
            if ("REVIEW_REQUEST".equals(state)) {
                ReviewRequestSession session = RedisSessionStore.getReviewSession(chatId);
                if (session != null) {
                    switch (session.getStep()) {
                        case SEARCH_SCREENSHOT:
                            // Обработка скриншота поиска уже выполнена в handleSearchScreenshot
                            // Этот case не должен обрабатывать текстовые сообщения
                            break;

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
                                    "(<strong>Только Сбербанк</strong>, другие банки не поддерживаются):", "Отмена покупки товара");

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
                            String bankName = msg.trim().toLowerCase();
                            if (bankName.contains("сбер") || bankName.contains("sber")) {
                                session.getRequest().setBankName(msg.trim());
                                session.setStep(ReviewRequestSession.Step.DELIVERY_SCREENSHOT);
                                logicUI.sentBack(user, "📦 Прикрепите скриншот раздела доставки с подтверждением заказа:", "Отмена покупки товара");
                            } else {
                                createTelegramBot.sendMessage(user, "❌ Принимаем только карты Сбербанка. Пожалуйста, введите \"Сбер\" или \"Сбербанк\":");
                            }
                            break;

                        case DELIVERY_SCREENSHOT:
                            handleDeliveryScreenshot(update, user);
                            break;
                    }
                }
            }
            if(state.startsWith("addAdmin_")){
                String find = msg.replace("@","");
                User userFind = userDAO.findByUsername(find);
                if(userFind!=null){
                    if(userFind.getIdUser() == user.getIdUser()){
                        createTelegramBot.sendMessage(user,"❌ Нельзя изменить статус самого себя.\n\n" +
                                "Пожалуйста, введите username другого пользователя для изменения статуса (например: @username):");
                        // Не очищаем состояние, чтобы пользователь мог попробовать еще раз
                        return;
                    }else {
                        if(!userFind.isAdmin()){
                            userDAO.updateAdminByTgId(userFind.getIdUser(),true);
                            createTelegramBot.sendMessage(user,"Админ добавлен");
                            createTelegramBot.sendMessage(userFind,"Вы новый администратор");
                            // Отправляем /start новому админу для обновления меню
                            logicUI.sendStart(userFind.getIdUser(), update);
                            // Возвращаемся к управлению админами
                            logicUI.showUserManagementMenu(user);

                        }else {
                            userDAO.updateAdminByTgId(userFind.getIdUser(),false);
                            createTelegramBot.sendMessage(user,"Админ удален");
                            createTelegramBot.sendMessage(userFind,"Вы разжалованы");
                            logicUI.sendStart(userFind.getIdUser(),update);
                            // Возвращаемся к управлению админами
                            logicUI.showUserManagementMenu(user);
                        }
                    }
                }else {
                    createTelegramBot.sendMessage(user,"❌ Пользователь с таким именем не найден в базе данных.\n\n" +
                            "Пожалуйста, проверьте правильность написания username и попробуйте еще раз.\n" +
                            "Введите username пользователя для изменения статуса (например: @username):");
                    // Не очищаем состояние, чтобы пользователь мог попробовать еще раз
                    return;
                }

                RedisSessionStore.removeState(chatId);
                return;
            }
            
            // Обработка редактирования полей товара
            if(state.startsWith("edit_product_")){
                if(state.startsWith("edit_product_photo_")){
                    // Обработка фотографии будет в handlePhoto
                    return;
                } else {
                    handleProductFieldEdit(user, state, msg);
                    return;
                }
            }
            
            if("change_support".equals(state)){
                // Изменение настроек техподдержки
                AdminSettings.getInstance().setSupportMention(msg);
                createTelegramBot.sendMessage(user, "✅ Настройки техподдержки обновлены: " + msg);
                RedisSessionStore.removeState(chatId);
                return;
            }
        }
        } finally {
            // Завершаем измерение времени обработки
            metricsService.stopMessageProcessing(sample);
        }
    }

    /**
     * Обработка скриншота поиска товара
     */
    public void handleSearchScreenshot(Update update, User user) {
        Sent createTelegramBot = new Sent();
        long chatId = update.getMessage().getChatId();
        Message message = update.getMessage();
        ReviewRequestSession session = RedisSessionStore.getReviewSession(chatId);
        
        if (session == null || session.getStep() != ReviewRequestSession.Step.SEARCH_SCREENSHOT) {
            return;
        }
        
        if (message.getPhoto() == null || message.getPhoto().isEmpty()) {
            createTelegramBot.sendMessage(user, "Пожалуйста, приложите скриншот поиска товара картинкой.");
            return;
        }
        
        PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
        String fileId = photo.getFileId();
        
        // Отправляем пользователю сообщение о начале обработки
        createTelegramBot.sendMessage(user, "🔄 Обрабатываю скриншот поиска, пожалуйста подождите...");
        
        // Асинхронная обработка скриншота поиска
        System.out.println("🔍 Debug: Processing search screenshot");
        System.out.println("🔍 Debug: session.getProduct() = " + session.getProduct());
        System.out.println("🔍 Debug: ProductCreationSession = " + RedisSessionStore.getProductSession(chatId));
        
        AsyncService.processSearchScreenshotAsync(session, user, photo, fileId)
            .thenRun(() -> {
                System.out.println("🔍 Debug: Search screenshot processing completed");
                System.out.println("🔍 Debug: session.getProduct() after processing = " + session.getProduct());
                if (session.getProduct() != null) {
                    // Товар уже выбран - переходим к вводу артикула
                    session.setStep(ReviewRequestSession.Step.ARTICUL_CHECK);
                    createTelegramBot.sendMessage(user, "✅ Скриншот поиска принят!\n\n🔢 Теперь введите артикул товара Wildberries для проверки:");
                } else {
                    // Товар не выбран - это означает, что процесс создания товара не завершен
                    // Нужно завершить создание товара и показать админ-меню
                    System.out.println("🔍 Debug: session.getProduct() is null, completing product creation");
                    
                    // Проверяем, есть ли активная сессия создания товара
                    ProductCreationSession productSession = RedisSessionStore.getProductSession(chatId);
                    if (productSession != null && productSession.getProduct() != null) {
                        // Товар создан, но не завершен - завершаем процесс
                        System.out.println("🔍 Debug: Found incomplete product creation, completing...");
                        productSession.setStep(ProductCreationSession.Step.PHOTO);
                        createTelegramBot.sendMessage(user, "✅ Скриншот поиска принят!\n\n📷 Теперь отправьте фотографию товара:");
                    } else {
                        // Нет активной сессии создания товара - показываем админ-меню
                        System.out.println("🔍 Debug: No active product creation session, showing admin menu");
                        createTelegramBot.sendMessage(user, "✅ Скриншот поиска принят!");
                        LogicUI logicUI = new LogicUI();
                        logicUI.showAdminMenu(user);
                    }
                }
            })
            .exceptionally(throwable -> {
                System.err.println("❌ Search screenshot processing error: " + throwable.getMessage());
                createTelegramBot.sendMessage(user, "❌ Не удалось обработать скриншот поиска. Попробуйте ещё раз.");
                return null;
            });
    }
    
    /**
     * Обработка скриншота раздела доставки
     */
    public void handleDeliveryScreenshot(Update update, User user) {
        Sent createTelegramBot = new Sent();
        long chatId = update.getMessage().getChatId();
        Message message = update.getMessage();
        ReviewRequestSession session = RedisSessionStore.getReviewSession(chatId);
        
        if (session == null || session.getStep() != ReviewRequestSession.Step.DELIVERY_SCREENSHOT) {
            return;
        }
        
        if (message.getPhoto() == null || message.getPhoto().isEmpty()) {
            createTelegramBot.sendMessage(user, "Пожалуйста, приложите скриншот раздела доставки картинкой.");
            return;
        }
        
        PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
        String fileId = photo.getFileId();
        
        // Отправляем пользователю сообщение о начале обработки
        createTelegramBot.sendMessage(user, "🔄 Обрабатываю скриншот доставки, пожалуйста подождите...");
        
        // Асинхронная обработка скриншота доставки
        AsyncService.processDeliveryScreenshotAsync(session, user, photo, fileId)
            .thenRun(() -> {
                // Успешная обработка
                String finishText =
                        "Спасибо за участие!\n\n" +
                        "После получения товара (на следующий день после забора с ПВЗ):\n" +
                        "1️⃣ Перейдите в главное меню → «📝 Оставить отзыв»\n" +
                        "2️⃣ Заполните форму по инструкции\n" +
                        "3️⃣ После утверждения отзыва администратором, перейдите в раздел " +
                        "→ «💸 Получить кешбек» и отправьте скриншот вашего отзыва";

                LogicUI logicUI = new LogicUI();
                logicUI.sendMenu(user, finishText);
                RedisSessionStore.removeReviewSession(chatId);
                
                // Отменяем бронь
                ReservationManager.getInstance().cancelReservation(chatId);
                
                // Уведомление в группу с двумя фотографиями
                String text =
                        "Пользователь купил товар \"" + session.getProduct().getProductName() + "\"\n"
                        + "ФИО: " + session.getRequest().getFullName() + "\n"
                        + "Номер телефона: <code>" + session.getRequest().getPhoneNumber() + "</code>\n"
                        + "Банк: " + session.getRequest().getBankName() + "\n"
                        + "Реквизиты: <code>" + session.getRequest().getCardNumber() + "</code>\n"
                        + "Стоимость для пользователя: <code>" + session.getRequest().getPurchaseAmount() + "</code>\n";
                
                Long groupMessageId = createTelegramBot.sendTwoPhotosToGroup(user, text, session.getSearchScreenshotPath(), session.getDeliveryScreenshotPath());
                
                System.out.println("🔍 Debug: groupMessageId from sendTwoPhotosToGroup = " + groupMessageId);
                
                // Сохраняем ID сообщения в сессии для последующего сохранения в БД
                session.setGroupMessageId(groupMessageId);
                System.out.println("🔍 Debug: session.setGroupMessageId(" + groupMessageId + ")");
                
                // Сразу сохраняем ID сообщения в базе данных
                if (groupMessageId != null) {
                    try {
                        PurchaseDAO purchaseDAO = new PurchaseDAO();
                        Purchase purchase = purchaseDAO.findByUserAndProduct(user, session.getProduct());
                        if (purchase != null) {
                            purchase.setOrderMessageId(groupMessageId);
                            purchaseDAO.update(purchase);
                            System.out.println("✅ Order message ID saved directly: " + groupMessageId);
                        } else {
                            System.out.println("❌ Purchase not found for user and product");
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Error saving order message ID: " + e.getMessage());
                    }
                }
            })
            .exceptionally(throwable -> {
                System.err.println("❌ Delivery screenshot processing error: " + throwable.getMessage());
                createTelegramBot.sendMessage(user, "❌ Не удалось обработать скриншот доставки. Попробуйте ещё раз.");
                return null;
            });
    }

    public void handlePhoto(Update update, User user) {
        Sent createTelegramBot = new Sent();
        long chatId = update.getMessage().getChatId();
        Message message = update.getMessage();
        
        // Проверяем, не редактируется ли фотография товара
        String state = RedisSessionStore.getState(chatId);
        
        if (state != null && state.startsWith("edit_product_photo_")) {
            handleProductPhotoEdit(update, user, state);
            return;
        }
        
        ProductCreationSession session = RedisSessionStore.getProductSession(chatId);

        if (session != null && session.getStep() == ProductCreationSession.Step.PHOTO) {
            if (message.getPhoto() == null || message.getPhoto().isEmpty()) {
                createTelegramBot.sendMessage(user, "Пожалуйста, отправьте фотографию товара, а не текст.");
                return;
            }
            
            PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
            String photoId = photo.getFileId();
            
            // Отправляем пользователю сообщение о начале обработки
            createTelegramBot.sendMessage(user, "🔄 Обрабатываю фотографию товара, пожалуйста подождите...");
            
            // Асинхронная обработка фото товара
            AsyncService.processProductPhotoAsync(photoId, session.getProduct().getProductName())
                .thenAccept(filePath -> {
                    if (filePath != null) {
                        // Успешная обработка
                        session.getProduct().setPhoto(filePath);
                        ProductDAO productDAO = new ProductDAO();
                        productDAO.save(session.getProduct());
                        
                        RedisSessionStore.clearAll(chatId);
                
                // Регистрируем создание товара
                MetricsService.getInstance().recordProductCreation();
                        
                        // Короткий caption для фотографии
                        String photoCaption = "✅ Товар успешно создан!\n\n" +
                                "📦 " + session.getProduct().getProductName() + "\n" +
                                "💰 Кешбек: " + session.getProduct().getCashbackPercentage() + "%\n" +
                                "🔍 Запрос: " + session.getProduct().getKeyQuery();
                        
                        // Отправляем только фотографию с коротким caption
                        createTelegramBot.sendPhoto(user.getIdUser(), filePath, photoCaption);
                        LogicUI logicUI = new LogicUI();
                        // Показываем админ-меню после создания товара
                        logicUI.showAdminMenu(user);
                    } else {
                        createTelegramBot.sendMessage(user, "❌ Произошла ошибка при загрузке фотографии.");
                        // Показываем админ-меню даже при ошибке загрузки
                        LogicUI logicUI = new LogicUI();
                        logicUI.showAdminMenu(user);
                    }
                })
                .exceptionally(throwable -> {
                    // Обработка ошибок
                    System.err.println("❌ Async photo processing error: " + throwable.getMessage());
                    createTelegramBot.sendMessage(user, "❌ Произошла ошибка при загрузке фотографии.");
                    // Показываем админ-меню даже при ошибке
                    LogicUI logicUI = new LogicUI();
                    logicUI.showAdminMenu(user);
                    return null;
                });
        }
    }
    public void callBackQuery(Update update) {
        Sent createTelegramBot = new Sent();
        LogicUI logicUI = new LogicUI();
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        RedisSessionStore.clearAll(chatId);

        UserDAO userDAO = new UserDAO();
        User user = userDAO.findById(chatId);
        
        if (user == null) {
            return;
        }
        

        // Обработка админ-интерфейса
        if (data.equals("admin_menu") || data.equals("admin_products") || data.equals("admin_stats") || 
            data.equals("admin_settings") || data.equals("admin_add_product") || data.equals("admin_user_management") ||
            data.equals("admin_add_admin") || data.equals("admin_change_support") || data.equals("admin_back_to_menu") ||
            data.equals("admin_back_to_main_menu") || data.equals("admin_back_to_admin_menu") || data.equals("admin_back_to_products") || data.equals("back_to_menu") ||
            data.startsWith("admin_product_") || data.startsWith("admin_user_") || data.startsWith("admin_back_to_purchases_") ||
            data.startsWith("admin_edit_product_") || data.startsWith("admin_edit_product_name_") ||
            data.startsWith("admin_edit_product_articul_") || data.startsWith("admin_edit_product_cashback_") ||
            data.startsWith("admin_edit_product_query_") || data.startsWith("admin_edit_product_conditions_") ||
            data.startsWith("admin_edit_product_photo_") || data.startsWith("admin_edit_product_visibility_") ||
            data.startsWith("admin_view_stage_")) {
            System.out.println("🔍 Admin callback received: " + data);
            handleCallbackQuery(update, user);
            return;
        }

        if(data.startsWith("product_")){
            // Проверяем, является ли пользователь админом И находится ли он в режиме админа
            if(user.isAdmin() && !user.isUserFlag()) {
                // Для админов в режиме админа показываем админ-меню товара
                ProductDAO productDAO = new ProductDAO();
                String[] parts = data.split(":");
                int productId = Integer.parseInt(parts[1]);
                int messageId = Integer.parseInt(parts[2]);
                
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showProductPurchases(user, productId);
            } else {
                // Для обычных пользователей и админов в режиме пользователя показываем товар
                ProductDAO productDAO = new ProductDAO();
                String[] parts = data.split(":");
                Product selected = productDAO.findById(Integer.parseInt(parts[1]));
                logicUI.sentOneProduct(user,selected, Integer.parseInt(parts[2]));
            }
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
        } else if (data.startsWith("leave_review_")) {
            // Обработка начала процесса оставления отзыва
            int purchaseId = Integer.parseInt(data.substring("leave_review_".length()));
            startReviewSubmission(user, purchaseId);
        }
        if (data.contains(":")) {
            String[] parts = data.split(":", 2);
            String command = parts[0];
            String messageID = parts[1];
            switch (command) {
                case "addAdmin": {
                    if (user.isAdmin()) {
                        RedisSessionStore.setState(chatId,"addAdmin_");
                        createTelegramBot.editMessageMarkup(user, Integer.parseInt(messageID), "Отправьте тег (Например @qwerty123)", null);
                    }
                    break;
                }
                case "addProduct": {
                    if (user.isAdmin()) {
                        System.out.println("🔍 Debug: Creating new ProductCreationSession for chatId: " + chatId);
                        ProductCreationSession session = new ProductCreationSession();
                        RedisSessionStore.setProductSession(chatId, session);
                        RedisSessionStore.setState(chatId, "PRODUCT_CREATION");
                        System.out.println("🔍 Debug: Session created and stored, state set to PRODUCT_CREATION");
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
                    // Не удаляем сообщение, так как оно уже было заменено на информацию о товаре
                    // Просто показываем каталог товаров
                    logicUI.sendProducts(user);
                    break;
                }
                case "buy_product":{
                    ProductDAO productDAO = new ProductDAO();
                    Product product = productDAO.findById(Integer.parseInt(messageID));
                    ReviewRequestSession session = new ReviewRequestSession();
                    session.setProduct(product);
                    session.setStep(ReviewRequestSession.Step.SEARCH_SCREENSHOT);
                    RedisSessionStore.setReviewSession(chatId,session);

                    RedisSessionStore.setState(chatId, "REVIEW_REQUEST");
                    
                    // Регистрируем запрос на покупку
                    MetricsService.getInstance().recordPurchaseRequest();

                    logicUI.sentBack(user, "📸 Прикрепите скриншот поиска товара на Wildberries с поисковой строкой и найденным товаром:", "Отмена покупки товара");

                    break;
                }
            }
        }
    }

    /**
     * Обработка callback-запросов для админ-интерфейса
     */
    private void handleCallbackQuery(Update update, User user) {
        String callbackData = update.getCallbackQuery().getData();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        System.out.println("🔍 Callback received: " + callbackData);
        
        if (!user.isAdmin()) {
            System.out.println("❌ User is not admin: " + user.getUsername());
            return; // Только админы могут использовать админ-интерфейс
        }

        System.out.println("✅ User is admin: " + user.getUsername());
        LogicUI logicUI = new LogicUI();

        switch (callbackData) {
            case "admin_menu" -> {
                System.out.println("📋 Showing admin menu");
                // Удаляем текущее сообщение и показываем админ-меню
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showAdminMenu(user);
            }
            case "back_to_menu" -> {
                System.out.println("🏠 Back to main menu");
                logicUI.updateMenu(user, messageId, null);
            }
            case "admin_products" -> {
                System.out.println("📦 Showing products list");
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showProductsList(user);
            }
            case "admin_stats" -> {
                System.out.println("📊 Showing stats");
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showStats(user);
            }
            case "admin_settings" -> {
                System.out.println("⚙️ Showing settings");
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showSettings(user);
            }
            case "admin_change_support" -> {
                System.out.println("🆘 Changing support settings");
                changeSupportSettings(user, messageId);
            }
            case "admin_add_product" -> {
                System.out.println("➕ Adding product");
                // Устанавливаем состояние для добавления товара
                RedisSessionStore.setState(user.getIdUser(), "PRODUCT_CREATION");
                // Создаем сессию для создания товара
                ProductCreationSession session = new ProductCreationSession();
                RedisSessionStore.setProductSession(user.getIdUser(), session);
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showAddProductMenu(user);
            }
            case "admin_user_management" -> {
                System.out.println("👥 User management");
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showUserManagementMenu(user);
            }
            case "admin_add_admin" -> {
                System.out.println("➕ Adding admin");
                changeAdminSettings(user, messageId);
            }
            case "admin_back_to_menu" -> {
                System.out.println("⬅️ Back to admin menu");
                // Удаляем текущее сообщение и показываем админ-меню
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showAdminMenu(user);
            }
            case "admin_back_to_main_menu" -> {
                System.out.println("🏠 Back to main menu");
                // Удаляем админ-сообщение и показываем обычное меню
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.sendMenu(user, null);
            }
            case "admin_back_to_admin_menu" -> {
                System.out.println("⬅️ Back to admin menu");
                // Удаляем текущее сообщение и показываем админ-меню
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showAdminMenu(user);
            }
            case "admin_back_to_products" -> {
                System.out.println("📦 Back to products");
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showProductsList(user);
            }
            default -> {
                if (callbackData.startsWith("admin_product_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_product_".length()));
                    System.out.println("🛒 Showing product purchases for ID: " + productId);
                    TelegramBot telegramBot = new TelegramBot();
                    telegramBot.deleteMessage(user.getIdUser(), messageId);
                    logicUI.showProductPurchases(user, productId);
                } else if (callbackData.startsWith("admin_user_")) {
                    int purchaseId = Integer.parseInt(callbackData.substring("admin_user_".length()));
                    System.out.println("👤 Showing purchase details for ID: " + purchaseId);
                    TelegramBot telegramBot = new TelegramBot();
                    telegramBot.deleteMessage(user.getIdUser(), messageId);
                    logicUI.showPurchaseDetails(user, purchaseId);
                } else if (callbackData.startsWith("admin_back_to_purchases_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_back_to_purchases_".length()));
                    System.out.println("🛒 Back to purchases for product ID: " + productId);
                    TelegramBot telegramBot = new TelegramBot();
                    telegramBot.deleteMessage(user.getIdUser(), messageId);
                    logicUI.showProductPurchases(user, productId);
                } else if (callbackData.startsWith("admin_edit_product_name_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_name_".length()));
                    System.out.println("✏️ Editing product name for ID: " + productId);
                    startProductFieldEdit(user, productId, "name");
                } else if (callbackData.startsWith("admin_edit_product_articul_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_articul_".length()));
                    System.out.println("✏️ Editing product articul for ID: " + productId);
                    startProductFieldEdit(user, productId, "articul");
                } else if (callbackData.startsWith("admin_edit_product_cashback_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_cashback_".length()));
                    System.out.println("✏️ Editing product cashback for ID: " + productId);
                    startProductFieldEdit(user, productId, "cashback");
                } else if (callbackData.startsWith("admin_edit_product_query_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_query_".length()));
                    System.out.println("✏️ Editing product query for ID: " + productId);
                    startProductFieldEdit(user, productId, "query");
                } else if (callbackData.startsWith("admin_edit_product_participants_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_participants_".length()));
                    System.out.println("✏️ Editing product participants for ID: " + productId);
                    startProductFieldEdit(user, productId, "participants");
                } else if (callbackData.startsWith("admin_edit_product_conditions_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_conditions_".length()));
                    System.out.println("✏️ Editing product conditions for ID: " + productId);
                    startProductFieldEdit(user, productId, "conditions");
                } else if (callbackData.startsWith("admin_edit_product_photo_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_photo_".length()));
                    System.out.println("📷 Editing product photo for ID: " + productId);
                    startProductPhotoEdit(user, productId);
                } else if (callbackData.startsWith("admin_edit_product_visibility_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_visibility_".length()));
                    System.out.println("✏️ Toggling product visibility for ID: " + productId);
                    toggleProductVisibility(user, productId);
                } else if (callbackData.startsWith("admin_edit_product_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_".length()));
                    System.out.println("✏️ Showing edit menu for product ID: " + productId);
                    TelegramBot telegramBot = new TelegramBot();
                    telegramBot.deleteMessage(user.getIdUser(), messageId);
                    logicUI.showEditProductMenu(user, productId);
                } else if (callbackData.startsWith("admin_view_stage_")) {
                    handleViewStage(user, callbackData);
                } else {
                    System.out.println("❓ Unknown callback: " + callbackData);
                }
            }
        }
    }

    /**
     * Начало процесса поиска товара с фотографией
     */
    private void startSearchProcess(Update update, User user) {
        Sent createTelegramBot = new Sent();
        long chatId = update.getMessage().getChatId();
        Message message = update.getMessage();
        
        if (message.getPhoto() == null || message.getPhoto().isEmpty()) {
            createTelegramBot.sendMessage(user, "Пожалуйста, приложите скриншот поиска товара картинкой.");
            return;
        }
        
        PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
        String fileId = photo.getFileId();
        
        // Создаем сессию поиска товара
        ReviewRequestSession session = new ReviewRequestSession();
        session.setStep(ReviewRequestSession.Step.SEARCH_SCREENSHOT);
        RedisSessionStore.setReviewSession(chatId, session);
        RedisSessionStore.setState(chatId, "REVIEW_REQUEST");
        
        // Обрабатываем скриншот поиска
        handleSearchScreenshot(update, user);
    }
    
    /**
     * Изменение настроек техподдержки
     */
    private void changeSupportSettings(User admin, int messageId) {
        String text = "🆘 Изменение настроек техподдержки:\n\n" +
                     "Текущее значение: " + AdminSettings.getInstance().getSupportMention() + "\n\n" +
                     "Введите новое упоминание техподдержки (например: @admin или @support):";

        // Устанавливаем состояние для изменения техподдержки
        RedisSessionStore.setState(admin.getIdUser(), "change_support");

        Sent sent = new Sent();
        sent.editMessageMarkup(admin, messageId, text, null);
    }

    /**
     * Изменение настроек админа
     */
    private void changeAdminSettings(User admin, int messageId) {
        String text = "👑 Управление администраторами:\n\n" +
                     "Введите username пользователя для изменения статуса (например: @username):";

        // Устанавливаем состояние для изменения админа
        RedisSessionStore.setState(admin.getIdUser(), "addAdmin_" + messageId);

        // Создаем кнопку "Назад"
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад к управлению админами");
        backButton.setCallbackData("admin_user_management");
        
        rows.add(List.of(backButton));
        keyboard.setKeyboard(rows);

        // Создаем EditMessageReplyMarkup
        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(String.valueOf(admin.getIdUser()));
        editMarkup.setMessageId(messageId);
        editMarkup.setReplyMarkup(keyboard);

        Sent sent = new Sent();
        sent.editMessageMarkup(admin, messageId, text, editMarkup);
    }
    
    /**
     * Начать редактирование поля товара
     */
    private void startProductFieldEdit(User admin, int productId, String field) {
        String fieldName = getFieldDisplayName(field);
        String text = "✏️ Редактирование " + fieldName + ":\n\n" +
                     "Введите новое значение для " + fieldName.toLowerCase() + ":";
        
        // Устанавливаем состояние для редактирования
        RedisSessionStore.setState(admin.getIdUser(), "edit_product_" + field + "_" + productId);
        
        Sent sent = new Sent();
        sent.sendMessage(admin, text);
    }
    
    /**
     * Начать редактирование фотографии товара
     */
    private void startProductPhotoEdit(User admin, int productId) {
        String text = "📷 Редактирование фотографии товара:\n\n" +
                     "Отправьте новую фотографию товара:";
        
        // Устанавливаем состояние для редактирования фотографии
        String state = "edit_product_photo_" + productId;
        RedisSessionStore.setState(admin.getIdUser(), state);
        
        Sent sent = new Sent();
        sent.sendMessage(admin, text);
    }
    
    /**
     * Переключить видимость товара
     */
    private void toggleProductVisibility(User admin, int productId) {
        ProductDAO productDAO = new ProductDAO();
        Product product = productDAO.findById(productId);
        
        if (product == null) {
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Товар не найден");
            return;
        }
        
        // Переключаем видимость
        product.setVisible(!product.isVisible());
        productDAO.update(product);
        
        String status = product.isVisible() ? "видимым" : "скрытым";
        Sent sent = new Sent();
        sent.sendMessage(admin, "✅ Товар \"" + product.getProductName() + "\" теперь " + status);
        
        // Показываем обновленное меню редактирования
        LogicUI logicUI = new LogicUI();
        logicUI.showEditProductMenu(admin, productId);
    }
    
    /**
     * Получить отображаемое имя поля
     */
    private String getFieldDisplayName(String field) {
        switch (field) {
            case "name": return "Название";
            case "articul": return "Артикул";
            case "cashback": return "Кэшбэк";
            case "query": return "Ключевой запрос";
            case "conditions": return "Дополнительные условия";
            default: return field;
        }
    }
    
    /**
     * Обработать редактирование поля товара
     */
    private void handleProductFieldEdit(User admin, String state, String newValue) {
        // Парсим состояние: edit_product_field_productId
        String[] parts = state.split("_");
        if (parts.length < 4) {
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Ошибка в состоянии редактирования");
            return;
        }
        
        String field = parts[2];
        int productId = Integer.parseInt(parts[3]);
        
        ProductDAO productDAO = new ProductDAO();
        Product product = productDAO.findById(productId);
        
        if (product == null) {
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Товар не найден");
            return;
        }
        
        try {
            // Обновляем соответствующее поле
            switch (field) {
                case "name":
                    product.setProductName(newValue);
                    break;
                case "articul":
                    int articul = Integer.parseInt(newValue);
                    product.setArticul(articul);
                    break;
                case "cashback":
                    int cashback = Integer.parseInt(newValue);
                    product.setCashbackPercentage(cashback);
                    break;
                case "query":
                    product.setKeyQuery(newValue);
                    break;
                case "conditions":
                    product.setAdditionalСonditions(newValue);
                    break;
                default:
                    Sent sent = new Sent();
                    sent.sendMessage(admin, "❌ Неизвестное поле для редактирования");
                    return;
            }
            
            // Сохраняем изменения
            productDAO.update(product);
            
            String fieldName = getFieldDisplayName(field);
            Sent sent = new Sent();
            sent.sendMessage(admin, "✅ " + fieldName + " товара обновлено!");
            
            // Показываем обновленное меню редактирования
            LogicUI logicUI = new LogicUI();
            logicUI.showEditProductMenu(admin, productId);
            
        } catch (NumberFormatException e) {
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Неверный формат числа. Пожалуйста, введите корректное значение.");
        }
        
        // Очищаем состояние
        RedisSessionStore.removeState(admin.getIdUser());
    }
    
    /**
     * Безопасное удаление сообщения с обработкой ошибок
     */
    private void safeDeleteMessage(long userId, int messageId) {
        TelegramBot telegramBot = new TelegramBot();
        telegramBot.deleteMessage(userId, messageId);
    }
    
    /**
     * Обработать редактирование фотографии товара
     */
    private void handleProductPhotoEdit(Update update, User admin, String state) {
        // Парсим состояние: edit_product_photo_productId
        String[] parts = state.split("_");
        if (parts.length < 4) {
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Ошибка в состоянии редактирования фотографии");
            return;
        }
        
        int productId = Integer.parseInt(parts[3]);
        
        ProductDAO productDAO = new ProductDAO();
        Product product = productDAO.findById(productId);
        
        if (product == null) {
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Товар не найден");
            return;
        }
        
        Message message = update.getMessage();
        if (message.getPhoto() == null || message.getPhoto().isEmpty()) {
            Sent sent = new Sent();
            sent.sendMessage(admin, "Пожалуйста, отправьте фотографию товара, а не текст.");
            return;
        }
        
        PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
        String photoId = photo.getFileId();
        
        // Отправляем пользователю сообщение о начале обработки
        Sent sent = new Sent();
        sent.sendMessage(admin, "🔄 Обрабатываю новую фотографию товара, пожалуйста подождите...");
        
        // Асинхронная обработка фото товара
        AsyncService.processProductPhotoAsync(photoId, product.getProductName())
            .thenAccept(filePath -> {
                if (filePath != null) {
                    // Успешная обработка - получаем товар заново из базы данных
                    Product updatedProduct = productDAO.findById(productId);
                    if (updatedProduct != null) {
                        updatedProduct.setPhoto(filePath);
                        productDAO.update(updatedProduct);
                    }
                    
                    // Очищаем состояние
                    RedisSessionStore.removeState(admin.getIdUser());
                    
                    sent.sendMessage(admin, "✅ Фотография товара обновлена!");
                    
                    // Показываем обновленное меню редактирования
                    LogicUI logicUI = new LogicUI();
                    logicUI.showEditProductMenu(admin, productId);
                } else {
                    sent.sendMessage(admin, "❌ Не удалось обработать фотографию товара. Попробуйте ещё раз.");
                }
            })
            .exceptionally(throwable -> {
                System.err.println("Error processing product photo: " + throwable.getMessage());
                sent.sendMessage(admin, "❌ Произошла ошибка при обработке фотографии товара.");
                return null;
            });
    }
    
    /**
     * Обработать просмотр этапа покупки
     */
    private void handleViewStage(User admin, String callbackData) {
        // Парсим callback: admin_view_stage_purchaseId_stage
        String[] parts = callbackData.split("_");
        if (parts.length < 5) {
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Ошибка в callback этапа");
            return;
        }
        
        int purchaseId = Integer.parseInt(parts[3]);
        String stage = parts[4];
        
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        Purchase purchase = purchaseDAO.findById(purchaseId);
        
        if (purchase == null) {
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Покупка не найдена");
            return;
        }
        
        Long messageId = null;
        String stageName = "";
        
        switch (stage) {
            case "order":
                messageId = purchase.getOrderMessageId();
                stageName = "Товар заказан";
                break;
            case "review":
                messageId = purchase.getReviewMessageId();
                stageName = "Оставить отзыв";
                break;
            case "cashback":
                messageId = purchase.getCashbackMessageId();
                stageName = "Получить кешбек";
                break;
            default:
                Sent sent = new Sent();
                sent.sendMessage(admin, "❌ Неизвестный этап");
                return;
        }
        
        if (messageId == null) {
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Сообщение для этапа \"" + stageName + "\" не найдено");
            return;
        }
        
        try {
            // Получаем ID группы из конфигурации
            ResourceBundle rb = ResourceBundle.getBundle("app");
            String groupIdStr = rb.getString("tg.group");
            
            // Убираем префикс "-100" если он есть
            String cleanGroupId = groupIdStr;
            if (groupIdStr.startsWith("-100")) {
                cleanGroupId = groupIdStr.substring(4); // Убираем "-100"
            } else if (groupIdStr.startsWith("100")) {
                cleanGroupId = groupIdStr.substring(3); // Убираем "100"
            }
            
            // Создаем ссылку на сообщение в группе
            String groupLink = "https://t.me/c/" + cleanGroupId + "/" + messageId;
            String text = "🔗 Перейти к сообщению этапа \"" + stageName + "\":\n\n" + groupLink;
            
            Sent sent = new Sent();
            sent.sendMessage(admin, text);
        } catch (Exception e) {
            System.err.println("Ошибка при получении ID группы: " + e.getMessage());
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Ошибка при получении ссылки на группу");
        }
    }
    
    /**
     * Начать процесс подачи отзыва
     */
    private void startReviewSubmission(User user, int purchaseId) {
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        Purchase purchase = purchaseDAO.findById(purchaseId);
        
        if (purchase == null || purchase.getUser().getIdUser() != user.getIdUser()) {
            Sent sent = new Sent();
            sent.sendMessage(user, "❌ Покупка не найдена");
            return;
        }
        
        // Создаем сессию подачи отзыва
        ReviewSubmissionSession session = new ReviewSubmissionSession(purchase);
        RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
        RedisSessionStore.setState(user.getIdUser(), "REVIEW_SUBMISSION");
        
        // Показываем инструкцию
        showReviewInstructions(user);
    }
    
    /**
     * Показать инструкцию для отзыва
     */
    private void showReviewInstructions(User user) {
        String instructions = "⭐ Инструкция по оставлению отзыва:\n\n" +
                            "Для получения кешбека необходимо:\n\n" +
                            "📸 Отправить от 3 до 4 фотографий товара:\n" +
                            "• Фото 1: Общий вид товара\n" +
                            "• Фото 2: Детали/качество товара\n" +
                            "• Фото 3: Товар в использовании\n" +
                            "• Фото 4: Упаковка/этикетка (опционально)\n\n" +
                            "🎥 Отправить 1 видео:\n" +
                            "• Видео: Демонстрация товара (до 1 минуты)\n\n" +
                            "📝 Написать отзыв на Wildberries с этими материалами\n\n" +
                            "Готовы начать? Отправьте первое фото!";
        
        Sent sent = new Sent();
        sent.sendMessage(user, instructions);
    }
    
    /**
     * Обработать медиа для отзыва
     */
    private void handleReviewMedia(Update update, User user) {
        ReviewSubmissionSession session = RedisSessionStore.getReviewSubmissionSession(user.getIdUser());
        
        if (session == null) {
            Sent sent = new Sent();
            sent.sendMessage(user, "❌ Сессия отзыва не найдена. Начните заново.");
            return;
        }
        
        if (update.getMessage().hasPhoto()) {
            handleReviewPhoto(update, user, session);
        } else if (update.getMessage().hasVideo()) {
            handleReviewVideo(update, user, session);
        } else {
            Sent sent = new Sent();
            sent.sendMessage(user, "❌ Пожалуйста, отправьте фотографию или видео.");
        }
    }
    
    /**
     * Обработать фотографию для отзыва
     */
    private void handleReviewPhoto(Update update, User user, ReviewSubmissionSession session) {
        if (session.getPhotosReceived() >= 4) {
            Sent sent = new Sent();
            sent.sendMessage(user, "❌ Вы уже отправили 4 фотографии. Теперь отправьте видео.");
            return;
        }
        
        Message message = update.getMessage();
        PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
        String photoId = photo.getFileId();
        
        // Сохраняем file_id фотографии
        session.addPhoto(photoId);
        RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
        
        // Проверяем, можно ли завершить процесс
        if (session.getPhotosReceived() >= 3 && session.isVideoReceived()) {
            session.setStep(ReviewSubmissionSession.Step.COMPLETE);
            RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
            completeReviewSubmission(user, session);
            return;
        }
        
        int remaining = 4 - session.getPhotosReceived();
        if (remaining > 0) {
            Sent sent = new Sent();
            sent.sendMessage(user, "✅ Фото " + session.getPhotosReceived() + "/4 получено!\n\n📸 Отправьте еще " + remaining + " фотографий или видео:");
        } else {
            Sent sent = new Sent();
            sent.sendMessage(user, "✅ Все 4 фотографии получены!\n\n🎥 Теперь отправьте видео демонстрации товара:");
            session.setStep(ReviewSubmissionSession.Step.VIDEO);
            RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
        }
    }
    
    /**
     * Обработать видео для отзыва
     */
    private void handleReviewVideo(Update update, User user, ReviewSubmissionSession session) {
        if (session.isVideoReceived()) {
            Sent sent = new Sent();
            sent.sendMessage(user, "❌ Вы уже отправили видео.");
            return;
        }
        
        Message message = update.getMessage();
        Video video = message.getVideo();
        String videoId = video.getFileId();
        
        // Сохраняем file_id видео
        session.setVideoFileId(videoId);
        session.setVideoReceived(true);
        RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
        
        // Проверяем, можно ли завершить процесс
        if (session.getPhotosReceived() >= 3) {
            session.setStep(ReviewSubmissionSession.Step.COMPLETE);
            RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
            completeReviewSubmission(user, session);
        } else {
            Sent sent = new Sent();
            sent.sendMessage(user, "✅ Видео получено!\n\n📸 Теперь отправьте еще " + (3 - session.getPhotosReceived()) + " фотографий:");
        }
    }
    
    /**
     * Завершить подачу отзыва
     */
    private void completeReviewSubmission(User user, ReviewSubmissionSession session) {
        // Проверяем количество медиа
        if (session.getPhotosReceived() < 3 || session.getPhotosReceived() > 4) {
            Sent sent = new Sent();
            sent.sendMessage(user, "❌ Неверное количество фотографий. Нужно от 3 до 4 фото. У вас: " + session.getPhotosReceived());
            return;
        }
        
        if (!session.isVideoReceived()) {
            Sent sent = new Sent();
            sent.sendMessage(user, "❌ Необходимо отправить 1 видео.");
            return;
        }
        
        // Обновляем статус покупки
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        Purchase purchase = session.getPurchase();
        purchase.setPurchaseStage(2); // Этап: отзыв оставлен
        purchaseDAO.update(purchase);
        
        // Отправляем медиа в группу
        sendReviewMediaToGroup(user, session);
        
        // Очищаем сессию
        RedisSessionStore.removeReviewSubmissionSession(user.getIdUser());
        RedisSessionStore.removeState(user.getIdUser());
        
        Sent sent = new Sent();
        sent.sendMessage(user, "✅ Отзыв успешно отправлен!\n\n📝 Теперь оставьте отзыв на Wildberries с отправленными материалами.\n\n💰 После публикации отзыва вы получите кешбек!");
    }
    
    /**
     * Отправить медиа отзыва в группу
     */
    private void sendReviewMediaToGroup(User user, ReviewSubmissionSession session) {
        try {
            ResourceBundle rb = ResourceBundle.getBundle("app");
            long groupID = Long.parseLong(rb.getString("tg.group"));
            
            String text = "⭐ Пользователь @" + user.getUsername() + " оставил отзыв!\n\n" +
                        "📦 Товар: " + session.getPurchase().getProduct().getProductName() + "\n" +
                        "📸 Фотографий: " + session.getPhotosReceived() + "\n" +
                        "🎥 Видео: 1\n\n" +
                        "📝 Ожидается публикация отзыва на Wildberries";
            
            Sent sent = new Sent();
            sent.sendMessageToGroup(groupID, text);
            
            // Пересылаем фотографии
            for (String photoFileId : session.getPhotoFileIds()) {
                if (photoFileId != null) {
                    sent.forwardPhotoToGroup(groupID, photoFileId);
                }
            }
            
            // Пересылаем видео
            if (session.getVideoFileId() != null) {
                sent.forwardVideoToGroup(groupID, session.getVideoFileId());
            }
            
            // Сохраняем ID сообщения в группе
            // TODO: Получить и сохранить messageId из ответа
            // purchase.setReviewMessageId(messageId);
            
        } catch (Exception e) {
            System.err.println("Ошибка при отправке отзыва в группу: " + e.getMessage());
        }
    }

}