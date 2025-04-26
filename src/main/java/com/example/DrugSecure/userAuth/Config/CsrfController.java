package com.example.DrugSecure.userAuth.Config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Spring Controller example
@RestController
public class CsrfController {

    @GetMapping("/api/csrf")
    public ResponseEntity<Map<String, Object>> getCsrfToken(HttpServletRequest request) {
        org.springframework.security.web.csrf.CsrfToken csrfToken = (org.springframework.security.web.csrf.CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return ResponseEntity.ok()
                .header(csrfToken.getHeaderName(), csrfToken.getToken())
                .body(Map.of("token", csrfToken.getToken()));
    }
}