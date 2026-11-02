package com.cleancity.cleancity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponseDto {

    private String message;
    private String name;
    private String email;
    private String role;
}