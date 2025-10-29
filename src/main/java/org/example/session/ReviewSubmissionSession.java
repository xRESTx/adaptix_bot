package org.example.session;

import org.example.table.Purchase;

public class ReviewSubmissionSession {
    private Purchase purchase;
    private Step step = Step.INSTRUCTIONS;
    private int photosReceived = 0;
    private boolean videoReceived = false;
    private String[] photoFileIds = new String[3];
    private String videoFileId;
    private String reviewText;
    
    // Для пересылки сообщений
    private Integer[] photoMessageIds = new Integer[3];
    private Integer videoMessageId;
    private Long userChatId;
    private boolean isCompleted = false; // Флаг завершения процесса
    
    public enum Step {
        INSTRUCTIONS,    // Показ инструкции
        TEXT,            // Получение текста отзыва
        MEDIA,           // Получение медиа (фото и видео)
        COMPLETE         // Завершение
    }
    
    public ReviewSubmissionSession(Purchase purchase) {
        this.purchase = purchase;
    }
    
    public Purchase getPurchase() {
        return purchase;
    }
    
    public void setPurchase(Purchase purchase) {
        this.purchase = purchase;
    }
    
    public Step getStep() {
        return step;
    }
    
    public void setStep(Step step) {
        this.step = step;
    }
    
    public int getPhotosReceived() {
        return photosReceived;
    }
    
    public void setPhotosReceived(int photosReceived) {
        this.photosReceived = photosReceived;
    }
    
    public void addPhoto(String photoFileId) {
        if (photosReceived < 3) {
            photoFileIds[photosReceived] = photoFileId;
            photosReceived++;
        }
    }
    
    public void addPhotoWithMessageId(String photoFileId, Integer messageId) {
        if (photosReceived < 3) {
            photoFileIds[photosReceived] = photoFileId;
            photoMessageIds[photosReceived] = messageId;
            photosReceived++;
        }
    }
    
    public String[] getPhotoFileIds() {
        return photoFileIds;
    }
    
    public void setPhotoFileIds(String[] photoFileIds) {
        this.photoFileIds = photoFileIds;
    }
    
    public boolean isVideoReceived() {
        return videoReceived;
    }
    
    public void setVideoReceived(boolean videoReceived) {
        this.videoReceived = videoReceived;
    }
    
    public String getVideoFileId() {
        return videoFileId;
    }
    
    public void setVideoFileId(String videoFileId) {
        this.videoFileId = videoFileId;
    }
    
    public String getReviewText() {
        return reviewText;
    }
    
    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }
    
    public Integer[] getPhotoMessageIds() {
        return photoMessageIds;
    }
    
    public void setPhotoMessageIds(Integer[] photoMessageIds) {
        this.photoMessageIds = photoMessageIds;
    }
    
    public Integer getVideoMessageId() {
        return videoMessageId;
    }
    
    public void setVideoMessageId(Integer videoMessageId) {
        this.videoMessageId = videoMessageId;
    }
    
    public Long getUserChatId() {
        return userChatId;
    }
    
    public void setUserChatId(Long userChatId) {
        this.userChatId = userChatId;
    }
    
    public boolean isCompleted() {
        return isCompleted;
    }
    
    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }
    
    public boolean isComplete() {
        return photosReceived == 3 && videoReceived;
    }
}
