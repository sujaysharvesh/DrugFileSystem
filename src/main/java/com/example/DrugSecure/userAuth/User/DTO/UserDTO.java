package com.example.DrugSecure.userAuth.User.DTO;


import org.springframework.stereotype.Component;

@Component
public class UserDTO {
    private String username;
    private String email;

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    public void setUsername(String username) {
    }
}
