package org.example.session;

import org.example.table.Purchase;

public class ReviewSubmissionSession {
    private Purchase purchase;
    private Step step = Step.INSTRUCTIONS;
    private int photosReceived = 0;
    private boolean videoReceived = false;
    private String[] photoFileIds = new String[4];
    private String videoFileId;
    
    public enum Step {
        INSTRUCTIONS,    // Показ инструкции
        PHOTOS,          // Получение 4 фотографий
        VIDEO,           // Получение 1 видео
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
        if (photosReceived < 4) {
            photoFileIds[photosReceived] = photoFileId;
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
    
    public boolean isComplete() {
        return photosReceived == 4 && videoReceived;
    }
}
