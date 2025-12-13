package com.project.familytree.controllers;

import com.project.familytree.dto.CustomApiResponse;
import com.project.familytree.dto.InviteRequest;
import com.project.familytree.dto.TreeRequest;
import com.project.familytree.dto.UserDTO;
import com.project.familytree.models.User;
import com.project.familytree.services.TreeService;
import com.project.familytree.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/trees")
@Tag(name = "Tree Controller", description = "API для создания, управления и совместной работы с семейными древами")
public class TreeController {
    private final TreeService treeService;
    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<CustomApiResponse<String>> createTree(
            @Valid @RequestBody TreeRequest treeRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = userService.findIdByDetails(userDetails);
        treeService.createTree(treeRequest.getName(), userId);
        return ResponseEntity.ok(CustomApiResponse.success("Дерево успешно создано"));
    }

    @PostMapping("/{treeId}/invite")
    public ResponseEntity<CustomApiResponse<String>> inviteByEmail(
            @PathVariable Long treeId,
            @Valid @RequestBody InviteRequest inviteRequest,
            @AuthenticationPrincipal UserDetails userDetails) throws AccessDeniedException {

        Long inviterId = userService.findIdByDetails(userDetails);
        treeService.sendInviteByEmail(treeId, inviteRequest.email(), inviteRequest.role(), inviterId);

        return ResponseEntity.ok(CustomApiResponse.success(
                "Приглашение отправлено на " + inviteRequest.email()));
    }

    @PostMapping("/{treeId}/invite-link")
    public ResponseEntity<CustomApiResponse<Map<String, String>>> generateInviteLink(
            @PathVariable Long treeId,
            @Valid @RequestBody InviteRequest inviteRequest,
            @AuthenticationPrincipal UserDetails userDetails) throws AccessDeniedException {

        Long inviterId = userService.findIdByDetails(userDetails);
        String token = treeService.createInviteToken(treeId, inviteRequest.email(), inviteRequest.role(), inviterId);
        String inviteLink = "https://familytree.example.com/invite/" + token;

        return ResponseEntity.ok(CustomApiResponse.success(
                Map.of("inviteLink", inviteLink)));
    }

    @GetMapping("/invite/{token}")
    public ResponseEntity<CustomApiResponse<String>> acceptInvite(
            @PathVariable String token,
            @AuthenticationPrincipal UserDetails userDetails) throws AccessDeniedException {

        Long userId = userService.findIdByDetails(userDetails);
        treeService.acceptInvitation(token, userId);

        return ResponseEntity.ok(CustomApiResponse.success("Вы успешно присоединились к дереву!"));
    }

    @GetMapping("/{treeId}/members")
    public ResponseEntity<CustomApiResponse<List<UserDTO>>> getMembers(
            @PathVariable Long treeId,
            @AuthenticationPrincipal UserDetails userDetails) throws AccessDeniedException {

        Long userId = userService.findIdByDetails(userDetails);
        if (!treeService.canView(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на просмотр участников");
        }

        List<UserDTO> members = treeService.getMembers(treeId);
        return ResponseEntity.ok(CustomApiResponse.success(members));
    }
}
