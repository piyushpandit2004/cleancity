package com.cleancity.cleancity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {

    private String message;
    private String email;
    private String role;
    private String token;
}