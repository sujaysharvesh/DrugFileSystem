package com.example.DrugSecure.userAuth.User;


import com.example.DrugSecure.userAuth.User.DTO.LoginSuccessDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Service
@Transactional(transactionManager = "userTransactionManager")
public interface UserServices {
    User registerUser(String username, String email, String password);
    Optional<User> getAllUser();
    LoginSuccessDTO loginUser(String email, String password);
    User updateUser(User user);
    boolean deleteUser(String email, String password);
    String currentUser();
    void logoutUser(HttpServletRequest request);
}
