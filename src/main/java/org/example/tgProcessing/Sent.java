package org.example.tgProcessing;

import org.example.telegramBots.TelegramBot;
import org.example.table.User;
import org.example.telegramBots.TelegramBotLogs;
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
    TelegramBotLogs telegramBotLogs = new TelegramBotLogs();

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
            Message sentMessage = telegramBotLogs.trySendMessage(sendGroup);
            return sentMessage != null ? (long) sentMessage.getMessageId() : null;
        }else {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(groupID);
            sendPhoto.setCaption(text);
            sendPhoto.setParseMode("HTML");

            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("File does not exist: " + filePath);
                return null;
            }

            InputFile inputFile = new InputFile(file);
            sendPhoto.setPhoto(inputFile);

            Message sentMessage = telegramBotLogs.trySendPhoto(sendPhoto);
            return sentMessage != null ? (long) sentMessage.getMessageId() : null;
        }
    }

    public void sendMessageStart(User user, String messageText, SendMessage sendMessage) {
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

        telegramBotLogs.trySendMessage(groupMessage);
    }

    public void sendMessageUser(long chatId, Integer messageThreadId, String messageText, SendMessage message) {
        message.setChatId(chatId);
        message.setText(messageText);
        message.setParseMode("HTML");
        message.setMessageThreadId(messageThreadId);

        telegramBotLogs.trySendMessage(message);
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
            System.out.println("File does not exist: " + filePath);
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
            System.out.println("File does not exist: " + filePath);
            return;
        }

        InputFile inputFile = new InputFile(file);
        sendPhoto.setPhoto(inputFile);

        telegramBot.trySendPhoto(sendPhoto);
    }

    /**
     * Отправка двух фотографий в группу с текстом
     */
    public Long sendTwoPhotosToGroup(User user, String text, String firstPhotoPath, String secondPhotoPath) {
        ResourceBundle rb = ResourceBundle.getBundle("app");
        long groupID = Long.parseLong(rb.getString("tg.group"));
        
        // Отправляем первое сообщение с текстом и первой фотографией
        SendPhoto firstPhoto = new SendPhoto();
        firstPhoto.setChatId(groupID);
        firstPhoto.setCaption(text);
        firstPhoto.setParseMode("HTML");
        firstPhoto.setMessageThreadId(user.getId_message());

        File firstFile = new File(firstPhotoPath);
        if (!firstFile.exists()) {
            return null;
        }

        InputFile firstInputFile = new InputFile(firstFile);
        firstPhoto.setPhoto(firstInputFile);

        Message firstMessage = telegramBotLogs.trySendPhoto(firstPhoto);
        
        if (firstMessage == null) {
            return null;
        }
        
        // Отправляем вторую фотографию как медиа-группу
        if (firstMessage != null && secondPhotoPath != null) {
            File secondFile = new File(secondPhotoPath);
            if (secondFile.exists()) {
                SendPhoto secondPhoto = new SendPhoto();
                secondPhoto.setChatId(groupID);
                secondPhoto.setCaption("📦 Скриншот раздела доставки:");
                secondPhoto.setParseMode("HTML");
                secondPhoto.setMessageThreadId(user.getId_message());

                InputFile secondInputFile = new InputFile(secondFile);
                secondPhoto.setPhoto(secondInputFile);

                telegramBotLogs.trySendPhoto(secondPhoto);
            }
        }
        
        return firstMessage != null ? (long) firstMessage.getMessageId() : null;
    }
    
    /**
     * Отправить сообщение в группу
     */
    public void sendMessageToGroup(long groupId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(groupId);
        message.setText(text);
        message.setParseMode("HTML");
        
        telegramBotLogs.trySendMessage(message);
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
        
        return telegramBotLogs.trySendMessage(message);
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
            
            System.out.println("📸 Forwarding photo to group " + groupId + " with file ID: " + photoFileId);
            telegramBotLogs.trySendPhoto(sendPhoto);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при пересылке фото в группу: " + e.getMessage());
            System.err.println("❌ File ID: " + photoFileId);
            // Попробуем отправить текстовое сообщение вместо фото
            try {
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(groupId);
                errorMessage.setText("📸 [Фото недоступно для пересылки]");
                telegramBotLogs.trySendMessage(errorMessage);
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
            
            System.out.println("🎥 Forwarding video to group " + groupId + " with file ID: " + videoFileId);
            telegramBotLogs.trySendVideo(sendVideo);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при пересылке видео в группу: " + e.getMessage());
            System.err.println("❌ File ID: " + videoFileId);
            // Попробуем отправить текстовое сообщение вместо видео
            try {
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(groupId);
                errorMessage.setText("🎥 [Видео недоступно для пересылки]");
                telegramBotLogs.trySendMessage(errorMessage);
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
            
            System.out.println("📤 Forwarding message " + messageId + " from chat " + fromChatId + " to group " + groupId);
            telegramBotLogs.tryForwardMessage(forwardMessage);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при пересылке сообщения в группу: " + e.getMessage());
            System.err.println("❌ From chat: " + fromChatId + ", Message ID: " + messageId);
            // Попробуем отправить текстовое сообщение вместо пересылки
            try {
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(groupId);
                errorMessage.setText("📤 [Сообщение недоступно для пересылки]");
                telegramBotLogs.trySendMessage(errorMessage);
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
            
            System.out.println("📸 Sending photo from file to group " + groupId + ": " + filePath);
            telegramBotLogs.trySendPhoto(sendPhoto);
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
            
            System.out.println("📸 Sending photo from file to group " + groupId + " in thread " + messageThreadId + ": " + filePath);
            telegramBotLogs.trySendPhoto(sendPhoto);
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
            
            System.out.println("🎥 Sending video from file to group " + groupId + ": " + filePath);
            telegramBotLogs.trySendVideo(sendVideo);
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
            
            System.out.println("🎥 Sending video from file to group " + groupId + " in thread " + messageThreadId + ": " + filePath);
            telegramBotLogs.trySendVideo(sendVideo);
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
            
            telegramBotLogs.execute(deleteMessage);
            System.out.println("✅ Message deleted: " + messageId);
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
        
        System.out.println("📝 Sending message to group " + groupId + " in thread " + messageThreadId);
        return telegramBotLogs.trySendMessage(message);
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
            
            System.out.println("📸 Sending photo to group " + groupId + " in thread " + messageThreadId + " with file ID: " + photoFileId);
            
            org.telegram.telegrambots.meta.api.objects.Message response = telegramBotLogs.execute(sendPhoto);
            
            if (response != null) {
                System.out.println("✅ Photo sent successfully!");
            } else {
                System.err.println("❌ Failed to send photo");
            }
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
            
            System.out.println("🎥 Sending video to group " + groupId + " in thread " + messageThreadId + " with file ID: " + videoFileId);
            
            org.telegram.telegrambots.meta.api.objects.Message response = telegramBotLogs.execute(sendVideo);
            
            if (response != null) {
                System.out.println("✅ Video sent successfully!");
            } else {
                System.err.println("❌ Failed to send video");
            }
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
