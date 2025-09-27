# 🎨 IDEAWORKSPACE MODULE - SENARYO BAZLI VERİ AKIŞ DOKÜMANTASYONU

## 📁 **KLASÖR YAPISI**
```
IdeaWorkspace/
├── Controller/     ← Canvas REST endpoints
├── Service/        ← Canvas business logic
├── Repository/     ← Canvas data access
├── Entity/         ← Canvas entities
└── Dto/           ← Canvas DTOs
    ├── request/    ← Canvas request DTOs
    └── response/   ← Canvas response DTOs
```

---

## 🎯 **SENARYO 1: CANVAS CREATION - "Project Architecture" Canvas Oluşturma**

### **📥 Frontend Request**
```json
POST /api/v1/canvas
{
  "name": "Project Architecture",
  "description": "System design and component relationships",
  "workspaceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → CanvasController → CanvasServiceImpl → WorkspaceRepository → PostgreSQL → WorkspaceRepository
    ↓           ↓                  ↓                    ↓                   ↓              ↓
CreateReq   @PostMapping       createCanvas()       findById()         SELECT query    Workspace entity
    ↓           ↓                  ↓                    ↓                   ↓              ↓
JSON Body   @Valid check       validation           JPA query          workspace table  Optional<Workspace>
    ↓           ↓                  ↓                    ↓                   ↓              ↓
Request DTO userId extract     parameter check      database hit       row data        entity mapping

CanvasServiceImpl → UserRepository → CanvasBoard Entity → CanvasBoardRepository → Database
       ↓                ↓                  ↓                    ↓                    ↓
   findById()      User entity        Builder.build()       save()              INSERT
       ↓                ↓                  ↓                    ↓                    ↓
   user lookup     user validation    canvas creation      JPA persist         canvas_boards
       ↓                ↓                  ↓                    ↓                    ↓
   database hit    authorization      default Excalidraw   transaction         row created

CanvasBoard → CanvasCollaborator → CollaboratorRepository → Database → CanvasServiceImpl → Frontend
     ↓              ↓                      ↓                   ↓             ↓              ↓
saved canvas   creator collaborator    save()              INSERT        return canvas   JSON Response
     ↓              ↓                      ↓                   ↓             ↓              ↓
entity with ID  ADMIN permission       JPA persist         collaborator   CanvasResponse  HTTP 201
     ↓              ↓                      ↓                   ↓             ↓              ↓
UUID generated  ACTIVE status          transaction         relationship   response DTO    client update
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Controller/CanvasController.java**
```java
@PostMapping("/")
↓ @Valid CreateCanvasRequest validation
↓ Authentication.getName() → UUID userId
↓ canvasService.createCanvas(request.name, request.description, request.workspaceId, userId)
↓ CanvasResponse.fromEntity(savedCanvas)
↓ ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
```

**2️⃣ Service/impl/CanvasServiceImpl.java**
```java
createCanvas() method
↓ log.info("Creating canvas '{}' in workspace {} by user {}", name, workspaceId, userId)
↓ Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow(...)
↓ User user = userRepository.findById(userId).orElseThrow(...)
↓ CanvasBoard canvas = CanvasBoard.builder()
    .name(name).description(description).workspace(workspace)
    .canvasData("{\"type\":\"excalidraw\",\"version\":2,\"elements\":[],\"files\":{}}")
    .createdBy(userId).updatedBy(userId).build()
↓ CanvasBoard savedCanvas = canvasBoardRepository.save(canvas)
↓ CanvasCollaborator creatorCollaborator = CanvasCollaborator.builder()
    .canvasBoard(savedCanvas).user(user)
    .permission(Permission.ADMIN).status(CollaboratorStatus.ACTIVE).build()
↓ collaboratorRepository.save(creatorCollaborator)
↓ return savedCanvas
```

---

## 🎯 **SENARYO 2: CANVAS DATA UPDATE - Real-time Excalidraw Güncelleme**

### **📥 Frontend Request**
```json
PUT /api/v1/canvas/550e8400-e29b-41d4-a716-446655440000/data
{
  "canvasData": "{\"type\":\"excalidraw\",\"version\":2,\"elements\":[{\"type\":\"rectangle\",\"x\":100,\"y\":100,\"width\":200,\"height\":150}],\"files\":{}}",
  "version": 5
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → CanvasController → CanvasServiceImpl → CanvasBoardRepository → PostgreSQL
    ↓           ↓                  ↓                    ↓                     ↓
UpdateReq   @PutMapping        updateCanvasData()   findById()            SELECT query
    ↓           ↓                  ↓                    ↓                     ↓
JSON Body   path variable      validation           JPA query             canvas_boards
    ↓           ↓                  ↓                    ↓                     ↓
Excalidraw  canvasId extract   access check         database hit          row data

CanvasServiceImpl → Optimistic Locking → CanvasBoard Entity → CanvasBoardRepository → Database
       ↓                    ↓                   ↓                    ↓                   ↓
validateCanvasEditAccess() version check     setCanvasData()      save()             UPDATE
       ↓                    ↓                   ↓                    ↓                   ↓
permission check          concurrent control  data update         JPA persist        row updated
       ↓                    ↓                   ↓                    ↓                   ↓
EDITOR/ADMIN required     version mismatch    JSON validation     transaction        version++

Database → CanvasBoard → CanvasServiceImpl → CanvasController → Frontend
    ↓           ↓              ↓                  ↓               ↓
UPDATE result  updated entity  return canvas    response wrap   JSON Response
    ↓           ↓              ↓                  ↓               ↓
success/fail   new version    canvas data      CanvasResponse  HTTP 200
    ↓           ↓              ↓                  ↓               ↓
optimistic ok  lastAccessed   updated content  success flag    client sync
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/CanvasServiceImpl.java**
```java
updateCanvasData() method
↓ log.debug("Updating canvas data for canvas {} by user {}", canvasId, userId)
↓ validateCanvasEditAccess(canvasId, userId) → permission check
↓ CanvasBoard canvas = canvasBoardRepository.findById(canvasId).orElseThrow(...)
↓ if (!canvas.getVersion().equals(version)) throw new RuntimeException("Concurrent modification")
↓ if (!isValidCanvasData(canvasData)) throw new RuntimeException("Invalid data format")
↓ canvas.setCanvasData(canvasData)
↓ canvas.setUpdatedBy(userId)
↓ canvas.updateLastAccessed() → LocalDateTime.now()
↓ return canvasBoardRepository.save(canvas)
```

**2️⃣ Validation Logic**
```java
validateCanvasEditAccess() method
↓ CanvasBoard canvas = canvasBoardRepository.findById(canvasId).orElseThrow(...)
↓ if (canvas.getCreatedBy().equals(userId)) return // Creator access
↓ Permission permission = collaboratorRepository.getUserPermission(canvasId, userId).orElseThrow(...)
↓ if (!Permission.EDITOR.equals(permission) && !Permission.ADMIN.equals(permission))
    throw new RuntimeException("Edit permission required")
```

---

## 🎯 **SENARYO 3: CANVAS FILE UPLOAD - Image/Document Ekleme**

### **📥 Frontend Request**
```javascript
POST /api/v1/canvas/550e8400-e29b-41d4-a716-446655440000/files
FormData: {
  file: architecture_diagram.png (1.2MB)
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → CanvasFileController → CanvasFileServiceImpl → CanvasServiceImpl → CanvasBoardRepository
    ↓            ↓                      ↓                      ↓                    ↓
Multipart    @PostMapping           uploadFile()          validateCanvasAccess()  findByIdWithAccess()
Request      /{canvasId}/files      file validation       permission check       JPA query
    ↓            ↓                      ↓                      ↓                    ↓
Binary Data  path extraction        size/type check       access control         database hit

File System ← CanvasFileServiceImpl ← File Hash ← CanvasFileRepository ← Duplicate Check
    ↓                  ↓                ↓               ↓                    ↓
Physical Storage   saveFileToDisk()  SHA-256         findByFileHash()     SELECT query
    ↓                  ↓                ↓               ↓                    ↓
/uploads/canvas/   UUID filename     hash calc       duplicate lookup     existing file?

CanvasFile Entity → CanvasFileRepository → Database → CanvasFileServiceImpl → Frontend
       ↓                   ↓                  ↓              ↓                   ↓
Builder.build()         save()            INSERT          return entity      JSON Response
       ↓                   ↓                  ↓              ↓                   ↓
metadata set           JPA persist       canvas_files     CanvasFileResponse  HTTP 201
       ↓                   ↓                  ↓              ↓                   ↓
URL generation         transaction       row created      response DTO        client update
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/CanvasFileServiceImpl.java**
```java
uploadFile() method
↓ log.info("Uploading file '{}' to canvas {} by user {}", filename, canvasBoardId, userId)
↓ validateCanvasAccess(canvasBoardId, userId) → permission check
↓ validateFile(file) → size, type, content validation
↓ String fileHash = calculateFileHash(file) → SHA-256
↓ Optional<CanvasFile> existing = canvasFileRepository.findByFileHashAndIsActiveTrue(fileHash)
↓ if (existing.isPresent()) return existing.get() // Deduplication
↓ String storedFilename = generateUniqueFilename(file.getOriginalFilename()) → UUID + extension
↓ Path filePath = saveFileToDisk(file, storedFilename) → physical storage
↓ CanvasFile canvasFile = CanvasFile.builder()
    .canvasBoard(canvas).originalFilename().storedFilename().filePath()
    .fileUrl(generateFileUrl()).mimeType().fileSize().fileHash()
    .status(FileStatus.UPLOADED).build()
↓ if (isImageFile()) setImageDimensions(canvasFile, file)
↓ return canvasFileRepository.save(canvasFile)
```

**2️⃣ File Processing**
```java
saveFileToDisk() method
↓ Path uploadPath = Paths.get(uploadDir) → /uploads/canvas/
↓ if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath)
↓ Path filePath = uploadPath.resolve(filename)
↓ Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING)
↓ return filePath
```

---

## 🎯 **SENARYO 4: CANVAS COLLABORATION - Real-time İşbirliği**

### **📥 Frontend Request**
```json
POST /api/v1/canvas/550e8400-e29b-41d4-a716-446655440000/collaborators
{
  "email": "colleague@example.com",
  "permission": "EDITOR"
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → CanvasCollaboratorController → CanvasCollaboratorServiceImpl → UserRepository → PostgreSQL
    ↓                ↓                            ↓                           ↓               ↓
AddCollabReq    @PostMapping                addCollaborator()            findByEmail()    SELECT query
    ↓                ↓                            ↓                           ↓               ↓
JSON Body       path variable               email validation            JPA query        users table
    ↓                ↓                            ↓                           ↓               ↓
email/permission canvasId extract           user lookup                 database hit     user data

CanvasCollaboratorServiceImpl → CanvasBoardRepository → CanvasCollaborator Entity → CollaboratorRepository
           ↓                           ↓                          ↓                         ↓
    validateCanvasAccess()         findById()                Builder.build()            save()
           ↓                           ↓                          ↓                         ↓
    permission check               canvas lookup              collaborator creation       JPA persist
           ↓                           ↓                          ↓                         ↓
    ADMIN required                 entity validation          permission set             transaction

Database → Email Notification → WebSocket Event → Frontend Clients
    ↓              ↓                    ↓               ↓
INSERT result   async email         real-time update  collaboration UI
    ↓              ↓                    ↓               ↓
row created     invitation sent     WebSocket push    user notification
    ↓              ↓                    ↓               ↓
relationship    email service       connected clients  UI refresh
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/CanvasCollaboratorServiceImpl.java**
```java
addCollaborator() method
↓ log.info("Adding collaborator {} to canvas {} with permission {}", email, canvasId, permission)
↓ validateCanvasAccess(canvasId, userId) → ADMIN permission required
↓ User user = userRepository.findByEmailAndIsActiveTrue(email).orElseThrow(...)
↓ Optional<CanvasCollaborator> existing = collaboratorRepository.findByCanvasBoardIdAndUserId(canvasId, user.getId())
↓ if (existing.isPresent()) throw new RuntimeException("User already collaborator")
↓ CanvasCollaborator collaborator = CanvasCollaborator.builder()
    .canvasBoard(canvas).user(user).permission(Permission.valueOf(permission))
    .status(CollaboratorStatus.PENDING).joinedAt(LocalDateTime.now()).build()
↓ CanvasCollaborator saved = collaboratorRepository.save(collaborator)
↓ emailService.sendCollaborationInvitation(user.getEmail(), canvas.getName()) → async
↓ webSocketService.notifyCanvasUpdate(canvasId, "COLLABORATOR_ADDED") → real-time
↓ return saved
```

---

## 🎯 **SENARYO 5: CANVAS SEARCH & FILTERING - Canvas Arama**

### **📥 Frontend Request**
```json
GET /api/v1/canvas/search?q=architecture&workspace=550e8400-e29b-41d4-a716-446655440000&tags=design,system
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → CanvasController → CanvasServiceImpl → CanvasBoardRepository → PostgreSQL
    ↓           ↓                  ↓                    ↓                     ↓
GET params  @GetMapping        searchCanvases()     searchByNameInWorkspace() LIKE query
    ↓           ↓                  ↓                    ↓                     ↓
query params parameter extract  search logic        JPA query             canvas_boards
    ↓           ↓                  ↓                    ↓                     ↓
q, workspace, tags validation    filter building     SQL generation        WHERE clauses

PostgreSQL → CanvasBoardRepository → CanvasServiceImpl → CanvasController → Frontend
    ↓              ↓                      ↓                  ↓               ↓
LIKE + AND     List<CanvasBoard>      filter results     response build   JSON Response
    ↓              ↓                      ↓                  ↓               ↓
text search    entity mapping         access control     CanvasResponse[] HTTP 200
    ↓              ↓                      ↓                  ↓               ↓
tag filtering  JPA results           user permissions    pagination       search results
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Repository/CanvasBoardRepository.java**
```java
@Query("SELECT c FROM CanvasBoard c WHERE " +
       "c.workspace.id = :workspaceId AND " +
       "c.isActive = true AND " +
       "(LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
       "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
↓ JPA query execution
↓ PostgreSQL: SELECT * FROM canvas_boards WHERE workspace_id = ? AND is_active = true 
   AND (LOWER(name) LIKE LOWER(?) OR LOWER(description) LIKE LOWER(?))
↓ ResultSet mapping to CanvasBoard entities
↓ List<CanvasBoard> return
```

Bu IdeaWorkspace modülü, kullanıcıların görsel tasarım ve işbirliği yapabilmesini sağlayan tam özellikli bir canvas yönetim sistemidir.
