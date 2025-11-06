package org.example.tgProcessing;

import org.example.telegramBots.TelegramBot;
import org.example.table.User;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.List;
import java.util.ResourceBundle;

public class Sent {
    TelegramBot telegramBot = new TelegramBot();

    public void sendPhoto(long chatId, Integer ThreadId, long fromChatID, int messageID){
        CopyMessage copyPhoto = new CopyMessage();
        copyPhoto.setChatId(chatId);
        copyPhoto.setMessageId(messageID);
        copyPhoto.setFromChatId(fromChatID);
        if(ThreadId!=null){
            copyPhoto.setMessageThreadId(ThreadId);
        }
        telegramBot.trySendMessage(copyPhoto);
    }

    public Message sendMessage(User user, String messageText, SendMessage sendMessage) {
        // Проверяем, что текст не пустой
        if (messageText == null || messageText.trim().isEmpty()) {
            System.err.println("❌ Attempted to send empty message to user " + user.getIdUser());
            messageText = "📝 Информационное сообщение\n\n" +
                         "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                         "ℹ️ Это автоматическое сообщение от бота.\n" +
                         "Если вы видите это сообщение, значит произошла\n" +
                         "техническая ошибка при отправке основного текста.\n\n" +
                         "🔄 Пожалуйста, попробуйте повторить действие.\n\n" +
                         "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
        }
        
        sendMessage.setChatId(user.getIdUser());
        sendMessage.setText(messageText);
        sendMessage.setParseMode("HTML");

        Message sentMessage = telegramBot.trySendMessage(sendMessage);

//        ResourceBundle rb = ResourceBundle.getBundle("app");
//        long groupID = Long.parseLong(rb.getString("tg.group"));
//        if(sendMessage.getReplyMarkup()!=null){
//            SendMessage replyMessage = new SendMessage();
//
//            replyMessage.setReplyMarkup(sendMessage.getReplyMarkup());
//
//            sendMessageUser(groupID,user.getId_message(),messageText, replyMessage);
//        }else{
//            sendMessageUser(groupID,user.getId_message(),messageText);
//        }
        return sentMessage;
    }

    public Long sendMessageGroup(User user,String text, String filePath){
        ResourceBundle rb = ResourceBundle.getBundle("app");
        long groupID = Long.parseLong(rb.getString("tg.group"));
        if(filePath == null){
            SendMessage sendGroup = new SendMessage();
            sendGroup.setChatId(groupID);
            sendGroup.setText(text);
            sendGroup.setParseMode("HTML");
            sendGroup.setMessageThreadId(user.getId_message());
            Message sentMessage = telegramBot.trySendMessage(sendGroup);
            return sentMessage != null ? (long) sentMessage.getMessageId() : null;
        }else {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(groupID);
            sendPhoto.setCaption(text);
            sendPhoto.setParseMode("HTML");

            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }

            InputFile inputFile = new InputFile(file);
            sendPhoto.setPhoto(inputFile);

            Message sentMessage = telegramBot.trySendPhoto(sendPhoto);
            return sentMessage != null ? (long) sentMessage.getMessageId() : null;
        }
    }

    public void sendMessageStart(User user, String messageText, SendMessage sendMessage) {
        // Проверяем, что текст не пустой
        if (messageText == null || messageText.trim().isEmpty()) {
            System.err.println("❌ Attempted to send empty start message to user " + user.getIdUser());
            messageText = "🏠 Добро пожаловать!\n\n" +
                         "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                         "👋 Привет! Я AdaptixBot - ваш помощник\n" +
                         "для работы с товарами и кешбеком.\n\n" +
                         "📋 Доступные функции:\n" +
                         "• 📦 Каталог товаров\n" +
                         "• ⭐ Оставить отзыв\n" +
                         "• 💸 Получить кешбек\n" +
                         "• 👤 Личный кабинет\n" +
                         "• 🆘 Техподдержка\n\n" +
                         "Выберите нужное действие в меню ниже!\n\n" +
                         "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
        }
        
        sendMessage.setChatId(user.getIdUser());
        sendMessage.setText(messageText);
        sendMessage.setParseMode("HTML");

        telegramBot.trySendMessage(sendMessage);
    }

    public void sendMessageUser(long chatId, Integer messageThreadId, String messageText) {
        SendMessage groupMessage = new SendMessage();
        groupMessage.setChatId(chatId);
        groupMessage.setText(messageText);
        groupMessage.setParseMode("HTML");
        groupMessage.setMessageThreadId(messageThreadId);

        telegramBot.trySendMessage(groupMessage);
    }

    public void sendMessageUser(long chatId, Integer messageThreadId, String messageText, SendMessage message) {
        message.setChatId(chatId);
        message.setText(messageText);
        message.setParseMode("HTML");
        message.setMessageThreadId(messageThreadId);

        telegramBot.trySendMessage(message);
    }

    public void sendMessageFromBot(long chatId, String messageText) {
        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText(messageText);
        message.setParseMode("HTML");

        telegramBot.trySendMessage(message);
    }

    public void sendMessage(User user, String messageText) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(user.getIdUser());
        sendMessage.setText(messageText);
        sendMessage.setParseMode("HTML");

        telegramBot.trySendMessage(sendMessage);
    }
    
    /**
     * Отправить сообщение с разметкой
     */
    public void sendMessageWithMarkup(User user, SendMessage sendMessage) {
        // Проверяем, что текст не пустой
        String messageText = sendMessage.getText();
        if (messageText == null || messageText.trim().isEmpty()) {
            System.err.println("❌ Attempted to send empty message with markup to user " + user.getIdUser());
            messageText = "📋 Информационная панель\n\n" +
                         "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                         "ℹ️ Это информационная панель с кнопками.\n" +
                         "Используйте кнопки ниже для навигации.\n\n" +
                         "🔧 Если вы видите это сообщение, значит\n" +
                         "произошла техническая ошибка при загрузке\n" +
                         "основного контента.\n\n" +
                         "🔄 Попробуйте выбрать действие заново.\n\n" +
                         "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
            sendMessage.setText(messageText);
        }
        
        sendMessage.setChatId(user.getIdUser());
        sendMessage.setParseMode("HTML");
        
        telegramBot.trySendMessage(sendMessage);
    }
    
    public void sendReplyKeyboardMarkup(User user, ReplyKeyboardMarkup replyKeyboardMarkup, String text){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(user.getIdUser());
        sendMessage.setText(text);
        sendMessage.setParseMode("HTML");
        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        telegramBot.trySendMessage(sendMessage);
    }

    public void editMessageMarkup(User user, Integer messageId, String newText, EditMessageReplyMarkup markup) {
        EditMessageText editText = new EditMessageText();

        editText.setChatId(String.valueOf(user.getIdUser()));
        editText.setMessageId(messageId);
        editText.setText(newText);
        editText.setParseMode("HTML");
        InlineKeyboardMarkup finalMarkup;

        if (markup != null) {
            finalMarkup = markup.getReplyMarkup();
        } else {
            InlineKeyboardButton btnExit = new InlineKeyboardButton();
            btnExit.setText("Назад");
            btnExit.setCallbackData("Exit:" + messageId);

            finalMarkup = new InlineKeyboardMarkup();
            finalMarkup.setKeyboard(List.of(List.of(btnExit)));
        }

        editText.setReplyMarkup(finalMarkup);

        try {
            telegramBot.execute(editText);
        } catch (TelegramApiException e) {
            System.err.println("❌ Ошибка при обновлении текста и клавиатуры: " + e.getMessage());
        }
    }
    public void sendPhoto(long chatId, String filePath, String caption) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setCaption(caption);

        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }

        InputFile inputFile = new InputFile(file);
        sendPhoto.setPhoto(inputFile);

        telegramBot.trySendPhoto(sendPhoto);
    }

    public void sendPhotoWithButton(long chatId, String filePath, String caption, SendPhoto sendPhoto) {
        sendPhoto.setChatId(chatId);
        sendPhoto.setCaption(caption);

        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }

        InputFile inputFile = new InputFile(file);
        sendPhoto.setPhoto(inputFile);

        telegramBot.trySendPhoto(sendPhoto);
    }

    /**
     * Отправка двух фотографий в группу с текстом (используя file_id)
     */
    public Long sendTwoPhotosToGroup(User user, String text, String searchFileId, String deliveryFileId) {
        try {
            ResourceBundle rb = ResourceBundle.getBundle("app");
            long groupID = Long.parseLong(rb.getString("tg.group"));
            
            // Сначала отправляем текстовое сообщение
            SendMessage textMessage = new SendMessage();
            textMessage.setChatId(groupID);
            textMessage.setText(text);
            textMessage.setParseMode("HTML");
            textMessage.setMessageThreadId(user.getId_message());
            
            Message textMsg = telegramBot.trySendMessage(textMessage);
            if (textMsg == null) {
                System.err.println("❌ Failed to send text message");
                return null;
            }
            
            // Отправляем первое фото
            if (searchFileId != null && !searchFileId.trim().isEmpty()) {
                try {
                    SendPhoto firstPhoto = new SendPhoto();
                    firstPhoto.setChatId(groupID);
                    firstPhoto.setPhoto(new InputFile(searchFileId));
                    firstPhoto.setCaption("📸 <b>Скриншот поиска товара:</b>");
                    firstPhoto.setParseMode("HTML");
                    firstPhoto.setMessageThreadId(user.getId_message());
                    
                    telegramBot.trySendPhoto(firstPhoto);
                } catch (Exception e) {
                    System.err.println("❌ Failed to send first photo: " + e.getMessage());
                }
            }
            
            // Отправляем второе фото
            if (deliveryFileId != null && !deliveryFileId.trim().isEmpty()) {
                try {
                    SendPhoto secondPhoto = new SendPhoto();
                    secondPhoto.setChatId(groupID);
                    secondPhoto.setPhoto(new InputFile(deliveryFileId));
                    secondPhoto.setCaption("📦 <b>Скриншот раздела доставки:</b>");
                    secondPhoto.setParseMode("HTML");
                    secondPhoto.setMessageThreadId(user.getId_message());
                    
                    telegramBot.trySendPhoto(secondPhoto);

                } catch (Exception e) {
                    System.err.println("❌ Failed to send second photo: " + e.getMessage());
                }
            }
            
            return textMsg != null ? (long) textMsg.getMessageId() : null;
            
        } catch (Exception e) {
            System.err.println("❌ Error in sendTwoPhotosToGroup: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    
    /**
     * Отправить сообщение в группу
     */
    public void sendMessageToGroup(long groupId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(groupId);
        message.setText(text);
        message.setParseMode("HTML");
        
        telegramBot.trySendMessage(message);
    }
    
    /**
     * Отправить сообщение в группу с разметкой
     */
    public org.telegram.telegrambots.meta.api.objects.Message sendMessageToGroupWithMarkup(long groupId, String text, InlineKeyboardMarkup markup) {
        SendMessage message = new SendMessage();
        message.setChatId(groupId);
        message.setText(text);
        message.setParseMode("HTML");
        message.setReplyMarkup(markup);
        
        return telegramBot.trySendMessage(message);
    }
    
    /**
     * Отправить фотографию в группу с текстом
     */
    public Long sendPhotoToGroup(long groupId, String photoPath, String caption) {
        try {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(groupId);
            sendPhoto.setCaption(caption);
            sendPhoto.setParseMode("HTML");
            
            File file = new File(photoPath);
            if (!file.exists()) {
                System.err.println("❌ File does not exist: " + photoPath);
                return null;
            }
            
            InputFile inputFile = new InputFile(file);
            sendPhoto.setPhoto(inputFile);
            
            Message sentMessage = telegramBot.trySendPhoto(sendPhoto);
            return sentMessage != null ? (long) sentMessage.getMessageId() : null;
            
        } catch (Exception e) {
            System.err.println("❌ Error sending photo to group: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Скопировать сообщение из группы пользователю (без шапки "Переслано от")
     */
    public void copyMessageFromGroup(long userId, long groupId, int messageId) {
        try {
            CopyMessage copyMessage = new CopyMessage();
            copyMessage.setChatId(String.valueOf(userId));
            copyMessage.setFromChatId(String.valueOf(groupId));
            copyMessage.setMessageId(messageId);
            
            telegramBot.trySendMessage(copyMessage);
            
        } catch (Exception e) {
            System.err.println("❌ Error copying message from group: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Переслать сообщение из группы пользователю (без имени отправителя)
     */
    public void forwardMessageFromGroupAnonymous(long userId, long groupId, int messageId) {
        try {
            ForwardMessage forwardMessage = new ForwardMessage();
            forwardMessage.setChatId(String.valueOf(userId));
            forwardMessage.setFromChatId(String.valueOf(groupId));
            forwardMessage.setMessageId(messageId);
            // Скрываем имя отправителя
            forwardMessage.setDisableNotification(true);
            
           telegramBot.trySendMessage(forwardMessage);
            
        } catch (Exception e) {
            System.err.println("❌ Error forwarding message anonymously from group: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Переслать сообщение из группы пользователю
     */
    public void forwardMessageFromGroup(long userId, long groupId, int messageId) {
        try {
            ForwardMessage forwardMessage = new ForwardMessage();
            forwardMessage.setChatId(String.valueOf(userId));
            forwardMessage.setFromChatId(String.valueOf(groupId));
            forwardMessage.setMessageId(messageId);
            
            telegramBot.trySendMessage(forwardMessage);
            
        } catch (Exception e) {
            System.err.println("❌ Error forwarding message from group: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Переслать фотографию в группу
     */
    public void forwardPhotoToGroup(long groupId, String photoFileId) {
        try {
            // Проверяем валидность file_id
            if (photoFileId == null || photoFileId.trim().isEmpty()) {
                System.err.println("❌ Invalid photo file ID: " + photoFileId);
                return;
            }
            
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(groupId);
            sendPhoto.setPhoto(new InputFile(photoFileId));
            
            telegramBot.trySendPhoto(sendPhoto);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при пересылке фото в группу: " + e.getMessage());
            System.err.println("❌ File ID: " + photoFileId);
            // Попробуем отправить текстовое сообщение вместо фото
            try {
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(groupId);
                errorMessage.setText("📸 [Фото недоступно для пересылки]");
                telegramBot.trySendMessage(errorMessage);
            } catch (Exception ex) {
                System.err.println("❌ Не удалось отправить даже текстовое сообщение: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Переслать видео в группу
     */
    public void forwardVideoToGroup(long groupId, String videoFileId) {
        try {
            // Проверяем валидность file_id
            if (videoFileId == null || videoFileId.trim().isEmpty()) {
                System.err.println("❌ Invalid video file ID: " + videoFileId);
                return;
            }
            
            SendVideo sendVideo = new SendVideo();
            sendVideo.setChatId(groupId);
            sendVideo.setVideo(new InputFile(videoFileId));
            
            telegramBot.trySendVideo(sendVideo);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при пересылке видео в группу: " + e.getMessage());
            System.err.println("❌ File ID: " + videoFileId);
            // Попробуем отправить текстовое сообщение вместо видео
            try {
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(groupId);
                errorMessage.setText("🎥 [Видео недоступно для пересылки]");
                telegramBot.trySendMessage(errorMessage);
            } catch (Exception ex) {
                System.err.println("❌ Не удалось отправить даже текстовое сообщение: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Переслать сообщение в группу
     */
    public void forwardMessageToGroup(long groupId, long fromChatId, int messageId) {
        try {
            ForwardMessage forwardMessage = new ForwardMessage();
            forwardMessage.setChatId(groupId);
            forwardMessage.setFromChatId(fromChatId);
            forwardMessage.setMessageId(messageId);
            
            telegramBot.tryForwardMessage(forwardMessage);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при пересылке сообщения в группу: " + e.getMessage());
            System.err.println("❌ From chat: " + fromChatId + ", Message ID: " + messageId);
            // Попробуем отправить текстовое сообщение вместо пересылки
            try {
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(groupId);
                errorMessage.setText("📤 [Сообщение недоступно для пересылки]");
                telegramBot.trySendMessage(errorMessage);
            } catch (Exception ex) {
                System.err.println("❌ Не удалось отправить даже текстовое сообщение: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Отправить фотографию в группу из файла
     */
    public void sendPhotoToGroupFromFile(long groupId, String filePath) {
        try {
            File photoFile = new File(filePath);
            if (!photoFile.exists()) {
                System.err.println("❌ Photo file does not exist: " + filePath);
                return;
            }
            
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(groupId);
            sendPhoto.setPhoto(new InputFile(photoFile));
            
            telegramBot.trySendPhoto(sendPhoto);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при отправке фото из файла в группу: " + e.getMessage());
            System.err.println("❌ File path: " + filePath);
        }
    }
    
    /**
     * Отправить фотографию в группу из файла в подгруппу
     */
    public void sendPhotoToGroupFromFile(long groupId, String filePath, int messageThreadId) {
        try {
            File photoFile = new File(filePath);
            if (!photoFile.exists()) {
                System.err.println("❌ Photo file does not exist: " + filePath);
                return;
            }
            
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(groupId);
            sendPhoto.setPhoto(new InputFile(photoFile));
            sendPhoto.setMessageThreadId(messageThreadId);
            
            telegramBot.trySendPhoto(sendPhoto);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при отправке фото из файла в подгруппу: " + e.getMessage());
            System.err.println("❌ File path: " + filePath);
        }
    }
    
    /**
     * Отправить видео в группу из файла
     */
    public void sendVideoToGroupFromFile(long groupId, String filePath) {
        try {
            File videoFile = new File(filePath);
            if (!videoFile.exists()) {
                System.err.println("❌ Video file does not exist: " + filePath);
                return;
            }
            
            SendVideo sendVideo = new SendVideo();
            sendVideo.setChatId(groupId);
            sendVideo.setVideo(new InputFile(videoFile));
            
            telegramBot.trySendVideo(sendVideo);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при отправке видео из файла в группу: " + e.getMessage());
            System.err.println("❌ File path: " + filePath);
        }
    }
    
    /**
     * Отправить видео в группу из файла в подгруппу
     */
    public void sendVideoToGroupFromFile(long groupId, String filePath, int messageThreadId) {
        try {
            File videoFile = new File(filePath);
            if (!videoFile.exists()) {
                System.err.println("❌ Video file does not exist: " + filePath);
                return;
            }
            
            SendVideo sendVideo = new SendVideo();
            sendVideo.setChatId(groupId);
            sendVideo.setVideo(new InputFile(videoFile));
            sendVideo.setMessageThreadId(messageThreadId);
            
            telegramBot.trySendVideo(sendVideo);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при отправке видео из файла в подгруппу: " + e.getMessage());
            System.err.println("❌ File path: " + filePath);
        }
    }
    
    /**
     * Удалить сообщение
     */
    public void deleteMessage(long chatId, int messageId) {
        try {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(String.valueOf(chatId));
            deleteMessage.setMessageId(messageId);
            
            telegramBot.execute(deleteMessage);
        } catch (Exception e) {
            System.err.println("❌ Error deleting message: " + e.getMessage());
        }
    }
    
    /**
     * Отправить сообщение в группу с подгруппой (topic)
     */
    public org.telegram.telegrambots.meta.api.objects.Message sendMessageToGroupWithMarkup(long groupId, String text, InlineKeyboardMarkup markup, int messageThreadId) {
        SendMessage message = new SendMessage();
        message.setChatId(groupId);
        message.setText(text);
        message.setParseMode("HTML");
        message.setReplyMarkup(markup);
        message.setMessageThreadId(messageThreadId);
        
        return telegramBot.trySendMessage(message);
    }
    
    /**
     * Переслать фотографию в группу с подгруппой (topic) используя ForwardMessage
     */
    public void forwardPhotoToGroup(long groupId, String photoFileId, int messageThreadId) {
        // Этот метод больше не используется - заменен на forwardMessageToGroupWithThread
    }
    
    /**
     * Отправить фотографию в группу с подгруппой (topic) из file_id
     */
    public void sendPhotoToGroupFromFileId(long groupId, String photoFileId, int messageThreadId) {
        try {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(String.valueOf(groupId));
            sendPhoto.setPhoto(new InputFile(photoFileId));
            sendPhoto.setMessageThreadId(messageThreadId);
            

            org.telegram.telegrambots.meta.api.objects.Message response = telegramBot.execute(sendPhoto);

        } catch (Exception e) {
            System.err.println("❌ Ошибка при отправке фото в группу: " + e.getMessage());
            System.err.println("❌ File ID: " + photoFileId + ", Thread: " + messageThreadId);
            e.printStackTrace();
        }
    }
    
    /**
     * Отправить видео в группу с подгруппой (topic) из file_id
     */
    public void sendVideoToGroupFromFileId(long groupId, String videoFileId, int messageThreadId) {
        try {
            SendVideo sendVideo = new SendVideo();
            sendVideo.setChatId(String.valueOf(groupId));
            sendVideo.setVideo(new InputFile(videoFileId));
            sendVideo.setMessageThreadId(messageThreadId);

            org.telegram.telegrambots.meta.api.objects.Message response = telegramBot.execute(sendVideo);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при отправке видео в группу: " + e.getMessage());
            System.err.println("❌ File ID: " + videoFileId + ", Thread: " + messageThreadId);
            e.printStackTrace();
        }
    }
    
    /**
     * Переслать видео в группу с подгруппой (topic) используя ForwardMessage
     */
    public void forwardVideoToGroup(long groupId, String videoFileId, int messageThreadId) {
        // Этот метод больше не используется - заменен на forwardMessageToGroupWithThread
    }
    
    /**
     * Переслать медиа (фото/видео) в тему пользователя в группе
     */
    public void forwardMediaToUserTopic(long userId, long groupId, int messageThreadId, String fileId, String mediaType) {
        try {
            if (fileId == null || fileId.trim().isEmpty()) {
                System.err.println("❌ Invalid " + mediaType + " file ID: " + fileId);
                return;
            }
            
            if (mediaType.equals("photo")) {
                SendPhoto sendPhoto = new SendPhoto();
                sendPhoto.setChatId(String.valueOf(groupId));
                sendPhoto.setPhoto(new InputFile(fileId));
                sendPhoto.setMessageThreadId(messageThreadId);
                
                telegramBot.trySendPhoto(sendPhoto);
                
            } else if (mediaType.equals("video")) {
                SendVideo sendVideo = new SendVideo();
                sendVideo.setChatId(String.valueOf(groupId));
                sendVideo.setVideo(new InputFile(fileId));
                sendVideo.setMessageThreadId(messageThreadId);
                
                telegramBot.trySendVideo(sendVideo);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Ошибка при пересылке " + mediaType + " в тему пользователя: " + e.getMessage());
            System.err.println("❌ File ID: " + fileId + ", User: " + userId + ", Thread: " + messageThreadId);
            
            // Попробуем отправить текстовое сообщение вместо медиа
            try {
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(String.valueOf(groupId));
                errorMessage.setMessageThreadId(messageThreadId);
                errorMessage.setText("📎 [" + (mediaType.equals("photo") ? "Фото" : "Видео") + " недоступно для пересылки]");
                telegramBot.trySendMessage(errorMessage);
            } catch (Exception ex) {
                System.err.println("❌ Не удалось отправить даже текстовое сообщение: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Переслать скриншот поиска в тему пользователя
     */
    public void forwardSearchScreenshotToUserTopic(User user, String photoFileId) {
        try {
            ResourceBundle rb = ResourceBundle.getBundle("app");
            long groupId = Long.parseLong(rb.getString("tg.group"));
            int userTopicId = user.getId_message();
            

            // Отправляем заголовок
            SendMessage headerMessage = new SendMessage();
            headerMessage.setChatId(String.valueOf(groupId));
            headerMessage.setMessageThreadId(userTopicId);
            headerMessage.setText("📸 <b>Скриншот поиска товара:</b>");
            headerMessage.setParseMode("HTML");
            telegramBot.trySendMessage(headerMessage);
            
            // Отправляем фото по file_id
            if (photoFileId != null && !photoFileId.trim().isEmpty()) {
                SendPhoto photo = new SendPhoto();
                photo.setChatId(groupId);
                photo.setPhoto(new InputFile(photoFileId));
                photo.setMessageThreadId(userTopicId);
                
                telegramBot.trySendPhoto(photo);
                }
            
        } catch (Exception e) {
            System.err.println("❌ Ошибка при отправке скриншота поиска: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Переслать скриншот доставки в тему пользователя
     */
    public void forwardDeliveryScreenshotToUserTopic(User user, String photoFileId) {
        try {
            ResourceBundle rb = ResourceBundle.getBundle("app");
            long groupId = Long.parseLong(rb.getString("tg.group"));
            int userTopicId = user.getId_message();
            

            // Отправляем заголовок
            SendMessage headerMessage = new SendMessage();
            headerMessage.setChatId(String.valueOf(groupId));
            headerMessage.setMessageThreadId(userTopicId);
            headerMessage.setText("📦 <b>Скриншот раздела доставки:</b>");
            headerMessage.setParseMode("HTML");
            telegramBot.trySendMessage(headerMessage);
            
            // Отправляем фото по file_id
            if (photoFileId != null && !photoFileId.trim().isEmpty()) {
                SendPhoto photo = new SendPhoto();
                photo.setChatId(groupId);
                photo.setPhoto(new InputFile(photoFileId));
                photo.setMessageThreadId(userTopicId);
                
                telegramBot.trySendPhoto(photo);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Ошибка при отправке скриншота доставки: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Переслать все медиа пользователя в его тему после завершения покупки
     */
    public void forwardAllUserMediaToTopic(User user, String searchScreenshotFileId, String deliveryScreenshotFileId) {
        try {

            // Отправляем скриншот поиска
            if (searchScreenshotFileId != null && !searchScreenshotFileId.trim().isEmpty()) {
                forwardSearchScreenshotToUserTopic(user, searchScreenshotFileId);
            }
            
            // Небольшая задержка между отправками
            Thread.sleep(1000);
            
            // Отправляем скриншот доставки
            if (deliveryScreenshotFileId != null && !deliveryScreenshotFileId.trim().isEmpty()) {
                forwardDeliveryScreenshotToUserTopic(user, deliveryScreenshotFileId);
            }
            

        } catch (Exception e) {
            System.err.println("❌ Ошибка при отправке всех медиа пользователя: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Отправить медиа отзыва в группу используя копирование сообщений
     */
    public Long sendReviewMediaToGroup(User user, String[] photoFileIds, Integer[] photoMessageIds, 
                                     String videoFileId, Integer videoMessageId, 
                                     String text, InlineKeyboardMarkup markup) {
        try {
            ResourceBundle rb = ResourceBundle.getBundle("app");
            long groupID = Long.parseLong(rb.getString("tg.group"));
            int userSubgroupId = user.getId_message();
            
            // Отправляем текстовое сообщение с кнопками
            SendMessage textMessage = new SendMessage();
            textMessage.setChatId(groupID);
            textMessage.setText(text);
            textMessage.setParseMode("HTML");
            textMessage.setMessageThreadId(userSubgroupId);
            textMessage.setReplyMarkup(markup);
            
            Message sentMessage = telegramBot.trySendMessage(textMessage);
            
            // Возвращаем ID текстового сообщения
            Long textMessageId = sentMessage != null ? (long) sentMessage.getMessageId() : null;
            
            // Копируем фотографии
            if (photoFileIds != null && photoMessageIds != null) {
                for (int i = 0; i < photoFileIds.length; i++) {
                    if (photoFileIds[i] != null && photoMessageIds[i] != null) {
                        try {
                            // Отправляем фото по file_id
                            SendPhoto photo = new SendPhoto();
                            photo.setChatId(groupID);
                            photo.setPhoto(new InputFile(photoFileIds[i]));
                            photo.setMessageThreadId(userSubgroupId);
                            
                            telegramBot.trySendPhoto(photo);
                        } catch (Exception e) {
                            System.err.println("❌ Failed to send review photo " + (i + 1) + ": " + e.getMessage());
                        }
                    }
                }
            }
            
            // Копируем видео
            if (videoFileId != null && videoMessageId != null) {
                try {
                    // Отправляем видео по file_id
                    SendVideo video = new SendVideo();
                    video.setChatId(groupID);
                    video.setVideo(new InputFile(videoFileId));
                    video.setMessageThreadId(userSubgroupId);
                    
                    telegramBot.trySendVideo(video);
                } catch (Exception e) {
                    System.err.println("❌ Failed to send review video: " + e.getMessage());
                }
            }
            
            return textMessageId;
            
        } catch (Exception e) {
            System.err.println("❌ Error in sendReviewMediaToGroup: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Отправить скриншот кешбека в группу используя file_id
     */
    public Long sendCashbackScreenshotToGroup(User user, String screenshotFileId, Integer screenshotMessageId, 
                                             String text, InlineKeyboardMarkup markup) {
        try {
            ResourceBundle rb = ResourceBundle.getBundle("app");
            long groupID = Long.parseLong(rb.getString("tg.group"));
            int userSubgroupId = user.getId_message();
            
            // Отправляем текстовое сообщение с кнопками
            SendMessage textMessage = new SendMessage();
            textMessage.setChatId(groupID);
            textMessage.setText(text);
            textMessage.setParseMode("HTML");
            textMessage.setMessageThreadId(userSubgroupId);
            textMessage.setReplyMarkup(markup);
            
            Message sentMessage = telegramBot.trySendMessage(textMessage);
            // Возвращаем ID текстового сообщения
            Long textMessageId = sentMessage != null ? (long) sentMessage.getMessageId() : null;
            
            // Отправляем скриншот кешбека
            if (screenshotFileId != null && screenshotMessageId != null) {
                try {
                    // Отправляем фото по file_id
                    SendPhoto photo = new SendPhoto();
                    photo.setChatId(groupID);
                    photo.setPhoto(new InputFile(screenshotFileId));
                    photo.setMessageThreadId(userSubgroupId);
                    
                    telegramBot.trySendPhoto(photo);

                } catch (Exception e) {
                    System.err.println("❌ Failed to send cashback screenshot: " + e.getMessage());
                }
            }
            
            return textMessageId;
            
        } catch (Exception e) {
            System.err.println("❌ Error in sendCashbackScreenshotToGroup: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Ответ на callback query
     */
    public void answerCallbackQuery(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery answerCallbackQuery) {
        try {
            telegramBot.execute(answerCallbackQuery);
        } catch (TelegramApiException e) {
            System.err.println("❌ Ошибка при ответе на callback query: " + e.getMessage());
        }
    }
}
