package org.example.tgProcessing;

import org.example.dao.PeopleDAO;
import org.example.dao.TariffDAO;
import org.example.table.People;
import org.example.table.Tariff;
import org.example.telegramBots.TelegramBotLogs;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogicUI {

    public void sendStart(long chatId,Update update) {
        Sent sent = new Sent();
        TelegramBotLogs telegramBotLogs = new TelegramBotLogs();
        PeopleDAO peopleDAO = new PeopleDAO();

        People people = peopleDAO.findById(chatId);

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
        if(people==null){
            people = new People();
            people.setTgId(chatId);
            people.setMarketing(true);
            people.setActive(true);
            people.setSubscriptionTime("0");
            people.setAdmin(false);
            people.setUsername(update.getMessage().getFrom().getUserName());
            people.setFirstName(update.getMessage().getFrom().getFirstName());
            people.setUser_flag(true);
            sent.sendMessageStart(people, people.getFirstName() + ", Вас приветствует WB бот подписок", sendMessage);
            List<Long> messageIdAndGroup = telegramBotLogs.createTopic(update);
            people.setGroupID(messageIdAndGroup.getFirst());
            people.setId_message(Math.toIntExact(messageIdAndGroup.getLast()));
            peopleDAO.save(people);
        }else{
            sent.sendMessage(people,people.getFirstName() + ", с возвращением!", sendMessage);
        }
    }

    public void sendSubscription(People people) {
        Sent sent = new Sent();
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

        sent.sendMessage(people,"Выберите действие Подписки",sendMessage);
    }

    public void sendMenu(People people) {
        Sent sent = new Sent();

        SendMessage sendMessage = new SendMessage();
        Message sentMessage = sent.sendMessage(people,"Выберите действие Меню",sendMessage);
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
        if(people.isMarketing() && (!people.isAdmin() || people.isUser_flag())){
            InlineKeyboardButton btnOff = new InlineKeyboardButton();
            btnOff.setText("Отключить рассылку");
            btnOff.setCallbackData("off:" + messageId);
            row2.add(btnOff);
        }else if(!people.isAdmin() || people.isUser_flag()){
            InlineKeyboardButton btnOn = new InlineKeyboardButton();
            btnOn.setText("Включить рассылку");
                btnOn.setCallbackData("on:" + messageId);
            row2.add(btnOn);
        }
        if(people.isAdmin()){
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
        editMarkup.setChatId(people.getTgId());
        editMarkup.setMessageId(messageId);
        editMarkup.setReplyMarkup(inlineKeyboard);

        sent.editMessageMarkup(people,messageId,"Выберите действие: Меню",editMarkup);
    }
    public void updateMenu(People people, int messageId){
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
        if(people.isMarketing() && (!people.isAdmin() || people.isUser_flag())){
            InlineKeyboardButton btnOff = new InlineKeyboardButton();
            btnOff.setText("Отключить рассылку");
            btnOff.setCallbackData("off:" + messageId);
            row2.add(btnOff);
        }else if(!people.isAdmin() || people.isUser_flag()){
            InlineKeyboardButton btnOn = new InlineKeyboardButton();
            btnOn.setText("Включить рассылку");
            btnOn.setCallbackData("on:" + messageId);
            row2.add(btnOn);
        }
        if(people.isAdmin()){
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
        editMarkup.setChatId(people.getTgId());
        editMarkup.setMessageId(messageId);
        editMarkup.setReplyMarkup(inlineKeyboard);

        sent.editMessageMarkup(people,messageId,"Выберите действие: меню",editMarkup);
    }
    public void sendAdminMenu(People people, int messageId){
        Sent sent = new Sent();
        if(people.isAdmin()){
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
            if(people.isUser_flag()){
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
            editMarkup.setChatId(people.getTgId());
            editMarkup.setMessageId(messageId);
            editMarkup.setReplyMarkup(inlineKeyboard);

            sent.editMessageMarkup(people,messageId,"Выберите действие: Админ меню",editMarkup);
        }else sendMenu(people);
    }

    public void sendTariff(People people) {
        Sent sent = new Sent();
        TariffDAO tariffDAO = new TariffDAO();

        SendMessage sendMessage = new SendMessage();
        Message sentMessage = sent.sendMessage(people,"📦 Выберите тариф:",sendMessage);
        int messageId = sentMessage.getMessageId();

        List<Tariff> tariffs = (people.isAdmin() && people.isUser_flag())
                ? tariffDAO.findAllVisible()
                : tariffDAO.findAll();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Tariff tariff : tariffs) {
            InlineKeyboardButton button = new InlineKeyboardButton();

            BigDecimal discount = tariff.getDiscount().multiply(BigDecimal.valueOf(0.01));
            BigDecimal discountedPrice = tariff.getPrice().subtract(tariff.getPrice().multiply(discount));
            BigDecimal formattedPrice = discountedPrice.setScale(1, RoundingMode.HALF_UP);

            button.setText(tariff.getName() + " - " + formattedPrice + " ₽");

            button.setCallbackData("tariff_:" + tariff.getId() + ":" + messageId);

            rows.add(List.of(button));
        }
        if(people.isAdmin() && !people.isUser_flag()){
            InlineKeyboardButton addTariff = new InlineKeyboardButton("Добавить тариф");
            addTariff.setCallbackData("addTariff_");
            rows.add(List.of(addTariff));
        }

        InlineKeyboardButton back = new InlineKeyboardButton("⬅️ Назад");
        back.setCallbackData("Menu:" + messageId);
        rows.add(List.of(back));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);

        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(people.getTgId());
        editMarkup.setMessageId(messageId);
        editMarkup.setReplyMarkup(markup);

        sent.editMessageMarkup(people, messageId, "📦 Выберите тариф:", editMarkup);
    }

    public void sendTariff(People people,int messageId) {
        Sent sent = new Sent();
        TariffDAO tariffDAO = new TariffDAO();

        List<Tariff> tariffs = (people.isAdmin() && people.isUser_flag())
                ? tariffDAO.findAllVisible()
                : tariffDAO.findAll();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Tariff tariff : tariffs) {
            InlineKeyboardButton button = new InlineKeyboardButton();

            BigDecimal discount = tariff.getDiscount().multiply(BigDecimal.valueOf(0.01));
            BigDecimal discountedPrice = tariff.getPrice().subtract(tariff.getPrice().multiply(discount));
            BigDecimal formattedPrice = discountedPrice.setScale(1, RoundingMode.HALF_UP);

            button.setText(tariff.getName() + " - " + formattedPrice + " ₽");

            button.setCallbackData("tariff_:" + tariff.getId() + ":" + messageId);

            rows.add(List.of(button));
        }
        if(people.isAdmin() && !people.isUser_flag()){
            InlineKeyboardButton addTariff = new InlineKeyboardButton("Добавить тариф");
            addTariff.setCallbackData("addTariff_");
            rows.add(List.of(addTariff));
        }

        InlineKeyboardButton back = new InlineKeyboardButton("⬅️ Назад");
        back.setCallbackData("Menu:" + messageId);
        rows.add(List.of(back));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);

        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(people.getTgId());
        editMarkup.setMessageId(messageId);
        editMarkup.setReplyMarkup(markup);

        sent.editMessageMarkup(people, messageId, "📦 Выберите тариф:", editMarkup);
    }


    public void mySubscribe(People people){
        Sent sent = new Sent();
        if(!people.isAdmin() || people.isUser_flag()){
            LocalDateTime localDateTime = LocalDateTime.now();

            sent.sendMessage(people, String.valueOf(localDateTime.plusMonths(1)));
        }else{
            sent.sendMessage(people,"Вы администратор, у вас и так безграничный доступ");
        }
    }

    public void sentOneTariff(People people, Tariff selected, int messageId){
        Sent sent = new Sent();
        String sentText = "<b> Тариф: " + selected.getName() + "</b>\n" +
                "<i>Описание:</i> " + selected.getDescription() + "\n" +
                "<b>Стоимость</b>: <code>" + (selected.getPrice().subtract(selected.getPrice().multiply(selected.getDiscount().multiply(BigDecimal.valueOf(0.01))))).setScale(1, RoundingMode.HALF_UP) + "₽</code>\n" +
                "<b>Срок:</b> " + selected.getTerm() + " суток";
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        if(messageId!=0){
            InlineKeyboardButton back = new InlineKeyboardButton("⬅️ Назад");
            back.setCallbackData("back_tariffs:" + messageId);
            keyboard.add(List.of(back));
        }
        if(!people.isUser_flag() && people.isAdmin()){
            sentText += "\n<b>Скидка:</b> " + selected.getDiscount() + "%";

            InlineKeyboardButton name = new InlineKeyboardButton("Наименование");
            name.setCallbackData("update_tariffs_name_" + selected.getId());

            InlineKeyboardButton description = new InlineKeyboardButton("Описание");
            description.setCallbackData("update_tariffs_description_" + selected.getId());

            InlineKeyboardButton price = new InlineKeyboardButton("Стоимость");
            price.setCallbackData("update_tariffs_price_" + selected.getId());

            InlineKeyboardButton term = new InlineKeyboardButton("Продолжительность");
            term.setCallbackData("update_tariffs_term_" + selected.getId());

            InlineKeyboardButton discount = new InlineKeyboardButton("Скидка");
            discount.setCallbackData("update_tariffs_discount_" + selected.getId());

            InlineKeyboardButton visible;
            if(selected.isVisible()){
                visible = new InlineKeyboardButton("Видим ✅");
            }else{
                visible = new InlineKeyboardButton("Невидим ️⚠️");
            }
            visible.setCallbackData("changeVisible_" + selected.getId());
            keyboard.add(Arrays.asList(name,description,price,term,discount,visible));
        } else {
            InlineKeyboardButton buy = new InlineKeyboardButton("✅ Купить");
            buy.setCallbackData("buy_tariffs_:" + selected.getId());
            keyboard.add(List.of(buy));
        }

        InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();
        inlineMarkup.setKeyboard(keyboard);

        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(String.valueOf(people.getTgId()));
        editMarkup.setMessageId(messageId);
        editMarkup.setReplyMarkup(inlineMarkup);

        // 4. Вызываем имеющийся метод
        sent.editMessageMarkup(people, messageId, sentText, editMarkup);
    }
    public void sentRequisites(People people){
        //тут будут выводиться способы оплаты, также нужно сделать вывод 1 оплаты, сохранение в БД
        //реквизитов, добавить кнопку для админов "Изменение оплат". Скорей всего оно будет редактироваться при покупке
        //и будет в целом кнопка у админ панели
    /*CREATE TABLE payment_requisites (
        id              SERIAL PRIMARY KEY,
        method_name     VARCHAR(32)   NOT NULL,
        requisites      TEXT          NOT NULL,
        recipient_name  VARCHAR(128),
        support_contacts TEXT,
        note            TEXT,
        created_at      TIMESTAMP     DEFAULT NOW()
    );*/
    }
}
