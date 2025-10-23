package org.example.tgProcessing;

import org.example.telegramBots.TelegramBot;
import org.example.table.User;
import org.example.telegramBots.TelegramBotLogs;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
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

        ResourceBundle rb = ResourceBundle.getBundle("app");
        long groupID = Long.parseLong(rb.getString("tg.group"));

        sendMessage.setChatId(user.getIdUser());
        sendMessage.setText(messageText);
        sendMessage.setParseMode("HTML");

        telegramBot.trySendMessage(sendMessage);

//        sendMessageUser(groupID,user.getId_message(),messageText);
    }

    public void sendReplyKeyboardMarkup(User user, ReplyKeyboardMarkup replyKeyboardMarkup, String text){
        SendMessage sendMessage = new SendMessage();

        ResourceBundle rb = ResourceBundle.getBundle("app");
        long groupID = Long.parseLong(rb.getString("tg.group"));

        sendMessage.setChatId(user.getIdUser());
        sendMessage.setText(text);
        sendMessage.setParseMode("HTML");
        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        telegramBot.trySendMessage(sendMessage);
//        sendMessageUser(groupID,user.getId_message(),text);
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
            System.out.println("First photo does not exist: " + firstPhotoPath);
            return null;
        }

        InputFile firstInputFile = new InputFile(firstFile);
        firstPhoto.setPhoto(firstInputFile);

        Message firstMessage = telegramBotLogs.trySendPhoto(firstPhoto);
        
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
     * Переслать фотографию в группу
     */
    public void forwardPhotoToGroup(long groupId, String photoFileId) {
        try {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(groupId);
            sendPhoto.setPhoto(new InputFile(photoFileId));
            
            telegramBotLogs.trySendPhoto(sendPhoto);
        } catch (Exception e) {
            System.err.println("Ошибка при пересылке фото в группу: " + e.getMessage());
        }
    }
    
    /**
     * Переслать видео в группу
     */
    public void forwardVideoToGroup(long groupId, String videoFileId) {
        try {
            SendVideo sendVideo = new SendVideo();
            sendVideo.setChatId(groupId);
            sendVideo.setVideo(new InputFile(videoFileId));
            
            telegramBotLogs.trySendVideo(sendVideo);
        } catch (Exception e) {
            System.err.println("Ошибка при пересылке видео в группу: " + e.getMessage());
        }
    }
}
