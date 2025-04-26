package com.example.DrugSecure.userAuth.User.DTO;


import org.springframework.stereotype.Component;

@Component
public class LoginSuccessDTO {
    private String username;
    private String email;
    private String token;

    public LoginSuccessDTO(String username, String email, String token) {
        this.username = username;
        this.email = email;
        this.token = token;
    }
    public LoginSuccessDTO() {}

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
