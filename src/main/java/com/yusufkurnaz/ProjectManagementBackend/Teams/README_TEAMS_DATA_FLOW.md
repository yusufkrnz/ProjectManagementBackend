# 👥 TEAMS MODULE - SENARYO BAZLI VERİ AKIŞ DOKÜMANTASYONU

## 📁 **KLASÖR YAPISI**
```
Teams/
├── Controller/     ← Team REST endpoints
├── Service/        ← Team business logic
├── Repository/     ← Team data access
├── Entities/       ← Team entities
├── Model/          ← Team models
└── Dto/           ← Team DTOs
```

---

## 🎯 **SENARYO 1: CREATE TEAM - Yeni Takım Oluşturma**

### **📥 Frontend Request**
```json
POST /api/v1/teams
{
  "name": "Backend Development Team",
  "description": "Responsible for API development and database design",
  "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
  "teamType": "DEVELOPMENT",
  "maxMembers": 8,
  "isPublic": false
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → TeamController → TeamServiceImpl → WorkspaceRepository → PostgreSQL
    ↓           ↓                ↓                   ↓                ↓
CreateTeamReq @PostMapping   createTeam()        findById()       SELECT query
    ↓           ↓                ↓                   ↓                ↓
JSON Body   @Valid check     validation          workspace check  workspaces table
    ↓           ↓                ↓                   ↓                ↓
team data   userId extract   permission check    entity fetch     row data

TeamServiceImpl → Team Entity → TeamRepository → Database → TeamMember Entity
       ↓              ↓             ↓              ↓              ↓
   Builder.build()  team creation  save()        INSERT      creator member
       ↓              ↓             ↓              ↓              ↓
   metadata set     entity build   JPA persist   teams table  relationship
       ↓              ↓             ↓              ↓              ↓
   team config      validation     transaction   row created  team_members

Database → TeamMemberRepository → TeamController → Frontend
    ↓              ↓                    ↓               ↓
member added   save member          response build  JSON Response
    ↓              ↓                    ↓               ↓
relationship   JPA persist          TeamResponse    HTTP 201
    ↓              ↓                    ↓               ↓
ADMIN role     transaction          success format  client update
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Controller/TeamController.java**
```java
@PostMapping("/")
↓ @Valid CreateTeamRequest validation
↓ Authentication.getName() → UUID creatorId
↓ teamService.createTeam(request, creatorId)
↓ TeamResponse.fromEntity(savedTeam)
↓ ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
```

**2️⃣ Service/impl/TeamServiceImpl.java**
```java
createTeam() method
↓ log.info("Creating team '{}' in workspace {} by user {}", name, workspaceId, creatorId)
↓ Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow(...)
↓ validateTeamCreationPermissions(creatorId, workspace)
↓ Team team = Team.builder()
    .name(request.name).description(request.description).workspace(workspace)
    .teamType(TeamType.valueOf(request.teamType)).maxMembers(request.maxMembers)
    .isPublic(request.isPublic).createdBy(creatorId).build()
↓ Team savedTeam = teamRepository.save(team)
↓ TeamMember creatorMember = TeamMember.builder()
    .team(savedTeam).user(creator).role(TeamRole.ADMIN)
    .joinedAt(LocalDateTime.now()).status(MemberStatus.ACTIVE).build()
↓ teamMemberRepository.save(creatorMember)
↓ activityService.logTeamActivity(savedTeam.getId(), "TEAM_CREATED", creatorId)
↓ return savedTeam
```

---

## 🎯 **SENARYO 2: ADD TEAM MEMBER - Takıma Üye Ekleme**

### **📥 Frontend Request**
```json
POST /api/v1/teams/550e8400-e29b-41d4-a716-446655440008/members
{
  "userEmail": "developer@example.com",
  "role": "DEVELOPER",
  "permissions": ["READ", "WRITE", "COMMENT"]
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → TeamController → TeamServiceImpl → UserRepository → PostgreSQL
    ↓           ↓                ↓                ↓               ↓
AddMemberReq @PostMapping    addTeamMember()   findByEmail()   SELECT query
    ↓           ↓                ↓                ↓               ↓
JSON Body   path variable    member validation user lookup     users table
    ↓           ↓                ↓                ↓               ↓
user email  teamId extract   permission check  entity fetch    row data

TeamServiceImpl → TeamRepository → TeamMember Entity → TeamMemberRepository → Database
       ↓              ↓                 ↓                    ↓                   ↓
validateTeamAccess() findById()     Builder.build()       save()             INSERT
       ↓              ↓                 ↓                    ↓                   ↓
permission check    team lookup     member creation       JPA persist        team_members
       ↓              ↓                 ↓                    ↓                   ↓
ADMIN required      entity fetch    role assignment       transaction        relationship

Database → NotificationService → ActivityService → TeamController → Frontend
    ↓              ↓                    ↓               ↓               ↓
member added   team invitation      activity log    response build  JSON Response
    ↓              ↓                    ↓               ↓               ↓
relationship   async notification   audit trail     MemberResponse  HTTP 201
    ↓              ↓                    ↓               ↓               ↓
successful     email/push notify    team activity   success format  client update
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/TeamServiceImpl.java**
```java
addTeamMember() method
↓ log.info("Adding member {} to team {} with role {}", userEmail, teamId, role)
↓ Team team = teamRepository.findById(teamId).orElseThrow(...)
↓ validateTeamManagementPermissions(userId, team) → ADMIN/MODERATOR only
↓ User user = userRepository.findByEmailAndIsActiveTrue(userEmail).orElseThrow(...)
↓ Optional<TeamMember> existing = teamMemberRepository.findByTeamIdAndUserId(teamId, user.getId())
↓ if (existing.isPresent()) throw new RuntimeException("User already team member")
↓ if (team.getCurrentMemberCount() >= team.getMaxMembers())
    throw new RuntimeException("Team is at maximum capacity")
↓ TeamMember member = TeamMember.builder()
    .team(team).user(user).role(TeamRole.valueOf(role))
    .permissions(request.permissions).joinedAt(LocalDateTime.now())
    .invitedBy(userId).status(MemberStatus.PENDING).build()
↓ TeamMember savedMember = teamMemberRepository.save(member)
↓ notificationService.sendTeamInvitation(user.getEmail(), team.getName()) → async
↓ activityService.logTeamActivity(teamId, "MEMBER_ADDED", userId, user.getName())
↓ return savedMember
```

---

## 🎯 **SENARYO 3: TEAM COLLABORATION - Takım İşbirliği**

### **📥 Frontend Request**
```json
GET /api/v1/teams/550e8400-e29b-41d4-a716-446655440008/collaboration
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → TeamController → TeamServiceImpl → Multiple Repositories → PostgreSQL
    ↓           ↓                ↓                    ↓                   ↓
GET request @GetMapping     getTeamCollaboration() parallel queries    MULTIPLE SELECT
    ↓           ↓                ↓                    ↓                   ↓
team ID     path variable   collaboration data     repository calls    different tables
    ↓           ↓                ↓                    ↓                   ↓
UUID param  validation      data aggregation       JPA queries         join operations

TeamMemberRepository → ProjectRepository → TaskRepository → MessageRepository
       ↓                      ↓                ↓                ↓
member activities         team projects     team tasks       team messages
       ↓                      ↓                ↓                ↓
activity tracking         project list      task distribution message volume
       ↓                      ↓                ↓                ↓
contribution stats        project progress  workload balance  communication

All Repositories → TeamServiceImpl → CollaborationMetrics → TeamController → Frontend
       ↓                  ↓                   ↓                   ↓               ↓
aggregated data       metrics calculation  DTO creation       response build  JSON Response
       ↓                  ↓                   ↓                   ↓               ↓
team statistics       performance analysis collaboration object success format HTTP 200
       ↓                  ↓                   ↓                   ↓               ↓
member productivity   team health metrics  complete metrics   ApiResponse     client charts
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/TeamServiceImpl.java**
```java
getTeamCollaboration() method
↓ log.debug("Building collaboration metrics for team: {}", teamId)
↓ validateTeamAccess(userId, teamId)

// Parallel metrics calculation
↓ CompletableFuture<List<TeamMember>> members = CompletableFuture.supplyAsync(() -> 
    teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId))
↓ CompletableFuture<List<Project>> projects = CompletableFuture.supplyAsync(() -> 
    projectRepository.findByTeamId(teamId))
↓ CompletableFuture<TaskDistribution> taskDistribution = CompletableFuture.supplyAsync(() -> 
    taskRepository.getTeamTaskDistribution(teamId))
↓ CompletableFuture<CommunicationMetrics> communication = CompletableFuture.supplyAsync(() -> 
    messageRepository.getTeamCommunicationMetrics(teamId))

↓ CompletableFuture.allOf(members, projects, taskDistribution, communication).join()

↓ TeamCollaborationMetrics metrics = TeamCollaborationMetrics.builder()
    .teamId(teamId).members(members.get()).projects(projects.get())
    .taskDistribution(taskDistribution.get()).communication(communication.get())
    .collaborationScore(calculateCollaborationScore(members.get(), taskDistribution.get()))
    .build()
↓ return metrics
```

---

## 🎯 **SENARYO 4: TEAM PERFORMANCE - Takım Performansı**

### **📥 Frontend Request**
```json
GET /api/v1/teams/550e8400-e29b-41d4-a716-446655440008/performance?period=MONTHLY
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → TeamController → TeamServiceImpl → PerformanceService → Multiple Repositories
    ↓           ↓                ↓                   ↓                     ↓
GET params  @GetMapping     getTeamPerformance() calculateMetrics()   performance queries
    ↓           ↓                ↓                   ↓                     ↓
period param path variable  performance analysis metric calculation    database aggregation
    ↓           ↓                ↓                   ↓                     ↓
MONTHLY     teamId extract  period filtering     statistical analysis   complex queries

TaskRepository → TimeTrackingRepository → ProjectRepository → TeamMemberRepository
      ↓                  ↓                      ↓                   ↓
task completion     time analytics          project delivery     member productivity
      ↓                  ↓                      ↓                   ↓
velocity metrics    hours tracking          milestone tracking   individual performance
      ↓                  ↓                      ↓                   ↓
burn down charts    overtime analysis       delivery rate        contribution analysis

All Metrics → PerformanceService → TeamPerformance → TeamController → Frontend
     ↓              ↓                     ↓               ↓               ↓
data aggregation performance calculation DTO creation   response build  JSON Response
     ↓              ↓                     ↓               ↓               ↓
team analytics    KPI calculations      performance obj success format  HTTP 200
     ↓              ↓                     ↓               ↓               ↓
trend analysis    benchmark comparison  complete metrics ApiResponse    client dashboard
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/PerformanceServiceImpl.java**
```java
calculateTeamPerformance() method
↓ log.info("Calculating performance for team {} with period {}", teamId, period)
↓ LocalDate startDate = calculatePeriodStart(period)
↓ LocalDate endDate = LocalDate.now()

// Task performance metrics
↓ TaskPerformanceMetrics taskMetrics = TaskPerformanceMetrics.builder()
    .totalTasks(taskRepository.countTeamTasks(teamId, startDate, endDate))
    .completedTasks(taskRepository.countCompletedTeamTasks(teamId, startDate, endDate))
    .averageCompletionTime(taskRepository.getAverageCompletionTime(teamId, startDate, endDate))
    .velocity(calculateTeamVelocity(teamId, startDate, endDate))
    .build()

// Time tracking metrics
↓ TimeTrackingMetrics timeMetrics = TimeTrackingMetrics.builder()
    .totalHours(timeTrackingRepository.getTotalTeamHours(teamId, startDate, endDate))
    .averageHoursPerMember(timeTrackingRepository.getAverageHoursPerMember(teamId, startDate, endDate))
    .overtimeHours(timeTrackingRepository.getOvertimeHours(teamId, startDate, endDate))
    .build()

↓ TeamPerformanceReport report = TeamPerformanceReport.builder()
    .teamId(teamId).period(period).startDate(startDate).endDate(endDate)
    .taskMetrics(taskMetrics).timeMetrics(timeMetrics)
    .performanceScore(calculateOverallScore(taskMetrics, timeMetrics))
    .build()
↓ return report
```

---

## 🎯 **SENARYO 5: TEAM COMMUNICATION - Takım İletişimi**

### **📥 Frontend Request**
```json
GET /api/v1/teams/550e8400-e29b-41d4-a716-446655440008/messages?page=0&size=20
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → TeamController → TeamServiceImpl → MessageRepository → PostgreSQL
    ↓           ↓                ↓                ↓                 ↓
GET params  @GetMapping     getTeamMessages()  findTeamMessages() COMPLEX query
    ↓           ↓                ↓                ↓                 ↓
page, size  path variable   message retrieval  JPA query         messages table
    ↓           ↓                ↓                ↓                 ↓
pagination  teamId extract  permission check   WHERE clause      team messages

PostgreSQL → MessageRepository → TeamServiceImpl → TeamController → Frontend
     ↓              ↓                ↓                ↓               ↓
message data   Page<Message>    message processing response build JSON Response
     ↓              ↓                ↓                ↓               ↓
JOIN operations entity mapping   format messages   MessageResponse HTTP 200
     ↓              ↓                ↓                ↓               ↓
sender/team info lazy loading    conversation build page metadata  client display
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Repository/MessageRepository.java**
```java
@Query("""
    SELECT m FROM Message m 
    LEFT JOIN FETCH m.sender 
    WHERE m.messageType = 'TEAM' 
    AND m.teamId = :teamId 
    AND EXISTS (
        SELECT tm FROM TeamMember tm 
        WHERE tm.team.id = :teamId 
        AND tm.user.id = :userId 
        AND tm.status = 'ACTIVE'
    )
    ORDER BY m.sentAt DESC
""")
↓ Page<Message> findTeamMessages(@Param("teamId") UUID teamId, 
    @Param("userId") UUID userId, Pageable pageable)
↓ JPA query with security check (team membership)
↓ PostgreSQL: EXISTS subquery for permission validation
↓ Eager loading of sender information
↓ Pagination and sorting by sent date
↓ Page<Message> return with total count
```

**2️⃣ Service/impl/TeamServiceImpl.java**
```java
getTeamMessages() method
↓ log.debug("Getting messages for team {} by user {}", teamId, userId)
↓ validateTeamMembership(userId, teamId) → security check
↓ Page<Message> messages = messageRepository.findTeamMessages(teamId, userId, pageable)
↓ TeamMessagesResponse response = TeamMessagesResponse.builder()
    .messages(messages.getContent().stream()
        .map(MessageResponse::fromEntity)
        .collect(Collectors.toList()))
    .totalElements(messages.getTotalElements())
    .totalPages(messages.getTotalPages())
    .currentPage(messages.getNumber())
    .pageSize(messages.getSize())
    .build()
↓ return response
```

Bu Teams modülü, takım oluşturma, üye yönetimi, performans takibi ve iletişimi destekleyen kapsamlı bir takım yönetim sistemidir.
