package org.example.tgProcessing;

import org.example.dao.UserDAO;
import org.example.table.User;
import org.example.telegramBots.TelegramBotLogs;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.ArrayList;
import java.util.List;

public class LogicUI {

    public void sendStart(long chatId,Update update) {
        Sent sent = new Sent();
        TelegramBotLogs telegramBotLogs = new TelegramBotLogs();
        UserDAO userDAO = new UserDAO();

        User user = userDAO.findById(chatId);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Моя подписка");
        row1.add("Тарифы");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Меню");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(List.of(row1,row2));
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

    public void sendMenu(User user) {
        Sent sent = new Sent();

        SendMessage sendMessage = new SendMessage();
        Message sentMessage = sent.sendMessage(user,"Выберите действие Меню",sendMessage);
        int messageId = sentMessage.getMessageId();

        List<InlineKeyboardButton> row1 = new ArrayList<>();

        InlineKeyboardButton btnCount = new InlineKeyboardButton();
        btnCount.setText("Количество");
        btnCount.setCallbackData("Count:" + messageId);
        row1.add(btnCount);

        InlineKeyboardButton btnSubs = new InlineKeyboardButton();
        btnSubs.setText("Подписки");
        btnSubs.setCallbackData("Subs:" + messageId);
        row1.add(btnSubs);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        if(!user.isAdmin() || user.isUserFlag()){
            InlineKeyboardButton btnOn = new InlineKeyboardButton();
            btnOn.setText("Включить рассылку");
                btnOn.setCallbackData("on:" + messageId);
            row2.add(btnOn);
        }
        if(user.isAdmin()){
            InlineKeyboardButton btnAdmin = new InlineKeyboardButton();
            btnAdmin.setText("Админ меню");
            btnAdmin.setCallbackData("AdminMenu:" + messageId);
            row2.add(btnAdmin);
        }

        InlineKeyboardButton btnOther = new InlineKeyboardButton();
        btnOther.setText("Что-то");
        btnOther.setCallbackData("Wait:" + messageId);
        row2.add(btnOther);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        inlineKeyboard.setKeyboard(keyboard);

        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(user.getIdUser());
        editMarkup.setMessageId(messageId);
        editMarkup.setReplyMarkup(inlineKeyboard);

        sent.editMessageMarkup(user,messageId,"Выберите действие: Меню",editMarkup);
    }
    public void updateMenu(User user, int messageId){
        Sent sent = new Sent();

        List<InlineKeyboardButton> row1 = new ArrayList<>();

        InlineKeyboardButton btnCount = new InlineKeyboardButton();
        btnCount.setText("Количество");
        btnCount.setCallbackData("Count:" + messageId);
        row1.add(btnCount);

        InlineKeyboardButton btnSubs = new InlineKeyboardButton();
        btnSubs.setText("Подписки");
        btnSubs.setCallbackData("Subs:" + messageId);
        row1.add(btnSubs);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        if(!user.isAdmin() || user.isUserFlag()){
            InlineKeyboardButton btnOn = new InlineKeyboardButton();
            btnOn.setText("Включить рассылку");
            btnOn.setCallbackData("on:" + messageId);
            row2.add(btnOn);
        }
        if(user.isAdmin()){
            InlineKeyboardButton btnAdmin = new InlineKeyboardButton();
            btnAdmin.setText("Админ меню");
            btnAdmin.setCallbackData("AdminMenu:" + messageId);
            row2.add(btnAdmin);
        }

        InlineKeyboardButton btnOther = new InlineKeyboardButton();
        btnOther.setText("Что-то");
        btnOther.setCallbackData("Wait:" + messageId);
        row2.add(btnOther);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        inlineKeyboard.setKeyboard(keyboard);

        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(user.getIdUser());
        editMarkup.setMessageId(messageId);
        editMarkup.setReplyMarkup(inlineKeyboard);

        sent.editMessageMarkup(user,messageId,"Выберите действие: меню",editMarkup);
    }
    public void sendAdminMenu(User user, int messageId){
        Sent sent = new Sent();
        if(user.isAdmin()){
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton btnAddAdmin = new InlineKeyboardButton();
            btnAddAdmin.setText("Добавить админа");
            btnAddAdmin.setCallbackData("addAdmin:" + messageId);
            row1.add(btnAddAdmin);

            InlineKeyboardButton btnCreateMailing = new InlineKeyboardButton();
            btnCreateMailing.setText("Создать рассылку");
            btnCreateMailing.setCallbackData("createMailing:" + messageId);
            row1.add(btnCreateMailing);

            InlineKeyboardButton btnIsAdmin= new InlineKeyboardButton();
            InlineKeyboardButton btnTariffs = new InlineKeyboardButton();
            if(user.isUserFlag()){
                btnIsAdmin.setText("Пользователь");
                btnIsAdmin.setCallbackData("isUser:" + messageId);
                row2.add(btnIsAdmin);

                btnTariffs.setText("Тарифы");
                btnTariffs.setCallbackData("isTariff:" + messageId);
                row2.add(btnTariffs);
            }else {
                btnIsAdmin.setText("Админ");
                btnIsAdmin.setCallbackData("isUser:" + messageId);
                row2.add(btnIsAdmin);

                btnTariffs.setText("Изменить тарифы");
                btnTariffs.setCallbackData("isTariff:" + messageId);
                row2.add(btnTariffs);
            }

            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            keyboard.add(row1);
            keyboard.add(row2);

            InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
            inlineKeyboard.setKeyboard(keyboard);

            EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
            editMarkup.setChatId(user.getIdUser());
            editMarkup.setMessageId(messageId);
            editMarkup.setReplyMarkup(inlineKeyboard);

            sent.editMessageMarkup(user,messageId,"Выберите действие: Админ меню",editMarkup);
        }else sendMenu(user);
    }
}
