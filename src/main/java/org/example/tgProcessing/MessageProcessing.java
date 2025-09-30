package org.example.tgProcessing;

import org.example.dao.UserDAO;
import org.example.table.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ResourceBundle;

public class MessageProcessing {

    public void sentPhotoUpdate(Update update){
        Sent sent = new Sent();
        long chatId = update.getMessage().getChatId();
        UserDAO userDAO = new UserDAO();
        Integer threadID = update.getMessage().getMessageThreadId();
        if(threadID!=null){
            User user = userDAO.findByIdMessage(threadID);
            sent.sendPhoto(user.getIdUser(),null,chatId,update.getMessage().getMessageId());
        }else {
            User people = userDAO.findById(chatId);
            ResourceBundle rb = ResourceBundle.getBundle("app");
            long groupID = Long.parseLong(rb.getString("tg.group"));

            sent.sendPhoto(groupID,people.getId_message(),chatId,update.getMessage().getMessageId());
        }
    }

    public void handleUpdate(Update update) throws TelegramApiException, IOException {
        Sent createTelegramBot = new Sent();
        LogicUI logicUI = new LogicUI();

        ResourceBundle rb = ResourceBundle.getBundle("app");
        long groupID = Long.parseLong(rb.getString("tg.group"));

        String msg = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        UserDAO userDAO = new UserDAO();
        Integer threadID = update.getMessage().getMessageThreadId();
        if(threadID!=null){
            User user = userDAO.findByIdMessage(threadID);
            createTelegramBot.sendMessageFromBot(user.getIdUser(),msg);
            return;
        }

        User user = userDAO.findById(chatId);

        if(user!=null){
            createTelegramBot.sendMessageUser(groupID,user.getId_message(),"Пользователь: " + msg);
        }else {
            logicUI.sendStart(chatId,update);
            return;
        }

//        String state = SessionStore.getState(chatId);
//
//        if(state!= null && !msg.equals("back")){
//
//        }

        switch (msg) {
            case "/start" -> logicUI.sendStart(chatId,update);
            case "Меню" -> logicUI.sendMenu(user);
            default -> {
                logicUI.sendMenu(user);
            }
        }
    }


    public void callBackQuery(Update update){
        Sent createTelegramBot = new Sent();
        LogicUI logicUI = new LogicUI();
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        UserDAO userDAO = new UserDAO();
        User user = userDAO.findById(chatId);

        if(data.contains(":")){
            String[] parts = data.split(":",2);
            String command = parts[0];
            String messageID = parts[1];
            switch (command){
                case "Menu" :{
                    logicUI.updateMenu(user, Integer.parseInt(messageID));
                    break;
                }
                case "AdminMenu" :{
                    logicUI.sendAdminMenu(user, Integer.parseInt(messageID));
                    break;
                }
                case "addAdmin" :{
                    if(user.isAdmin()){
//                        SessionStore.setState(chatId,"addAdmin_");
                        createTelegramBot.editMessageMarkup(user, Integer.parseInt(messageID), "Отправьте тег (Например @qwerty123)", null);
                    }
                    else {
                        logicUI.sendMenu(user);
                    }
                    break;
                }
            }
        }
        switch (data) {
            case "Меню" -> logicUI.sendMenu(user);
            default -> {
            }
        }
    }
}