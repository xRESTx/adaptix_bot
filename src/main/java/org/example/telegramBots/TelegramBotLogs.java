package org.example.telegramBots;

import org.example.tgProcessing.Sent;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.forum.CreateForumTopic;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.forum.ForumTopic;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelegramBotLogs extends TelegramLongPollingBot {
    @Override
    public void onUpdateReceived(Update update) {

    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        for(Update update : updates){
        }
    }

    @Override
    public String getBotUsername() {
        ResourceBundle rb = ResourceBundle.getBundle("app");
        return rb.getString("bot.username2");
    }
    @Override
    public String getBotToken() {
        ResourceBundle rb = ResourceBundle.getBundle("app");
        return rb.getString("bot.token2");
    }

    public void trySendMessage(SendMessage sendMessage) {
        boolean sent = false;
        while (!sent) {
            try {
                execute(sendMessage);
                sent = true;
            } catch (TelegramApiException e) {
                System.err.println("❌ Failed to send: " + e.getMessage());
                int retryAfterSeconds = extractRetryAfterSeconds(e.getMessage());
                try {
                    Thread.sleep((retryAfterSeconds > 0 ? retryAfterSeconds : 1) * 1000L);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                    break;
                }
            }
        }
    }
    public void trySendPhoto(SendPhoto sendPhoto) {
        boolean sent = false;
        while (!sent) {
            try {
                execute(sendPhoto);
                sent = true;
            } catch (TelegramApiException e) {
                System.err.println("❌ Failed to send: " + e.getMessage());
                int retryAfterSeconds = extractRetryAfterSeconds(e.getMessage());
                try {
                    Thread.sleep((retryAfterSeconds > 0 ? retryAfterSeconds : 1) * 1000L);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                    break;
                }
            }
        }
    }
    public void trySendMessage(CopyMessage sendMessage) {
        boolean sent = false;
        while (!sent) {
            try {
                execute(sendMessage);
                sent = true;
            } catch (TelegramApiException e) {
                System.err.println("❌ Failed to send: " + e.getMessage());
                int retryAfterSeconds = extractRetryAfterSeconds(e.getMessage());
                try {
                    Thread.sleep((retryAfterSeconds > 0 ? retryAfterSeconds : 1) * 1000L);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                    break;
                }
            }
        }
    }

    private int extractRetryAfterSeconds(String message) {
        if (message != null && message.contains("Too Many Requests")) {
            String[] parts = message.split("retry after");
            if (parts.length > 1) {
                try {
                    return Integer.parseInt(parts[1].trim().split(" ")[0]);
                } catch (NumberFormatException ignored) {}
            }
        }
        return -1;
    }
    public List<Long> createTopic(Update update) {
        Sent sent = new Sent();
        List<Long> list = new ArrayList<>();

        ResourceBundle rb = ResourceBundle.getBundle("app");
        Long groupTg = Long.parseLong(rb.getString("tg.group"));

        list.add(groupTg);

        CreateForumTopic topic = CreateForumTopic.builder()
                .chatId(groupTg)
                .name(update.getMessage().getFrom().getUserName())
                .iconColor(0xFFD67E)
                .build();

        ForumTopic topicMessage = tryCreateTopicWithRetry(topic);
        list.add(Long.valueOf(topicMessage.getMessageThreadId()));
        sent.sendMessageUser(groupTg,topicMessage.getMessageThreadId(),update.getMessage().getText());
        return list;
    }

    public ForumTopic tryCreateTopicWithRetry(CreateForumTopic topic) {
        boolean success = false;
        ForumTopic result = null;

        while (!success) {
            try {
                result = execute(topic);
                success = true;
            } catch (TelegramApiException e) {
                System.err.println("❌ Не удалось создать тему: " + e.getMessage());
                int retryAfter = extractRetryAfterSeconds(e.getMessage());
                try {
                    Thread.sleep((retryAfter > 0 ? retryAfter : 1) * 1000L);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                    break;
                }
            }
        }

        return result;
    }

    public void deleteMessage(Long chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(messageId);

        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
