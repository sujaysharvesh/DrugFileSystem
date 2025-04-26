package com.example.DrugSecure.userAuth.User;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.UUID;


@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    @Column(nullable = false, unique = true)
    private UUID id;

    @NotNull(message = "Username is required")
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Email(message = "Valid email is required")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotNull(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long") // ✅ Fixed validation
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    // Default constructor (needed for JPA)
    public User() {}

    // Parameterized constructor
    public User(String email, String password, String username, Boolean verified) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.verified = verified;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public boolean getVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}
