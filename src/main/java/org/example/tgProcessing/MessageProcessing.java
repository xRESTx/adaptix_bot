package org.example.tgProcessing;

import org.example.async.AsyncService;
import org.example.dao.ProductDAO;
import org.example.dao.PurchaseDAO;
import org.example.dao.UserDAO;
import org.example.monitoring.MetricsService;
import io.micrometer.core.instrument.Timer;
import org.example.session.ProductCreationSession;
import org.example.session.PurchaseCancellationSession;
import org.example.session.ReviewRequestSession;
import org.example.session.ReviewSubmissionSession;
import org.example.session.ReviewRejectionSession;
import org.example.session.CashbackSession;
import org.example.session.RedisSessionStore;
import org.example.session.ReservationService;
import org.example.session.ReservationManager;
import org.example.settings.AdminSettings;
import java.util.ResourceBundle;
import java.util.List;
import java.util.ArrayList;
import org.example.table.Product;
import org.example.table.Purchase;
import org.example.table.User;
import org.example.telegramBots.TelegramBot;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Video;
import org.telegram.telegrambots.meta.api.objects.VideoNote;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
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
        
        try {
            Sent createTelegramBot = new Sent();
            LogicUI logicUI = new LogicUI();

            String msg = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            
            // Регистрируем сообщение от пользователя
            metricsService.recordUserMessage();
            
        if (String.valueOf(chatId).startsWith("-100")) {
            Integer threadID = update.getMessage().getMessageThreadId();
            if (threadID != null) {
                UserDAO userDAO = new UserDAO();
                User userInThread = userDAO.findByIdMessage(threadID);
                if (userInThread != null) {
                    // Проверяем, есть ли активная сессия отклонения для этого пользователя
                    // Ищем сессию по ID подгруппы (threadID), а не по ID пользователя
                    ReviewRejectionSession rejectionSession = RedisSessionStore.getReviewRejectionSession((long) threadID);
                    if (rejectionSession != null) {
                        // Обрабатываем причину отклонения
                        handleReviewRejectionReason(update, userInThread);
                        metricsService.stopMessageProcessing(sample);
                        return;
                    }
                }
            }
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

        if(user == null){
            logicUI.sendStart(chatId, update);
            metricsService.stopMessageProcessing(sample);
            return;
        }

        if (user.isBlock()) {
            if (msg.equals("Техподдержка")) {
                String supportMention = AdminSettings.getInstance().getSupportMention();
                createTelegramBot.sendMessage(user,
                        "🆘 Техподдержка: " + supportMention + "\n\nОпишите вашу проблему, и мы обязательно поможем!");
            }
            return; // для всех заблокированных пользователей завершаем метод
        }

            // Проверяем состояние пользователя
        // String userState = RedisSessionStore.getState(chatId);

        // Проверяем, есть ли медиа и активная сессия покупки товара
        Message message = update.getMessage();
        boolean hasMedia = message.hasPhoto() || message.hasVideo() || message.hasDocument() || 
                          message.hasVideoNote() || message.hasVoice() || message.hasAudio() ||
                          message.hasSticker() || message.hasContact() || message.hasLocation() ||
                          message.hasPoll() || message.hasDice() || message.hasInvoice() ||
                          message.hasSuccessfulPayment() || message.hasPassportData();
        
        if (hasMedia) {
            // Сначала проверяем, не редактируется ли фотография товара
            String state = RedisSessionStore.getState(chatId);
            if (state != null && state.startsWith("edit_product_photo_")) {
                handlePhoto(update, user);
                metricsService.stopMessageProcessing(sample);
                return;
            }
            
            // Обработка медиа для состояний REVIEW_SUBMISSION (только для медиа)
            if (state != null && (state.equals("REVIEW_SUBMISSION") || state.equals("REVIEW_SUBMISSION_TEXT"))) {
                handleReviewMedia(update, user);
                metricsService.stopMessageProcessing(sample);
                return;
            }
            
            // Проверяем, не идет ли процесс получения кешбека
            if (state != null && state.startsWith("CASHBACK_REQUEST_")) {
                int purchaseId = Integer.parseInt(state.substring("CASHBACK_REQUEST_".length()));
                handleCashbackScreenshot(update, user, purchaseId);
                metricsService.stopMessageProcessing(sample);
                return;
            }
            
            // Проверяем, не идет ли процесс отправки скриншота кешбека
            if (state != null && state.startsWith("CASHBACK_SCREENSHOT_")) {
                int purchaseId = Integer.parseInt(state.substring("CASHBACK_SCREENSHOT_".length()));
                handleCashbackScreenshotWithCard(update, user, purchaseId);
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
                    // Медиа без активной сессии — подсказываем, куда зайти
                    Sent hintSender = new Sent();
                    String hint = "📸 Получено медиа без активного процесса.\n\n" +
                            "Чтобы отправить скриншоты/видео, пожалуйста, перейдите в соответствующий раздел:\n\n" +
                            "• Каталог товаров — для скриншотов поиска и доставки при покупке\n" +
                            "• Оставить отзыв — для фото/видео отзыва после получения товара\n" +
                            "• Получить кешбек — для скриншота опубликованного отзыва";
                    hintSender.sendMessage(user, hint);
                    metricsService.stopMessageProcessing(sample);
                    return;
                }
            }
        } else {
            // Нет медиа
        }

        // Обрабатываем состояния, которые не зависят от наличия медиа
        String textState = RedisSessionStore.getState(chatId);

        // ПРИОРИТЕТНАЯ ПРОВЕРКА: Обрабатываем кнопку "Назад" в первую очередь
        if(msg!=null && msg.equals("⬅️ Назад")) {
            // Возвращаемся в главное меню
            // Проверяем, есть ли активная сессия подачи отзыва
            String currentState = RedisSessionStore.getState(chatId);
            ReviewSubmissionSession reviewSession = RedisSessionStore.getReviewSubmissionSession(user.getIdUser());
            
            if ("REVIEW_SUBMISSION".equals(currentState) || "REVIEW_SUBMISSION_TEXT".equals(currentState) || reviewSession != null) {
                RedisSessionStore.removeReviewSubmissionSession(user.getIdUser());
                RedisSessionStore.removeState(chatId);
                
                Sent sent = new Sent();
                sent.sendMessage(user, "❌ Подача отзыва отменена. Вы вернулись в главное меню.");
            }
            
            // Отменяем бронь товара при выходе из процесса покупки
            cancelUserReservation(user, chatId);
            
            // Очищаем все остальные состояния сессии при возврате в главное меню
            RedisSessionStore.removeReviewSession(chatId);
            RedisSessionStore.removeProductSession(chatId);
            
            logicUI.sendMenu(user, null);
            metricsService.stopMessageProcessing(sample);
            return;
        }
        
        if (textState != null && textState.equals("REVIEW_SUBMISSION_TEXT")) {
            ReviewSubmissionSession session = RedisSessionStore.getReviewSubmissionSession(user.getIdUser());
            if (session != null && session.getStep() == ReviewSubmissionSession.Step.TEXT) {
                // Если шаг TEXT, обрабатываем как текст отзыва
                handleReviewTextSubmission(update, user);
                metricsService.stopMessageProcessing(sample);
                return;
            }
        }
        
        if (textState != null && (textState.equals("REVIEW_SUBMISSION") || textState.equals("REVIEW_SUBMISSION_TEXT"))) {
            ReviewSubmissionSession session = RedisSessionStore.getReviewSubmissionSession(user.getIdUser());
            if (session != null) {
                if (session.getStep() == ReviewSubmissionSession.Step.TEXT) {
                    // Если шаг TEXT, обрабатываем как текст отзыва
                    handleReviewTextSubmission(update, user);
                    metricsService.stopMessageProcessing(sample);
                    return;
                }
                if (session.getStep() == ReviewSubmissionSession.Step.MEDIA) {
                    // Если шаг MEDIA, обрабатываем как медиа
                    handleReviewMedia(update, user);
                    metricsService.stopMessageProcessing(sample);
                    return;
                }
            }
        }
        
        // Обработка ввода номера карты для кешбека
        if (textState != null && textState.startsWith("CASHBACK_CARD_INPUT_")) {
            int purchaseId = Integer.parseInt(textState.substring("CASHBACK_CARD_INPUT_".length()));
            handleCashbackCardInput(update, user, purchaseId);
            metricsService.stopMessageProcessing(sample);
            return;
        }
        
        // Обработка ввода username для блокировки пользователя
        if (textState != null && textState.equals("ADMIN_BLOCK_USER")) {
            handleBlockUserInput(update, user);
            metricsService.stopMessageProcessing(sample);
            return;
        }

        if(msg!=null){
            switch (msg) {
                case "/start" -> {
                    // Отменяем бронь товара при переходе в главное меню
                    cancelUserReservation(user, chatId);
                    logicUI.sendStart(chatId, update);
                }
               case "Админ меню" -> {
                    if(user.isAdmin()){
                        // Показываем админ-меню (обычное меню остается)
                        logicUI.showAdminMenu(user);
                        metricsService.recordAdminAction();
                    }
               }
                case "Каталог товаров" -> {
                    // Ограничение: не чаще 1 заказа в 14 дней (кроме администраторов)
                    if (!user.isAdmin()) {
                        PurchaseDAO purchaseDAO = new PurchaseDAO();
                        List<Purchase> purchases = purchaseDAO.findByUserId(user.getIdUser());
                        java.time.LocalDate lastOrderDate = null;
                        for (Purchase p : purchases) {
                            if (p.getDate() != null) {
                                if (lastOrderDate == null || p.getDate().isAfter(lastOrderDate)) {
                                    lastOrderDate = p.getDate();
                                }
                            }
                        }
                        if (lastOrderDate != null && lastOrderDate.isAfter(java.time.LocalDate.now().minusDays(14))) {
                            java.time.LocalDate nextAllowed = lastOrderDate.plusDays(14);
                            Sent sent = new Sent();
                            String msgText = "⏳ Вы можете заказывать товар не чаще чем раз в 14 дней.";
                            msgText += "\n\n📅 Последний заказ: " + formatLocalDate(lastOrderDate);
                            msgText += "\n🔓 Следующая доступная дата: " + formatLocalDate(nextAllowed);
                            sent.sendMessage(user, msgText);
                            break; // не показываем список товаров
                        }
                    }
                    // Отменяем бронь товара при переходе в каталог
                    cancelUserReservation(user, chatId);
                    logicUI.sendProducts(user);
                }
                case "Оставить отзыв" -> {
                    // Показываем товары пользователя для выбора отзыва
                    logicUI.showUserProductsForReview(user);
                }
                case "Техподдержка" -> {
                    String supportMention = AdminSettings.getInstance().getSupportMention();
                    createTelegramBot.sendMessage(user, "🆘 Техподдержка: " + supportMention + "\n\nОпишите вашу проблему, и мы обязательно поможем!");
                }
                case "Получить кешбек" -> {
                    // Показываем покупки пользователя для получения кешбека
                    logicUI.showUserPurchases(user);
                }
                case "Личный кабинет" -> {
                    // Показываем личный кабинет пользователя
                    logicUI.showUserCabinet(user);
                }
                // Обработка кнопки "⬅️ Назад" перенесена выше для приоритетной обработки
                case "Отмена добавления товара" -> {
                    if(user.isAdmin()){
                        RedisSessionStore.removeState(chatId);
                        logicUI.sendMenu(user, null);
                    }
                }
                case "Отмена покупки товара" -> {
                    // Отменяем бронь товара
                    cancelUserReservation(user, chatId);
                    
                    // Очищаем сессию и возвращаемся в каталог
                    RedisSessionStore.removeState(chatId);
                    RedisSessionStore.removeReviewSession(chatId);
                    logicUI.sendProducts(user);
                }
            }
        }


        String state = RedisSessionStore.getState(chatId);
        if(state!= null) {
            if ("PRODUCT_CREATION".equals(state)) {
                ProductCreationSession session = RedisSessionStore.getProductSession(chatId);
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
                ReviewRequestSession session = RedisSessionStore.getReviewSession(chatId);
                if (session != null && session.getProduct() != null) {
                    // Проверяем, существует ли еще бронь
                    ReservationService reservationService = ReservationService.getInstance();
                    if (!reservationService.isReservedByUser(user, session.getProduct())) {
                        // Бронь была отменена - очищаем сессию и уведомляем пользователя
                        RedisSessionStore.removeReviewSession(chatId);
                        RedisSessionStore.removeState(chatId);
                        createTelegramBot.sendMessage(user, 
                            "❌ Ваше бронирование было отменено из-за неактивности.\n\n" +
                            "Если вы хотите приобрести этот товар, пожалуйста, начните процесс заново.");
                        return;
                    }
                    
                    // Обновляем активность при любом действии в сессии
                    updateReservationActivity(user, session);
                    
                    switch (session.getStep()) {
                        case SEARCH_SCREENSHOT:
                            // Обработка скриншота поиска уже выполнена в handleSearchScreenshot
                            // Этот case не должен обрабатывать текстовые сообщения
                            break;

                        case ARTICUL_CHECK:
                            if(Objects.equals(msg, String.valueOf(session.getProduct().getArticul()))){
                                session.getRequest().setArticul(msg.trim());
                                session.setStep(ReviewRequestSession.Step.FULL_NAME);
                                RedisSessionStore.setReviewSession(chatId, session);
                                updateReservationActivity(user, session);
                                createTelegramBot.sendMessage(user,
                                        "Введите, пожалуйста, ваше полное ФИО без сокращений:");
                            }else {
                                updateReservationActivity(user, session);
                                createTelegramBot.sendMessage(user,"Введен неправильный артикль, повторите попытку");
                            }
                            break;

                        case FULL_NAME:
                            session.getRequest().setFullName(msg.trim());
                            session.setStep(ReviewRequestSession.Step.PHONE_NUMBER);
                            RedisSessionStore.setReviewSession(chatId, session);
                            updateReservationActivity(user, session);

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
                            RedisSessionStore.setReviewSession(chatId, session);
                            updateReservationActivity(user, session);

                            logicUI.sentBack(user, "Введите номер карты для получения кешбэка \n" +
                                    "(<strong>Только Сбербанк</strong>, другие банки не поддерживаются):", "Отмена покупки товара");

                            break;

                        case CARD_NUMBER:
                            // можно добавить лёгкую валидацию, если нужно
                            session.getRequest().setCardNumber(msg);
                            session.setStep(ReviewRequestSession.Step.PURCHASE_AMOUNT);
                            RedisSessionStore.setReviewSession(chatId, session);
                            updateReservationActivity(user, session);
                            createTelegramBot.sendMessage(user,
                                    "Укажите сумму покупки товара на Wildberries:");
                            break;

                        case PURCHASE_AMOUNT:
                            try {
                                String digits = msg.replaceAll("\\D", "");
                                if (digits.isEmpty()) throw new NumberFormatException("empty amount");
                                Integer sum = Integer.parseInt(digits);

                                session.getRequest().setPurchaseAmount(String.valueOf(sum));
                                session.setStep(ReviewRequestSession.Step.BANK_NAME);
                                RedisSessionStore.setReviewSession(chatId, session);
                                updateReservationActivity(user, session);
                                logicUI.sendMessageBank(user,
                                        "Укажите название банка, выпустившего карту:");
                                break;
                            } catch (NumberFormatException e) {
                                updateReservationActivity(user, session);
                                createTelegramBot.sendMessage(user, "Некорректная сумма. Пожалуйста, введите число (например: 1999).");
                                break;
                            }


                        case BANK_NAME:
                            String bankName = msg.trim().toLowerCase();
                            if (bankName.contains("сбер") || bankName.contains("sber")) {
                                session.getRequest().setBankName(msg.trim());
                                session.setStep(ReviewRequestSession.Step.DELIVERY_SCREENSHOT);
                                RedisSessionStore.setReviewSession(chatId, session);
                                updateReservationActivity(user, session);
                                logicUI.sentBack(user, "📦 Прикрепите скриншот раздела доставки с подтверждением заказа:", "Отмена покупки товара");
                            } else {
                                updateReservationActivity(user, session);
                                createTelegramBot.sendMessage(user, "❌ Принимаем только карты Сбербанка. Пожалуйста, введите \"Сбер\":");
                            }
                            break;

                        case DELIVERY_SCREENSHOT:
                            handleDeliveryScreenshot(update, user);
                            break;
                            
                        case COMPLETE:
                            // Процесс завершен, ничего не делаем
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
            
            if("REVIEW_SUBMISSION".equals(state)){
                // Обработка подачи отзыва
                handleReviewTextSubmission(update, user);
                return;
            }
            
            if("REVIEW_REJECTION".equals(state)){
                // Обработка ввода причины отказа отзыва
                handleReviewRejectionReason(update, user);
                return;
            }
            
            // Проверяем, есть ли активная сессия отмены покупки
            PurchaseCancellationSession cancellationSession = RedisSessionStore.getPurchaseCancellationSession(chatId);
            if (cancellationSession != null) {
                // Обрабатываем причину отмены покупки
                handlePurchaseCancellationReason(update, user, cancellationSession);
                return;
            }
            
            if(state.startsWith("CASHBACK_REQUEST_")){
                // Обработка запроса на получение кешбека
                int purchaseId = Integer.parseInt(state.substring("CASHBACK_REQUEST_".length()));
                handleCashbackScreenshot(update, user, purchaseId);
                return;
            }
        }
        } finally {
            // Завершаем измерение времени обработки
            metricsService.stopMessageProcessing(sample);
        }
    }

    /**
     * Отменить бронь товара для пользователя
     */
    private void cancelUserReservation(User user, long chatId) {
        ReviewRequestSession reviewRequestSession = RedisSessionStore.getReviewSession(chatId);
        if (reviewRequestSession != null && reviewRequestSession.getProduct() != null) {
            ReservationService reservationService = ReservationService.getInstance();
            reservationService.cancelReservation(user, reviewRequestSession.getProduct());
        }
    }
    
    /**
     * Обновить время последней активности пользователя в бронировании
     */
    private void updateReservationActivity(User user, ReviewRequestSession session) {
        if (session != null && session.getProduct() != null) {
            ReservationService reservationService = ReservationService.getInstance();
            reservationService.updateLastActivity(user, session.getProduct());
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
        
        // Проверяем, существует ли еще бронь
        if (session.getProduct() != null) {
            ReservationService reservationService = ReservationService.getInstance();
            if (!reservationService.isReservedByUser(user, session.getProduct())) {
                // Бронь была отменена - очищаем сессию и уведомляем пользователя
                RedisSessionStore.removeReviewSession(chatId);
                RedisSessionStore.removeState(chatId);
                createTelegramBot.sendMessage(user, 
                    "❌ Ваше бронирование было отменено из-за неактивности.\n\n" +
                    "Если вы хотите приобрести этот товар, пожалуйста, начните процесс заново.");
                return;
            }
        }
        
        if (message.getPhoto() == null || message.getPhoto().isEmpty()) {
            createTelegramBot.sendMessage(user, "Пожалуйста, приложите скриншот поиска товара картинкой.");
            return;
        }
        
        PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
        String fileId = photo.getFileId();
        
        // Проверяем, не отправлял ли пользователь уже этот скриншот
        if (session.getSearchScreenshotMessageId() != null) {
            createTelegramBot.sendMessage(user, "⚠️ Вы уже отправили скриншот поиска. Пожалуйста, перейдите к следующему шагу.");
            return;
        }
        
        // Сохраняем file_id и ID сообщения в сессии для последующей отправки
        session.setSearchScreenshotFileId(fileId);
        session.setSearchScreenshotMessageId(message.getMessageId());
        RedisSessionStore.setReviewSession(chatId, session);
        
        // Обновляем активность пользователя
        updateReservationActivity(user, session);
        
        // Отправляем пользователю сообщение о начале обработки
        createTelegramBot.sendMessage(user, "🔄 Обрабатываю скриншот поиска, пожалуйста подождите...");
        
        // Асинхронная обработка скриншота поиска

        AsyncService.processSearchScreenshotAsync(session, user, photo, fileId)
            .thenRun(() -> {
                // Обновляем активность после обработки скриншота
                updateReservationActivity(user, session);
                
                if (session.getProduct() != null) {
                    // Товар уже выбран - переходим к вводу артикула
                    session.setStep(ReviewRequestSession.Step.ARTICUL_CHECK);
                    RedisSessionStore.setReviewSession(chatId, session);
                    updateReservationActivity(user, session);
                    createTelegramBot.sendMessage(user, "✅ Скриншот поиска принят!\n\n🔢 Теперь введите артикул товара Wildberries для проверки:");
                } else {
                    // Товар не выбран - это означает, что процесс покупки товара не завершен
                    // Продолжаем процесс покупки товара

                    // Проверяем, есть ли активная сессия создания товара (только для админов)
                    ProductCreationSession productSession = RedisSessionStore.getProductSession(chatId);
                    if (productSession != null && productSession.getProduct() != null && user.isAdmin()) {
                        // Админ создает товар - завершаем процесс создания товара
                        productSession.setStep(ProductCreationSession.Step.PHOTO);
                        createTelegramBot.sendMessage(user, "✅ Скриншот поиска принят!\n\n📷 Теперь отправьте фотографию товара:");
                    } else {
                        // Обычный пользователь покупает товар - продолжаем процесс покупки
                        session.setStep(ReviewRequestSession.Step.ARTICUL_CHECK);
                        RedisSessionStore.setReviewSession(chatId, session);
                        updateReservationActivity(user, session);
                        createTelegramBot.sendMessage(user, "✅ Скриншот поиска принят!\n\n🔢 Теперь введите артикул товара Wildberries для проверки:");
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
        
        // Проверяем, существует ли еще бронь
        if (session.getProduct() != null) {
            ReservationService reservationService = ReservationService.getInstance();
            if (!reservationService.isReservedByUser(user, session.getProduct())) {
                // Бронь была отменена - очищаем сессию и уведомляем пользователя
                RedisSessionStore.removeReviewSession(chatId);
                RedisSessionStore.removeState(chatId);
                createTelegramBot.sendMessage(user, 
                    "❌ Ваше бронирование было отменено из-за неактивности.\n\n" +
                    "Если вы хотите приобрести этот товар, пожалуйста, начните процесс заново.");
                return;
            }
        }
        
        if (message.getPhoto() == null || message.getPhoto().isEmpty()) {
            createTelegramBot.sendMessage(user, "Пожалуйста, приложите скриншот раздела доставки картинкой.");
            return;
        }
        
        PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
        String fileId = photo.getFileId();
        
        // Проверяем, не отправлял ли пользователь уже этот скриншот
        if (session.getDeliveryScreenshotMessageId() != null) {
            createTelegramBot.sendMessage(user, "⚠️ Вы уже отправили скриншот доставки. Пожалуйста, перейдите к следующему шагу.");
            return;
        }
        
        // Сохраняем file_id и ID сообщения в сессии для последующей отправки
        session.setDeliveryScreenshotFileId(fileId);
        session.setDeliveryScreenshotMessageId(message.getMessageId());

        RedisSessionStore.setReviewSession(chatId, session);
        
        // Обновляем активность пользователя
        updateReservationActivity(user, session);
        
        // Отправляем пользователю сообщение о начале обработки
        createTelegramBot.sendMessage(user, "🔄 Обрабатываю скриншот доставки, пожалуйста подождите...");
        

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

                // Завершаем бронирование, чтобы убрать пользователя из мониторинга неактивности
                if (session.getProduct() != null) {
                    ReservationService.getInstance().completeReservation(user, session.getProduct());
                }

                LogicUI logicUI = new LogicUI();
                logicUI.sendMenu(user, finishText);
                RedisSessionStore.removeReviewSession(chatId);
                
                // Уведомление в группу с двумя фотографиями
                String text = "";
                if (session.getProduct() != null && session.getRequest() != null) {
                    text = "Пользователь купил товар \"" + session.getProduct().getProductName() + "\"\n"
                            + "ФИО: " + session.getRequest().getFullName() + "\n"
                            + "Номер телефона: <code>" + session.getRequest().getPhoneNumber() + "</code>\n"
                            + "Банк: " + session.getRequest().getBankName() + "\n"
                            + "Реквизиты: <code>" + session.getRequest().getCardNumber() + "</code>\n"
                            + "Стоимость для пользователя: <code>" + session.getRequest().getPurchaseAmount() + "</code>\n";
                } else {
                    text = "Пользователь завершил покупку товара";
                }
                
                String searchFileId = session.getSearchScreenshotFileId();
                String deliveryFileId = session.getDeliveryScreenshotFileId();
                
                Long groupMessageId = null;
                if(user.getId_message() == 0){
                    TelegramBot telegramBot = new TelegramBot();
                    List<Long> messageIdAndGroup = telegramBot.createTopic(update);
                    user.setId_message(Math.toIntExact(messageIdAndGroup.getLast()));
                    UserDAO userDAO = new UserDAO();
                    userDAO.update(user);
                }

                if (searchFileId != null && deliveryFileId != null) {
                    groupMessageId = createTelegramBot.sendTwoPhotosToGroup(user, text, searchFileId, deliveryFileId);
                } else {
                    System.err.println("⚠️ Missing screenshot file IDs: search=" + searchFileId + ", delivery=" + deliveryFileId);
                }
                
                // Сохраняем ID сообщения в сессии для последующего сохранения в БД
                session.setGroupMessageId(groupMessageId);
                
                // Обновляем orderMessageId в созданной покупке
                if (groupMessageId != null && session.getPurchaseId() != null) {
                    PurchaseDAO purchaseDAO = new PurchaseDAO();
                    Purchase purchase = purchaseDAO.findById(session.getPurchaseId());
                    
                    if (purchase != null) {
                        purchase.setOrderMessageId(groupMessageId);
                        purchase.setGroupMessageId(groupMessageId);
                        purchaseDAO.update(purchase);
                        
                    } else {
                        System.err.println("❌ Purchase not found with ID: " + session.getPurchaseId());
                    }
                }
                
                // Медиа уже отправлены в sendTwoPhotosToGroup выше
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
        
        // Проверяем состояние пользователя
        String state = RedisSessionStore.getState(chatId);
        
        // Если пользователь в процессе подачи отзыва, обрабатываем фото как медиа для отзыва
        if (state != null && (state.equals("REVIEW_SUBMISSION") || state.equals("REVIEW_SUBMISSION_TEXT"))) {
            handleReviewMedia(update, user);
            return;
        }
        
        // Проверяем, не редактируется ли фотография товара
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
                        
                        // Отправляем товар в группу с темами
                        try {
                            Long groupMessageId = sendProductToGroup(session.getProduct(), filePath);
                            session.getProduct().setGroupMessageId(groupMessageId);
                        } catch (Exception e) {
                            System.err.println("❌ Error sending product to group: " + e.getMessage());
                            e.printStackTrace();
                        }
                        
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
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

        UserDAO userDAO = new UserDAO();
        User user = userDAO.findById(chatId);
        if (user.isBlock()) return;
        // Если это групповой чат и пользователь не найден, проверяем callback data для обработки отзывов
        if (user == null) {
            // Обрабатываем кнопки подтверждения/отклонения отзывов в группе
            if (data.startsWith("approve_review_") || data.startsWith("reject_review_")) {
                handleGroupReviewCallback(update, data);
                return;
            }
            
            // Обрабатываем кнопку "Оплачено" для кешбека
            if (data.startsWith("cashback_paid_")) {
                handleCashbackPaidCallback(update, data);
                return;
            }
            
            return;
        }
        
        // Очищаем сессии только для пользователей (не для групповых чатов)
        // ВАЖНО: не очищаем все сессии здесь, чтобы не сбрасывать активные процессы (покупка/отзыв/кешбек)
        // Очистка выполняется точечно в обработчиках навигации/отмены (back_to_menu, Exit_Product, "Отмена покупки товара" и т.п.)
        

        // Обработка админ-интерфейса
        if (data.equals("admin_menu") || data.equals("admin_products") || data.equals("admin_stats") || 
            data.equals("admin_settings") || data.equals("admin_add_product") || data.equals("admin_user_management") ||
            data.equals("admin_add_admin") || data.equals("admin_change_support") || data.equals("admin_back_to_menu") ||
            data.equals("admin_back_to_main_menu") || data.equals("admin_back_to_admin_menu") || data.equals("admin_back_to_products") ||
            data.equals("admin_block_user") ||
            data.startsWith("admin_product_") || data.startsWith("admin_user_") || data.startsWith("admin_back_to_purchases_") ||
            data.startsWith("admin_edit_product_") || data.startsWith("admin_edit_product_name_") ||
            data.startsWith("admin_edit_product_articul_") || data.startsWith("admin_edit_product_cashback_") ||
            data.startsWith("admin_edit_product_query_") || data.startsWith("admin_edit_product_conditions_") ||
            data.startsWith("admin_edit_product_photo_") || data.startsWith("admin_edit_product_visibility_") ||
            data.startsWith("admin_view_stage_")) {
            handleCallbackQuery(update, user);
            return;
        }

        if(data.startsWith("product_")){
            // Каталог товаров всегда работает в пользовательском режиме
            String[] parts = data.split(":");
            ProductDAO productDAO = new ProductDAO();
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
                        ProductCreationSession session = new ProductCreationSession();
                        RedisSessionStore.setProductSession(chatId, session);
                        RedisSessionStore.setState(chatId, "PRODUCT_CREATION");
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
                case "back_to_menu":{
                    // Возвращаемся в главное меню
                    // Отменяем бронь товара при выходе из процесса покупки
                    cancelUserReservation(user, chatId);
                    
                    // Удаляем текущее сообщение с inline кнопками
                    safeDeleteMessage(user.getIdUser(), messageId);
                    // Показываем главное меню
                    logicUI.sendMenu(user, null);
                    break;
                }
                case "Exit_Product":{
                    // Просто показываем каталог товаров
                    logicUI.sendProducts(user);
                    break;
                }
                case "product_sold_out":{
                    Sent sent = new Sent();
                    sent.sendMessage(user, "❌ К сожалению, все товары в этой акции уже выкуплены. " +
                                       "Попробуйте выбрать другой товар из каталога.");
                    break;
                }
                case "product_reserved":{
                    Sent sent = new Sent();
                    sent.sendMessage(user, "⏳ Этот товар уже забронирован другим пользователем. Попробуйте позже.");
                    break;
                }
                case "order_rate_limited":{
                    // Сообщение при ограничении частоты заказов (ранее, на выборе товара)
                    PurchaseDAO purchaseDAO = new PurchaseDAO();
                    List<Purchase> purchases = purchaseDAO.findByUserId(user.getIdUser());
                    java.time.LocalDate lastOrderDate = null;
                    for (Purchase p : purchases) {
                        if (p.getDate() != null) {
                            if (lastOrderDate == null || p.getDate().isAfter(lastOrderDate)) {
                                lastOrderDate = p.getDate();
                            }
                        }
                    }
                    java.time.LocalDate nextAllowed = lastOrderDate != null ? lastOrderDate.plusDays(14) : null;
                    Sent sent = new Sent();
                    String msgText = "⏳ Вы можете заказывать товар не чаще чем раз в 14 дней.";
                    if (lastOrderDate != null) {
                        msgText += "\n\n📅 Последний заказ: " + formatLocalDate(lastOrderDate);
                    }
                    if (nextAllowed != null) {
                        msgText += "\n🔓 Следующая доступная дата: " + formatLocalDate(nextAllowed);
                    }
                    sent.sendMessage(user, msgText);
                    break;
                }
                case "buy_product":{
                    ProductDAO productDAO = new ProductDAO();
                    // Для buy_product messageID содержит ID товара
                    Product product = productDAO.findById(Integer.parseInt(messageID));
                    
                    if (product == null) {
                        Sent sent = new Sent();
                        sent.sendMessage(user, "❌ Товар не найден");
                        break;
                    }
                    
                    // Если уже есть активная заявка по этому товару — не начинаем заново
                    ReviewRequestSession existing = RedisSessionStore.getReviewSession(chatId);
                    if (existing != null && existing.getProduct() != null &&
                        existing.getProduct().getIdProduct() == product.getIdProduct() &&
                        existing.getStep() != ReviewRequestSession.Step.COMPLETE) {
                        Sent sent = new Sent();
                        sent.sendMessage(user, "⏳ Вы уже заполняете заявку на покупку этого товара. Продолжайте в текущем диалоге.");
                        break;
                    }
                    
                    // Ограничение: не чаще 1 заказа в 14 дней (кроме администраторов)
                    if (!user.isAdmin()) {
                        PurchaseDAO purchaseDAO = new PurchaseDAO();
                        List<Purchase> recentPurchases = purchaseDAO.findByUserId(user.getIdUser());
                        java.time.LocalDate now = java.time.LocalDate.now();
                        java.time.LocalDate lastOrderDate = null;
                        for (Purchase p : recentPurchases) {
                            if (p.getDate() != null) {
                                if (lastOrderDate == null || p.getDate().isAfter(lastOrderDate)) {
                                    lastOrderDate = p.getDate();
                                }
                            }
                        }
                        if (lastOrderDate != null && lastOrderDate.isAfter(now.minusDays(14))) {
                            java.time.LocalDate nextAllowed = lastOrderDate.plusDays(14);
                            Sent sent = new Sent();
                            sent.sendMessage(user,
                                "⏳ Вы можете заказывать товар не чаще чем раз в 14 дней.\n\n" +
                                "📅 Последний заказ: " + formatLocalDate(lastOrderDate) + "\n" +
                                "🔓 Следующая доступная дата: " + formatLocalDate(nextAllowed));
                            break;
                        }
                    }

                    // Проверяем, не покупал ли пользователь этот товар ранее
                    PurchaseDAO purchaseDAO = new PurchaseDAO();
                    List<Purchase> userPurchases = purchaseDAO.findByUserId(user.getIdUser());
                    boolean hasPurchased = false;
                    for (Purchase purchase : userPurchases) {
                        if (purchase.getProduct() != null && purchase.getProduct().getIdProduct() == product.getIdProduct()) {
                            hasPurchased = true;
                            break;
                        }
                    }
                    
                    if (hasPurchased) {
                        Sent sent = new Sent();
                        sent.sendMessage(user, "❌ Вы уже покупали этот товар. Выберите другой товар из каталога.");
                        break;
                    }
                    
                    // Пытаемся забронировать товар
                    ReservationService reservationService = ReservationService.getInstance();
                    boolean reserved = reservationService.reserveProduct(user, product);
                    
                    if (!reserved) {
                        // Если бронь уже за этим пользователем — показываем корректное сообщение
                        if (reservationService.isReservedByUser(user, product)) {
                            Sent sent = new Sent();
                            sent.sendMessage(user, "⏳ Вы уже заполняете заявку на покупку этого товара. Продолжайте в текущем диалоге.");
                            break;
                        }
                        Sent sent = new Sent();
                        sent.sendMessage(user, "❌ К сожалению, все товары в этой акции уже выкуплены или забронированы. " +
                                           "Попробуйте выбрать другой товар.");
                        break;
                    }
                    
                    // Товар успешно забронирован
                    ReviewRequestSession session = new ReviewRequestSession();
                    session.setProduct(product);
                    session.setStep(ReviewRequestSession.Step.SEARCH_SCREENSHOT);
                    RedisSessionStore.setReviewSession(chatId, session);

                    RedisSessionStore.setState(chatId, "REVIEW_REQUEST");
                    
                    // Регистрируем запрос на покупку
                    MetricsService.getInstance().recordPurchaseRequest();

                    String reservationMessage = "✅ Товар забронирован за вами на 30 минут!\n\n" +
                            "📸 Прикрепите скриншот поиска товара на Wildberries с поисковой строкой и найденным товаром:";
                    
                    logicUI.sentBack(user, reservationMessage, "Отмена покупки товара");

                    break;
                }
                case "cashback_purchase":{
                    // Обработка выбора покупки для получения кешбека
                    int purchaseId = Integer.parseInt(messageID);
                    handleCashbackRequest(user, purchaseId);
                    break;
                }
                case "review_product":{
                    // Обработка выбора товара для оставления отзыва
                    int purchaseId = Integer.parseInt(messageID);
                    handleReviewProductSelection(user, purchaseId);
                    break;
                }
                case "approve_review":{
                    // Обработка подтверждения отзыва администратором
                    int purchaseId = Integer.parseInt(messageID);
                    handleReviewApproval(user, purchaseId, true);
                    break;
                }
                case "reject_review":{
                    // Обработка отклонения отзыва администратором
                    int purchaseId = Integer.parseInt(messageID);
                    handleReviewRejection(user, purchaseId);
                    break;
                }
            }
        }
    }

    private String formatLocalDate(java.time.LocalDate date) {
        if (date == null) return "Не указано";
        return String.format("%02d.%02d.%02d", date.getDayOfMonth(), date.getMonthValue(), date.getYear() % 100);
    }

    /**
     * Обработка callback-запросов для админ-интерфейса
     */
    private void handleCallbackQuery(Update update, User user) {
        if (user.isBlock()) return;
        String callbackData = update.getCallbackQuery().getData();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();

        if (!user.isAdmin()) {
            return; // Только админы могут использовать админ-интерфейс
        }
        LogicUI logicUI = new LogicUI();

        switch (callbackData) {
            case "admin_menu" -> {
                // Удаляем текущее сообщение и показываем админ-меню
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showAdminMenu(user);
            }
            case "admin_products" -> {
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showProductsList(user);
            }
            case "admin_stats" -> {
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showStats(user);
            }
            case "admin_settings" -> {
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showSettings(user);
            }
            case "admin_change_support" -> {
                changeSupportSettings(user, messageId);
            }
            case "admin_add_product" -> {
                // Устанавливаем состояние для добавления товара
                RedisSessionStore.setState(user.getIdUser(), "PRODUCT_CREATION");
                // Создаем сессию для создания товара
                ProductCreationSession session = new ProductCreationSession();
                RedisSessionStore.setProductSession(user.getIdUser(), session);
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showAddProductMenu(user);
            }
            case "admin_user_management" -> {
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showUserManagementMenu(user);
            }
            case "admin_block_user" -> {
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showBlockUserInterface(user);
                // Устанавливаем состояние для ввода username
                RedisSessionStore.setState(user.getIdUser(), "ADMIN_BLOCK_USER");
            }
            case "admin_add_admin" -> {
                changeAdminSettings(user, messageId);
            }
            case "admin_back_to_menu" -> {
                // Удаляем текущее сообщение и показываем админ-меню
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showAdminMenu(user);
            }
            case "admin_back_to_main_menu" -> {
                // Удаляем админ-сообщение и показываем обычное меню
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.sendMenu(user, null);
            }
            case "admin_back_to_admin_menu" -> {
                // Удаляем текущее сообщение и показываем админ-меню
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showAdminMenu(user);
            }
            case "admin_back_to_products" -> {
                safeDeleteMessage(user.getIdUser(), messageId);
                logicUI.showProductsList(user);
            }
            default -> {
                if (callbackData.startsWith("admin_product_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_product_".length()));

                    TelegramBot telegramBot = new TelegramBot();
                    telegramBot.deleteMessage(user.getIdUser(), messageId);
                    logicUI.showProductPurchases(user, productId);

                } else if (callbackData.startsWith("admin_user_")) {
                    int purchaseId = Integer.parseInt(callbackData.substring("admin_user_".length()));
                    TelegramBot telegramBot = new TelegramBot();
                    telegramBot.deleteMessage(user.getIdUser(), messageId);
                    logicUI.showPurchaseDetails(user, purchaseId);
                } else if (callbackData.startsWith("admin_back_to_purchases_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_back_to_purchases_".length()));
                    TelegramBot telegramBot = new TelegramBot();
                    telegramBot.deleteMessage(user.getIdUser(), messageId);
                    logicUI.showProductPurchases(user, productId);
                } else if (callbackData.startsWith("admin_cancel_purchase_")) {
                    int purchaseId = Integer.parseInt(callbackData.substring("admin_cancel_purchase_".length()));
                    TelegramBot telegramBot = new TelegramBot();
                    telegramBot.deleteMessage(user.getIdUser(), messageId);
                    handleCancelPurchase(user, purchaseId);
                } else if (callbackData.startsWith("admin_edit_product_name_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_name_".length()));
                    startProductFieldEdit(user, productId, "name");
                } else if (callbackData.startsWith("admin_edit_product_articul_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_articul_".length()));
                    startProductFieldEdit(user, productId, "articul");
                } else if (callbackData.startsWith("admin_edit_product_cashback_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_cashback_".length()));
                    startProductFieldEdit(user, productId, "cashback");
                } else if (callbackData.startsWith("admin_edit_product_query_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_query_".length()));
                    startProductFieldEdit(user, productId, "query");
                } else if (callbackData.startsWith("admin_edit_product_participants_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_participants_".length()));
                    startProductFieldEdit(user, productId, "participants");
                } else if (callbackData.startsWith("admin_edit_product_conditions_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_conditions_".length()));
                    startProductFieldEdit(user, productId, "conditions");
                } else if (callbackData.startsWith("admin_edit_product_photo_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_photo_".length()));
                    startProductPhotoEdit(user, productId);
                } else if (callbackData.startsWith("admin_edit_product_visibility_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_visibility_".length()));
                    toggleProductVisibility(user, productId);
                } else if (callbackData.startsWith("admin_edit_product_")) {
                    int productId = Integer.parseInt(callbackData.substring("admin_edit_product_".length()));
                    TelegramBot telegramBot = new TelegramBot();
                    telegramBot.deleteMessage(user.getIdUser(), messageId);
                    logicUI.showEditProductMenu(user, productId);
                } else if (callbackData.startsWith("admin_view_stage_")) {
                    handleViewStage(user, callbackData);
                } else if (callbackData.equals("no_message_available")) {
                    Sent sent = new Sent();
                    sent.sendMessage(user, "ℹ️ Ссылка на сообщение недоступна. Этап выполнен, но сообщение не найдено.");
                }
            }
        }
    }

    /**
     * Обработка отмены покупки администратором
     */
    private void handleCancelPurchase(User admin, int purchaseId) {
        try {
            PurchaseDAO purchaseDAO = new PurchaseDAO();
            Purchase purchase = purchaseDAO.findById(purchaseId);
            
            if (purchase == null) {
                Sent sent = new Sent();
                sent.sendMessage(admin, "❌ Покупка не найдена");
                return;
            }
            
            // Проверяем, что покупка не завершена полностью
            if (purchase.getPurchaseStage() >= 4) {
                Sent sent = new Sent();
                sent.sendMessage(admin, "❌ Нельзя отменить завершенную покупку");
                return;
            }
            
            // Создаем сессию для ввода причины отмены
            PurchaseCancellationSession cancellationSession = new PurchaseCancellationSession(admin, purchase);
            RedisSessionStore.savePurchaseCancellationSession(admin.getIdUser(), cancellationSession);
            
            // Запрашиваем причину отмены
            String message = "❌ Отмена покупки\n\n" +
                           "👤 Пользователь: @" + (purchase.getUser() != null ? purchase.getUser().getUsername() : "Unknown") + "\n" +
                           "📦 Товар: " + purchase.getProduct().getProductName() + "\n" +
                           "📅 Дата: " + purchase.getDate() + "\n\n" +
                           "📝 Пожалуйста, укажите причину отмены покупки:";
            
            Sent sent = new Sent();
            sent.sendMessage(admin, message);

        } catch (Exception e) {
            System.err.println("❌ Error requesting cancellation reason for purchase " + purchaseId + ": " + e.getMessage());
            e.printStackTrace();
            
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Ошибка при запросе причины отмены: " + e.getMessage());
        }
    }

    /**
     * Обработка ввода причины отмены покупки
     */
    private void handlePurchaseCancellationReason(Update update, User admin, PurchaseCancellationSession cancellationSession) {
        try {
            String reason = update.getMessage().getText();
            cancellationSession.setReason(reason);
            cancellationSession.setReasonEntered(true);
            
            // Сохраняем обновленную сессию
            RedisSessionStore.savePurchaseCancellationSession(admin.getIdUser(), cancellationSession);
            
            // Выполняем отмену покупки с указанной причиной
            processPurchaseCancellation(admin, cancellationSession);
            
            // Удаляем сессию
            RedisSessionStore.removePurchaseCancellationSession(admin.getIdUser());
            
        } catch (Exception e) {
            System.err.println("❌ Error processing purchase cancellation reason: " + e.getMessage());
            e.printStackTrace();
            
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Ошибка при обработке причины отмены: " + e.getMessage());
            
            // Удаляем сессию в случае ошибки
            RedisSessionStore.removePurchaseCancellationSession(admin.getIdUser());
        }
    }
    
    /**
     * Выполнение отмены покупки с указанной причиной
     */
    private void processPurchaseCancellation(User admin, PurchaseCancellationSession cancellationSession) {
        try {
            Purchase purchase = cancellationSession.getPurchase();
            String reason = cancellationSession.getReason();
            
            // Уменьшаем количество участников товара
            Product product = purchase.getProduct();
            if (product != null) {
                ReservationManager.decrementProductParticipants(product.getIdProduct());
            }
            
            // Обновляем статус покупки на отмененную (статус -1)
            purchase.setPurchaseStage(-1);
            PurchaseDAO purchaseDAO = new PurchaseDAO();
            purchaseDAO.update(purchase);
            
            // Отправляем уведомление пользователю в его тему в группе
            User purchaseUser = purchase.getUser();
            if (purchaseUser != null) {
                try {
                    ResourceBundle rb = ResourceBundle.getBundle("app");
                    long groupID = Long.parseLong(rb.getString("tg.group"));
                    int userSubgroupId = purchaseUser.getId_message();
                    
                    String message = "❌ Ваша покупка была отменена администратором\n\n" +
                                   "📦 Товар: " + product.getProductName() + "\n" +
                                   "📅 Дата: " + purchase.getDate() + "\n" +
                                   "📝 Причина отмены: " + reason + "\n\n" +
                                   "Если у вас есть вопросы, обратитесь к администратору.";
                    
                    Sent sent = new Sent();
                    sent.sendMessageUser(groupID, userSubgroupId, message);
                    
                } catch (Exception e) {
                    System.err.println("❌ Error sending notification to group topic: " + e.getMessage());
                    // Fallback: отправляем в личные сообщения
                    String message = "❌ Ваша покупка была отменена администратором\n\n" +
                                   "📦 Товар: " + product.getProductName() + "\n" +
                                   "📅 Дата: " + purchase.getDate() + "\n" +
                                   "📝 Причина отмены: " + reason + "\n\n" +
                                   "Если у вас есть вопросы, обратитесь к администратору.";
                    
                    Sent sent = new Sent();
                    sent.sendMessage(purchaseUser, message);
                    
                }
            }
            
            // Отправляем подтверждение администратору
            String adminMessage = "✅ Покупка успешно отменена\n\n" +
                                "👤 Пользователь: @" + (purchaseUser != null ? purchaseUser.getUsername() : "Unknown") + "\n" +
                                "📦 Товар: " + product.getProductName() + "\n" +
                                "📅 Дата: " + purchase.getDate() + "\n" +
                                "📝 Причина: " + reason + "\n\n" +
                                "Количество участников товара уменьшено на 1";
            
            Sent sent = new Sent();
            sent.sendMessage(admin, adminMessage);
        } catch (Exception e) {
            System.err.println("❌ Error processing purchase cancellation: " + e.getMessage());
            e.printStackTrace();
            
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Ошибка при отмене покупки: " + e.getMessage());
        }
    }

    /**
     * Экранирование HTML-символов для безопасной отправки
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }

    /**
     * Отправка товара в группу с темами (в формате для пользователя)
     */
    private Long sendProductToGroup(Product product, String photoPath) throws Exception {
        try {
            ResourceBundle rb = ResourceBundle.getBundle("app");
            long groupID = Long.parseLong(rb.getString("tg.group"));
            
            // Формируем текст сообщения точно как для пользователя
            String productText = "Вы выбрали товар: " + escapeHtml(product.getProductName()) + " \n" +
                    "\n" +
                    "Кешбек " + product.getCashbackPercentage() + "% после публикации отзыва 🙏\n" +
                    "Принимаем только карты Сбера (Россия)\n" +
                    "\n" +
                    "Условия участия:\n" +
                    "- Подпишитесь на наш канал @adaptix_focus 😉\n" +
                    "- Сделайте скриншот поисковой строки (мы его можем запросить)\n" +
                    "- Найдите наш товар по запросу \"" + escapeHtml(product.getKeyQuery()) + "\" 🔎\n" +
                    "- Закажите товар и заполните заявку\n" +
                    "- Заберите товар с ПВЗ в течении 3 дней👍\n" +
                    "- Согласуйте свой отзыв с фотографиями в нашем боте\n" +
                    "- Оставьте свой отзыв и заполните форму получения кешбека (только когда отзыв опубликовали)\n" +
                    "- Кешбек ВЫПЛАЧИВАЕТСЯ В ПН И ПТ💳\n" +
                    "\n" +
                    "Важно:\n" +
                    "- Участвовать можно только в одной раздаче на один аккаунт не чаще чем раз в две недели\n" +
                    "- ФИО в заказе должно совпадать с номером карты👤\n" +
                    "- Качественные фотографии в отзыве обязательны📸\n" +
                    "- Отзыв нужно оставить не позднее 3 дней после забора товара с ПВЗ 📅\n" +
                    "- Желающие возвращать товар на ПВЗ не могут участвовать в акции 🚫";
            
            // Отправляем сообщение с фотографией в группу через TelegramBot
            TelegramBot telegramBot = new TelegramBot();
            Long messageId = telegramBot.sendPhotoToGroup(groupID, photoPath, productText);
            return messageId;
            
        } catch (Exception e) {
            System.err.println("❌ Error sending product to group: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Обработка callback query для подтверждения/отклонения отзывов в группе
     */
    private void handleGroupReviewCallback(Update update, String data) {
        try {
            if (data.startsWith("approve_review_")) {
                // Извлекаем ID покупки из callback data
                int purchaseId = Integer.parseInt(data.substring("approve_review_".length()));
                
                // Находим покупку
                PurchaseDAO purchaseDAO = new PurchaseDAO();
                Purchase purchase = purchaseDAO.findById(purchaseId);
                
                if (purchase != null) {
                    // Получаем администратора, который нажал кнопку
                    Long adminChatId = update.getCallbackQuery().getFrom().getId();
                    UserDAO userDAO = new UserDAO();
                    User admin = userDAO.findById(adminChatId);
                    
                    if (admin != null && admin.isAdmin()) {
                        // Подтверждаем отзыв - передаем администратора
                        handleReviewApproval(admin, purchaseId, true);
                        
                        // Отвечаем на callback query
                        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
                        answerCallbackQuery.setCallbackQueryId(update.getCallbackQuery().getId());
                        answerCallbackQuery.setText("✅ Отзыв подтвержден!");
                        answerCallbackQuery.setShowAlert(false);
                        
                        Sent sent = new Sent();
                        sent.answerCallbackQuery(answerCallbackQuery);
                    } else {
                        System.err.println("❌ Admin not found or not authorized: " + adminChatId);
                    }
                }
                
            } else if (data.startsWith("reject_review_")) {
                // Извлекаем ID покупки из callback data
                int purchaseId = Integer.parseInt(data.substring("reject_review_".length()));
                
                // Находим покупку
                PurchaseDAO purchaseDAO = new PurchaseDAO();
                Purchase purchase = purchaseDAO.findById(purchaseId);
                
                if (purchase != null) {
                    // Получаем администратора, который нажал кнопку
                    Long adminChatId = update.getCallbackQuery().getFrom().getId();
                    UserDAO userDAO = new UserDAO();
                    User admin = userDAO.findById(adminChatId);
                    
                    if (admin != null && admin.isAdmin()) {
                        // Инициируем процесс отклонения - передаем администратора
                        handleReviewRejection(admin, purchaseId);
                        
                        // Отвечаем на callback query
                        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
                        answerCallbackQuery.setCallbackQueryId(update.getCallbackQuery().getId());
                        answerCallbackQuery.setText("❌ Отзыв отклонен. Укажите причину.");
                        answerCallbackQuery.setShowAlert(false);
                        
                        Sent sent = new Sent();
                        sent.answerCallbackQuery(answerCallbackQuery);
                    } else {
                        System.err.println("❌ Admin not found or not authorized: " + adminChatId);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error processing group review callback: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Обработка callback query для кнопки "Оплачено" кешбека
     */
    private void handleCashbackPaidCallback(Update update, String data) {

        try {
            if (data.startsWith("cashback_paid_")) {
                // Извлекаем ID покупки из callback data
                int purchaseId = Integer.parseInt(data.substring("cashback_paid_".length()));
                
                // Находим покупку
                PurchaseDAO purchaseDAO = new PurchaseDAO();
                Purchase purchase = purchaseDAO.findById(purchaseId);
                
                if (purchase != null) {
                    // Обновляем статус покупки на "кешбек выплачен"
                    purchase.setPurchaseStage(4); // Этап: кешбек выплачен
                    purchaseDAO.update(purchase);
                    
                    // Отправляем уведомление пользователю
                    User reviewUser = purchase.getUser();
                    String cardInfo = "";
                    if (purchase.getCardNumber() != null && !purchase.getCardNumber().isEmpty()) {
                        cardInfo = "💳 Карта: <code>" + purchase.getCardNumber() + "</code>\n";
                    }
                    
                    String message = "🎉 Кешбек выплачен!\n\n" +
                            "📦 Товар: " + purchase.getProduct().getProductName() + "\n" +
                            "💰 Размер кешбека: " + purchase.getProduct().getCashbackPercentage() + "%\n" +
                            cardInfo + "\n" +
                            "✅ Кешбек переведен на указанную карту.\n" +
                            "Спасибо за участие в нашей программе! 🙏";
                    
                    Sent sent = new Sent();
                    sent.sendMessage(reviewUser, message);
                    
                    // Отвечаем на callback query
                    AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
                    answerCallbackQuery.setCallbackQueryId(update.getCallbackQuery().getId());
                    answerCallbackQuery.setText("✅ Кешбек отмечен как выплаченный!");
                    answerCallbackQuery.setShowAlert(false);
                    
                    sent.answerCallbackQuery(answerCallbackQuery);
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error processing cashback paid callback: " + e.getMessage());
            e.printStackTrace();
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
        RedisSessionStore.setState(user.getIdUser(), "REVIEW_SUBMISSION_TEXT");

        // Проверяем, что сессия действительно сохранилась
        ReviewSubmissionSession checkSession = RedisSessionStore.getReviewSubmissionSession(user.getIdUser());
        
        // Показываем инструкцию
        showReviewInstructions(user);
    }
    
    /**
     * Показать инструкцию для отзыва
     */
    private void showReviewInstructions(User user) {
        String instructions = "⭐ Инструкция по оставлению отзыва:\n\n" +
                            "Для получения кешбека необходимо:\n\n" +
                            "📸 Отправить 3 фотографии товара\n" +
                            "🎥 Отправить 1 видео:\n" +
                            "• Видео: Демонстрация товара (до 1 минуты)\n\n" +
                            "📝 Написать отзыв на Wildberries с этими материалами\n\n";
        
        // Устанавливаем шаг TEXT для начала процесса
        ReviewSubmissionSession session = RedisSessionStore.getReviewSubmissionSession(user.getIdUser());
        if (session != null) {
            session.setStep(ReviewSubmissionSession.Step.TEXT);
            RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);

            // Проверяем, что сессия действительно сохранилась
            ReviewSubmissionSession checkSession = RedisSessionStore.getReviewSubmissionSession(user.getIdUser());
        }
        
        Sent sent = new Sent();
        sent.sendMessage(user, instructions);
    }
    
    /**
     * Проверить, является ли фото на самом деле видео (большие размеры могут указывать на видео)
     * ОБНОВЛЕНО: Более консервативная логика, чтобы избежать ложных срабатываний
     */
    private boolean isLikelyVideoPhoto(java.util.List<org.telegram.telegrambots.meta.api.objects.PhotoSize> photos) {
        if (photos == null || photos.isEmpty()) return false;
        
        // Берем самое большое фото
        org.telegram.telegrambots.meta.api.objects.PhotoSize largestPhoto = photos.get(photos.size() - 1);

        int width = largestPhoto.getWidth();
        int height = largestPhoto.getHeight();
        
        // БОЛЕЕ КОНСЕРВАТИВНЫЕ КРИТЕРИИ: Только очень специфичные случаи
        // Проверяем размеры (только если ОЧЕНЬ большое)
        if (width > 4000 || height > 4000) {
            return true;
        }
        
        // Проверяем соотношение сторон (только очень экстремальные случаи)
        double ratio = (double) width / height;
        if (ratio > 3.0 || ratio < 0.3) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Проверить, является ли аудио видео (иногда видео отправляется как аудио)
     */
    private boolean isVideoAudio(org.telegram.telegrambots.meta.api.objects.Audio audio) {
        if (audio == null) return false;
        
        String mimeType = audio.getMimeType();
        String fileName = audio.getFileName();
        

        // Проверяем MIME тип
        if (mimeType != null && mimeType.startsWith("video/")) {
            return true;
        }
        
        // Проверяем расширение файла
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            String[] videoExtensions = {
                ".mp4", ".avi", ".mov", ".mkv", ".webm", ".3gp", ".flv", ".wmv", 
                ".m4v", ".mpg", ".mpeg", ".m2v", ".ogv", ".asf", ".rm", ".rmvb",
                ".vob", ".ts", ".mts", ".m2ts", ".divx", ".xvid", ".h264", ".h265",
                ".hevc", ".vp8", ".vp9", ".av1", ".gif", ".gifv"
            };
            
            for (String ext : videoExtensions) {
                if (lowerFileName.endsWith(ext)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Проверить, является ли голосовое сообщение видео
     */
    private boolean isVideoVoice(org.telegram.telegrambots.meta.api.objects.Voice voice) {
        if (voice == null) return false;
        
        String mimeType = voice.getMimeType();
        // Проверяем MIME тип
        if (mimeType != null && mimeType.startsWith("video/")) {
            return true;
        }
        return false;
    }
    
    /**
     * Проверить, является ли документ видео
     */
    private boolean isVideoDocument(Document document) {
        if (document == null) return false;
        
        String mimeType = document.getMimeType();
        String fileName = document.getFileName();

        // Проверяем MIME тип
        if (mimeType != null && mimeType.startsWith("video/")) {
            return true;
        }
        
        // Проверяем расширение файла - ВСЕ ВОЗМОЖНЫЕ ФОРМАТЫ ВИДЕО
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            String[] videoExtensions = {
                ".mp4", ".avi", ".mov", ".mkv", ".webm", ".3gp", ".flv", ".wmv", 
                ".m4v", ".mpg", ".mpeg", ".m2v", ".ogv", ".asf", ".rm", ".rmvb",
                ".vob", ".ts", ".mts", ".m2ts", ".divx", ".xvid", ".h264", ".h265",
                ".hevc", ".vp8", ".vp9", ".av1", ".gif", ".gifv"
            };
            
            for (String ext : videoExtensions) {
                if (lowerFileName.endsWith(ext)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Обработать медиа для отзыва
     */
    private void handleReviewMedia(Update update, User user) {
        try {
            Message message = update.getMessage();
            ReviewSubmissionSession session = RedisSessionStore.getReviewSubmissionSession(user.getIdUser());
            
            if (session == null) {
                Sent sent = new Sent();
                sent.sendMessage(user, "❌ Сессия отзыва не найдена. Начните заново.");
                return;
            }
            
            // Если состояние REVIEW_SUBMISSION_TEXT, меняем его на REVIEW_SUBMISSION при получении медиа
            String redisState = RedisSessionStore.getState(user.getIdUser());
            if (redisState != null && redisState.equals("REVIEW_SUBMISSION_TEXT")) {
                RedisSessionStore.setState(user.getIdUser(), "REVIEW_SUBMISSION");
            }
            
            // Проверяем, есть ли текст в сообщении с медиа
            if (message.hasText() && (session.getReviewText() == null || session.getReviewText().isEmpty())) {
                String reviewText = message.getText().trim();
                if (!reviewText.isEmpty()) {
                    session.setReviewText(reviewText);
                }
            }
            
            // Определяем тип медиа
            boolean isVideo = message.hasVideo() || 
                        (message.hasDocument() && isVideoDocument(message.getDocument())) ||
                        message.hasVideoNote() ||
                        (message.hasAudio() && isVideoAudio(message.getAudio())) ||
                        (message.hasVoice() && isVideoVoice(message.getVoice()));
            
            // Обрабатываем медиа - сначала фото, потом видео
            boolean mediaProcessed = false;
            
            // Если уже есть 3 фото, а пришло ещё фото (без видео) — просим отправить видео
            if (message.hasPhoto() && session.getPhotosReceived() >= 3 && !session.isVideoReceived()) {
                Sent sent = new Sent();
                sent.sendMessage(user, "✅ У вас уже есть 3 фотографии. Теперь отправьте видео демонстрации товара:");
                return;
            }

            // Сначала обрабатываем фото
            if (message.hasPhoto() && session.getPhotosReceived() < 3) {
                // Проверяем, не является ли это видео, замаскированным под фото
                if (isLikelyVideoPhoto(message.getPhoto())) {
                    if (!session.isVideoReceived()) {
                        handleReviewVideo(update, user, session);
                    }
                } else {
                    handleReviewPhoto(update, user, session);
                }
                mediaProcessed = true;
            }
            
            // Потом обрабатываем видео
            if (isVideo && !session.isVideoReceived()) {
                handleReviewVideo(update, user, session);
                mediaProcessed = true;
            }
            
            // Если все медиа уже получены, сразу завершаем
            if (session.getPhotosReceived() >= 3 && session.isVideoReceived() && !session.isCompleted()) {
                session.setStep(ReviewSubmissionSession.Step.COMPLETE);
                RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
                completeReviewSubmission(user, session);
                return;
            }
            
            // Убираем все промежуточные сообщения - завершение процесса происходит выше
            // Промежуточные сообщения будут отправляться только при отправке медиа по частям
            
            // Если медиа не было обработано, показываем подсказку/обрабатываем по типу
            if (!mediaProcessed) {
                if (session.getPhotosReceived() >= 3 && session.isVideoReceived() && !session.isCompleted()) {
                    // Все уже получено
                    session.setStep(ReviewSubmissionSession.Step.COMPLETE);
                    RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
                    completeReviewSubmission(user, session);
                } else {
                    // FALLBACK: Четко разделяем фото/видео
                    if (message.hasPhoto()) {
                        if (session.getPhotosReceived() < 3) {
                            handleReviewPhoto(update, user, session);
                        } else {
                            Sent sent = new Sent();
                            sent.sendMessage(user, "✅ У вас уже есть 3 фотографии. Теперь отправьте видео демонстрации товара:");
                        }
                    } else if (message.hasVideo() || message.hasDocument() || message.hasVideoNote() || message.hasAudio() || message.hasVoice()) {
                        handleReviewVideo(update, user, session);
                    } else {
                        Sent sent = new Sent();
                        sent.sendMessage(user, "❌ Неверный тип медиа. Отправьте фото или видео.");
                    }
                }
            }
            
            // Убираем все промежуточные сообщения - система молча обрабатывает медиа
            // Пользователь отправляет медиа, система завершает процесс когда все получено
        } catch (Exception e) {
            System.err.println("Ошибка при обработке медиа отзыва: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Обработать фотографию для отзыва
     */
    private void handleReviewPhoto(Update update, User user, ReviewSubmissionSession session) {
        if (session.getPhotosReceived() >= 3) {
            // Уже отправлено 3 фото - проверяем, есть ли видео
            if (session.isVideoReceived() && !session.isCompleted()) {
                // Все медиа есть - завершаем процесс
                session.setStep(ReviewSubmissionSession.Step.COMPLETE);
                RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
                completeReviewSubmission(user, session);
            } else if (!session.isCompleted()) {
                // Нужно отправить видео
                Sent sent = new Sent();
                sent.sendMessage(user, "✅ У вас уже есть 3 фотографии. Теперь отправьте видео демонстрации товара:");
            }
            return;
        }
        
        Message message = update.getMessage();
        PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
        String photoId = photo.getFileId();
        Integer messageId = message.getMessageId();
        
        // Сохраняем file_id фотографии и message_id
        session.addPhotoWithMessageId(photoId, messageId);
        session.setUserChatId(message.getChatId());
        RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
        
        // Проверяем, можно ли завершить процесс
        if (session.getPhotosReceived() >= 3 && session.isVideoReceived() && !session.isCompleted()) {
            session.setStep(ReviewSubmissionSession.Step.COMPLETE);
            RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
            completeReviewSubmission(user, session);
            return;
        }
        
        // Убираем промежуточные сообщения - завершение процесса происходит в handleReviewMedia
    }
    
    /**
     * Обработать видео для отзыва
     */
    private void handleReviewVideo(Update update, User user, ReviewSubmissionSession session) {
        if (session.isVideoReceived()) {
            // Видео уже отправлено - проверяем, есть ли все фото
            if (session.getPhotosReceived() >= 3 && !session.isCompleted()) {
                // Все медиа есть - завершаем процесс
                session.setStep(ReviewSubmissionSession.Step.COMPLETE);
                RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
                completeReviewSubmission(user, session);
            }
            return;
        }
        
        Message message = update.getMessage();
        String videoId = null;
        // String videoType = "unknown";
        
        // Берем file_id из любого типа видео контента
        if (message.hasVideo()) {
            Video video = message.getVideo();
            videoId = video.getFileId();
        } else if (message.hasDocument() && isVideoDocument(message.getDocument())) {
            Document document = message.getDocument();
            videoId = document.getFileId();
        } else if (message.hasVideoNote()) {
            VideoNote videoNote = message.getVideoNote();
            videoId = videoNote.getFileId();
        } else if (message.hasAudio() && isVideoAudio(message.getAudio())) {
            org.telegram.telegrambots.meta.api.objects.Audio audio = message.getAudio();
            videoId = audio.getFileId();
        } else if (message.hasVoice() && isVideoVoice(message.getVoice())) {
            org.telegram.telegrambots.meta.api.objects.Voice voice = message.getVoice();
            videoId = voice.getFileId();
        } else if (message.hasPhoto() && isLikelyVideoPhoto(message.getPhoto())) {
            // Обрабатываем "большое фото" как видео
            java.util.List<org.telegram.telegrambots.meta.api.objects.PhotoSize> photos = message.getPhoto();
            org.telegram.telegrambots.meta.api.objects.PhotoSize largestPhoto = photos.get(photos.size() - 1);
            videoId = largestPhoto.getFileId();
        }
        
        if (videoId == null) {
            Sent sent = new Sent();
            sent.sendMessage(user, "❌ Не удалось получить медиа для видео.");
            return;
        }
        
        // Сохраняем file_id видео и message_id
        session.setVideoFileId(videoId);
        session.setVideoMessageId(message.getMessageId());
        session.setUserChatId(message.getChatId());
        session.setVideoReceived(true);
        RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
        
        // Проверяем, готов ли процесс к завершению
        if (session.getPhotosReceived() >= 3 && session.isVideoReceived() && !session.isCompleted()) {
            session.setStep(ReviewSubmissionSession.Step.COMPLETE);
            RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
            completeReviewSubmission(user, session);
        }
        
        // Убираем промежуточные сообщения - завершение процесса происходит в handleReviewMedia
    }
    
    /**
     * Завершить подачу отзыва
     */
    private void completeReviewSubmission(User user, ReviewSubmissionSession session) {
        // Проверяем, не завершен ли уже процесс
        if (session.isCompleted()) {
            return;
        }
        
        // Устанавливаем флаг завершения
        session.setCompleted(true);
        RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
        
        // Проверки медиа уже выполнены в handleReviewMedia, здесь просто завершаем процесс

        // Обновляем статус покупки
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        Purchase purchase = session.getPurchase();
        purchase.setPurchaseStage(2); // Этап: отзыв оставлен
        purchaseDAO.update(purchase);

        // Отправляем медиа в группу
        sendReviewMediaToGroup(user, session);
        // Отправляем сообщение пользователю о завершении
        Sent sent = new Sent();
        sent.sendMessage(user, "✅ Ваш отзыв отправлен на согласование администратору.\n\nПосле утверждения вы получите уведомление о необходимости опубликовать отзыв на Wildberries 🔔\n\n❗️Отзыв можно оставить на следующий день после получения товара с ПВЗ");
        
        // Отправляем пользовательское меню
        LogicUI logicUI = new LogicUI();
        logicUI.sendMenu(user, null);
        
        // Очищаем сессию
        RedisSessionStore.removeReviewSubmissionSession(user.getIdUser());
        RedisSessionStore.removeState(user.getIdUser());
    }
    
    /**
     * Отправить медиа отзыва в группу
     */
    private void sendReviewMediaToGroup(User user, ReviewSubmissionSession session) {
        try {
            // оставлено на будущее при необходимости локализации/настроек
            String text = "⭐ Пользователь @" + user.getUsername() + " оставил отзыв!\n\n" +
                        "📦 Товар: " + session.getPurchase().getProduct().getProductName() + "\n" +
                        "📸 Фотографий: " + session.getPhotosReceived() + "\n" +
                        "🎥 Видео: 1\n\n" +
                        "📝 Текст отзыва: " + session.getReviewText() + "\n\n" +
                        "📝 Ожидается публикация отзыва на Wildberries";
            
            // Создаем кнопки подтверждения
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            
            InlineKeyboardButton approveButton = new InlineKeyboardButton();
            approveButton.setText("✅ Подтвердить");
            approveButton.setCallbackData("approve_review_" + session.getPurchase().getIdPurchase());
            
            InlineKeyboardButton rejectButton = new InlineKeyboardButton();
            rejectButton.setText("❌ Отказать");
            rejectButton.setCallbackData("reject_review_" + session.getPurchase().getIdPurchase());
            
            rows.add(List.of(approveButton, rejectButton));
            
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(rows);
            
            Sent sent = new Sent();
            
            // Отправляем медиа отзыва используя file_id (без скачивания)
            Long reviewMessageId = sent.sendReviewMediaToGroup(
                user,
                session.getPhotoFileIds(),
                session.getPhotoMessageIds(),
                session.getVideoFileId(),
                session.getVideoMessageId(),
                text,
                markup
            );
            
            // Сохраняем реальный ID сообщения в группе
            if (reviewMessageId != null) {
                session.getPurchase().setReviewMessageId(reviewMessageId);
                PurchaseDAO purchaseDAO = new PurchaseDAO();
                purchaseDAO.update(session.getPurchase());
            } else {
                System.err.println("❌ Failed to get review message ID");
            }
            
        } catch (Exception e) {
            System.err.println("Ошибка при отправке отзыва в группу: " + e.getMessage());
        }
    }
    
    /**
     * Обработать скриншот отзыва для получения кешбека
     */
    private void handleCashbackScreenshot(Update update, User user, int purchaseId) {
        Sent createTelegramBot = new Sent();
        long chatId = update.getMessage().getChatId();
        Message message = update.getMessage();
        
        if (message.getPhoto() == null || message.getPhoto().isEmpty()) {
            createTelegramBot.sendMessage(user, "Пожалуйста, приложите скриншот вашего отзыва картинкой.");
            return;
        }
        
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        Purchase purchase = purchaseDAO.findById(purchaseId);
        
        if (purchase == null || purchase.getUser().getIdUser() != user.getIdUser()) {
            createTelegramBot.sendMessage(user, "❌ Покупка не найдена");
            RedisSessionStore.removeState(chatId);
            return;
        }
        
        PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
        String fileId = photo.getFileId();
        Integer messageId = message.getMessageId();
        
        // Отправляем пользователю сообщение о начале обработки
        createTelegramBot.sendMessage(user, "🔄 Обрабатываю скриншот отзыва, пожалуйста подождите...");
        
        // Сохраняем file_id и message_id в сессии (если есть активная сессия)
        CashbackSession cashbackSession = RedisSessionStore.getCashbackSession(chatId);
        if (cashbackSession != null) {
            cashbackSession.setScreenshotFileId(fileId);
            cashbackSession.setScreenshotMessageId(messageId);
            RedisSessionStore.setCashbackSession(chatId, cashbackSession);
        }
        
        // Асинхронная обработка скриншота отзыва (без скачивания)
        AsyncService.processCashbackScreenshotAsync(purchase, user, photo, fileId)
            .thenAccept(filePath -> {
                // Используем результат для подавления варнинга о неиспользуемой переменной
                if (filePath == null) {
                    // no-op
                }
                // Успешная обработка
                String finishText = "✅ Скриншот отзыва принят!\n\n" +
                        "📦 Товар: " + purchase.getProduct().getProductName() + "\n" +
                        "💰 Кешбек: " + purchase.getProduct().getCashbackPercentage() + "%\n\n" +
                        "Ваш запрос на кешбек отправлен администратору на рассмотрение.\n" +
                        "После одобрения кешбек будет переведен на указанную карту.\n\n" +
                        "Спасибо за участие! 🎉";
                
                LogicUI logicUI = new LogicUI();
                logicUI.sendMenu(user, finishText);
                RedisSessionStore.removeState(chatId);
                
                // Обновляем статус покупки на этап получения кешбека
                purchase.setPurchaseStage(3);
                purchaseDAO.update(purchase);
                
                // Уведомление в группу с пересылкой скриншота
                try {
                    Integer purchaseAmount = purchase.getPurchaseAmount();
                    int percent = purchase.getProduct().getCashbackPercentage();
                    String payoutText;
                    if (purchaseAmount != null) {
                        long payout = Math.round(purchaseAmount * (percent / 100.0));
                        int payoutInt = (int) payout;
                        payoutText = "💰 К выплате: <code>" + payoutInt + "</code> ₽\n";
                    } else {
                        payoutText = "💰 Размер кешбека: " + percent + "%\n";
                    }
                    String text = "💸 Пользователь @" + user.getUsername() + " запросил кешбек!\n\n" +
                            "📦 Товар: " + purchase.getProduct().getProductName() + "\n" +
                            payoutText +
                            "📅 Дата покупки: " + purchase.getDate() + "\n\n" +
                            "📸 Скриншот отзыва прикреплен ниже";
                    
                    // Создаем кнопку "Оплачено"
                    List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                    
                    InlineKeyboardButton paidButton = new InlineKeyboardButton();
                    paidButton.setText("✅ Оплачено");
                    paidButton.setCallbackData("cashback_paid_" + purchase.getIdPurchase());
                    
                    rows.add(List.of(paidButton));
                    
                    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                    markup.setKeyboard(rows);
                    
                    // Отправляем скриншот кешбека в группу используя file_id
                    Long cashbackMessageId = createTelegramBot.sendCashbackScreenshotToGroup(
                        user,
                        fileId,
                        messageId,
                        text,
                        markup
                    );
                    
                    // Сохраняем реальный ID сообщения о кешбеке в базе данных
                    if (cashbackMessageId != null) {
                        purchase.setCashbackMessageId(cashbackMessageId);
                        purchaseDAO.update(purchase);
                    } else {
                        System.err.println("❌ Failed to get cashback message ID");
                    }
                    
                } catch (Exception e) {
                    System.err.println("Ошибка при отправке уведомления в группу: " + e.getMessage());
                }
            })
            .exceptionally(throwable -> {
                System.err.println("❌ Cashback screenshot processing error: " + throwable.getMessage());
                createTelegramBot.sendMessage(user, "❌ Не удалось обработать скриншот отзыва. Попробуйте ещё раз.");
                return null;
            });
    }
    
    /**
     * Обработать запрос на получение кешбека
     */
    private void handleCashbackRequest(User user, int purchaseId) {
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        Purchase purchase = purchaseDAO.findById(purchaseId);
        
        if (purchase == null || purchase.getUser().getIdUser() != user.getIdUser()) {
            Sent sent = new Sent();
            sent.sendMessage(user, "❌ Покупка не найдена");
            return;
        }
        
        // Проверяем, что отзыв уже оставлен (этап >= 2)
        if (purchase.getPurchaseStage() < 2) {
            Sent sent = new Sent();
            sent.sendMessage(user, "❌ Отзыв еще не оставлен. Сначала оставьте отзыв через «Оставить отзыв»");
            return;
        }
        
        // Проверяем, что кешбек еще не получен (этап < 4)
        if (purchase.getPurchaseStage() >= 4) {
            Sent sent = new Sent();
            sent.sendMessage(user, "✅ Кешбек по этой покупке уже получен!");
            return;
        }
        
        // Проверяем, что кешбек уже запрошен (этап 3)
        if (purchase.getPurchaseStage() == 3) {
            Sent sent = new Sent();
            sent.sendMessage(user, "⏳ Ваш запрос на кешбек уже отправлен администратору на рассмотрение.\n\n" +
                    "📦 Товар: " + purchase.getProduct().getProductName() + "\n" +
                    "💰 Размер кешбека: " + purchase.getProduct().getCashbackPercentage() + "%\n\n" +
                    "Пожалуйста, дождитесь решения администратора. Вы получите уведомление о результате.");
            return;
        }
        
        // Показываем инструкцию для получения кешбека
        String instruction = "💸 Инструкция для получения кешбека:\n\n" +
                "📦 Товар: " + purchase.getProduct().getProductName() + "\n" +
                "💰 Размер кешбека: " + purchase.getProduct().getCashbackPercentage() + "%\n\n" +
                "Для получения кешбека:\n" +
                "1️⃣ Введите номер карты для получения кешбека\n" +
                "2️⃣ Отправьте скриншот вашего опубликованного отзыва\n" +
                "3️⃣ Дождитесь одобрения администратором\n" +
                "4️⃣ Получите кешбек на указанную карту\n\n" +
                "💳 Введите номер карты (16 цифр):";
        
        // Создаем сессию кешбека
        CashbackSession cashbackSession = new CashbackSession(purchase);
        RedisSessionStore.setCashbackSession(user.getIdUser(), cashbackSession);
        
        // Устанавливаем состояние для ввода номера карты
        RedisSessionStore.setState(user.getIdUser(), "CASHBACK_CARD_INPUT_" + purchaseId);
        
        Sent sent = new Sent();
        sent.sendMessage(user, instruction);
    }
    
    /**
     * Обработать выбор товара для оставления отзыва
     */
    private void handleReviewProductSelection(User user, int purchaseId) {
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        Purchase purchase = purchaseDAO.findById(purchaseId);
        
        if (purchase == null || purchase.getUser().getIdUser() != user.getIdUser()) {
            Sent sent = new Sent();
            sent.sendMessage(user, "❌ Покупка не найдена");
            return;
        }
        
        // Проверяем, что товар заказан и отзыв еще не оставлен
        if (purchase.getPurchaseStage() < 0 || purchase.getPurchaseStage() >= 2) {
            Sent sent = new Sent();
            sent.sendMessage(user, "❌ Этот товар не готов для оставления отзыва");
            return;
        }
        
        // Создаем сессию оставления отзыва
        ReviewSubmissionSession session = new ReviewSubmissionSession(purchase);
        RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
        RedisSessionStore.setState(user.getIdUser(), "REVIEW_SUBMISSION");

        // Отправляем сообщение с просьбой написать текст отзыва
        String productName = (purchase.getProduct() != null && purchase.getProduct().getProductName() != null) 
            ? purchase.getProduct().getProductName() 
            : "Неизвестный товар";
        String message = "📝 Вы оставляли заявку на товар: \"" + productName + "\"\n\n" +
                        "Пожалуйста, напишите текст вашего отзыва о товаре 🖊";
        
        Sent sent = new Sent();
        sent.sendMessage(user, message);
    }
    
    /**
     * Обработать отправку текста отзыва
     */
    private void handleReviewTextSubmission(Update update, User user) {
        ReviewSubmissionSession session = RedisSessionStore.getReviewSubmissionSession(user.getIdUser());
        
        if (session == null) {
            Sent sent = new Sent();
            sent.sendMessage(user, "❌ Сессия отзыва не найдена. Начните заново.");
            return;
        }
        
        String reviewText = update.getMessage().getText();

        if (reviewText == null || reviewText.trim().isEmpty()) {
            Sent sent = new Sent();
            sent.sendMessage(user, "❌ Пожалуйста, введите текст отзыва.");
            return;
        }
        
        // Сохраняем текст отзыва
        session.setReviewText(reviewText.trim());
        session.setStep(ReviewSubmissionSession.Step.MEDIA);
        RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);

        // Отправляем сообщение с просьбой прикрепить медиа
        String message = "Отлично! Теперь, пожалуйста, прикрепите фотографии и/или видео товара (3 фото и 1 видео) 📷\n\n" +
                "💡 Совет: Вы можете отправить текст и медиа в одном сообщении!";
        
        Sent sent = new Sent();
        sent.sendMessage(user, message);
    }
    
    /**
     * Обработать подтверждение/отклонение отзыва администратором
     */
    private void handleReviewApproval(User admin, int purchaseId, boolean approved) {
        // Проверяем, что пользователь - администратор
        if (admin == null || !admin.isAdmin()) {
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ У вас нет прав для выполнения этого действия.");
            return;
        }
        
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        Purchase purchase = purchaseDAO.findById(purchaseId);
        
        if (purchase == null) {
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Покупка не найдена.");
            return;
        }
        
        User reviewUser = purchase.getUser();
        
        // Подтверждаем отзыв
        purchase.setPurchaseStage(2); // Этап: отзыв утвержден
        purchaseDAO.update(purchase);
        
        // Отправляем уведомление пользователю
        String message = "✅ Ваш отзыв утвержден администратором.\n\nПосле его публикации на WB нажмите кнопку «💸 Получить кешбек»";
        
        Sent sent = new Sent();
        sent.sendMessage(reviewUser, message);
    }
    
    /**
     * Обработка отклонения отзыва администратором с запросом причины
     */
    private void handleReviewRejection(User admin, int purchaseId) {

        PurchaseDAO purchaseDAO = new PurchaseDAO();
        Purchase purchase = purchaseDAO.findById(purchaseId);
        
        if (purchase == null) {
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Покупка не найдена.");
            return;
        }
        
        // Получаем ID подгруппы пользователя
        int userSubgroupId = purchase.getUser().getId_message();

        // Создаем сессию для ввода причины отказа
        ReviewRejectionSession rejectionSession = new ReviewRejectionSession(purchase);
        // Сохраняем сессию для подгруппы пользователя, чтобы администратор мог отвечать в группе
        RedisSessionStore.setReviewRejectionSession((long) userSubgroupId, rejectionSession);
        RedisSessionStore.setState((long) userSubgroupId, "REVIEW_REJECTION");

        // Отправляем сообщение в группу с запросом причины
        try {
            ResourceBundle rb = ResourceBundle.getBundle("app");
            long groupID = Long.parseLong(rb.getString("tg.group"));
            
            String message = "❌ Отклонение отзыва\n\n" +
                    "Пользователь: @" + purchase.getUser().getUsername() + "\n" +
                    "Товар: " + purchase.getProduct().getProductName() + "\n\n" +
                    "📝 Пожалуйста, укажите причину отказа:";
            
            Sent sent = new Sent();
            sent.sendMessageUser(groupID, userSubgroupId, message);
            
        } catch (Exception e) {
            System.err.println("❌ Error sending rejection reason request to group: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Обработка ввода причины отказа отзыва
     */
    private void handleReviewRejectionReason(Update update, User userInThread) {

        String reason = update.getMessage().getText();

        // Получаем администратора, который отправил сообщение
        Long adminChatId = update.getMessage().getFrom().getId();
        UserDAO userDAO = new UserDAO();
        User admin = userDAO.findById(adminChatId);
        
        if (admin == null || !admin.isAdmin()) {
            return;
        }
        
        // Получаем ID подгруппы (threadID) из сообщения
        Integer threadID = update.getMessage().getMessageThreadId();

        // Ищем сессию отклонения по ID подгруппы (threadID)
        ReviewRejectionSession rejectionSession = RedisSessionStore.getReviewRejectionSession((long) threadID);
        
        if (rejectionSession == null) {
            return;
        }

        // Сохраняем причину отказа
        rejectionSession.setRejectionReason(reason);
        RedisSessionStore.setReviewRejectionSession((long) threadID, rejectionSession);
        
        // Обрабатываем отклонение
        processReviewRejection(admin, rejectionSession);
        
        // Очищаем сессию
        RedisSessionStore.removeReviewRejectionSession((long) threadID);
        RedisSessionStore.removeState((long) threadID);
    }
    
    /**
     * Обработка отклонения отзыва с причиной
     */
    private void processReviewRejection(User admin, ReviewRejectionSession rejectionSession) {
        Purchase purchase = rejectionSession.getPurchase();
        String reason = rejectionSession.getRejectionReason();
        
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        
        // Возвращаем к этапу: товар получен
        purchase.setPurchaseStage(1);
        purchaseDAO.update(purchase);
        
        User reviewUser = purchase.getUser();
        
        // Отправляем уведомление пользователю с причиной
        String userMessage = "❌ Ваш отзыв отклонен администратором.\n\n" +
                "📝 Причина отказа: " + reason + "\n\n" +
                "Пожалуйста, отправьте новый отзыв с улучшенными фотографиями и видео.";
        
        Sent sent = new Sent();
        sent.sendMessage(reviewUser, userMessage);
        
        // Отправляем подтверждение в группу в ту же подгруппу пользователя
        try {
            ResourceBundle rb = ResourceBundle.getBundle("app");
            long groupID = Long.parseLong(rb.getString("tg.group"));
            
            // Получаем ID подгруппы пользователя
            int userSubgroupId = reviewUser.getId_message();
            
            String groupMessage = "✅ Отзыв пользователя @" + reviewUser.getUsername() + " отклонен с причиной:\n" + reason;
            sent.sendMessageUser(groupID, userSubgroupId, groupMessage);
            
        } catch (Exception e) {
            System.err.println("❌ Error sending rejection confirmation to group: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Обработка ввода номера карты для кешбека
     */
    private void handleCashbackCardInput(Update update, User user, int purchaseId) {
        Sent createTelegramBot = new Sent();
        long chatId = update.getMessage().getChatId();
        String cardNumber = update.getMessage().getText();
        
        // Убираем пробелы из номера карты
        if (cardNumber != null) {
            cardNumber = cardNumber.replaceAll("\\s+", "");
        }
        
        // Проверяем формат номера карты (16 цифр)
        if (cardNumber == null || !cardNumber.matches("\\d{16}")) {
            createTelegramBot.sendMessage(user, "❌ Неверный формат номера карты!\n\n" +
                    "Пожалуйста, введите номер карты из 16 цифр (например: 1234567890123456 или 1234 5678 9012 3456)");
            return;
        }
        
        // Получаем сессию кешбека
        CashbackSession cashbackSession = RedisSessionStore.getCashbackSession(chatId);
        if (cashbackSession == null) {
            createTelegramBot.sendMessage(user, "❌ Сессия кешбека не найдена. Попробуйте начать процесс заново.");
            RedisSessionStore.removeState(chatId);
            return;
        }
        
        // Проверяем, что это правильная покупка
        if (cashbackSession.getPurchase().getIdPurchase() != purchaseId) {
            createTelegramBot.sendMessage(user, "❌ Ошибка сессии. Попробуйте начать процесс заново.");
            RedisSessionStore.removeState(chatId);
            RedisSessionStore.removeCashbackSession(chatId);
            return;
        }
        
        // Сохраняем номер карты в сессии
        cashbackSession.setCardNumber(cardNumber);
        cashbackSession.setStep(CashbackSession.Step.SCREENSHOT);
        RedisSessionStore.setCashbackSession(chatId, cashbackSession);
        
        // Обновляем состояние для отправки скриншота
        RedisSessionStore.setState(chatId, "CASHBACK_SCREENSHOT_" + purchaseId);
        
        // Отправляем сообщение с инструкцией по скриншоту
        String instruction = "✅ Номер карты принят!\n\n" +
                "📦 Товар: " + cashbackSession.getPurchase().getProduct().getProductName() + "\n" +
                "💰 Размер кешбека: " + cashbackSession.getPurchase().getProduct().getCashbackPercentage() + "%\n" +
                "💳 Карта: <code>" + cardNumber + "</code>\n\n" +
                "📸 Теперь отправьте скриншот вашего опубликованного отзыва:";
        
        createTelegramBot.sendMessage(user, instruction);
    }
    
    /**
     * Обработка скриншота кешбека с номером карты
     */
    private void handleCashbackScreenshotWithCard(Update update, User user, int purchaseId) {
        Sent createTelegramBot = new Sent();
        long chatId = update.getMessage().getChatId();
        Message message = update.getMessage();
        
        if (message.getPhoto() == null || message.getPhoto().isEmpty()) {
            createTelegramBot.sendMessage(user, "Пожалуйста, приложите скриншот вашего отзыва картинкой.");
            return;
        }
        
        // Получаем сессию кешбека
        CashbackSession cashbackSession = RedisSessionStore.getCashbackSession(chatId);
        if (cashbackSession == null) {
            createTelegramBot.sendMessage(user, "❌ Сессия кешбека не найдена. Попробуйте начать процесс заново.");
            RedisSessionStore.removeState(chatId);
            return;
        }
        
        Purchase purchase = cashbackSession.getPurchase();
        if (purchase.getIdPurchase() != purchaseId) {
            createTelegramBot.sendMessage(user, "❌ Ошибка сессии. Попробуйте начать процесс заново.");
            RedisSessionStore.removeState(chatId);
            RedisSessionStore.removeCashbackSession(chatId);
            return;
        }
        
        PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
        String fileId = photo.getFileId();
        Integer messageId = message.getMessageId();
        
        // Отправляем пользователю сообщение о начале обработки
        createTelegramBot.sendMessage(user, "🔄 Обрабатываю скриншот отзыва, пожалуйста подождите...");
        
        // Сохраняем file_id и message_id в сессии
        cashbackSession.setScreenshotFileId(fileId);
        cashbackSession.setScreenshotMessageId(messageId);
        RedisSessionStore.setCashbackSession(chatId, cashbackSession);
        
        // Сохраняем номер карты в покупке
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        purchase.setCardNumber(cashbackSession.getCardNumber());
        purchaseDAO.update(purchase);
        
        // Асинхронная обработка скриншота отзыва (без скачивания)
        AsyncService.processCashbackScreenshotAsync(purchase, user, photo, fileId)
            .thenAccept(filePath -> {
                // Используем результат для подавления варнинга о неиспользуемой переменной
                if (filePath == null) {
                    // no-op
                }
                // Успешная обработка
                String finishText = "✅ Скриншот отзыва принят!\n\n" +
                        "📦 Товар: " + purchase.getProduct().getProductName() + "\n" +
                        "💰 Кешбек: " + purchase.getProduct().getCashbackPercentage() + "%\n" +
                        "💳 Карта: <code>" + cashbackSession.getCardNumber() + "</code>\n\n" +
                        "Ваш запрос на кешбек отправлен администратору на рассмотрение.\n" +
                        "После одобрения кешбек будет переведен на указанную карту.\n\n" +
                        "Спасибо за участие! 🎉";
                
                LogicUI logicUI = new LogicUI();
                logicUI.sendMenu(user, finishText);
                RedisSessionStore.removeState(chatId);
                RedisSessionStore.removeCashbackSession(chatId);
                
                // Обновляем статус покупки на этап получения кешбека
                purchase.setPurchaseStage(3);
                purchaseDAO.update(purchase);
                
                // Уведомление в группу с пересылкой скриншота
                try {
                    Integer purchaseAmount = purchase.getPurchaseAmount();
                    int percent = purchase.getProduct().getCashbackPercentage();
                    String payoutText;
                    if (purchaseAmount != null) {
                        long payout = Math.round(purchaseAmount * (percent / 100.0));
                        payoutText = "💰 К выплате: <code>" + payout + " ₽</code>\n";
                    } else {
                        payoutText = "💰 Размер кешбека: " + percent + "%\n";
                    }
                    String text = "💸 Пользователь @" + user.getUsername() + " запросил кешбек!\n\n" +
                            "📦 Товар: " + purchase.getProduct().getProductName() + "\n" +
                            payoutText +
                            "📅 Дата покупки: " + purchase.getDate() + "\n\n" +
                            "📸 Скриншот отзыва прикреплен ниже";
                    
                    // Создаем кнопку "Оплачено"
                    List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                    
                    InlineKeyboardButton paidButton = new InlineKeyboardButton();
                    paidButton.setText("✅ Оплачено");
                    paidButton.setCallbackData("cashback_paid_" + purchase.getIdPurchase());
                    
                    rows.add(List.of(paidButton));
                    
                    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                    markup.setKeyboard(rows);
                    
                    // Отправляем скриншот кешбека в группу используя file_id
                    Long cashbackMessageId = createTelegramBot.sendCashbackScreenshotToGroup(
                        user,
                        fileId,
                        messageId,
                        text,
                        markup
                    );
                    
                    // Сохраняем реальный ID сообщения о кешбеке в базе данных
                    if (cashbackMessageId != null) {
                        purchase.setCashbackMessageId(cashbackMessageId);
                        purchaseDAO.update(purchase);
                    } else {
                        System.err.println("❌ Failed to get cashback message ID");
                    }
                    
                } catch (Exception e) {
                    System.err.println("Ошибка при отправке уведомления в группу: " + e.getMessage());
                }
            })
            .exceptionally(throwable -> {
                System.err.println("❌ Cashback screenshot processing error: " + throwable.getMessage());
                createTelegramBot.sendMessage(user, "❌ Не удалось обработать скриншот отзыва. Попробуйте ещё раз.");
                return null;
            });
    }

    /**
     * Обработка ввода username для блокировки пользователя
     */
    private void handleBlockUserInput(Update update, User admin) {

        String username = update.getMessage().getText();
        if (username == null) {
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Введите корректный username пользователя без @");
            RedisSessionStore.removeState(admin.getIdUser());
            return;
        }
        
        username = username.trim();
        if (username.startsWith("@")) {
            username = username.substring(1);
        }
        if (username.isEmpty()) {
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Username не может быть пустым. Попробуйте еще раз:");
            return;
        }

        // Сбрасываем состояние перед обработкой
        RedisSessionStore.removeState(admin.getIdUser());
        
        try {
            LogicUI logicUI = new LogicUI();
            logicUI.blockUser(admin, username);
        } catch (Exception e) {
            System.err.println("❌ Error in handleBlockUserInput: " + e.getMessage());
            e.printStackTrace();
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Произошла ошибка при блокировке пользователя: " + e.getMessage());
        }
    }
}