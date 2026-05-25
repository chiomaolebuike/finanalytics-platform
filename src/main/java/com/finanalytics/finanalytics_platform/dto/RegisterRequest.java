package com.finanalytics.finanalytics_platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank                        String email,
        @NotBlank @Size(min = 2, max = 200)     String fullName,
        @NotBlank @Size(min = 8, max = 100)     String password
) {}
