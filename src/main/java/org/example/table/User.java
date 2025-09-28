package org.example.table;

import jakarta.persistence.*;

@Entity
@Table(name = "user")
public class User {

    @Id
    @Column(name = "idUser")
    private int idUser;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @Column(name = "block", nullable = false)
    private boolean block;

    @Column(name = "isAdmin", nullable = false)
    private boolean isAdmin;

    @Column(name = "userFlag", nullable = false)
    private boolean userFlag;

    @Column(name = "id_message", nullable = false)
    private int id_message;

    public int getId_message() {
        return id_message;
    }

    public void setId_message(int id_message) {
        this.id_message = id_message;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isBlock() {
        return block;
    }

    public void setBlock(boolean block) {
        this.block = block;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public boolean isUserFlag() {
        return userFlag;
    }

    public void setUserFlag(boolean userFlag) {
        this.userFlag = userFlag;
    }
}