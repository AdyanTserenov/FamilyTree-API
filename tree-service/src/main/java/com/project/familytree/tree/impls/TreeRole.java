package com.project.familytree.tree.impls;

public enum TreeRole {
    VIEWER,
    EDITOR,
    OWNER;

    public boolean hasPermission(TreeRole required) {
        return this.ordinal() >= required.ordinal();
    }
}