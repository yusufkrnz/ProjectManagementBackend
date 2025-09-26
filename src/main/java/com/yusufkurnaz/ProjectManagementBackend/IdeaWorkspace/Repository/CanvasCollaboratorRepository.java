package com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Repository;

import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Entity.CanvasCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CanvasCollaborator entity
 * Manages canvas collaboration and permissions
 */
@Repository
public interface CanvasCollaboratorRepository extends JpaRepository<CanvasCollaborator, UUID> {

    // Find by canvas board
    List<CanvasCollaborator> findByCanvasBoardIdAndIsActiveTrue(UUID canvasBoardId);

    // Find by user
    @Query("SELECT cc FROM CanvasCollaborator cc WHERE cc.user.id = :userId AND cc.isActive = true")
    List<CanvasCollaborator> findByUserIdAndIsActiveTrue(@Param("userId") UUID userId);

    // Find specific collaborator
    @Query("SELECT cc FROM CanvasCollaborator cc WHERE cc.canvasBoard.id = :canvasBoardId AND cc.user.id = :userId AND cc.isActive = true")
    Optional<CanvasCollaborator> findByCanvasBoardIdAndUserIdAndIsActiveTrue(
        @Param("canvasBoardId") UUID canvasBoardId, @Param("userId") UUID userId);

    // Find by status
    List<CanvasCollaborator> findByCanvasBoardIdAndStatusAndIsActiveTrue(
        UUID canvasBoardId, CanvasCollaborator.CollaboratorStatus status);

    List<CanvasCollaborator> findByStatusAndIsActiveTrue(CanvasCollaborator.CollaboratorStatus status);

    // Find by permission level
    List<CanvasCollaborator> findByCanvasBoardIdAndPermissionAndIsActiveTrue(
        UUID canvasBoardId, CanvasCollaborator.Permission permission);

    // Find online collaborators
    @Query("SELECT cc FROM CanvasCollaborator cc WHERE cc.canvasBoard.id = :canvasBoardId AND " +
           "cc.isOnline = true AND cc.isActive = true")
    List<CanvasCollaborator> findOnlineCollaborators(@Param("canvasBoardId") UUID canvasBoardId);

    // Find by session ID
    Optional<CanvasCollaborator> findBySessionIdAndIsActiveTrue(String sessionId);

    // Check if user is collaborator
    @Query("SELECT COUNT(cc) > 0 FROM CanvasCollaborator cc WHERE cc.canvasBoard.id = :canvasBoardId AND " +
           "cc.user.id = :userId AND cc.status = 'ACTIVE' AND cc.isActive = true")
    boolean isActiveCollaborator(@Param("canvasBoardId") UUID canvasBoardId, @Param("userId") UUID userId);

    // Check user permission
    @Query("SELECT cc.permission FROM CanvasCollaborator cc WHERE cc.canvasBoard.id = :canvasBoardId AND " +
           "cc.user.id = :userId AND cc.status = 'ACTIVE' AND cc.isActive = true")
    Optional<CanvasCollaborator.Permission> getUserPermission(@Param("canvasBoardId") UUID canvasBoardId, 
                                                            @Param("userId") UUID userId);

    // Update operations
    @Modifying
    @Query("UPDATE CanvasCollaborator cc SET cc.lastActiveAt = :activeTime WHERE cc.id = :collaboratorId")
    void updateLastActive(@Param("collaboratorId") UUID collaboratorId, @Param("activeTime") LocalDateTime activeTime);

    @Modifying
    @Query("UPDATE CanvasCollaborator cc SET cc.cursorPositionX = :x, cc.cursorPositionY = :y, " +
           "cc.lastActiveAt = :activeTime WHERE cc.id = :collaboratorId")
    void updateCursorPosition(@Param("collaboratorId") UUID collaboratorId,
                            @Param("x") Double x, @Param("y") Double y,
                            @Param("activeTime") LocalDateTime activeTime);

    @Modifying
    @Query("UPDATE CanvasCollaborator cc SET cc.isOnline = :isOnline, cc.sessionId = :sessionId, " +
           "cc.lastActiveAt = :activeTime WHERE cc.id = :collaboratorId")
    void updateOnlineStatus(@Param("collaboratorId") UUID collaboratorId,
                          @Param("isOnline") Boolean isOnline,
                          @Param("sessionId") String sessionId,
                          @Param("activeTime") LocalDateTime activeTime);

    @Modifying
    @Query("UPDATE CanvasCollaborator cc SET cc.permission = :permission WHERE cc.id = :collaboratorId")
    void updatePermission(@Param("collaboratorId") UUID collaboratorId, 
                        @Param("permission") CanvasCollaborator.Permission permission);

    // Statistics
    @Query("SELECT COUNT(cc) FROM CanvasCollaborator cc WHERE cc.canvasBoard.id = :canvasBoardId AND " +
           "cc.status = 'ACTIVE' AND cc.isActive = true")
    Long countActiveCollaborators(@Param("canvasBoardId") UUID canvasBoardId);

    @Query("SELECT COUNT(cc) FROM CanvasCollaborator cc WHERE cc.canvasBoard.id = :canvasBoardId AND " +
           "cc.isOnline = true AND cc.isActive = true")
    Long countOnlineCollaborators(@Param("canvasBoardId") UUID canvasBoardId);

    // Recently active collaborators
    @Query("SELECT cc FROM CanvasCollaborator cc WHERE cc.canvasBoard.id = :canvasBoardId AND " +
           "cc.status = 'ACTIVE' AND cc.isActive = true ORDER BY cc.lastActiveAt DESC")
    List<CanvasCollaborator> findRecentlyActiveCollaborators(@Param("canvasBoardId") UUID canvasBoardId,
                                                           org.springframework.data.domain.Pageable pageable);

    // Find collaborators with specific permissions
    @Query("SELECT cc FROM CanvasCollaborator cc WHERE cc.canvasBoard.id = :canvasBoardId AND " +
           "cc.permission IN :permissions AND cc.status = 'ACTIVE' AND cc.isActive = true")
    List<CanvasCollaborator> findCollaboratorsWithPermissions(@Param("canvasBoardId") UUID canvasBoardId,
                                                            @Param("permissions") List<CanvasCollaborator.Permission> permissions);

    // Find pending invitations
    @Query("SELECT cc FROM CanvasCollaborator cc WHERE cc.canvasBoard.id = :canvasBoardId AND " +
           "cc.status = 'PENDING' AND cc.isActive = true ORDER BY cc.invitationSentAt DESC")
    List<CanvasCollaborator> findPendingInvitations(@Param("canvasBoardId") UUID canvasBoardId);

    @Query("SELECT cc FROM CanvasCollaborator cc WHERE cc.user.id = :userId AND " +
           "cc.status = 'PENDING' AND cc.isActive = true ORDER BY cc.invitationSentAt DESC")
    List<CanvasCollaborator> findPendingInvitationsForUser(@Param("userId") UUID userId);

    // Workspace-level collaborator queries
    @Query("SELECT DISTINCT cc.user FROM CanvasCollaborator cc WHERE cc.canvasBoard.workspace.id = :workspaceId AND " +
           "cc.status = 'ACTIVE' AND cc.isActive = true")
    List<com.yusufkurnaz.ProjectManagementBackend.Common.Model.User> findWorkspaceCollaborators(@Param("workspaceId") UUID workspaceId);

    // Cleanup operations
    @Modifying
    @Query("UPDATE CanvasCollaborator cc SET cc.isOnline = false, cc.sessionId = null WHERE cc.sessionId = :sessionId")
    void disconnectSession(@Param("sessionId") String sessionId);

    @Modifying
    @Query("UPDATE CanvasCollaborator cc SET cc.isOnline = false, cc.sessionId = null WHERE " +
           "cc.lastActiveAt < :cutoffTime AND cc.isOnline = true")
    void disconnectInactiveCollaborators(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Modifying
    @Query("UPDATE CanvasCollaborator cc SET cc.status = 'INACTIVE' WHERE cc.canvasBoard.id = :canvasBoardId")
    void deactivateAllCollaborators(@Param("canvasBoardId") UUID canvasBoardId);

    // Advanced queries
    @Query("SELECT cc FROM CanvasCollaborator cc WHERE cc.canvasBoard.workspace.id = :workspaceId AND " +
           "cc.user.id = :userId AND cc.status = 'ACTIVE' AND cc.isActive = true")
    List<CanvasCollaborator> findUserCollaborationsInWorkspace(@Param("workspaceId") UUID workspaceId,
                                                             @Param("userId") UUID userId);
}
