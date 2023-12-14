package com.example.models;

import lombok.Data;

@Data
public class LoginResponse {
    private String jwt;
    public LoginResponse(String jwt) {
        this.jwt = jwt;
    }
}
