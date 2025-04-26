package com.example.DrugSecure.userAuth.User;

import com.example.DrugSecure.DrugFile;
import com.example.DrugSecure.FileLocalStoreService;
import com.example.DrugSecure.userAuth.User.CustomUser.CustomUserDetails;
import com.example.DrugSecure.userAuth.User.DTO.LoginRequestDTO;
import com.example.DrugSecure.userAuth.User.DTO.LoginSuccessDTO;
import com.example.DrugSecure.userAuth.User.DTO.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserServices userServices;


    private final FileLocalStoreService fileLocalStoreService;

    @Autowired
    private UserDTO userDTO;

    public UserController(FileLocalStoreService fileLocalStoreService) {
        this.fileLocalStoreService = fileLocalStoreService;
    }

    @GetMapping("/")
    public String Home() {
        return "Hello";
    }

    @GetMapping("/files")
    public ResponseEntity<List<DrugFile>> getUserFiles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        List<DrugFile> files = fileLocalStoreService.getUserFiles(customUserDetails.getId());
        return ResponseEntity.ok(files);
    }
    @GetMapping(value = "/me", produces = "application/json")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body("Invalid Token");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return ResponseEntity.ok().body(userDetails);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerUser(@Valid @RequestBody User user) {
        try {
            User newUser = userServices.registerUser(user.getUsername(), user.getEmail(), user.getPassword());
            UserDTO userDTO = new UserDTO();
            userDTO.setUsername(newUser.getUsername());
            userDTO.setEmail(newUser.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "User Registered Successfully", "user", newUser));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email or username already exists"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Something went wrong", "details", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginUser(@Valid @RequestBody LoginRequestDTO loginRequestDTO){
        try {
            LoginSuccessDTO loginSuccessDTO = userServices.loginUser(loginRequestDTO.getEmail(), loginRequestDTO.getPassword());
            Map<String, Object> response = Map.of(
                    "message", "User Logged In Successfully",
                    "user", loginSuccessDTO
            );
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            Map<String, Object> errorResponse = Map.of(
                    "message", "Something went wrong",
                    "details", ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @GetMapping("/logout")
    public ResponseEntity<Map<String, Object>> logoutUser(HttpServletRequest request) {
        try {
            userServices.logoutUser(request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("message", "User Logged Out Successfully"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Something went wrong", "details", ex.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteUser(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        try {
            userServices.deleteUser(loginRequestDTO.getEmail(), loginRequestDTO.getPassword());
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("message", "User Deleted Successfully"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Something went wrong", "details", ex.getMessage()));
        }
    }

}
