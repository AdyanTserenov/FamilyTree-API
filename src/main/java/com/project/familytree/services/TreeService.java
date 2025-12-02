package com.project.familytree.services;

import com.project.familytree.dto.UserDTO;
import com.project.familytree.exceptions.InvalidRequestException;
import com.project.familytree.impls.TreeRole;
import com.project.familytree.models.Tree;
import com.project.familytree.models.TreeMembership;
import com.project.familytree.models.User;
import com.project.familytree.repositories.TreeMembershipRepository;
import com.project.familytree.repositories.TreeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Member;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TreeService {
    private final UserService userService;
    private final TreeRepository treeRepository;
    private final TreeMembershipRepository membershipRepository;


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

    @Transactional
    public void addMember(Long treeId, Long userId, TreeRole role) {
        TreeMembership treeMembership = new TreeMembership();
        treeMembership.setTree(treeRepository.getReferenceById(treeId));
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
        return hasRole(treeId, userId, TreeRole.VIEWER);
    }

    public boolean canEdit(Long treeId, Long userId) {
        return hasRole(treeId, userId, TreeRole.EDITOR);
    }

    public boolean isOwner(Long treeId, Long userId) {
        return hasRole(treeId, userId, TreeRole.OWNER);
    }

    public List<UserDTO> getMembers(Long treeId) {
        List<TreeMembership> memberships = membershipRepository.findByTreeId(treeId);

        if (memberships.isEmpty()) {
            throw new InvalidRequestException("Дерево не существует");
        }

        return memberships.stream()
                .map(tm -> {
                    User u = tm.getUser();
                    return new UserDTO(u.getFirstName(), u.getLastName(), u.getMiddleName(), u.getEmail());
                })
                .toList();
    }
}
