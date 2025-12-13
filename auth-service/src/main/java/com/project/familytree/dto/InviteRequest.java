package com.project.familytree.dto;

import com.project.familytree.impls.TreeRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record InviteRequest(
        @Schema(example = "guest@example.com")
        @Email
        @NotNull
        String email,

        @Schema(example = "VIEWER")
        @NotNull
        TreeRole role
) {}