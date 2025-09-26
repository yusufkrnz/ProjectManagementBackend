package com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Controller;

import com.yusufkurnaz.ProjectManagementBackend.Common.Dto.ApiResponse;
import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Dto.request.CanvasCreateRequest;
import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Dto.request.CanvasDataUpdateRequest;
import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Dto.request.CanvasUpdateRequest;
import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Dto.response.CanvasDataResponse;
import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Dto.response.CanvasResponse;
import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Entity.CanvasBoard;
import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Service.CanvasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Canvas operations
 * Follows project conventions and SOLID principles
 */
@RestController
@RequestMapping("/api/v1/canvas")
@Tag(name = "Canvas Management", description = "Canvas board operations for IdeaWorkspace")
@RequiredArgsConstructor
@Slf4j
public class CanvasController {

    private final CanvasService canvasService;

    @PostMapping
    @Operation(summary = "Create new canvas", description = "Create a new canvas board in workspace")
    public ResponseEntity<ApiResponse<CanvasResponse>> createCanvas(
            @Valid @RequestBody CanvasCreateRequest request,
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId) {

        log.info("Creating canvas '{}' for user {}", request.getName(), userId);

        CanvasBoard canvas;
        if (request.getTemplateId() != null) {
            canvas = canvasService.createFromTemplate(request.getTemplateId(), 
                    request.getName(), request.getWorkspaceId(), userId);
        } else {
            canvas = canvasService.createCanvas(request.getName(), 
                    request.getDescription(), request.getWorkspaceId(), userId);
        }

        CanvasResponse response = CanvasResponse.fromEntity(canvas);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Canvas created successfully"));
    }

    @GetMapping("/{canvasId}")
    @Operation(summary = "Get canvas by ID", description = "Retrieve canvas board details")
    public ResponseEntity<ApiResponse<CanvasResponse>> getCanvas(
            @PathVariable UUID canvasId,
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId) {

        log.debug("Getting canvas {} for user {}", canvasId, userId);

        CanvasBoard canvas = canvasService.getCanvas(canvasId, userId);
        CanvasResponse response = CanvasResponse.fromEntity(canvas);

        return ResponseEntity.ok(
                ApiResponse.success(response, "Canvas retrieved successfully"));
    }

    @PutMapping("/{canvasId}")
    @Operation(summary = "Update canvas", description = "Update canvas board metadata")
    public ResponseEntity<ApiResponse<CanvasResponse>> updateCanvas(
            @PathVariable UUID canvasId,
            @Valid @RequestBody CanvasUpdateRequest request,
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId) {

        log.info("Updating canvas {} for user {}", canvasId, userId);

        CanvasBoard canvas = canvasService.updateCanvas(canvasId, 
                request.getName(), request.getDescription(), userId);

        // Update canvas settings if provided
        if (request.getCanvasWidth() != null || request.getCanvasHeight() != null ||
            request.getBackgroundColor() != null || request.getGridEnabled() != null) {
            canvas = canvasService.updateCanvasSettings(canvasId, 
                    request.getCanvasWidth(), request.getCanvasHeight(),
                    request.getBackgroundColor(), request.getGridEnabled(), userId);
        }

        // Update collaboration settings if provided
        if (request.getCollaborationEnabled() != null || request.getMaxCollaborators() != null) {
            canvas = canvasService.updateCollaborationSettings(canvasId,
                    request.getCollaborationEnabled(), request.getMaxCollaborators(), userId);
        }

        // Handle tags
        if (request.getAddTags() != null) {
            for (String tag : request.getAddTags()) {
                canvas = canvasService.addTag(canvasId, tag, userId);
            }
        }
        if (request.getRemoveTags() != null) {
            for (String tag : request.getRemoveTags()) {
                canvas = canvasService.removeTag(canvasId, tag, userId);
            }
        }

        // Handle visibility changes
        if (request.getIsPublic() != null) {
            if (request.getIsPublic()) {
                canvasService.makeCanvasPublic(canvasId, userId);
            } else {
                canvasService.makeCanvasPrivate(canvasId, userId);
            }
        }

        CanvasResponse response = CanvasResponse.fromEntity(canvas);
        
        return ResponseEntity.ok(
                ApiResponse.success(response, "Canvas updated successfully"));
    }

    @DeleteMapping("/{canvasId}")
    @Operation(summary = "Delete canvas", description = "Delete canvas board (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteCanvas(
            @PathVariable UUID canvasId,
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId) {

        log.info("Deleting canvas {} for user {}", canvasId, userId);

        canvasService.deleteCanvas(canvasId, userId);

        return ResponseEntity.ok(
                ApiResponse.success("Canvas deleted successfully"));
    }

    @GetMapping("/{canvasId}/data")
    @Operation(summary = "Get canvas data", description = "Retrieve canvas Excalidraw data")
    public ResponseEntity<ApiResponse<CanvasDataResponse>> getCanvasData(
            @PathVariable UUID canvasId,
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId) {

        log.debug("Getting canvas data for canvas {} by user {}", canvasId, userId);

        String canvasData = canvasService.getCanvasData(canvasId, userId);
        CanvasBoard canvas = canvasService.getCanvas(canvasId, userId);

        CanvasDataResponse response = CanvasDataResponse.builder()
                .canvasId(canvasId)
                .canvasData(canvasData)
                .version(canvas.getVersion())
                .lastUpdated(canvas.getUpdatedAt())
                .lastUpdatedBy(canvas.getUpdatedBy())
                .canvasName(canvas.getName())
                .collaborationEnabled(canvas.getCollaborationEnabled())
                .canEdit(canvasService.canUserEditCanvas(canvasId, userId))
                .build();

        return ResponseEntity.ok(
                ApiResponse.success(response, "Canvas data retrieved successfully"));
    }

    @PutMapping("/{canvasId}/data")
    @Operation(summary = "Update canvas data", description = "Update canvas Excalidraw data with optimistic locking")
    public ResponseEntity<ApiResponse<CanvasDataResponse>> updateCanvasData(
            @PathVariable UUID canvasId,
            @Valid @RequestBody CanvasDataUpdateRequest request,
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId) {

        log.debug("Updating canvas data for canvas {} by user {}", canvasId, userId);

        CanvasBoard canvas = canvasService.updateCanvasData(canvasId, 
                request.getCanvasData(), request.getVersion(), userId);

        CanvasDataResponse response = CanvasDataResponse.builder()
                .canvasId(canvasId)
                .canvasData(canvas.getCanvasData())
                .version(canvas.getVersion())
                .lastUpdated(canvas.getUpdatedAt())
                .lastUpdatedBy(canvas.getUpdatedBy())
                .canvasName(canvas.getName())
                .collaborationEnabled(canvas.getCollaborationEnabled())
                .canEdit(true)
                .build();

        return ResponseEntity.ok(
                ApiResponse.success(response, "Canvas data updated successfully"));
    }

    @PostMapping("/{canvasId}/fork")
    @Operation(summary = "Fork canvas", description = "Create a copy of existing canvas")
    public ResponseEntity<ApiResponse<CanvasResponse>> forkCanvas(
            @PathVariable UUID canvasId,
            @RequestParam String newName,
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId) {

        log.info("Forking canvas {} to '{}' by user {}", canvasId, newName, userId);

        CanvasBoard forkedCanvas = canvasService.forkCanvas(canvasId, newName, userId);
        CanvasResponse response = CanvasResponse.fromEntity(forkedCanvas);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Canvas forked successfully"));
    }

    @GetMapping("/user")
    @Operation(summary = "Get user canvases", description = "Retrieve canvases created by user")
    public ResponseEntity<ApiResponse<Page<CanvasResponse>>> getUserCanvases(
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CanvasBoard> canvasPage = canvasService.getUserCanvases(userId, pageable);
        Page<CanvasResponse> responsePage = canvasPage.map(CanvasResponse::fromEntity);

        return ResponseEntity.ok(
                ApiResponse.success(responsePage, "User canvases retrieved successfully"));
    }

    @GetMapping("/workspace/{workspaceId}")
    @Operation(summary = "Get workspace canvases", description = "Retrieve canvases in workspace")
    public ResponseEntity<ApiResponse<Page<CanvasResponse>>> getWorkspaceCanvases(
            @PathVariable UUID workspaceId,
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CanvasBoard> canvasPage = canvasService.getWorkspaceCanvases(workspaceId, userId, pageable);
        Page<CanvasResponse> responsePage = canvasPage.map(CanvasResponse::fromEntity);

        return ResponseEntity.ok(
                ApiResponse.success(responsePage, "Workspace canvases retrieved successfully"));
    }

    @GetMapping("/public")
    @Operation(summary = "Get public canvases", description = "Retrieve public canvases")
    public ResponseEntity<ApiResponse<Page<CanvasResponse>>> getPublicCanvases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "viewCount") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CanvasBoard> canvasPage = canvasService.getPublicCanvases(pageable);
        Page<CanvasResponse> responsePage = canvasPage.map(CanvasResponse::fromEntity);

        return ResponseEntity.ok(
                ApiResponse.success(responsePage, "Public canvases retrieved successfully"));
    }

    @GetMapping("/templates")
    @Operation(summary = "Get canvas templates", description = "Retrieve available canvas templates")
    public ResponseEntity<ApiResponse<List<CanvasResponse>>> getTemplates() {

        List<CanvasBoard> templates = canvasService.getTemplates();
        List<CanvasResponse> responses = templates.stream()
                .map(CanvasResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(
                ApiResponse.success(responses, "Templates retrieved successfully"));
    }

    @PostMapping("/{canvasId}/template")
    @Operation(summary = "Create template", description = "Create template from canvas")
    public ResponseEntity<ApiResponse<CanvasResponse>> createTemplate(
            @PathVariable UUID canvasId,
            @RequestParam String templateName,
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId) {

        log.info("Creating template from canvas {} by user {}", canvasId, userId);

        CanvasBoard template = canvasService.createTemplate(canvasId, templateName, userId);
        CanvasResponse response = CanvasResponse.fromEntity(template);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Template created successfully"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search canvases", description = "Search canvases by name or description")
    public ResponseEntity<ApiResponse<List<CanvasResponse>>> searchCanvases(
            @RequestParam String query,
            @RequestParam(required = false) UUID workspaceId,
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId) {

        List<CanvasBoard> canvases = canvasService.searchCanvases(query, workspaceId, userId);
        List<CanvasResponse> responses = canvases.stream()
                .map(CanvasResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(
                ApiResponse.success(responses, "Search completed successfully"));
    }

    @PostMapping("/{canvasId}/view")
    @Operation(summary = "Increment view count", description = "Track canvas view")
    public ResponseEntity<ApiResponse<Void>> incrementViewCount(
            @PathVariable UUID canvasId) {

        canvasService.incrementViewCount(canvasId);

        return ResponseEntity.ok(
                ApiResponse.success("View count updated"));
    }
}
