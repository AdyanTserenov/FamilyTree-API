package com.project.familytree.controllers;

import com.project.familytree.dto.CustomApiResponse;
import com.project.familytree.dto.InviteRequest;
import com.project.familytree.dto.TreeRequest;
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


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/trees")
@Tag(name = "Tree Controller", description = "API для созданий и изменений древ")
public class TreeController {
    private final TreeService treeService;
    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<CustomApiResponse<String>> createTree(@Valid @RequestBody TreeRequest treeRequest,
                                                                @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.findIdByDetails(userDetails);
        treeService.createTree(treeRequest.getName(), userId);

        return ResponseEntity.ok(CustomApiResponse.success("Дерево успешно создано"));
    }

    @PostMapping("/{treeId}/invite")
    public ResponseEntity<CustomApiResponse<String>> invite(
            @PathVariable Long treeId,
            @Valid @RequestBody InviteRequest inviteRequest,
            @AuthenticationPrincipal UserDetails userDetails) throws AccessDeniedException {

        Long userId = userService.findIdByDetails(userDetails);

        if (!treeService.isOwner(treeId, userId)) {
            throw new AccessDeniedException("Только владелец может создавать приглашения");
        }

        User invitedUser = userService.findByEmail(inviteRequest.email());
        treeService.addMember(treeId, invitedUser.getId(), inviteRequest.role());

        //TODO invite email send
        return ResponseEntity.ok(CustomApiResponse.success("Пользователь успешно добавлен"));
    }

    /*
    @GetMapping("/{treeId}/members")
    public ResponseEntity<CustomApiResponse<List<User>>> getMembers(@PathVariable Long treeId,
                                                                    @AuthenticationPrincipal UserDetails userDetails) throws AccessDeniedException {

        Long userId = userService.findIdByDetails(userDetails);

        if (!permissionService.canView(treeId, userId)) {
            throw new AccessDeniedException("Нет прав");
        }

    }

     TODO */
}
