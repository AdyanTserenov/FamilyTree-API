package com.project.familytree.tree.controllers;

import com.project.familytree.auth.dto.CustomApiResponse;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.PersonDTO;
import com.project.familytree.tree.dto.TreeDTO;
import com.project.familytree.tree.dto.InviteRequest;
import com.project.familytree.tree.dto.TreeMemberDTO;
import com.project.familytree.tree.dto.TreeRequest;
import com.project.familytree.tree.services.TreeService;
import io.swagger.v3.oas.annotations.Operation;
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

    @org.springframework.beans.factory.annotation.Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    public TreeController(TreeService treeService, UserService userService) {
        this.treeService = treeService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Получить список деревьев пользователя",
               description = "Возвращает все деревья, в которых пользователь является участником")
    public ResponseEntity<CustomApiResponse<List<TreeDTO>>> getUserTrees() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Getting trees for user {}", userId);
        List<TreeDTO> trees = treeService.getUserTrees(userId);
        log.info("Found {} trees", trees.size());
        return ResponseEntity.ok(CustomApiResponse.successData(trees));
    }

    @PostMapping
    @Operation(summary = "Создать новое дерево",
               description = "Создаёт семейное дерево и назначает текущего пользователя владельцем")
    public ResponseEntity<CustomApiResponse<String>> createTree(
            @Valid @RequestBody TreeRequest treeRequest) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        treeService.createTree(treeRequest.getName(), userId);
        log.info("Created tree '{}' for user {}", treeRequest.getName(), userId);
        return ResponseEntity.ok(CustomApiResponse.successMessage("Дерево создано"));
    }

    @PutMapping("/{treeId}")
    @Operation(summary = "Обновить дерево",
               description = "Изменяет название дерева. Требует роль OWNER.")
    public ResponseEntity<CustomApiResponse<TreeDTO>> updateTree(
            @PathVariable Long treeId,
            @Valid @RequestBody TreeRequest treeRequest) throws AccessDeniedException {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Updating tree {} by user {}", treeId, userId);
        TreeDTO updated = treeService.updateTree(treeId, treeRequest.getName(), userId);
        return ResponseEntity.ok(CustomApiResponse.successData(updated));
    }

    @DeleteMapping("/{treeId}")
    @Operation(summary = "Удалить дерево",
               description = "Удаляет дерево со всеми персонами и связями. Требует роль OWNER.")
    public ResponseEntity<CustomApiResponse<String>> deleteTree(
            @PathVariable Long treeId) throws AccessDeniedException {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Deleting tree {} by user {}", treeId, userId);
        treeService.deleteTree(treeId, userId);
        return ResponseEntity.ok(CustomApiResponse.successMessage("Дерево удалено"));
    }

    @PostMapping("/{treeId}/invite")
    @Operation(summary = "Пригласить пользователя по email",
               description = "Отправляет письмо с приглашением. Требует роль OWNER.")
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
    @Operation(summary = "Сгенерировать ссылку-приглашение",
               description = "Создаёт токен приглашения и возвращает ссылку. Требует роль OWNER.")
    public ResponseEntity<CustomApiResponse<Map<String, String>>> generateInviteLink(
            @PathVariable Long treeId,
            @Valid @RequestBody InviteRequest inviteRequest) throws AccessDeniedException {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long inviterId = userService.findIdByDetails(userDetails);
        String token = treeService.createInviteToken(treeId, inviteRequest.email(), inviteRequest.role(), inviterId);
        String inviteLink = baseUrl + "/invite/" + token;
        return ResponseEntity.ok(CustomApiResponse.successData(Map.of("inviteLink", inviteLink)));
    }

    @GetMapping("/invite/{token}")
    @Operation(summary = "Принять приглашение",
               description = "Добавляет текущего пользователя в дерево по токену приглашения")
    public ResponseEntity<CustomApiResponse<String>> acceptInvite(
            @PathVariable String token) throws AccessDeniedException {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        treeService.acceptInvitation(token, userId);
        return ResponseEntity.ok(CustomApiResponse.successMessage("Вы успешно присоединились к дереву!"));
    }

    @GetMapping("/{treeId}/members")
    @Operation(summary = "Получить список участников дерева",
               description = "Возвращает всех пользователей с доступом к дереву. Требует роль VIEWER или выше.")
    public ResponseEntity<CustomApiResponse<List<TreeMemberDTO>>> getMembers(
            @PathVariable Long treeId) throws AccessDeniedException {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        if (!treeService.canView(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на просмотр участников");
        }
        List<TreeMemberDTO> members = treeService.getMembers(treeId);
        return ResponseEntity.ok(CustomApiResponse.successData(members));
    }

    @PostMapping("/{treeId}/public-link")
    @Operation(summary = "Создать публичную ссылку",
               description = "Генерирует токен публичного доступа к дереву. Требует роль OWNER.")
    public ResponseEntity<CustomApiResponse<String>> generatePublicLink(
            @PathVariable Long treeId) throws AccessDeniedException {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        String token = treeService.generatePublicLink(treeId, userId);
        return ResponseEntity.ok(CustomApiResponse.successData(token));
    }

    @DeleteMapping("/{treeId}/public-link")
    @Operation(summary = "Отозвать публичную ссылку",
               description = "Удаляет токен публичного доступа к дереву. Требует роль OWNER.")
    public ResponseEntity<CustomApiResponse<String>> revokePublicLink(
            @PathVariable Long treeId) throws AccessDeniedException {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        treeService.revokePublicLink(treeId, userId);
        return ResponseEntity.ok(CustomApiResponse.successMessage("Публичная ссылка отозвана"));
    }

    @GetMapping("/public/{token}")
    @Operation(summary = "Получить публичное дерево",
               description = "Возвращает список персон дерева по публичному токену. Аутентификация не требуется.")
    public ResponseEntity<CustomApiResponse<List<PersonDTO>>> getPublicTree(
            @PathVariable String token) {
        List<PersonDTO> persons = treeService.getPublicTree(token);
        return ResponseEntity.ok(CustomApiResponse.successData(persons));
    }
}