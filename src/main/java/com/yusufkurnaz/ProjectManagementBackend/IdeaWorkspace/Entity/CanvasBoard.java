package com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Entity;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.BaseEntity;
import com.yusufkurnaz.ProjectManagementBackend.Workspace.Entity.Workspace;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Canvas Board entity for IdeaWorkspace
 * Stores Excalidraw-compatible canvas data
 */
@Entity
@Table(name = "canvas_boards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CanvasBoard extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "canvas_data", columnDefinition = "JSONB")
    private String canvasData; // Excalidraw JSON format

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L; // Optimistic locking for collaboration

    @Column(name = "last_accessed_at")
    private java.time.LocalDateTime lastAccessedAt;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "is_template")
    @Builder.Default
    private Boolean isTemplate = false;

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "fork_count")
    @Builder.Default
    private Long forkCount = 0L;

    // Canvas settings
    @Column(name = "canvas_width")
    @Builder.Default
    private Integer canvasWidth = 1920;

    @Column(name = "canvas_height")
    @Builder.Default
    private Integer canvasHeight = 1080;

    @Column(name = "background_color", length = 7)
    @Builder.Default
    private String backgroundColor = "#ffffff";

    @Column(name = "grid_enabled")
    @Builder.Default
    private Boolean gridEnabled = true;

    // Collaboration settings
    @Column(name = "collaboration_enabled")
    @Builder.Default
    private Boolean collaborationEnabled = true;

    @Column(name = "max_collaborators")
    @Builder.Default
    private Integer maxCollaborators = 10;

    // Tags for categorization
    @ElementCollection
    @CollectionTable(name = "canvas_board_tags", joinColumns = @JoinColumn(name = "canvas_board_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    // One-to-Many relationship with canvas files
    @OneToMany(mappedBy = "canvasBoard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CanvasFile> canvasFiles = new ArrayList<>();

    // Helper methods
    public void incrementViewCount() {
        this.viewCount = (this.viewCount != null ? this.viewCount : 0L) + 1;
    }

    public void incrementForkCount() {
        this.forkCount = (this.forkCount != null ? this.forkCount : 0L) + 1;
    }

    public void updateLastAccessed() {
        this.lastAccessedAt = java.time.LocalDateTime.now();
    }

    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }

    public void removeTag(String tag) {
        if (this.tags != null) {
            this.tags.remove(tag);
        }
    }

    // Workspace helper
    public UUID getWorkspaceId() {
        return this.workspace != null ? this.workspace.getId() : null;
    }
}
