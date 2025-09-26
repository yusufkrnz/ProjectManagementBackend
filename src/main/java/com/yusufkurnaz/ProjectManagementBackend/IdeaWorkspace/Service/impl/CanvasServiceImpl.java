package com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Service.impl;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.User;
import com.yusufkurnaz.ProjectManagementBackend.Common.Repository.UserRepository;
import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Entity.CanvasBoard;
import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Entity.CanvasCollaborator;
import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Repository.CanvasBoardRepository;
import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Repository.CanvasCollaboratorRepository;
import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Service.CanvasService;
import com.yusufkurnaz.ProjectManagementBackend.Workspace.Entity.Workspace;
import com.yusufkurnaz.ProjectManagementBackend.Workspace.Repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of CanvasService
 * Follows SOLID principles and project conventions
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CanvasServiceImpl implements CanvasService {

    private final CanvasBoardRepository canvasBoardRepository;
    private final CanvasCollaboratorRepository collaboratorRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    @Override
    public CanvasBoard createCanvas(String name, String description, UUID workspaceId, UUID userId) {
        log.info("Creating canvas '{}' in workspace {} by user {}", name, workspaceId, userId);

        // Validate workspace exists and user has access
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("Workspace not found: " + workspaceId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Create canvas board
        CanvasBoard canvas = CanvasBoard.builder()
                .name(name)
                .description(description)
                .workspace(workspace)
                .canvasData("{\"type\":\"excalidraw\",\"version\":2,\"source\":\"https://excalidraw.com\",\"elements\":[],\"appState\":{\"gridSize\":null,\"viewBackgroundColor\":\"#ffffff\"},\"files\":{}}")
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        CanvasBoard savedCanvas = canvasBoardRepository.save(canvas);
        
        // Add creator as admin collaborator
        CanvasCollaborator creatorCollaborator = CanvasCollaborator.builder()
                .canvasBoard(savedCanvas)
                .user(user)
                .permission(CanvasCollaborator.Permission.ADMIN)
                .status(CanvasCollaborator.CollaboratorStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        collaboratorRepository.save(creatorCollaborator);

        log.info("Canvas created successfully with ID: {}", savedCanvas.getId());
        return savedCanvas;
    }

    @Override
    @Transactional(readOnly = true)
    public CanvasBoard getCanvas(UUID canvasId, UUID userId) {
        log.debug("Getting canvas {} for user {}", canvasId, userId);

        CanvasBoard canvas = canvasBoardRepository.findByIdWithAccess(canvasId, userId)
                .orElseThrow(() -> new RuntimeException("Canvas not found or access denied: " + canvasId));

        // Update last accessed time
        canvas.updateLastAccessed();
        canvasBoardRepository.save(canvas);

        return canvas;
    }

    @Override
    public CanvasBoard updateCanvas(UUID canvasId, String name, String description, UUID userId) {
        log.info("Updating canvas {} by user {}", canvasId, userId);

        validateCanvasEditAccess(canvasId, userId);

        CanvasBoard canvas = canvasBoardRepository.findById(canvasId)
                .orElseThrow(() -> new RuntimeException("Canvas not found: " + canvasId));

        canvas.setName(name);
        canvas.setDescription(description);
        canvas.setUpdatedBy(userId);

        return canvasBoardRepository.save(canvas);
    }

    @Override
    public CanvasBoard updateCanvasData(UUID canvasId, String canvasData, Long version, UUID userId) {
        log.debug("Updating canvas data for canvas {} by user {}", canvasId, userId);

        validateCanvasEditAccess(canvasId, userId);

        CanvasBoard canvas = canvasBoardRepository.findById(canvasId)
                .orElseThrow(() -> new RuntimeException("Canvas not found: " + canvasId));

        // Optimistic locking check
        if (!canvas.getVersion().equals(version)) {
            throw new RuntimeException("Canvas has been modified by another user. Please refresh and try again.");
        }

        if (!isValidCanvasData(canvasData)) {
            throw new RuntimeException("Invalid canvas data format");
        }

        canvas.setCanvasData(canvasData);
        canvas.setUpdatedBy(userId);
        canvas.updateLastAccessed();

        return canvasBoardRepository.save(canvas);
    }

    @Override
    public void deleteCanvas(UUID canvasId, UUID userId) {
        log.info("Deleting canvas {} by user {}", canvasId, userId);

        CanvasBoard canvas = canvasBoardRepository.findById(canvasId)
                .orElseThrow(() -> new RuntimeException("Canvas not found: " + canvasId));

        // Only creator or admin can delete
        if (!canvas.getCreatedBy().equals(userId)) {
            CanvasCollaborator.Permission permission = collaboratorRepository
                    .getUserPermission(canvasId, userId)
                    .orElseThrow(() -> new RuntimeException("Access denied"));

            if (!CanvasCollaborator.Permission.ADMIN.equals(permission)) {
                throw new RuntimeException("Only canvas creator or admin can delete");
            }
        }

        canvas.setIsActive(false);
        canvas.setUpdatedBy(userId);
        canvasBoardRepository.save(canvas);

        // Deactivate all collaborators
        collaboratorRepository.deactivateAllCollaborators(canvasId);
    }

    @Override
    public CanvasBoard saveCanvasData(UUID canvasId, String canvasData, UUID userId) {
        return updateCanvasData(canvasId, canvasData, null, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getCanvasData(UUID canvasId, UUID userId) {
        validateCanvasAccess(canvasId, userId);

        CanvasBoard canvas = canvasBoardRepository.findById(canvasId)
                .orElseThrow(() -> new RuntimeException("Canvas not found: " + canvasId));

        return canvas.getCanvasData();
    }

    @Override
    public CanvasBoard forkCanvas(UUID sourceCanvasId, String newName, UUID userId) {
        log.info("Forking canvas {} to '{}' by user {}", sourceCanvasId, newName, userId);

        CanvasBoard sourceCanvas = canvasBoardRepository.findByIdWithAccess(sourceCanvasId, userId)
                .orElseThrow(() -> new RuntimeException("Source canvas not found or access denied"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Create forked canvas
        CanvasBoard forkedCanvas = CanvasBoard.builder()
                .name(newName)
                .description("Forked from: " + sourceCanvas.getName())
                .workspace(sourceCanvas.getWorkspace())
                .canvasData(sourceCanvas.getCanvasData())
                .canvasWidth(sourceCanvas.getCanvasWidth())
                .canvasHeight(sourceCanvas.getCanvasHeight())
                .backgroundColor(sourceCanvas.getBackgroundColor())
                .gridEnabled(sourceCanvas.getGridEnabled())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        CanvasBoard savedCanvas = canvasBoardRepository.save(forkedCanvas);

        // Add creator as admin collaborator
        CanvasCollaborator creatorCollaborator = CanvasCollaborator.builder()
                .canvasBoard(savedCanvas)
                .user(user)
                .permission(CanvasCollaborator.Permission.ADMIN)
                .status(CanvasCollaborator.CollaboratorStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        collaboratorRepository.save(creatorCollaborator);

        // Increment fork count on source
        canvasBoardRepository.incrementForkCount(sourceCanvasId);

        return savedCanvas;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserAccessCanvas(UUID canvasId, UUID userId) {
        return canvasBoardRepository.findByIdWithAccess(canvasId, userId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserEditCanvas(UUID canvasId, UUID userId) {
        try {
            validateCanvasEditAccess(canvasId, userId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void makeCanvasPublic(UUID canvasId, UUID userId) {
        log.info("Making canvas {} public by user {}", canvasId, userId);

        validateCanvasEditAccess(canvasId, userId);

        CanvasBoard canvas = canvasBoardRepository.findById(canvasId)
                .orElseThrow(() -> new RuntimeException("Canvas not found: " + canvasId));

        canvas.setIsPublic(true);
        canvas.setUpdatedBy(userId);
        canvasBoardRepository.save(canvas);
    }

    @Override
    public void makeCanvasPrivate(UUID canvasId, UUID userId) {
        log.info("Making canvas {} private by user {}", canvasId, userId);

        validateCanvasEditAccess(canvasId, userId);

        CanvasBoard canvas = canvasBoardRepository.findById(canvasId)
                .orElseThrow(() -> new RuntimeException("Canvas not found: " + canvasId));

        canvas.setIsPublic(false);
        canvas.setUpdatedBy(userId);
        canvasBoardRepository.save(canvas);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanvasBoard> getUserCanvases(UUID userId) {
        return canvasBoardRepository.findByCreatedByAndIsActiveTrue(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CanvasBoard> getUserCanvases(UUID userId, Pageable pageable) {
        return canvasBoardRepository.findByCreatedByAndIsActiveTrue(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanvasBoard> getWorkspaceCanvases(UUID workspaceId, UUID userId) {
        // TODO: Add workspace access validation
        return canvasBoardRepository.findByWorkspaceIdAndIsActiveTrue(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CanvasBoard> getWorkspaceCanvases(UUID workspaceId, UUID userId, Pageable pageable) {
        // TODO: Add workspace access validation
        return canvasBoardRepository.findByWorkspaceIdAndIsActiveTrue(workspaceId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanvasBoard> getPublicCanvases() {
        return canvasBoardRepository.findByIsPublicTrueAndIsActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CanvasBoard> getPublicCanvases(Pageable pageable) {
        return canvasBoardRepository.findByIsPublicTrueAndIsActiveTrue(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanvasBoard> searchCanvases(String searchTerm, UUID workspaceId, UUID userId) {
        if (workspaceId != null) {
            return canvasBoardRepository.searchByNameInWorkspace(workspaceId, searchTerm);
        }
        return canvasBoardRepository.searchByName(searchTerm);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanvasBoard> getCanvasesByTags(List<String> tags, UUID workspaceId, UUID userId) {
        List<String> lowerCaseTags = tags.stream().map(String::toLowerCase).toList();
        
        if (workspaceId != null) {
            return canvasBoardRepository.findByTagsInWorkspace(workspaceId, lowerCaseTags);
        }
        return canvasBoardRepository.findByTagsIn(lowerCaseTags);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanvasBoard> getTemplates() {
        return canvasBoardRepository.findByIsTemplateTrueAndIsActiveTrue();
    }

    @Override
    public CanvasBoard createTemplate(UUID canvasId, String templateName, UUID userId) {
        log.info("Creating template from canvas {} by user {}", canvasId, userId);

        validateCanvasAccess(canvasId, userId);

        CanvasBoard sourceCanvas = canvasBoardRepository.findById(canvasId)
                .orElseThrow(() -> new RuntimeException("Canvas not found: " + canvasId));

        // Create template canvas
        CanvasBoard template = CanvasBoard.builder()
                .name(templateName)
                .description("Template created from: " + sourceCanvas.getName())
                .workspace(sourceCanvas.getWorkspace())
                .canvasData(sourceCanvas.getCanvasData())
                .canvasWidth(sourceCanvas.getCanvasWidth())
                .canvasHeight(sourceCanvas.getCanvasHeight())
                .backgroundColor(sourceCanvas.getBackgroundColor())
                .gridEnabled(sourceCanvas.getGridEnabled())
                .isTemplate(true)
                .isPublic(true)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        return canvasBoardRepository.save(template);
    }

    @Override
    public CanvasBoard createFromTemplate(UUID templateId, String newName, UUID workspaceId, UUID userId) {
        log.info("Creating canvas from template {} by user {}", templateId, userId);

        CanvasBoard template = canvasBoardRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        if (!template.getIsTemplate()) {
            throw new RuntimeException("Canvas is not a template");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("Workspace not found: " + workspaceId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Create canvas from template
        CanvasBoard canvas = CanvasBoard.builder()
                .name(newName)
                .description("Created from template: " + template.getName())
                .workspace(workspace)
                .canvasData(template.getCanvasData())
                .canvasWidth(template.getCanvasWidth())
                .canvasHeight(template.getCanvasHeight())
                .backgroundColor(template.getBackgroundColor())
                .gridEnabled(template.getGridEnabled())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        CanvasBoard savedCanvas = canvasBoardRepository.save(canvas);

        // Add creator as admin collaborator
        CanvasCollaborator creatorCollaborator = CanvasCollaborator.builder()
                .canvasBoard(savedCanvas)
                .user(user)
                .permission(CanvasCollaborator.Permission.ADMIN)
                .status(CanvasCollaborator.CollaboratorStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        collaboratorRepository.save(creatorCollaborator);

        return savedCanvas;
    }

    @Override
    public void incrementViewCount(UUID canvasId) {
        canvasBoardRepository.incrementViewCount(canvasId, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanvasBoard> getRecentlyAccessed(UUID userId, int limit) {
        return canvasBoardRepository.findRecentlyAccessedByUser(userId, PageRequest.of(0, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanvasBoard> getMostViewed(int limit) {
        return canvasBoardRepository.findMostViewed(PageRequest.of(0, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public Long getCanvasCount(UUID workspaceId) {
        return canvasBoardRepository.countByWorkspace(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUserCanvasCount(UUID userId) {
        return canvasBoardRepository.countByUser(userId);
    }

    @Override
    public CanvasBoard addTag(UUID canvasId, String tag, UUID userId) {
        validateCanvasEditAccess(canvasId, userId);

        CanvasBoard canvas = canvasBoardRepository.findById(canvasId)
                .orElseThrow(() -> new RuntimeException("Canvas not found: " + canvasId));

        canvas.addTag(tag.toLowerCase());
        canvas.setUpdatedBy(userId);
        return canvasBoardRepository.save(canvas);
    }

    @Override
    public CanvasBoard removeTag(UUID canvasId, String tag, UUID userId) {
        validateCanvasEditAccess(canvasId, userId);

        CanvasBoard canvas = canvasBoardRepository.findById(canvasId)
                .orElseThrow(() -> new RuntimeException("Canvas not found: " + canvasId));

        canvas.removeTag(tag.toLowerCase());
        canvas.setUpdatedBy(userId);
        return canvasBoardRepository.save(canvas);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getPopularTags(UUID workspaceId, int limit) {
        // TODO: Implement popular tags query
        return List.of();
    }

    @Override
    public CanvasBoard updateCanvasSettings(UUID canvasId, Integer width, Integer height, 
                                          String backgroundColor, Boolean gridEnabled, UUID userId) {
        validateCanvasEditAccess(canvasId, userId);

        CanvasBoard canvas = canvasBoardRepository.findById(canvasId)
                .orElseThrow(() -> new RuntimeException("Canvas not found: " + canvasId));

        if (width != null) canvas.setCanvasWidth(width);
        if (height != null) canvas.setCanvasHeight(height);
        if (backgroundColor != null) canvas.setBackgroundColor(backgroundColor);
        if (gridEnabled != null) canvas.setGridEnabled(gridEnabled);
        
        canvas.setUpdatedBy(userId);
        return canvasBoardRepository.save(canvas);
    }

    @Override
    public CanvasBoard updateCollaborationSettings(UUID canvasId, Boolean collaborationEnabled, 
                                                 Integer maxCollaborators, UUID userId) {
        validateCanvasEditAccess(canvasId, userId);

        CanvasBoard canvas = canvasBoardRepository.findById(canvasId)
                .orElseThrow(() -> new RuntimeException("Canvas not found: " + canvasId));

        if (collaborationEnabled != null) canvas.setCollaborationEnabled(collaborationEnabled);
        if (maxCollaborators != null) canvas.setMaxCollaborators(maxCollaborators);
        
        canvas.setUpdatedBy(userId);
        return canvasBoardRepository.save(canvas);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCanvasAccess(UUID canvasId, UUID userId) {
        if (!canUserAccessCanvas(canvasId, userId)) {
            throw new RuntimeException("Access denied to canvas: " + canvasId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCanvasEditAccess(UUID canvasId, UUID userId) {
        CanvasBoard canvas = canvasBoardRepository.findById(canvasId)
                .orElseThrow(() -> new RuntimeException("Canvas not found: " + canvasId));

        // Creator always has edit access
        if (canvas.getCreatedBy().equals(userId)) {
            return;
        }

        // Check collaborator permission
        CanvasCollaborator.Permission permission = collaboratorRepository
                .getUserPermission(canvasId, userId)
                .orElseThrow(() -> new RuntimeException("Access denied to canvas: " + canvasId));

        if (!CanvasCollaborator.Permission.EDITOR.equals(permission) && 
            !CanvasCollaborator.Permission.ADMIN.equals(permission)) {
            throw new RuntimeException("Edit permission required for canvas: " + canvasId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValidCanvasData(String canvasData) {
        if (canvasData == null || canvasData.trim().isEmpty()) {
            return false;
        }

        try {
            // Basic JSON validation - could be enhanced with JSON schema validation
            return canvasData.trim().startsWith("{") && canvasData.trim().endsWith("}") &&
                   canvasData.contains("\"elements\"") && canvasData.contains("\"files\"");
        } catch (Exception e) {
            log.warn("Invalid canvas data format: {}", e.getMessage());
            return false;
        }
    }
}
