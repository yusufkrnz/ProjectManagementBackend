package com.yusufkurnaz.ProjectManagementBackend.Common.Controller;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.User;
import com.yusufkurnaz.ProjectManagementBackend.Common.Repository.UserRepository;
import com.yusufkurnaz.ProjectManagementBackend.Workspace.Entity.Workspace;
import com.yusufkurnaz.ProjectManagementBackend.Workspace.Repository.WorkspaceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Test Controller", description = "Database connection and basic operations test")
public class TestController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the application is running")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Application is running!");
    }

    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Retrieve all users from database")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/workspaces")
    @Operation(summary = "Get all workspaces", description = "Retrieve all workspaces from database")
    public ResponseEntity<List<Workspace>> getAllWorkspaces() {
        List<Workspace> workspaces = workspaceRepository.findAll();
        return ResponseEntity.ok(workspaces);
    }

    @PostMapping("/users")
    @Operation(summary = "Create test user", description = "Create a test user to verify database connection")
    public ResponseEntity<User> createTestUser(@RequestBody User user) {
        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(savedUser);
    }

    @PostMapping("/workspaces")
    @Operation(summary = "Create test workspace", description = "Create a test workspace to verify database connection")
    public ResponseEntity<Workspace> createTestWorkspace(@RequestBody Workspace workspace) {
        Workspace savedWorkspace = workspaceRepository.save(workspace);
        return ResponseEntity.ok(savedWorkspace);
    }
}
