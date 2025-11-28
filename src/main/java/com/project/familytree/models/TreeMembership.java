package com.project.familytree.models;

import com.project.familytree.impls.TreeRole;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Data
@Entity
@Table(name = "tree_memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"tree_id", "user_id"}))
public class TreeMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tree_id", nullable = false)
    private Tree tree;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TreeRole role;

    @CreationTimestamp
    private Instant createdAt;

}
