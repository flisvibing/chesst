package com.chesst.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(
        @NotBlank String email,
        @NotBlank @Size(min = 6, max = 6, message = "Code must be 6 digits") String code
) {}
