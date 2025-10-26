package org.example.telegramBots;


import org.example.session.ProductCreationSession;
import org.example.session.ReviewRequestSession;
import org.example.session.SessionStore;
import org.example.tgProcessing.MessageProcessing;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelegramBot extends TelegramLongPollingBot {

    private final ExecutorService executor = Executors.newFixedThreadPool(200);
    private final ThreadLocal<MessageProcessing> threadLocalProcessing =
            ThreadLocal.withInitial(MessageProcessing::new);

    @Override
    public void onUpdateReceived(Update update) {

    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        for(Update update : updates){
            executor.submit(()-> {
                System.out.println("🔍 === TELEGRAM BOT UPDATE RECEIVED ===");
                System.out.println("🔍 Update ID: " + update.getUpdateId());
                System.out.println("🔍 Has callback query: " + update.hasCallbackQuery());
                System.out.println("🔍 Has message: " + update.hasMessage());
                if (update.hasMessage()) {
                    System.out.println("🔍 Message chat ID: " + update.getMessage().getChatId());
                    System.out.println("🔍 Message ID: " + update.getMessage().getMessageId());
                    System.out.println("🔍 Chat type: " + update.getMessage().getChat().getType());
                }
                if (update.hasCallbackQuery()) {
                    System.out.println("🔍 Callback query ID: " + update.getCallbackQuery().getId());
                    System.out.println("🔍 Callback data: " + update.getCallbackQuery().getData());
                    System.out.println("🔍 Callback chat ID: " + update.getCallbackQuery().getMessage().getChatId());
                }
                System.out.println("🔍 === END TELEGRAM BOT UPDATE LOG ===");
                
                MessageProcessing messageProcessing = threadLocalProcessing.get();
                try {
                    if (update.hasCallbackQuery()) {
                        System.out.println("🔍 === TELEGRAM BOT CALLBACK QUERY DETECTED ===");
                        System.out.println("🔍 Update ID: " + update.getUpdateId());
                        System.out.println("🔍 Callback data: " + update.getCallbackQuery().getData());
                        System.out.println("🔍 Chat ID: " + update.getCallbackQuery().getMessage().getChatId());
                        System.out.println("🔍 Message ID: " + update.getCallbackQuery().getMessage().getMessageId());
                        System.out.println("🔍 === CALLING callBackQuery METHOD ===");
                        
                        messageProcessing.callBackQuery(update);
                        return;
                    }
                    // Обрабатываем все типы медиа (фото, видео, документы, аудио, голосовые и т.д.)
                    if(update.getMessage().hasPhoto() || update.getMessage().hasVideo() || 
                       update.getMessage().hasDocument() || update.getMessage().hasVideoNote() ||
                       update.getMessage().hasVoice() || update.getMessage().hasAudio() ||
                       update.getMessage().hasSticker() || update.getMessage().hasContact() ||
                       update.getMessage().hasLocation() || update.getMessage().hasPoll() ||
                       update.getMessage().hasDice() || update.getMessage().hasInvoice() ||
                       update.getMessage().hasSuccessfulPayment() || update.getMessage().hasPassportData()){
                        messageProcessing.handleUpdate(update);
                        return;
                    }
                    // Обрабатываем текстовые сообщения и определенные состояния сессий
                    if (((update.hasMessage() && update.getMessage().hasText()) || update.getMessage().hasContact() )
                            || (SessionStore.getProductSession(update.getMessage().getChatId()).getStep() == ProductCreationSession.Step.PHOTO)
                            || (SessionStore.getReviewSession(update.getMessage().getChatId()).getStep() == ReviewRequestSession.Step.SEARCH_SCREENSHOT)
                            || (SessionStore.getReviewSession(update.getMessage().getChatId()).getStep() == ReviewRequestSession.Step.DELIVERY_SCREENSHOT)) {
                        messageProcessing.handleUpdate(update);
                        return;
                    }

                } catch (TelegramApiException | IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    public String getBotUsername() {
        ResourceBundle rb = ResourceBundle.getBundle("app");
        return rb.getString("bot.username1");
    }
    @Override
    public String getBotToken() {
        ResourceBundle rb = ResourceBundle.getBundle("app");
        return rb.getString("bot.token1");
    }

    public Message trySendMessage(SendMessage sendMessage) {
        while (true) {
            try {
                return execute(sendMessage);
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
        return null;
    }
    public void downloadFile(String fileId, String filePath) throws TelegramApiException, IOException {
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);

        File file = execute(getFile);

        URL fileUrl = new URL(file.getFileUrl(getBotToken()));

        try (InputStream in = fileUrl.openStream();
             ReadableByteChannel rbc = Channels.newChannel(in);
             FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
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
    public void trySendPhoto(SendPhoto sendPhoto) {
        boolean sent = false;
        while (!sent) {
            try {
                execute(sendPhoto);
                sent = true;
            } catch (TelegramApiException e) {
                System.err.println("❌ Failed to send: " + e.getMessage());
                int retryAfterSeconds = extractRetryAfterSeconds(e.getMessage());
                if (retryAfterSeconds > 0) {
                    try {
                        Thread.sleep(retryAfterSeconds * 1000L);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                        break;
                    }
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

    public void deleteMessage(Long chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(messageId);

        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            // Логируем ошибку, но не выбрасываем исключение
            System.err.println("Failed to delete message " + messageId + " in chat " + chatId + ": " + e.getMessage());
        }
    }
}
