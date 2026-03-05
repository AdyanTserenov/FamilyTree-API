package com.project.familytree.services;

import com.project.familytree.dto.UserDTO;
import com.project.familytree.exceptions.InvalidRequestException;
import com.project.familytree.exceptions.InvalidTokenException;
import com.project.familytree.exceptions.TokenExpiredException;
import com.project.familytree.exceptions.TreeNotFoundException;
import com.project.familytree.impls.TreeRole;
import com.project.familytree.models.Invitation;
import com.project.familytree.models.Tree;
import com.project.familytree.models.TreeMembership;
import com.project.familytree.models.User;
import com.project.familytree.repositories.InvitationRepository;
import com.project.familytree.repositories.TreeMembershipRepository;
import com.project.familytree.repositories.TreeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TreeService {
    private final UserService userService;
    private final TreeRepository treeRepository;
    private final TreeMembershipRepository membershipRepository;
    private final MailSenderService mailSenderService;
    private final InvitationRepository invitationRepository;


    @Transactional
    public void createTree(String treeName, Long ownerId) {
        Tree tree = new Tree();
        tree.setName(treeName);
        tree = treeRepository.save(tree);

        TreeMembership owner = new TreeMembership();
        owner.setTree(tree);
        owner.setUser(userService.findById(ownerId));
        owner.setRole(TreeRole.OWNER);
        membershipRepository.save(owner);
    }

    public Tree getById(Long treeId) {
        return treeRepository.findById(treeId)
                .orElseThrow(() -> new TreeNotFoundException("Дерево не найдено"));
    }

    @Transactional
    public void addMember(Long treeId, Long userId, TreeRole role) {
        TreeMembership treeMembership = new TreeMembership();
        treeMembership.setTree(getById(treeId));
        treeMembership.setUser(userService.findById(userId));
        treeMembership.setRole(role);
        membershipRepository.save(treeMembership);
    }

    public boolean hasRole(Long treeId, Long userId, TreeRole required) {
        return membershipRepository.findByTreeIdAndUserId(treeId, userId)
                .map(m -> m.getRole().hasPermission(required))
                .orElse(false);
    }

    public boolean canView(Long treeId, Long userId) {
        return hasRole(treeId, userId, TreeRole.VIEWER) || isOwner(treeId, userId);
    }

    public boolean canEdit(Long treeId, Long userId) {
        return hasRole(treeId, userId, TreeRole.EDITOR);
    }

    public boolean isOwner(Long treeId, Long userId) {
        return hasRole(treeId, userId, TreeRole.OWNER);
    }

    public List<UserDTO> getMembers(Long treeId) {
        List<TreeMembership> memberships = membershipRepository.findByTreeId(treeId);

        return memberships.stream()
                .map(tm -> {
                    User u = tm.getUser();
                    return new UserDTO(u.getFirstName(), u.getLastName(), u.getMiddleName(), u.getEmail());
                })
                .toList();
    }

    public String createInviteToken(Long treeId, String email, TreeRole role, Long inviterId) throws AccessDeniedException {
        if (!isOwner(treeId, inviterId)) {
            throw new AccessDeniedException("Только владелец может приглашать");
        }

        Invitation invitation = new Invitation();
        invitation.setTree(getById(treeId));
        invitation.setEmail(email);
        invitation.setRole(role);
        invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));

        invitation = invitationRepository.save(invitation);

        return invitation.getToken();
    }

    @org.springframework.beans.factory.annotation.Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    private String buildInviteLink(String token) {
        return baseUrl + "/invite/" + token;
    }

    public void sendInviteByEmail(Long treeId, String email, TreeRole role, Long inviterId) throws AccessDeniedException {
        String token = createInviteToken(treeId, email, role, inviterId);
        String link = buildInviteLink(token);

        User inviter = userService.findById(inviterId);
        String treeName = getById(treeId).getName();

        mailSenderService.sendInvitationEmail(email, treeName, inviter, role, link);
    }

    public void acceptInvitation(String token, Long currentUserId) throws AccessDeniedException {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Неверная или просроченная ссылка"));

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Срок действия приглашения истёк");
        }

        User currentUser = userService.findById(currentUserId);
        if (!currentUser.getEmail().equals(invitation.getEmail())) {
            throw new AccessDeniedException("Вы не можете принять это приглашение");
        }

        addMember(invitation.getTree().getId(), currentUserId, invitation.getRole());
        invitation.setAccepted(true);

        invitationRepository.save(invitation);
    }
}
