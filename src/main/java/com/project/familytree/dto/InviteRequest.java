package com.project.familytree.dto;

import com.project.familytree.impls.TreeRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record InviteRequest(
        @Email String email,
        @NotNull TreeRole role
) {}
