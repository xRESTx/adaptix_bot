package org.example.telegramBots;


import org.example.session.ProductCreationSession;
import org.example.session.ReviewRequestSession;
import org.example.session.SessionStore;
import org.example.tgProcessing.MessageProcessing;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.forum.CreateForumTopic;
import org.telegram.telegrambots.meta.api.objects.forum.ForumTopic;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.example.tgProcessing.Sent;

import java.util.ArrayList;
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
    
    public void trySendMessage(ForwardMessage forwardMessage) {
        boolean sent = false;
        while (!sent) {
            try {
                execute(forwardMessage);
                sent = true;
            } catch (TelegramApiException e) {
                System.err.println("❌ Failed to forward: " + e.getMessage());
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
    public Message trySendPhoto(SendPhoto sendPhoto) {
        boolean sent = false;
        while (!sent) {
            try {
                Message result = execute(sendPhoto);
                sent = true;
                return result;
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
        return null;
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
    
    public Long sendPhotoToGroup(long groupId, String photoPath, String caption) {
        try {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(groupId);
            sendPhoto.setCaption(caption);
            sendPhoto.setParseMode("HTML");
            
            java.io.File file = new java.io.File(photoPath);
            if (!file.exists()) {
                System.err.println("❌ File does not exist: " + photoPath);
                return null;
            }
            
            InputFile inputFile = new InputFile(file);
            sendPhoto.setPhoto(inputFile);
            
            Message sentMessage = trySendPhoto(sendPhoto);
            return sentMessage != null ? (long) sentMessage.getMessageId() : null;
            
        } catch (Exception e) {
            System.err.println("❌ Error sending photo to group: " + e.getMessage());
            return null;
        }
    }

    public Message trySendVideo(SendVideo sendVideo) {
        boolean sent = false;
        while (!sent) {
            try {
                Message result = execute(sendVideo);
                sent = true;
                return result;
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
        return null;
    }

    public void tryForwardMessage(ForwardMessage forwardMessage) {
        boolean sent = false;
        while (!sent) {
            try {
                execute(forwardMessage);
                sent = true;
            } catch (TelegramApiException e) {
                System.err.println("❌ Failed to forward: " + e.getMessage());
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
}
