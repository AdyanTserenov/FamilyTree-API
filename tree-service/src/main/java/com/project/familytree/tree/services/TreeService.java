package com.project.familytree.tree.services;

import com.project.familytree.auth.dto.UserDTO;
import com.project.familytree.auth.services.MailSenderService;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.TreeDTO;
import com.project.familytree.tree.impls.TreeRole;
import com.project.familytree.tree.models.Invitation;
import com.project.familytree.tree.models.Tree;
import com.project.familytree.tree.models.TreeMembership;
import com.project.familytree.tree.repositories.InvitationRepository;
import com.project.familytree.tree.repositories.TreeMembershipRepository;
import com.project.familytree.tree.repositories.TreeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TreeService {
    private final UserService userService;
    private final TreeRepository treeRepository;
    private final TreeMembershipRepository membershipRepository;
    private final MailSenderService mailSenderService;
    private final InvitationRepository invitationRepository;

    public TreeService(UserService userService, TreeRepository treeRepository, TreeMembershipRepository membershipRepository, MailSenderService mailSenderService, InvitationRepository invitationRepository) {
        this.userService = userService;
        this.treeRepository = treeRepository;
        this.membershipRepository = membershipRepository;
        this.mailSenderService = mailSenderService;
        this.invitationRepository = invitationRepository;
    }

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
                .orElseThrow(() -> new RuntimeException("Дерево не найдено"));
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

    public List<TreeDTO> getUserTrees(Long userId) {
        List<TreeMembership> memberships = membershipRepository.findByUserId(userId);

        return memberships.stream()
                .map(tm -> {
                    Tree t = tm.getTree();
                    return new TreeDTO(t.getId(), t.getName(), t.getCreatedAt());
                })
                .toList();
    }

    public List<UserDTO> getMembers(Long treeId) {
        List<TreeMembership> memberships = membershipRepository.findByTreeId(treeId);

        return memberships.stream()
                .map(tm -> {
                    com.project.familytree.auth.models.User u = tm.getUser();
                    return new UserDTO(u.getId(), u.getFirstName(), u.getLastName(), u.getMiddleName(), u.getEmail());
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

    private String buildInviteLink(String token) {
        return "https://familytree.example.com/invite/" + token;
    }

    public void sendInviteByEmail(Long treeId, String email, TreeRole role, Long inviterId) throws AccessDeniedException {
        String token = createInviteToken(treeId, email, role, inviterId);
        String link = buildInviteLink(token);

        com.project.familytree.auth.models.User inviter = userService.findById(inviterId);
        String treeName = getById(treeId).getName();

        // Assuming MailSenderService has sendInvitationEmail, but in starter it's sendEmail. Need to add or adjust.
        // For now, use sendEmail with formatted text.
        String subject = "Приглашение в семейное дерево " + treeName;
        String text = "Вы приглашены в дерево '" + treeName + "' с ролью " + role + ". Перейдите по ссылке: " + link;
        mailSenderService.sendEmail(email, subject, text);
    }

    public void acceptInvitation(String token, Long currentUserId) throws AccessDeniedException {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Неверная или просроченная ссылка"));

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Срок действия приглашения истёк");
        }

        com.project.familytree.auth.models.User currentUser = userService.findById(currentUserId);
        if (!currentUser.getEmail().equals(invitation.getEmail())) {
            throw new AccessDeniedException("Вы не можете принять это приглашение");
        }

        addMember(invitation.getTree().getId(), currentUserId, invitation.getRole());
        invitation.setAccepted(true);

        invitationRepository.save(invitation);
    }
}