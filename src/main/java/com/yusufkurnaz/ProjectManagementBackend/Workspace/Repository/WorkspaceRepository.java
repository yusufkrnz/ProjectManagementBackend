package com.yusufkurnaz.ProjectManagementBackend.Workspace.Repository;

import com.yusufkurnaz.ProjectManagementBackend.Workspace.Entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    
    List<Workspace> findByStatus(Workspace.WorkspaceStatus status);
    
    List<Workspace> findByNameContainingIgnoreCase(String name);
}
