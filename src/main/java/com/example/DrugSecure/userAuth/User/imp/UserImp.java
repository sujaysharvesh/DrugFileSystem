package com.example.DrugSecure.userAuth.User.imp;

import com.example.DrugSecure.userAuth.User.DTO.LoginSuccessDTO;
import com.example.DrugSecure.userAuth.User.UserRepository;
import com.example.DrugSecure.userAuth.User.Redis.RedisTokenBlockListService;
import com.example.DrugSecure.userAuth.User.User;
import com.example.DrugSecure.userAuth.User.UserServices;
import com.example.DrugSecure.userAuth.User.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class UserImp implements UserServices {

    @Autowired
    private RedisTokenBlockListService tokenBlockListService;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    @Autowired
    private LoginSuccessDTO loginSuccessDTO;

    public UserImp(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public String hashPassword(String password){
        return passwordEncoder.encode(password);
    }

    public Boolean checkPassword(String password, String hashedPassword){
        return passwordEncoder.matches(password, hashedPassword);
    }

    @Override
    public User registerUser(String username, String email, String password) {
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Invalid Email");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        String hashedPassword = hashPassword(password);
        User user = new User(email, hashedPassword, username, false);
        return userRepository.save(user);
    }

    @Override
    public Optional<User> getAllUser() {
        return Optional.empty();
    }

    @Override
    public LoginSuccessDTO loginUser(String email, String password){
        User user = userRepository.findByEmail(email).filter(CurrentUser -> checkPassword(password, CurrentUser.getPassword()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid Credentials"));
        String token = jwtUtil.GenerateToken(user);
        return new LoginSuccessDTO(user.getUsername(), user.getEmail(), token);
    }

    @Override
    public User updateUser(User user) {
        return null;
    }

    @Override
    public boolean deleteUser(String email, String password) {
        User existingUser = userRepository.findByEmail(email)
                .filter(user -> checkPassword(password, user.getPassword()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        userRepository.delete(existingUser);
        return true;
    }

    @Override
    public String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        return null;
    }

    @Override
    public void logoutUser(HttpServletRequest request) {
        String token = jwtUtil.ExtractToken(request);
        if(token == null){
            throw new IllegalArgumentException("Invalid Token");
        }
        if (tokenBlockListService.isTokenBlocked(token)){
            throw new IllegalArgumentException("Token is already blocked");
        }
        long expirationTime = jwtUtil.getExpirationTime(token) - System.currentTimeMillis();
        if (expirationTime > 0) {
            tokenBlockListService.addTokenToBlockList(token, expirationTime);
        }
    }

}
