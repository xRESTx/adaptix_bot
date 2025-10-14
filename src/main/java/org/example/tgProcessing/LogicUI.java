package org.example.tgProcessing;

import org.example.dao.ProductDAO;
import org.example.dao.UserDAO;
import org.example.table.Product;
import org.example.table.User;
import org.example.telegramBots.TelegramBotLogs;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

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
        }
    }
    public void sendMenu(User user){
        Sent sent = new Sent();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Каталог товаров");
        row1.add("Оставить отзыв");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Техподдержка");
        row2.add("Получить кешбек");

        KeyboardRow row3 = new KeyboardRow();
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

    public void sendProducts(User user, Integer messageId){
        Sent sent = new Sent();
        SendMessage sendMessage = new SendMessage();
        ProductDAO productDAO = new ProductDAO();

        List<Product> products = (user.isAdmin() && user.isUserFlag())
                ? productDAO.findAllVisible()
                : productDAO.findAll();
        if(products.isEmpty()){
            sent.sendMessage(user,"К сожалению товаров на выкуп нет",sendMessage);
            return;
        }else if(messageId == null){
            messageId = sent.sendMessage(user,"📦 Выберите товар:",sendMessage).getMessageId();
        }

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for(Product product : products){
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Товар:" + product.getProductName() + "  " + product.getCashbackPercentage() + "%");
            button.setCallbackData("product_:" + product.getIdProduct() + ":" + messageId);
            rows.add(List.of(button));
        }
        if(user.isAdmin() && !user.isUserFlag()){
            InlineKeyboardButton btnAddProduct = new InlineKeyboardButton();
            btnAddProduct.setText("Добавить товар");
            btnAddProduct.setCallbackData("addProduct:" + messageId);
            rows.add(List.of(btnAddProduct));
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

        sent.editMessageMarkup(user, messageId, "📦 Выберите тариф:", editMarkup);
    }
    public void sendMessageBank(User user, String text){
        Sent sent = new Sent();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Т-Банк");
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
        if(messageId!=0){
            InlineKeyboardButton back = new InlineKeyboardButton("⬅️ Назад");
            back.setCallbackData("Exit:" + messageId);
            keyboard.add(List.of(back));
        }
        if(!user.isUserFlag() && user.isAdmin()){
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
            InlineKeyboardButton buy = new InlineKeyboardButton("✅ Купить");
            buy.setCallbackData("buy_product:" + selected.getIdProduct());
            keyboard.add(List.of(buy));
        }

        InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();
        inlineMarkup.setKeyboard(keyboard);

        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(String.valueOf(user.getIdUser()));
        editMarkup.setMessageId(messageId);
        editMarkup.setReplyMarkup(inlineMarkup);

        sent.editMessageMarkup(user, messageId, textProduct, editMarkup);
    }
    public void sentBack(User user, Consumer<User> UIFunction, String text, String buttonText){
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
}
