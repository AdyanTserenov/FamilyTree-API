package com.project.familytree.tree.controllers;

import com.project.familytree.auth.dto.CustomApiResponse;
import com.project.familytree.auth.dto.UserDTO;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.TreeDTO;
import com.project.familytree.tree.dto.InviteRequest;
import com.project.familytree.tree.dto.TreeRequest;
import com.project.familytree.tree.services.TreeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/trees")
@Tag(name = "Tree Controller", description = "API для создания, управления и совместной работы с семейными древами")
public class TreeController {
    private static final Logger log = LoggerFactory.getLogger(TreeController.class);

    private final TreeService treeService;
    private final UserService userService;

    public TreeController(TreeService treeService, UserService userService) {
        this.treeService = treeService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<CustomApiResponse<List<TreeDTO>>> getUserTrees() {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Getting trees for user {}", userId);
        List<TreeDTO> trees = treeService.getUserTrees(userId);
        log.info("Found {} trees", trees.size());
        return ResponseEntity.ok(CustomApiResponse.successData(trees));
    }

    @PostMapping
    public ResponseEntity<CustomApiResponse<String>> createTree(
            @Valid @RequestBody TreeRequest treeRequest) {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        treeService.createTree(treeRequest.getName(), userId);
        log.info("Created tree '{}' for user {}", treeRequest.getName(), userId);
        return ResponseEntity.ok(CustomApiResponse.successMessage("Дерево создано"));
    }

    @PostMapping("/{treeId}/invite")
    public ResponseEntity<CustomApiResponse<String>> inviteByEmail(
            @PathVariable Long treeId,
            @Valid @RequestBody InviteRequest inviteRequest) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long inviterId = userService.findIdByDetails(userDetails);
        treeService.sendInviteByEmail(treeId, inviteRequest.email(), inviteRequest.role(), inviterId);

        return ResponseEntity.ok(CustomApiResponse.successMessage(
                "Приглашение отправлено на " + inviteRequest.email()));
    }

    @PostMapping("/{treeId}/invite-link")
    public ResponseEntity<CustomApiResponse<Map<String, String>>> generateInviteLink(
            @PathVariable Long treeId,
            @Valid @RequestBody InviteRequest inviteRequest) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long inviterId = userService.findIdByDetails(userDetails);
        String token = treeService.createInviteToken(treeId, inviteRequest.email(), inviteRequest.role(), inviterId);
        String inviteLink = "https://familytree.example.com/invite/" + token;

        return ResponseEntity.ok(CustomApiResponse.successData(
                Map.of("inviteLink", inviteLink)));
    }

    @GetMapping("/invite/{token}")
    public ResponseEntity<CustomApiResponse<String>> acceptInvite(
            @PathVariable String token) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        treeService.acceptInvitation(token, userId);

        return ResponseEntity.ok(CustomApiResponse.successMessage("Вы успешно присоединились к дереву!"));
    }

    @GetMapping("/{treeId}/members")
    public ResponseEntity<CustomApiResponse<List<UserDTO>>> getMembers(
            @PathVariable Long treeId) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        if (!treeService.canView(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на просмотр участников");
        }

        List<UserDTO> members = treeService.getMembers(treeId);
        return ResponseEntity.ok(CustomApiResponse.successData(members));
    }
}