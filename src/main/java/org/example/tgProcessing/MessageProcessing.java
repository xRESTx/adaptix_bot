package org.example.tgProcessing;

import org.example.async.AsyncService;
import org.example.dao.ProductDAO;
import org.example.dao.PurchaseDAO;
import org.example.dao.UserDAO;
import org.example.monitoring.MetricsService;
import io.micrometer.core.instrument.Timer;
import org.example.session.ProductCreationSession;
import org.example.session.ReviewRequestSession;
import org.example.session.ReviewSubmissionSession;
import org.example.session.ReviewRejectionSession;
import org.example.session.RedisSessionStore;
import org.example.session.ReservationManager;
import org.example.settings.AdminSettings;
import java.util.ResourceBundle;
import java.util.List;
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
        // КРИТИЧЕСКОЕ ЛОГИРОВАНИЕ - проверяем ВСЕ входящие сообщения
        System.out.println("🚨 === CRITICAL UPDATE RECEIVED ===");
        System.out.println("🚨 Update ID: " + update.getUpdateId());
        System.out.println("🚨 Has message: " + update.hasMessage());
        System.out.println("🚨 Has callback query: " + update.hasCallbackQuery());
        
        if (update.hasMessage()) {
            Message message = update.getMessage();
            System.out.println("🚨 Message ID: " + message.getMessageId());
            System.out.println("🚨 Chat ID: " + message.getChatId());
            System.out.println("🚨 Has text: " + message.hasText());
            System.out.println("🚨 Has photo: " + message.hasPhoto());
            System.out.println("🚨 Has video: " + message.hasVideo());
            System.out.println("🚨 Has document: " + message.hasDocument());
            System.out.println("🚨 Has video note: " + message.hasVideoNote());
            System.out.println("🚨 Has sticker: " + message.hasSticker());
            System.out.println("🚨 Has contact: " + message.hasContact());
            System.out.println("🚨 Has location: " + message.hasLocation());
            System.out.println("🚨 Has venue: " + (message.getVenue() != null));
            System.out.println("🚨 Has voice: " + message.hasVoice());
            System.out.println("🚨 Has audio: " + message.hasAudio());
            
            if (message.hasText()) {
                System.out.println("🚨 Text: " + message.getText());
            }
            if (message.hasVideo()) {
                System.out.println("🚨 VIDEO DETECTED! File ID: " + message.getVideo().getFileId());
            }
            if (message.hasDocument()) {
                System.out.println("🚨 DOCUMENT DETECTED! File ID: " + message.getDocument().getFileId());
            }
        }
        System.out.println("🚨 === END CRITICAL LOGGING ===");
        
        // Начинаем измерение времени обработки
        MetricsService metricsService = MetricsService.getInstance();
        Timer.Sample sample = metricsService.startMessageProcessing();
        
        // ДЕТАЛЬНОЕ ЛОГИРОВАНИЕ
        System.out.println("🔄 === NEW UPDATE RECEIVED ===");
        System.out.println("🔄 Update ID: " + update.getUpdateId());
        System.out.println("🔄 Has message: " + update.hasMessage());
        System.out.println("🔄 Has callback query: " + update.hasCallbackQuery());
        
        if (update.hasMessage()) {
            Message message = update.getMessage();
            System.out.println("🔄 Message ID: " + message.getMessageId());
            System.out.println("🔄 Chat ID: " + message.getChatId());
            System.out.println("🔄 Has text: " + message.hasText());
            System.out.println("🔄 Has photo: " + message.hasPhoto());
            System.out.println("🔄 Has video: " + message.hasVideo());
            System.out.println("🔄 Has document: " + message.hasDocument());
            System.out.println("🔄 Has video note: " + message.hasVideoNote());
            System.out.println("🔄 Has sticker: " + message.hasSticker());
            System.out.println("🔄 Has contact: " + message.hasContact());
            System.out.println("🔄 Has location: " + message.hasLocation());
            System.out.println("🔄 Has venue: " + (message.getVenue() != null));
            System.out.println("🔄 Has voice: " + message.hasVoice());
            System.out.println("🔄 Has audio: " + message.hasAudio());
            
            // ДЕТАЛЬНАЯ ИНФОРМАЦИЯ О ВСЕХ ТИПАХ МЕДИА
            if (message.hasText()) {
                System.out.println("🔄 Text: " + message.getText());
            }
            if (message.hasPhoto()) {
                System.out.println("🔄 Photo count: " + message.getPhoto().size());
                for (int i = 0; i < message.getPhoto().size(); i++) {
                    PhotoSize photo = message.getPhoto().get(i);
                    System.out.println("🔄 Photo " + i + ": " + photo.getWidth() + "x" + photo.getHeight() + " (file_id: " + photo.getFileId() + ")");
                }
            }
            if (message.hasVideo()) {
                Video video = message.getVideo();
                System.out.println("🔄 Video file ID: " + video.getFileId());
                System.out.println("🔄 Video duration: " + video.getDuration());
                System.out.println("🔄 Video width: " + video.getWidth());
                System.out.println("🔄 Video height: " + video.getHeight());
                System.out.println("🔄 Video file size: " + video.getFileSize());
                System.out.println("🔄 Video MIME type: " + video.getMimeType());
                System.out.println("🔄 Video file name: " + video.getFileName());
            }
            if (message.hasDocument()) {
                Document doc = message.getDocument();
                System.out.println("🔄 Document file ID: " + doc.getFileId());
                System.out.println("🔄 Document MIME type: " + doc.getMimeType());
                System.out.println("🔄 Document file name: " + doc.getFileName());
                System.out.println("🔄 Document file size: " + doc.getFileSize());
                System.out.println("🔄 Document thumb: " + (doc.getThumbnail() != null ? doc.getThumbnail().getFileId() : "null"));
            }
            if (message.hasVideoNote()) {
                VideoNote videoNote = message.getVideoNote();
                System.out.println("🔄 Video note file ID: " + videoNote.getFileId());
                System.out.println("🔄 Video note duration: " + videoNote.getDuration());
                System.out.println("🔄 Video note length: " + videoNote.getLength());
                System.out.println("🔄 Video note file size: " + videoNote.getFileSize());
            }
            if (message.hasVoice()) {
                System.out.println("🔄 Voice file ID: " + message.getVoice().getFileId());
                System.out.println("🔄 Voice duration: " + message.getVoice().getDuration());
                System.out.println("🔄 Voice MIME type: " + message.getVoice().getMimeType());
            }
            if (message.hasAudio()) {
                System.out.println("🔄 Audio file ID: " + message.getAudio().getFileId());
                System.out.println("🔄 Audio duration: " + message.getAudio().getDuration());
                System.out.println("🔄 Audio MIME type: " + message.getAudio().getMimeType());
            }
        }
        
        
        try {
            Sent createTelegramBot = new Sent();
            LogicUI logicUI = new LogicUI();

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

        if(user == null){
            System.out.println("🚨 User is null, sending start message");
            logicUI.sendStart(chatId, update);
            metricsService.stopMessageProcessing(sample);
            return;
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
                    System.out.println("🚨 No active session - starting search process");
                    startSearchProcess(update, user);
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
        
        if (textState != null && textState.equals("REVIEW_SUBMISSION")) {
            ReviewSubmissionSession session = RedisSessionStore.getReviewSubmissionSession(user.getIdUser());
            if (session != null && session.getStep() == ReviewSubmissionSession.Step.MEDIA) {
                // Если шаг MEDIA, обрабатываем как медиа
                handleReviewMedia(update, user);
                metricsService.stopMessageProcessing(sample);
                return;
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
                    // Товар не выбран - это означает, что процесс покупки товара не завершен
                    // Продолжаем процесс покупки товара
                    System.out.println("🔍 Debug: session.getProduct() is null, continuing purchase process");
                    
                    // Проверяем, есть ли активная сессия создания товара (только для админов)
                    ProductCreationSession productSession = RedisSessionStore.getProductSession(chatId);
                    if (productSession != null && productSession.getProduct() != null && user.isAdmin()) {
                        // Админ создает товар - завершаем процесс создания товара
                        System.out.println("🔍 Debug: Found incomplete product creation, completing...");
                        productSession.setStep(ProductCreationSession.Step.PHOTO);
                        createTelegramBot.sendMessage(user, "✅ Скриншот поиска принят!\n\n📷 Теперь отправьте фотографию товара:");
                    } else {
                        // Обычный пользователь покупает товар - продолжаем процесс покупки
                        System.out.println("🔍 Debug: Continuing purchase process for user");
                        createTelegramBot.sendMessage(user, "✅ Скриншот поиска принят!\n\n🔢 Теперь введите артикул товара Wildberries для проверки:");
                        session.setStep(ReviewRequestSession.Step.ARTICUL_CHECK);
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
                
                // Сохраняем ID сообщения в сессии для последующего сохранения в БД
                session.setGroupMessageId(groupMessageId);
                
                // Обновляем orderMessageId в уже созданной покупке
                if (groupMessageId != null) {
                    // Находим последнюю покупку пользователя для этого товара
                    PurchaseDAO purchaseDAO = new PurchaseDAO();
                    List<Purchase> userPurchases = purchaseDAO.findByUserId(user.getIdUser());
                    
                    Purchase latestPurchase = null;
                    for (Purchase purchase : userPurchases) {
                        if (purchase.getProduct().getIdProduct() == session.getProduct().getIdProduct() &&
                            purchase.getOrderMessageId() == null) {
                            latestPurchase = purchase;
                            break;
                        }
                    }
                    
                    if (latestPurchase != null) {
                        latestPurchase.setOrderMessageId(groupMessageId);
                        latestPurchase.setGroupMessageId(groupMessageId);
                        purchaseDAO.update(latestPurchase);
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
        
        // Проверяем состояние пользователя
        String state = RedisSessionStore.getState(chatId);
        
        // Если пользователь в процессе подачи отзыва, обрабатываем фото как медиа для отзыва
        if (state != null && (state.equals("REVIEW_SUBMISSION") || state.equals("REVIEW_SUBMISSION_TEXT"))) {
            System.out.println("📸 Photo received during review submission, processing as review media");
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
        System.out.println("🔍 === CALLBACK QUERY START ===");
        System.out.println("🔍 Update ID: " + update.getUpdateId());
        System.out.println("🔍 Callback data: " + update.getCallbackQuery().getData());
        System.out.println("🔍 Chat ID: " + update.getCallbackQuery().getMessage().getChatId());
        
        Sent createTelegramBot = new Sent();
        LogicUI logicUI = new LogicUI();
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

        UserDAO userDAO = new UserDAO();
        User user = userDAO.findById(chatId);
        
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
        RedisSessionStore.clearAll(chatId);
        

        // Обработка админ-интерфейса
        if (data.equals("admin_menu") || data.equals("admin_products") || data.equals("admin_stats") || 
            data.equals("admin_settings") || data.equals("admin_add_product") || data.equals("admin_user_management") ||
            data.equals("admin_add_admin") || data.equals("admin_change_support") || data.equals("admin_back_to_menu") ||
            data.equals("admin_back_to_main_menu") || data.equals("admin_back_to_admin_menu") || data.equals("admin_back_to_products") ||
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
                case "back_to_menu":{
                    // Возвращаемся в главное меню
                    System.out.println("🏠 Back to menu button pressed by user: " + user.getUsername());
                    System.out.println("🏠 Deleting message ID: " + messageId);
                    // Удаляем текущее сообщение с inline кнопками
                    safeDeleteMessage(user.getIdUser(), messageId);
                    System.out.println("🏠 Sending main menu to user: " + user.getUsername());
                    // Показываем главное меню
                    logicUI.sendMenu(user, null);
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
                } else if (callbackData.equals("no_message_available")) {
                    System.out.println("ℹ️ No message available callback");
                    Sent sent = new Sent();
                    sent.sendMessage(user, "ℹ️ Ссылка на сообщение недоступна. Этап выполнен, но сообщение не найдено.");
                } else {
                    System.out.println("❓ Unknown callback: " + callbackData);
                }
            }
        }
    }

    /**
     * Обработка callback query для подтверждения/отклонения отзывов в группе
     */
    private void handleGroupReviewCallback(Update update, String data) {
        System.out.println("🔍 === GROUP REVIEW CALLBACK START ===");
        System.out.println("🔍 Callback data: " + data);
        System.out.println("🔍 Update ID: " + update.getUpdateId());
        System.out.println("🔍 Chat ID: " + update.getCallbackQuery().getMessage().getChatId());
        System.out.println("🔍 Message ID: " + update.getCallbackQuery().getMessage().getMessageId());
        
        try {
            if (data.startsWith("approve_review_")) {
                // Извлекаем ID покупки из callback data
                int purchaseId = Integer.parseInt(data.substring("approve_review_".length()));
                
                // Находим покупку
                PurchaseDAO purchaseDAO = new PurchaseDAO();
                Purchase purchase = purchaseDAO.findById(purchaseId);
                
                if (purchase != null) {
                    // Подтверждаем отзыв - передаем реального пользователя
                    User reviewUser = purchase.getUser();
                    reviewUser.setAdmin(true); // Временно делаем админом для обработки
                    handleReviewApproval(reviewUser, purchaseId, true);
                    
                    // Отвечаем на callback query
                    AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
                    answerCallbackQuery.setCallbackQueryId(update.getCallbackQuery().getId());
                    answerCallbackQuery.setText("✅ Отзыв подтвержден!");
                    answerCallbackQuery.setShowAlert(false);
                    
                    Sent sent = new Sent();
                    sent.answerCallbackQuery(answerCallbackQuery);
                }
                
            } else if (data.startsWith("reject_review_")) {
                // Извлекаем ID покупки из callback data
                int purchaseId = Integer.parseInt(data.substring("reject_review_".length()));
                
                // Находим покупку
                PurchaseDAO purchaseDAO = new PurchaseDAO();
                Purchase purchase = purchaseDAO.findById(purchaseId);
                
                if (purchase != null) {
                    // Инициируем процесс отклонения - передаем реального пользователя
                    User reviewUser = purchase.getUser();
                    reviewUser.setAdmin(true); // Временно делаем админом для обработки
                    handleReviewRejection(reviewUser, purchaseId);
                    
                    // Отвечаем на callback query
                    AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
                    answerCallbackQuery.setCallbackQueryId(update.getCallbackQuery().getId());
                    answerCallbackQuery.setText("❌ Отзыв отклонен. Укажите причину.");
                    answerCallbackQuery.setShowAlert(false);
                    
                    Sent sent = new Sent();
                    sent.answerCallbackQuery(answerCallbackQuery);
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error processing group review callback: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("🔍 === GROUP REVIEW CALLBACK END ===");
    }

    /**
     * Обработка callback query для кнопки "Оплачено" кешбека
     */
    private void handleCashbackPaidCallback(Update update, String data) {
        System.out.println("🔍 === CASHBACK PAID CALLBACK START ===");
        System.out.println("🔍 Callback data: " + data);
        System.out.println("🔍 Update ID: " + update.getUpdateId());
        System.out.println("🔍 Chat ID: " + update.getCallbackQuery().getMessage().getChatId());
        System.out.println("🔍 Message ID: " + update.getCallbackQuery().getMessage().getMessageId());
        
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
                    String message = "🎉 Кешбек выплачен!\n\n" +
                            "📦 Товар: " + purchase.getProduct().getProductName() + "\n" +
                            "💰 Размер кешбека: " + purchase.getProduct().getCashbackPercentage() + "%\n\n" +
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
        
        System.out.println("🔍 === CASHBACK PAID CALLBACK END ===");
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
     * Проверить, является ли фото на самом деле видео (большие размеры могут указывать на видео)
     * ОБНОВЛЕНО: Более консервативная логика, чтобы избежать ложных срабатываний
     */
    private boolean isLikelyVideoPhoto(java.util.List<org.telegram.telegrambots.meta.api.objects.PhotoSize> photos) {
        if (photos == null || photos.isEmpty()) return false;
        
        // Берем самое большое фото
        org.telegram.telegrambots.meta.api.objects.PhotoSize largestPhoto = photos.get(photos.size() - 1);
        
        System.out.println("🔍 Checking photo dimensions: " + largestPhoto.getWidth() + "x" + largestPhoto.getHeight());
        
        int width = largestPhoto.getWidth();
        int height = largestPhoto.getHeight();
        
        // БОЛЕЕ КОНСЕРВАТИВНЫЕ КРИТЕРИИ: Только очень специфичные случаи
        // Проверяем размеры (только если ОЧЕНЬ большое)
        if (width > 4000 || height > 4000) {
            System.out.println("🎥 Extremely large photo detected (dimensions: " + width + "x" + height + "), treating as potential video");
            return true;
        }
        
        // Проверяем соотношение сторон (только очень экстремальные случаи)
        double ratio = (double) width / height;
        if (ratio > 3.0 || ratio < 0.3) {
            System.out.println("🎥 Extreme aspect ratio detected (" + ratio + "), treating as potential video");
            return true;
        }
        
        System.out.println("📸 Photo appears to be normal photo");
        return false;
    }
    
    /**
     * Проверить, является ли аудио видео (иногда видео отправляется как аудио)
     */
    private boolean isVideoAudio(org.telegram.telegrambots.meta.api.objects.Audio audio) {
        if (audio == null) return false;
        
        String mimeType = audio.getMimeType();
        String fileName = audio.getFileName();
        
        System.out.println("🔍 Checking audio: " + fileName + " (MIME: " + mimeType + ")");
        
        // Проверяем MIME тип
        if (mimeType != null && mimeType.startsWith("video/")) {
            System.out.println("✅ Audio is video by MIME type: " + mimeType);
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
                    System.out.println("✅ Audio is video by file extension: " + ext);
                    return true;
                }
            }
        }
        
        System.out.println("❌ Audio is not video");
        return false;
    }
    
    /**
     * Проверить, является ли голосовое сообщение видео
     */
    private boolean isVideoVoice(org.telegram.telegrambots.meta.api.objects.Voice voice) {
        if (voice == null) return false;
        
        String mimeType = voice.getMimeType();
        
        System.out.println("🔍 Checking voice: MIME: " + mimeType);
        
        // Проверяем MIME тип
        if (mimeType != null && mimeType.startsWith("video/")) {
            System.out.println("✅ Voice is video by MIME type: " + mimeType);
            return true;
        }
        
        System.out.println("❌ Voice is not video");
        return false;
    }
    
    /**
     * Проверить, является ли документ видео
     */
    private boolean isVideoDocument(Document document) {
        if (document == null) return false;
        
        String mimeType = document.getMimeType();
        String fileName = document.getFileName();
        
        System.out.println("🔍 Checking document: " + fileName + " (MIME: " + mimeType + ")");
        
        // Проверяем MIME тип
        if (mimeType != null && mimeType.startsWith("video/")) {
            System.out.println("✅ Document is video by MIME type: " + mimeType);
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
                    System.out.println("✅ Document is video by file extension: " + ext);
                    return true;
                }
            }
        }
        
        System.out.println("❌ Document is not video");
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
            
            // Определяем тип медиа
            boolean isVideo = message.hasVideo() || 
                        (message.hasDocument() && isVideoDocument(message.getDocument())) ||
                        message.hasVideoNote() ||
                        (message.hasAudio() && isVideoAudio(message.getAudio())) ||
                        (message.hasVoice() && isVideoVoice(message.getVoice()));
            
            if (isVideo && !session.isVideoReceived()) {
                handleReviewVideo(update, user, session);
            } else if (message.hasPhoto() && session.getPhotosReceived() < 4) {
                // Проверяем, не является ли это видео, замаскированным под фото
                if (isLikelyVideoPhoto(message.getPhoto())) {
                    handleReviewVideo(update, user, session);
                } else {
                    handleReviewPhoto(update, user, session);
                }
            } else if (session.getPhotosReceived() >= 4 && session.isVideoReceived()) {
                // Все медиа уже отправлено - завершаем процесс
                System.out.println("✅ All media already received, completing submission");
                session.setStep(ReviewSubmissionSession.Step.COMPLETE);
                RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
                completeReviewSubmission(user, session);
            } else {
                // FALLBACK: Если ничего не подошло, но есть медиа, попробуем обработать как фото
                if (message.hasPhoto() || message.hasDocument() || message.hasVideo() || 
                    message.hasVideoNote() || message.hasAudio() || message.hasVoice()) {
                    if (session.getPhotosReceived() < 4) {
                        handleReviewPhoto(update, user, session);
                    } else {
                        handleReviewVideo(update, user, session);
                    }
                } else {
                    Sent sent = new Sent();
                    sent.sendMessage(user, "❌ Неверный тип медиа. Отправьте фото или видео.");
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при обработке медиа отзыва: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Обработать фотографию для отзыва
     */
    private void handleReviewPhoto(Update update, User user, ReviewSubmissionSession session) {
        if (session.getPhotosReceived() >= 4) {
            // Уже отправлено 4 фото - проверяем, есть ли видео
            if (session.isVideoReceived()) {
                // Все медиа есть - завершаем процесс
                System.out.println("✅ All media already received in handleReviewPhoto, completing submission");
                session.setStep(ReviewSubmissionSession.Step.COMPLETE);
                RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
                completeReviewSubmission(user, session);
            } else {
                // Нужно отправить видео
                Sent sent = new Sent();
                sent.sendMessage(user, "✅ У вас уже есть 4 фотографии. Теперь отправьте видео демонстрации товара:");
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
        if (session.getPhotosReceived() >= 4 && session.isVideoReceived()) {
            session.setStep(ReviewSubmissionSession.Step.COMPLETE);
            RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
            completeReviewSubmission(user, session);
            return;
        }
        
        // Отправляем сообщение о прогрессе только если процесс не завершен
        int remaining = 4 - session.getPhotosReceived();
        if (remaining > 0) {
            Sent sent = new Sent();
            sent.sendMessage(user, "✅ Фото " + session.getPhotosReceived() + "/4 получено!\n\n📸 Отправьте еще " + remaining + " фотографий или видео:");
        } else if (!session.isVideoReceived()) {
            Sent sent = new Sent();
            sent.sendMessage(user, "✅ Все 4 фотографии получены!\n\n🎥 Теперь отправьте видео демонстрации товара:");
            session.setStep(ReviewSubmissionSession.Step.MEDIA);
            RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
        }
    }
    
    /**
     * Обработать видео для отзыва
     */
    private void handleReviewVideo(Update update, User user, ReviewSubmissionSession session) {
        if (session.isVideoReceived()) {
            // Видео уже отправлено - проверяем, есть ли все фото
            if (session.getPhotosReceived() >= 4) {
                // Все медиа есть - завершаем процесс
                System.out.println("✅ All media already received in handleReviewVideo, completing submission");
                session.setStep(ReviewSubmissionSession.Step.COMPLETE);
                RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
                completeReviewSubmission(user, session);
            } else {
                // Нужно отправить еще фото
                int remaining = 4 - session.getPhotosReceived();
                Sent sent = new Sent();
                sent.sendMessage(user, "✅ Видео уже отправлено. Отправьте еще " + remaining + " фотографий товара:");
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
        if (session.getPhotosReceived() >= 4 && session.isVideoReceived()) {
            session.setStep(ReviewSubmissionSession.Step.COMPLETE);
            RedisSessionStore.setReviewSubmissionSession(user.getIdUser(), session);
            completeReviewSubmission(user, session);
        } else {
            int remainingPhotos = 4 - session.getPhotosReceived();
            if (remainingPhotos > 0) {
                Sent sent = new Sent();
                sent.sendMessage(user, "✅ Видео получено! Теперь отправьте еще " + remainingPhotos + " фотографий товара:");
            }
        }
    }
    
    /**
     * Завершить подачу отзыва
     */
    private void completeReviewSubmission(User user, ReviewSubmissionSession session) {
        System.out.println("✅ === completeReviewSubmission START ===");
        System.out.println("✅ User: " + user.getUsername() + " (ID: " + user.getIdUser() + ")");
        System.out.println("✅ Photos received: " + session.getPhotosReceived());
        System.out.println("✅ Video received: " + session.isVideoReceived());
        
        // Проверяем количество медиа
        if (session.getPhotosReceived() != 4) {
            System.out.println("❌ Invalid photo count: " + session.getPhotosReceived());
            int remaining = 4 - session.getPhotosReceived();
            Sent sent = new Sent();
            sent.sendMessage(user, "📸 Отправьте еще " + remaining + " фотографий товара:");
            return;
        }
        
        if (!session.isVideoReceived()) {
            System.out.println("❌ No video received");
            Sent sent = new Sent();
            sent.sendMessage(user, "🎥 Теперь отправьте видео демонстрации товара:");
            return;
        }
        
        System.out.println("✅ All media validation passed");
        
        // Обновляем статус покупки
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        Purchase purchase = session.getPurchase();
        System.out.println("✅ Updating purchase stage to 2");
        purchase.setPurchaseStage(2); // Этап: отзыв оставлен
        purchaseDAO.update(purchase);
        
        System.out.println("✅ Purchase updated successfully");
        
        // Отправляем медиа в группу
        System.out.println("✅ Sending media to group...");
        sendReviewMediaToGroup(user, session);
        System.out.println("✅ Media sent to group");
        
        // Отправляем сообщение пользователю о завершении
        Sent sent = new Sent();
        sent.sendMessage(user, "✅ Ваш отзыв отправлен на согласование администратору.\n\nПосле утверждения вы получите уведомление о необходимости опубликовать отзыв на Wildberries 🔔\n\n❗️Отзыв можно оставить на следующий день после получения товара с ПВЗ");
        
        // Отправляем пользовательское меню
        System.out.println("✅ Sending user menu after review submission");
        LogicUI logicUI = new LogicUI();
        logicUI.sendMenu(user, null);
        
        // Очищаем сессию
        System.out.println("✅ Clearing session...");
        RedisSessionStore.removeReviewSubmissionSession(user.getIdUser());
        RedisSessionStore.removeState(user.getIdUser());
        System.out.println("✅ Session cleared");
        
        System.out.println("✅ === completeReviewSubmission END ===");
    }
    
    /**
     * Отправить медиа отзыва в группу
     */
    private void sendReviewMediaToGroup(User user, ReviewSubmissionSession session) {
        try {
            ResourceBundle rb = ResourceBundle.getBundle("app");
            long groupID = Long.parseLong(rb.getString("tg.group"));
            
            // Отправляем в общую группу
            
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
            
            // Получаем ID подгруппы пользователя
            int userSubgroupId = user.getId_message();
            
            // Скачиваем и отправляем фотографии в группу
            String[] photoFileIds = session.getPhotoFileIds();
            
            for (int i = 0; i < session.getPhotosReceived(); i++) {
                if (photoFileIds[i] != null) {
                    // Скачиваем фото асинхронно и отправляем в подгруппу пользователя
                    AsyncService.processReviewPhotoAsync(photoFileIds[i], user.getIdUser(), i + 1)
                        .thenAccept(filePath -> {
                            if (filePath != null) {
                                sent.sendPhotoToGroupFromFile(groupID, filePath, userSubgroupId);
                            }
                        });
                }
            }
            
            // Скачиваем и отправляем видео в подгруппу пользователя
            if (session.getVideoFileId() != null) {
                // Скачиваем видео асинхронно и отправляем в подгруппу пользователя
                AsyncService.processReviewVideoAsync(session.getVideoFileId(), user.getIdUser())
                    .thenAccept(filePath -> {
                        if (filePath != null) {
                            sent.sendVideoToGroupFromFile(groupID, filePath, userSubgroupId);
                        }
                    });
            }
            
            // В конце отправляем текст с кнопками в подгруппу пользователя
            org.telegram.telegrambots.meta.api.objects.Message sentMessage = sent.sendMessageToGroupWithMarkup(groupID, text, markup, userSubgroupId);
            
            // Сохраняем ID сообщения в группе
            if (sentMessage != null) {
                session.getPurchase().setReviewMessageId((long) sentMessage.getMessageId());
                PurchaseDAO purchaseDAO = new PurchaseDAO();
                purchaseDAO.update(session.getPurchase());
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
        
        // Отправляем пользователю сообщение о начале обработки
        createTelegramBot.sendMessage(user, "🔄 Обрабатываю скриншот отзыва, пожалуйста подождите...");
        
        // Асинхронная обработка скриншота отзыва
        AsyncService.processCashbackScreenshotAsync(purchase, user, photo, fileId)
            .thenAccept(filePath -> {
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
                
                // Уведомление в группу
                try {
                    ResourceBundle rb = ResourceBundle.getBundle("app");
                    long groupID = Long.parseLong(rb.getString("tg.group"));
                    
                    String text = "💸 Пользователь @" + user.getUsername() + " запросил кешбек!\n\n" +
                            "📦 Товар: " + purchase.getProduct().getProductName() + "\n" +
                            "💰 Размер кешбека: " + purchase.getProduct().getCashbackPercentage() + "%\n" +
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
                    
                    org.telegram.telegrambots.meta.api.objects.Message sentMessage = createTelegramBot.sendMessageToGroupWithMarkup(groupID, text, markup);
                    
                    // Сохраняем ID сообщения о кешбеке в базе данных
                    if (sentMessage != null) {
                        purchase.setCashbackMessageId((long) sentMessage.getMessageId());
                        purchaseDAO.update(purchase);
                    }
                    
                    // Отправляем фото из скачанного файла
                    if (filePath != null) {
                        java.io.File photoFile = new java.io.File(filePath);
                        if (photoFile.exists()) {
                            createTelegramBot.sendPhotoToGroupFromFile(groupID, filePath);
                        } else {
                            System.err.println("❌ Cashback photo file not found: " + filePath);
                        }
                    } else {
                        System.err.println("❌ Cashback screenshot processing failed - no file path returned");
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
                "1️⃣ Убедитесь, что ваш отзыв опубликован на Wildberries\n" +
                "2️⃣ Отправьте скриншот вашего опубликованного отзыва\n" +
                "3️⃣ Дождитесь одобрения администратором\n" +
                "4️⃣ Получите кешбек на указанную карту\n\n" +
                "📸 Отправьте скриншот отзыва:";
        
        // Устанавливаем состояние для получения кешбека
        RedisSessionStore.setState(user.getIdUser(), "CASHBACK_REQUEST_" + purchaseId);
        
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
        
        System.out.println("📝 Created review submission session for user: " + user.getUsername());
        System.out.println("📝 User ID: " + user.getIdUser());
        System.out.println("📝 State set to: REVIEW_SUBMISSION");
        
        // Отправляем сообщение с просьбой написать текст отзыва
        String message = "Вы оставляли заявку на товар: \"" + purchase.getProduct().getProductName() + "\"\n" +
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
        String message = "Отлично! Теперь, пожалуйста, прикрепите фотографии и/или видео товара (4 фото и 1 видео) 📷";
        
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
        System.out.println("🔍 Handling review rejection for purchase: " + purchaseId);
        
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        Purchase purchase = purchaseDAO.findById(purchaseId);
        
        if (purchase == null) {
            System.out.println("❌ Purchase not found: " + purchaseId);
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Покупка не найдена.");
            return;
        }
        
        // Создаем сессию для ввода причины отказа
        ReviewRejectionSession rejectionSession = new ReviewRejectionSession(purchase);
        RedisSessionStore.setReviewRejectionSession(admin.getIdUser(), rejectionSession);
        RedisSessionStore.setState(admin.getIdUser(), "REVIEW_REJECTION");
        
        // Отправляем сообщение админу с запросом причины
        String message = "❌ Отклонение отзыва\n\n" +
                "Пользователь: @" + purchase.getUser().getUsername() + "\n" +
                "Товар: " + purchase.getProduct().getProductName() + "\n\n" +
                "📝 Пожалуйста, укажите причину отказа:";
        
        Sent sent = new Sent();
        sent.sendMessage(admin, message);
    }
    
    /**
     * Обработка ввода причины отказа отзыва
     */
    private void handleReviewRejectionReason(Update update, User admin) {
        System.out.println("🔍 Handling review rejection reason input");
        
        String reason = update.getMessage().getText();
        ReviewRejectionSession rejectionSession = RedisSessionStore.getReviewRejectionSession(admin.getIdUser());
        
        if (rejectionSession == null) {
            System.out.println("❌ Rejection session not found");
            Sent sent = new Sent();
            sent.sendMessage(admin, "❌ Сессия отклонения не найдена. Попробуйте еще раз.");
            return;
        }
        
        // Сохраняем причину отказа
        rejectionSession.setRejectionReason(reason);
        RedisSessionStore.setReviewRejectionSession(admin.getIdUser(), rejectionSession);
        
        // Обрабатываем отклонение
        processReviewRejection(admin, rejectionSession);
        
        // Очищаем сессию
        RedisSessionStore.removeReviewRejectionSession(admin.getIdUser());
        RedisSessionStore.removeState(admin.getIdUser());
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
        
        // Уведомляем администратора
        String adminMessage = "✅ Отзыв пользователя @" + reviewUser.getUsername() + " отклонен с причиной:\n" + reason;
        sent.sendMessage(admin, adminMessage);
    }
}