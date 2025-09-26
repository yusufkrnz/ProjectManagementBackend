package com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Service;

import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Entity.CanvasBoard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Canvas Board operations
 * Follows SOLID principles - Single responsibility for canvas management
 */
public interface CanvasService {

    // CRUD Operations
    CanvasBoard createCanvas(String name, String description, UUID workspaceId, UUID userId);
    
    CanvasBoard getCanvas(UUID canvasId, UUID userId);
    
    CanvasBoard updateCanvas(UUID canvasId, String name, String description, UUID userId);
    
    CanvasBoard updateCanvasData(UUID canvasId, String canvasData, Long version, UUID userId);
    
    void deleteCanvas(UUID canvasId, UUID userId);

    // Canvas data operations
    CanvasBoard saveCanvasData(UUID canvasId, String canvasData, UUID userId);
    
    String getCanvasData(UUID canvasId, UUID userId);
    
    CanvasBoard forkCanvas(UUID sourceCanvasId, String newName, UUID userId);

    // Access and permissions
    boolean canUserAccessCanvas(UUID canvasId, UUID userId);
    
    boolean canUserEditCanvas(UUID canvasId, UUID userId);
    
    void makeCanvasPublic(UUID canvasId, UUID userId);
    
    void makeCanvasPrivate(UUID canvasId, UUID userId);

    // Search and listing
    List<CanvasBoard> getUserCanvases(UUID userId);
    
    Page<CanvasBoard> getUserCanvases(UUID userId, Pageable pageable);
    
    List<CanvasBoard> getWorkspaceCanvases(UUID workspaceId, UUID userId);
    
    Page<CanvasBoard> getWorkspaceCanvases(UUID workspaceId, UUID userId, Pageable pageable);
    
    List<CanvasBoard> getPublicCanvases();
    
    Page<CanvasBoard> getPublicCanvases(Pageable pageable);
    
    List<CanvasBoard> searchCanvases(String searchTerm, UUID workspaceId, UUID userId);
    
    List<CanvasBoard> getCanvasesByTags(List<String> tags, UUID workspaceId, UUID userId);

    // Templates
    List<CanvasBoard> getTemplates();
    
    CanvasBoard createTemplate(UUID canvasId, String templateName, UUID userId);
    
    CanvasBoard createFromTemplate(UUID templateId, String newName, UUID workspaceId, UUID userId);

    // Statistics and analytics
    void incrementViewCount(UUID canvasId);
    
    List<CanvasBoard> getRecentlyAccessed(UUID userId, int limit);
    
    List<CanvasBoard> getMostViewed(int limit);
    
    Long getCanvasCount(UUID workspaceId);
    
    Long getUserCanvasCount(UUID userId);

    // Tags management
    CanvasBoard addTag(UUID canvasId, String tag, UUID userId);
    
    CanvasBoard removeTag(UUID canvasId, String tag, UUID userId);
    
    List<String> getPopularTags(UUID workspaceId, int limit);

    // Canvas settings
    CanvasBoard updateCanvasSettings(UUID canvasId, Integer width, Integer height, 
                                   String backgroundColor, Boolean gridEnabled, UUID userId);
    
    CanvasBoard updateCollaborationSettings(UUID canvasId, Boolean collaborationEnabled, 
                                          Integer maxCollaborators, UUID userId);

    // Validation and utilities
    void validateCanvasAccess(UUID canvasId, UUID userId);
    
    void validateCanvasEditAccess(UUID canvasId, UUID userId);
    
    boolean isValidCanvasData(String canvasData);
}
