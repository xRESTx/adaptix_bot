package org.example.table;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "TechnicalSupport")
@Table(name = "TechnicalSupport")

public class TechnicalSupport {
    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "name", nullable = false, length = 255)
    private String username;

    @Column(name = "key", nullable = false, length = 255)
    private String key;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}