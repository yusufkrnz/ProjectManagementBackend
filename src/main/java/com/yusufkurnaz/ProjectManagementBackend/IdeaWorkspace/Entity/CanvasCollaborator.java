package com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Entity;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.BaseEntity;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Canvas Collaborator entity for managing canvas access and permissions
 */
@Entity
@Table(name = "canvas_collaborators", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"canvas_board_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CanvasCollaborator extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canvas_board_id", nullable = false)
    private CanvasBoard canvasBoard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false)
    @Builder.Default
    private Permission permission = Permission.VIEWER;

    @Column(name = "invited_by")
    private java.util.UUID invitedBy;

    @Column(name = "invitation_sent_at")
    private java.time.LocalDateTime invitationSentAt;

    @Column(name = "joined_at")
    private java.time.LocalDateTime joinedAt;

    @Column(name = "last_active_at")
    private java.time.LocalDateTime lastActiveAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CollaboratorStatus status = CollaboratorStatus.PENDING;

    // Real-time collaboration metadata
    @Column(name = "cursor_position_x")
    private Double cursorPositionX;

    @Column(name = "cursor_position_y")
    private Double cursorPositionY;

    @Column(name = "is_online")
    @Builder.Default
    private Boolean isOnline = false;

    @Column(name = "user_color", length = 7)
    private String userColor; // Hex color for cursor/selection

    @Column(name = "session_id", length = 100)
    private String sessionId; // WebSocket session ID

    public enum Permission {
        VIEWER,    // Can only view
        EDITOR,    // Can edit canvas
        ADMIN      // Can manage collaborators
    }

    public enum CollaboratorStatus {
        PENDING,   // Invitation sent, not accepted
        ACTIVE,    // Active collaborator
        INACTIVE,  // Temporarily inactive
        REMOVED    // Removed from canvas
    }

    // Helper methods
    public void updateLastActive() {
        this.lastActiveAt = java.time.LocalDateTime.now();
    }

    public void updateCursorPosition(Double x, Double y) {
        this.cursorPositionX = x;
        this.cursorPositionY = y;
        updateLastActive();
    }

    public void setOnline(String sessionId) {
        this.isOnline = true;
        this.sessionId = sessionId;
        updateLastActive();
    }

    public void setOffline() {
        this.isOnline = false;
        this.sessionId = null;
        updateLastActive();
    }

    public void acceptInvitation() {
        this.status = CollaboratorStatus.ACTIVE;
        this.joinedAt = java.time.LocalDateTime.now();
        updateLastActive();
    }

    public boolean canEdit() {
        return Permission.EDITOR.equals(this.permission) || 
               Permission.ADMIN.equals(this.permission);
    }

    public boolean canManage() {
        return Permission.ADMIN.equals(this.permission);
    }

    public boolean isActive() {
        return CollaboratorStatus.ACTIVE.equals(this.status);
    }

    // Get user info helpers
    public java.util.UUID getUserId() {
        return this.user != null ? this.user.getId() : null;
    }

    public String getUserName() {
        return this.user != null ? this.user.getUsername() : null;
    }

    public String getUserEmail() {
        return this.user != null ? this.user.getEmail() : null;
    }
}
