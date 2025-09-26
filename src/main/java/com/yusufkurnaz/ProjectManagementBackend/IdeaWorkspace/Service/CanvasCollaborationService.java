package com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Service;

import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Entity.CanvasCollaborator;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Canvas Collaboration operations
 * Manages real-time collaboration, permissions, and user interactions
 */
public interface CanvasCollaborationService {

    // Collaborator management
    CanvasCollaborator inviteCollaborator(UUID canvasBoardId, UUID userId, UUID invitedUserId, 
                                        CanvasCollaborator.Permission permission, UUID invitedBy);
    
    CanvasCollaborator inviteCollaboratorByEmail(UUID canvasBoardId, String email, 
                                               CanvasCollaborator.Permission permission, UUID invitedBy);
    
    CanvasCollaborator acceptInvitation(UUID canvasBoardId, UUID userId);
    
    void rejectInvitation(UUID canvasBoardId, UUID userId);
    
    void removeCollaborator(UUID canvasBoardId, UUID userId, UUID removedBy);

    // Permission management
    CanvasCollaborator updateCollaboratorPermission(UUID canvasBoardId, UUID userId, 
                                                  CanvasCollaborator.Permission permission, UUID updatedBy);
    
    CanvasCollaborator.Permission getUserPermission(UUID canvasBoardId, UUID userId);
    
    boolean canUserEdit(UUID canvasBoardId, UUID userId);
    
    boolean canUserManage(UUID canvasBoardId, UUID userId);
    
    boolean isCollaborator(UUID canvasBoardId, UUID userId);

    // Collaborator queries
    List<CanvasCollaborator> getCanvasCollaborators(UUID canvasBoardId);
    
    List<CanvasCollaborator> getActiveCollaborators(UUID canvasBoardId);
    
    List<CanvasCollaborator> getOnlineCollaborators(UUID canvasBoardId);
    
    List<CanvasCollaborator> getPendingInvitations(UUID canvasBoardId);
    
    List<CanvasCollaborator> getUserInvitations(UUID userId);

    // Real-time collaboration
    void userJoinedCanvas(UUID canvasBoardId, UUID userId, String sessionId);
    
    void userLeftCanvas(UUID canvasBoardId, UUID userId, String sessionId);
    
    void updateUserCursor(UUID canvasBoardId, UUID userId, Double x, Double y);
    
    void updateUserActivity(UUID canvasBoardId, UUID userId);
    
    CanvasCollaborator getCollaboratorBySession(String sessionId);

    // Online presence
    List<CanvasCollaborator> getOnlineUsers(UUID canvasBoardId);
    
    Long getOnlineUserCount(UUID canvasBoardId);
    
    void setUserOnline(UUID canvasBoardId, UUID userId, String sessionId);
    
    void setUserOffline(UUID canvasBoardId, UUID userId);
    
    void disconnectSession(String sessionId);

    // Collaboration statistics
    Long getTotalCollaborators(UUID canvasBoardId);
    
    Long getActiveCollaboratorCount(UUID canvasBoardId);
    
    List<CanvasCollaborator> getRecentlyActiveCollaborators(UUID canvasBoardId, int limit);
    
    List<CanvasCollaborator> getMostActiveCollaborators(UUID canvasBoardId, int limit);

    // Workspace-level collaboration
    List<CanvasCollaborator> getWorkspaceCollaborations(UUID workspaceId, UUID userId);
    
    List<com.yusufkurnaz.ProjectManagementBackend.Common.Model.User> getWorkspaceCollaborators(UUID workspaceId);
    
    Long getWorkspaceCollaboratorCount(UUID workspaceId);

    // Invitation management
    void resendInvitation(UUID canvasBoardId, UUID userId, UUID resentBy);
    
    void cancelInvitation(UUID canvasBoardId, UUID userId, UUID cancelledBy);
    
    boolean hasValidInvitation(UUID canvasBoardId, UUID userId);
    
    void expireOldInvitations(int daysOld);

    // Collaboration settings
    void enableCollaboration(UUID canvasBoardId, UUID userId);
    
    void disableCollaboration(UUID canvasBoardId, UUID userId);
    
    void setMaxCollaborators(UUID canvasBoardId, Integer maxCollaborators, UUID userId);
    
    boolean isCollaborationEnabled(UUID canvasBoardId);

    // User color and preferences
    CanvasCollaborator setUserColor(UUID canvasBoardId, UUID userId, String color);
    
    String getUserColor(UUID canvasBoardId, UUID userId);
    
    String generateUniqueUserColor(UUID canvasBoardId);

    // Validation and security
    void validateCollaboratorAccess(UUID canvasBoardId, UUID userId);
    
    void validateManagePermission(UUID canvasBoardId, UUID userId);
    
    void validateEditPermission(UUID canvasBoardId, UUID userId);
    
    boolean canInviteCollaborators(UUID canvasBoardId, UUID userId);

    // Cleanup operations
    void cleanupInactiveCollaborators(UUID canvasBoardId);
    
    void deactivateAllCollaborators(UUID canvasBoardId);
    
    void removeInactiveCollaborators(int daysInactive);

    // Bulk operations
    List<CanvasCollaborator> inviteMultipleCollaborators(UUID canvasBoardId, List<UUID> userIds, 
                                                       CanvasCollaborator.Permission permission, UUID invitedBy);
    
    void removeMultipleCollaborators(UUID canvasBoardId, List<UUID> userIds, UUID removedBy);
    
    void updateMultiplePermissions(UUID canvasBoardId, List<UUID> userIds, 
                                 CanvasCollaborator.Permission permission, UUID updatedBy);

    // Notification integration
    void notifyCollaboratorInvited(UUID canvasBoardId, UUID invitedUserId, UUID invitedBy);
    
    void notifyCollaboratorJoined(UUID canvasBoardId, UUID joinedUserId);
    
    void notifyCollaboratorLeft(UUID canvasBoardId, UUID leftUserId);
    
    void notifyPermissionChanged(UUID canvasBoardId, UUID userId, CanvasCollaborator.Permission newPermission);
}
