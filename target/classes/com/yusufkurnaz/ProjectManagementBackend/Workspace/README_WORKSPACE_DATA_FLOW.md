# 🏢 WORKSPACE MODULE - SENARYO BAZLI VERİ AKIŞ DOKÜMANTASYONU

## 📁 **KLASÖR YAPISU**
```
Workspace/
├── Repository/     ← Workspace data access
├── Entity/         ← Workspace entities
├── Service/        ← Workspace business logic
├── Dto/           ← Workspace DTOs
└── Exception/     ← Workspace exceptions
```

---

## 🎯 **SENARYO 1: CREATE WORKSPACE - Yeni Çalışma Alanı Oluşturma**

### **📥 Frontend Request**
```json
POST /api/v1/workspaces
{
  "name": "Acme Corp Development",
  "description": "Main development workspace for all projects",
  "organizationId": "550e8400-e29b-41d4-a716-446655440000",
  "settings": {
    "isPrivate": false,
    "maxMembers": 50,
    "allowGuestAccess": true
  }
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → WorkspaceController → WorkspaceServiceImpl → OrganizationRepository → PostgreSQL
    ↓              ↓                     ↓                      ↓                  ↓
CreateWorkspaceReq @PostMapping      createWorkspace()      findById()         SELECT query
    ↓              ↓                     ↓                      ↓                  ↓
JSON Body      @Valid check          validation             org check          organizations
    ↓              ↓                     ↓                      ↓                  ↓
workspace data userId extract        permission check       entity fetch       row data

WorkspaceServiceImpl → Workspace Entity → WorkspaceRepository → Database → WorkspaceMember
       ↓                    ↓                   ↓                  ↓              ↓
   Builder.build()      workspace creation    save()            INSERT        creator member
       ↓                    ↓                   ↓                  ↓              ↓
   metadata set         entity instance      JPA persist       workspaces     relationship
       ↓                    ↓                   ↓                  ↓              ↓
   settings config      validation           transaction       row created    workspace_members

Database → WorkspaceMemberRepository → WorkspaceController → Frontend
    ↓              ↓                         ↓                   ↓
member added   save member                response build      JSON Response
    ↓              ↓                         ↓                   ↓
relationship   JPA persist                WorkspaceResponse   HTTP 201
    ↓              ↓                         ↓                   ↓
OWNER role     transaction                success format      client update
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/WorkspaceServiceImpl.java**
```java
createWorkspace() method
↓ log.info("Creating workspace '{}' for organization {} by user {}", name, organizationId, creatorId)
↓ Organization organization = organizationRepository.findById(organizationId).orElseThrow(...)
↓ validateWorkspaceCreationPermissions(creatorId, organization)
↓ Workspace workspace = Workspace.builder()
    .name(request.name).description(request.description).organization(organization)
    .isPrivate(request.settings.isPrivate).maxMembers(request.settings.maxMembers)
    .allowGuestAccess(request.settings.allowGuestAccess)
    .createdBy(creatorId).status(WorkspaceStatus.ACTIVE).build()
↓ Workspace savedWorkspace = workspaceRepository.save(workspace)
↓ WorkspaceMember creatorMember = WorkspaceMember.builder()
    .workspace(savedWorkspace).user(creator).role(WorkspaceRole.OWNER)
    .joinedAt(LocalDateTime.now()).status(MemberStatus.ACTIVE).build()
↓ workspaceMemberRepository.save(creatorMember)
↓ activityService.logWorkspaceActivity(savedWorkspace.getId(), "WORKSPACE_CREATED", creatorId)
↓ return savedWorkspace
```

---

## 🎯 **SENARYO 2: WORKSPACE DASHBOARD - Çalışma Alanı Özeti**

### **📥 Frontend Request**
```json
GET /api/v1/workspaces/550e8400-e29b-41d4-a716-446655440009/dashboard
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → WorkspaceController → WorkspaceServiceImpl → Multiple Repositories → PostgreSQL
    ↓              ↓                     ↓                      ↓                   ↓
GET request    @GetMapping          getWorkspaceDashboard()   parallel queries    MULTIPLE SELECT
    ↓              ↓                     ↓                      ↓                   ↓
workspace ID   path variable        dashboard build          repository calls    different tables
    ↓              ↓                     ↓                      ↓                   ↓
UUID param     validation           data aggregation         JPA queries         join operations

ProjectRepository → TeamRepository → TaskRepository → WorkspaceMemberRepository
       ↓                 ↓               ↓                    ↓
project statistics   team statistics  task statistics     member statistics
       ↓                 ↓               ↓                    ↓
project count        team count       task distribution    member roles
       ↓                 ↓               ↓                    ↓
active projects      active teams     completion rates     activity levels

All Repositories → WorkspaceServiceImpl → WorkspaceDashboard → WorkspaceController → Frontend
       ↓                    ↓                      ↓                    ↓                ↓
aggregated data         dashboard build         DTO creation        response build   JSON Response
       ↓                    ↓                      ↓                    ↓                ↓
workspace metrics       data processing        dashboard object     success format   HTTP 200
       ↓                    ↓                      ↓                    ↓                ↓
KPI calculations        business logic         complete data        ApiResponse      client display
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/WorkspaceServiceImpl.java**
```java
getWorkspaceDashboard() method
↓ log.debug("Building dashboard for workspace: {}", workspaceId)
↓ Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow(...)
↓ validateWorkspaceAccess(userId, workspace)

// Parallel data fetching for performance
↓ CompletableFuture<ProjectStatistics> projectStats = CompletableFuture.supplyAsync(() -> 
    projectRepository.getWorkspaceProjectStatistics(workspaceId))
↓ CompletableFuture<TeamStatistics> teamStats = CompletableFuture.supplyAsync(() -> 
    teamRepository.getWorkspaceTeamStatistics(workspaceId))
↓ CompletableFuture<TaskStatistics> taskStats = CompletableFuture.supplyAsync(() -> 
    taskRepository.getWorkspaceTaskStatistics(workspaceId))
↓ CompletableFuture<MemberStatistics> memberStats = CompletableFuture.supplyAsync(() -> 
    workspaceMemberRepository.getWorkspaceMemberStatistics(workspaceId))
↓ CompletableFuture<List<RecentActivity>> activities = CompletableFuture.supplyAsync(() -> 
    activityRepository.getRecentWorkspaceActivities(workspaceId, 15))

↓ CompletableFuture.allOf(projectStats, teamStats, taskStats, memberStats, activities).join()

↓ WorkspaceDashboard dashboard = WorkspaceDashboard.builder()
    .workspace(WorkspaceSummary.fromEntity(workspace))
    .projectStatistics(projectStats.get()).teamStatistics(teamStats.get())
    .taskStatistics(taskStats.get()).memberStatistics(memberStats.get())
    .recentActivities(activities.get())
    .healthScore(calculateWorkspaceHealth(projectStats.get(), taskStats.get()))
    .build()
↓ return dashboard
```

---

## 🎯 **SENARYO 3: WORKSPACE MEMBER MANAGEMENT - Üye Yönetimi**

### **📥 Frontend Request**
```json
POST /api/v1/workspaces/550e8400-e29b-41d4-a716-446655440009/members
{
  "userEmail": "manager@example.com",
  "role": "ADMIN",
  "permissions": ["MANAGE_PROJECTS", "MANAGE_TEAMS", "INVITE_MEMBERS"]
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → WorkspaceController → WorkspaceServiceImpl → UserRepository → PostgreSQL
    ↓              ↓                     ↓                   ↓               ↓
AddMemberReq   @PostMapping          addWorkspaceMember()  findByEmail()   SELECT query
    ↓              ↓                     ↓                   ↓               ↓
JSON Body      path variable         member validation     user lookup     users table
    ↓              ↓                     ↓                   ↓               ↓
user email     workspaceId extract   permission check     entity fetch    row data

WorkspaceServiceImpl → WorkspaceRepository → WorkspaceMember Entity → WorkspaceMemberRepository
       ↓                      ↓                      ↓                         ↓
validatePermissions()     findById()            Builder.build()            save()
       ↓                      ↓                      ↓                         ↓
role check               workspace lookup       member creation            JPA persist
       ↓                      ↓                      ↓                         ↓
ADMIN/OWNER required     entity fetch          role assignment            transaction

Database → NotificationService → ActivityService → WorkspaceController → Frontend
    ↓              ↓                    ↓                    ↓                ↓
member added   workspace invitation  activity log        response build   JSON Response
    ↓              ↓                    ↓                    ↓                ↓
relationship   async notification    audit trail         MemberResponse   HTTP 201
    ↓              ↓                    ↓                    ↓                ↓
successful     email/push notify     workspace activity  success format   client update
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/WorkspaceServiceImpl.java**
```java
addWorkspaceMember() method
↓ log.info("Adding member {} to workspace {} with role {}", userEmail, workspaceId, role)
↓ Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow(...)
↓ validateWorkspaceManagementPermissions(userId, workspace) → ADMIN/OWNER only
↓ User user = userRepository.findByEmailAndIsActiveTrue(userEmail).orElseThrow(...)
↓ Optional<WorkspaceMember> existing = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
↓ if (existing.isPresent()) throw new RuntimeException("User already workspace member")
↓ if (workspace.getCurrentMemberCount() >= workspace.getMaxMembers())
    throw new RuntimeException("Workspace is at maximum capacity")
↓ WorkspaceMember member = WorkspaceMember.builder()
    .workspace(workspace).user(user).role(WorkspaceRole.valueOf(role))
    .permissions(request.permissions).joinedAt(LocalDateTime.now())
    .invitedBy(userId).status(MemberStatus.PENDING).build()
↓ WorkspaceMember savedMember = workspaceMemberRepository.save(member)
↓ notificationService.sendWorkspaceInvitation(user.getEmail(), workspace.getName()) → async
↓ activityService.logWorkspaceActivity(workspaceId, "MEMBER_ADDED", userId, user.getName())
↓ return savedMember
```

---

## 🎯 **SENARYO 4: WORKSPACE SETTINGS - Ayarlar Yönetimi**

### **📥 Frontend Request**
```json
PUT /api/v1/workspaces/550e8400-e29b-41d4-a716-446655440009/settings
{
  "isPrivate": true,
  "maxMembers": 30,
  "allowGuestAccess": false,
  "defaultProjectVisibility": "PRIVATE",
  "requireApprovalForJoining": true
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → WorkspaceController → WorkspaceServiceImpl → WorkspaceRepository → PostgreSQL
    ↓              ↓                     ↓                      ↓                  ↓
UpdateSettingsReq @PutMapping       updateWorkspaceSettings() findById()         SELECT query
    ↓              ↓                     ↓                      ↓                  ↓
JSON Body      path variable        settings validation      workspace lookup   workspaces table
    ↓              ↓                     ↓                      ↓                  ↓
new settings   workspaceId extract  permission check         entity fetch       row data

WorkspaceServiceImpl → Settings Validation → Workspace Entity → WorkspaceRepository → Database
       ↓                       ↓                   ↓                   ↓                  ↓
validatePermissions()    validateSettings()   updateSettings()      save()            UPDATE
       ↓                       ↓                   ↓                   ↓                  ↓
OWNER required          business rules       entity update         JPA persist       row updated
       ↓                       ↓                   ↓                   ↓                  ↓
access control         constraint check     settings merge        transaction       settings saved

Database → Impact Analysis → NotificationService → WorkspaceController → Frontend
    ↓              ↓                 ↓                      ↓                ↓
update success settings impact   settings notification  response build   JSON Response
    ↓              ↓                 ↓                      ↓                ↓
settings changed member impact   async notify           SettingsResponse HTTP 200
    ↓              ↓                 ↓                      ↓                ↓
configuration   privacy changes   affected members       success format   client update
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/WorkspaceServiceImpl.java**
```java
updateWorkspaceSettings() method
↓ log.info("Updating settings for workspace {} by user {}", workspaceId, userId)
↓ Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow(...)
↓ validateWorkspaceOwnerPermissions(userId, workspace) → OWNER only for sensitive settings
↓ WorkspaceSettings currentSettings = workspace.getSettings()
↓ WorkspaceSettings newSettings = WorkspaceSettings.builder()
    .isPrivate(request.isPrivate).maxMembers(request.maxMembers)
    .allowGuestAccess(request.allowGuestAccess)
    .defaultProjectVisibility(request.defaultProjectVisibility)
    .requireApprovalForJoining(request.requireApprovalForJoining).build()
↓ validateSettingsConstraints(newSettings, workspace)
↓ workspace.updateSettings(newSettings)
↓ workspace.setUpdatedBy(userId)
↓ Workspace savedWorkspace = workspaceRepository.save(workspace)

// Handle settings impact
↓ if (settingsRequireMemberNotification(currentSettings, newSettings))
    notificationService.sendWorkspaceSettingsUpdate(workspace.getId(), newSettings)
↓ if (newSettings.isPrivate() && !currentSettings.isPrivate())
    handlePrivacyChange(workspace.getId()) → update related entities
↓ activityService.logWorkspaceActivity(workspaceId, "SETTINGS_UPDATED", userId)
↓ return savedWorkspace
```

---

## 🎯 **SENARYO 5: WORKSPACE ANALYTICS - Analitik Veriler**

### **📥 Frontend Request**
```json
GET /api/v1/workspaces/550e8400-e29b-41d4-a716-446655440009/analytics?period=QUARTERLY&metrics=productivity,collaboration,growth
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → WorkspaceController → WorkspaceServiceImpl → AnalyticsService → Multiple Repositories
    ↓              ↓                     ↓                      ↓                   ↓
GET params     @GetMapping          getWorkspaceAnalytics()  calculateMetrics()  analytics queries
    ↓              ↓                     ↓                      ↓                   ↓
analytics params path variable       analytics build        metric calculation   complex aggregation
    ↓              ↓                     ↓                      ↓                   ↓
period, metrics workspaceId extract  data collection        statistical analysis database operations

ProjectRepository → TaskRepository → TimeTrackingRepository → WorkspaceMemberRepository
       ↓                 ↓                  ↓                        ↓
project analytics    task analytics     time analytics           member analytics
       ↓                 ↓                  ↓                        ↓
delivery metrics     productivity       efficiency tracking      engagement metrics
       ↓                 ↓                  ↓                        ↓
trend analysis       velocity trends    resource utilization     collaboration patterns

All Metrics → AnalyticsService → WorkspaceAnalytics → WorkspaceController → Frontend
     ↓              ↓                     ↓                   ↓                ↓
data aggregation analytics processing  DTO creation        response build   JSON Response
     ↓              ↓                     ↓                   ↓                ↓
workspace insights trend analysis     analytics object     success format   HTTP 200
     ↓              ↓                     ↓                   ↓                ↓
KPI calculation    predictive metrics complete analytics   ApiResponse      client charts
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/AnalyticsServiceImpl.java**
```java
calculateWorkspaceAnalytics() method
↓ log.info("Calculating analytics for workspace {} with period {} and metrics {}", 
    workspaceId, period, metrics)
↓ LocalDate startDate = calculatePeriodStart(period)
↓ LocalDate endDate = LocalDate.now()

// Conditional metric calculation based on requested metrics
↓ WorkspaceAnalytics.WorkspaceAnalyticsBuilder builder = WorkspaceAnalytics.builder()
    .workspaceId(workspaceId).period(period).startDate(startDate).endDate(endDate)

↓ if (metrics.contains("productivity")) {
    ProductivityMetrics productivity = calculateProductivityMetrics(workspaceId, startDate, endDate)
    builder.productivityMetrics(productivity)
  }
↓ if (metrics.contains("collaboration")) {
    CollaborationMetrics collaboration = calculateCollaborationMetrics(workspaceId, startDate, endDate)
    builder.collaborationMetrics(collaboration)
  }
↓ if (metrics.contains("growth")) {
    GrowthMetrics growth = calculateGrowthMetrics(workspaceId, startDate, endDate)
    builder.growthMetrics(growth)
  }

↓ WorkspaceAnalytics analytics = builder
    .overallScore(calculateOverallWorkspaceScore(builder))
    .generatedAt(LocalDateTime.now())
    .build()
↓ return analytics
```

**2️⃣ Repository/WorkspaceRepository.java**
```java
@Query("""
    SELECT new com.example.dto.WorkspaceStatistics(
        w.id,
        COUNT(DISTINCT p.id) as projectCount,
        COUNT(DISTINCT t.id) as teamCount,
        COUNT(DISTINCT wm.id) as memberCount,
        COUNT(DISTINCT task.id) as taskCount,
        AVG(task.estimatedHours) as avgTaskComplexity
    )
    FROM Workspace w
    LEFT JOIN w.projects p
    LEFT JOIN w.teams t  
    LEFT JOIN w.members wm
    LEFT JOIN p.tasks task
    WHERE w.id = :workspaceId
    AND w.isActive = true
    GROUP BY w.id
""")
↓ WorkspaceStatistics getWorkspaceStatistics(@Param("workspaceId") UUID workspaceId)
↓ Complex aggregation query with multiple LEFT JOINs
↓ PostgreSQL: GROUP BY with multiple COUNT DISTINCT operations
↓ DTO projection for efficient data transfer
↓ Single query for comprehensive workspace statistics
```

Bu Workspace modülü, organizasyonel yapının temelini oluşturan ve tüm proje, takım ve üye yönetimini koordine eden merkezi bir yönetim sistemidir.
