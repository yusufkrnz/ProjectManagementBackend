# 📊 PROJECTS MODULE - SENARYO BAZLI VERİ AKIŞ DOKÜMANTASYONU

## 📁 **KLASÖR YAPISI**
```
Projects/
├── Controller/     ← Project REST endpoints
├── Service/        ← Project business logic
├── Repository/     ← Project data access
├── Model/          ← Project entities
└── Dto/           ← Project DTOs
    ├── request/    ← Project request DTOs
    └── response/   ← Project response DTOs
```

---

## 🎯 **SENARYO 1: CREATE PROJECT - Yeni Proje Oluşturma**

### **📥 Frontend Request**
```json
POST /api/v1/projects
{
  "name": "E-Commerce Platform",
  "description": "Modern e-commerce solution with microservices",
  "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
  "startDate": "2024-02-01",
  "endDate": "2024-08-31",
  "priority": "HIGH",
  "tags": ["microservices", "spring-boot", "react"]
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → ProjectController → ProjectServiceImpl → WorkspaceRepository → PostgreSQL
    ↓           ↓                    ↓                    ↓                  ↓
CreateProjectReq @PostMapping     createProject()      findById()         SELECT query
    ↓           ↓                    ↓                    ↓                  ↓
JSON Body   @Valid check         validation           workspace check    workspaces table
    ↓           ↓                    ↓                    ↓                  ↓
project data userId extract      permission check     entity fetch       row data

ProjectServiceImpl → Project Entity → ProjectRepository → Database → TeamService
       ↓                  ↓                ↓                ↓           ↓
   Builder.build()    project creation   save()         INSERT      create team
       ↓                  ↓                ↓                ↓           ↓
   metadata set       entity instance   JPA persist    projects     project team
       ↓                  ↓                ↓                ↓           ↓
   dates, priority    validation       transaction     row created   team entity

Database → ProjectMember → ProjectRepository → ProjectController → Frontend
    ↓           ↓               ↓                    ↓               ↓
team created project member   save member         response build  JSON Response
    ↓           ↓               ↓                    ↓               ↓
relationship creator as PM    relationship        ProjectResponse HTTP 201
    ↓           ↓               ↓                    ↓               ↓
team_projects  ADMIN role     project_members     success format  client update
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Controller/ProjectController.java**
```java
@PostMapping("/")
↓ @Valid CreateProjectRequest validation
↓ Authentication.getName() → UUID userId
↓ projectService.createProject(request, userId)
↓ ProjectResponse.fromEntity(savedProject)
↓ ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
```

**2️⃣ Service/impl/ProjectServiceImpl.java**
```java
createProject() method
↓ log.info("Creating project '{}' in workspace {} by user {}", name, workspaceId, userId)
↓ Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow(...)
↓ validateProjectCreationPermissions(userId, workspace)
↓ Project project = Project.builder()
    .name(request.name).description(request.description).workspace(workspace)
    .startDate(request.startDate).endDate(request.endDate)
    .priority(ProjectPriority.valueOf(request.priority))
    .status(ProjectStatus.PLANNING).createdBy(userId).build()
↓ Project savedProject = projectRepository.save(project)
↓ Team projectTeam = teamService.createProjectTeam(savedProject, userId) → auto team
↓ ProjectMember creator = ProjectMember.builder()
    .project(savedProject).user(user).role(ProjectRole.PROJECT_MANAGER)
    .joinedAt(LocalDateTime.now()).build()
↓ projectMemberRepository.save(creator)
↓ return savedProject
```

---

## 🎯 **SENARYO 2: PROJECT DASHBOARD - Proje Özet Bilgileri**

### **📥 Frontend Request**
```json
GET /api/v1/projects/550e8400-e29b-41d4-a716-446655440003/dashboard
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → ProjectController → ProjectServiceImpl → Multiple Repositories → PostgreSQL
    ↓           ↓                    ↓                      ↓                   ↓
GET request @GetMapping        getProjectDashboard()   parallel queries    MULTIPLE SELECT
    ↓           ↓                    ↓                      ↓                   ↓
project ID  path variable      dashboard build        repository calls    different tables
    ↓           ↓                    ↓                      ↓                   ↓
UUID param  validation         data aggregation       JPA queries         join operations

ProjectRepository → TaskRepository → ProjectMemberRepository → TimeTrackingRepository
       ↓                 ↓                    ↓                      ↓
project details      task statistics     member count           time spent
       ↓                 ↓                    ↓                      ↓
entity fetch         count queries       member list            sum calculation
       ↓                 ↓                    ↓                      ↓
basic info          task progress        role distribution      hours tracking

All Repositories → ProjectServiceImpl → ProjectDashboard → ProjectController → Frontend
       ↓                  ↓                    ↓                ↓               ↓
aggregated data       dashboard build      DTO creation      response wrap   JSON Response
       ↓                  ↓                    ↓                ↓               ↓
statistics           data processing      dashboard object  success format  HTTP 200
       ↓                  ↓                    ↓                ↓               ↓
metrics calc         business logic       complete data     ApiResponse     client display
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/ProjectServiceImpl.java**
```java
getProjectDashboard() method
↓ log.debug("Building dashboard for project: {}", projectId)
↓ Project project = projectRepository.findById(projectId).orElseThrow(...)
↓ validateProjectAccess(userId, project)

// Parallel data fetching
↓ CompletableFuture<TaskStatistics> taskStats = CompletableFuture.supplyAsync(() -> 
    taskRepository.getProjectTaskStatistics(projectId))
↓ CompletableFuture<List<ProjectMember>> members = CompletableFuture.supplyAsync(() -> 
    projectMemberRepository.findByProjectIdAndIsActiveTrue(projectId))
↓ CompletableFuture<ProjectTimeTracking> timeTracking = CompletableFuture.supplyAsync(() -> 
    timeTrackingRepository.getProjectTimeStatistics(projectId))
↓ CompletableFuture<List<RecentActivity>> activities = CompletableFuture.supplyAsync(() -> 
    activityRepository.getRecentProjectActivities(projectId, 10))

↓ CompletableFuture.allOf(taskStats, members, timeTracking, activities).join()

↓ ProjectDashboard dashboard = ProjectDashboard.builder()
    .project(ProjectSummary.fromEntity(project))
    .taskStatistics(taskStats.get()).members(members.get())
    .timeTracking(timeTracking.get()).recentActivities(activities.get())
    .progressPercentage(calculateProjectProgress(project, taskStats.get()))
    .build()
↓ return dashboard
```

**2️⃣ Repository/TaskRepository.java**
```java
@Query("""
    SELECT new com.example.dto.TaskStatistics(
        COUNT(t), 
        COUNT(CASE WHEN t.status = 'COMPLETED' THEN 1 END),
        COUNT(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 END),
        COUNT(CASE WHEN t.status = 'TODO' THEN 1 END),
        COUNT(CASE WHEN t.dueDate < CURRENT_DATE AND t.status != 'COMPLETED' THEN 1 END)
    )
    FROM Task t WHERE t.project.id = :projectId
""")
↓ TaskStatistics getProjectTaskStatistics(@Param("projectId") UUID projectId)
↓ JPA query with aggregation functions
↓ PostgreSQL: COUNT with CASE WHEN for conditional counting
↓ Single query for all task statistics
↓ DTO projection for efficient data transfer
```

---

## 🎯 **SENARYO 3: PROJECT MEMBER MANAGEMENT - Üye Yönetimi**

### **📥 Frontend Request**
```json
POST /api/v1/projects/550e8400-e29b-41d4-a716-446655440003/members
{
  "userEmail": "developer@example.com",
  "role": "DEVELOPER",
  "permissions": ["READ", "WRITE", "TASK_MANAGEMENT"]
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → ProjectController → ProjectServiceImpl → UserRepository → PostgreSQL
    ↓           ↓                    ↓                   ↓               ↓
AddMemberReq @PostMapping        addProjectMember()   findByEmail()   SELECT query
    ↓           ↓                    ↓                   ↓               ↓
JSON Body   @Valid check         member validation    user lookup     users table
    ↓           ↓                    ↓                   ↓               ↓
user email  projectId extract    permission check    entity fetch    row data

ProjectServiceImpl → ProjectMemberRepository → Database → NotificationService → Email
       ↓                      ↓                  ↓              ↓                ↓
validatePermissions()     save()              INSERT      project invitation  async send
       ↓                      ↓                  ↓              ↓                ↓
role check               member creation     project_members notification      email queue
       ↓                      ↓                  ↓              ↓                ↓
PM/ADMIN required        entity build        relationship    async processing  SMTP send

Database → ActivityLog → ProjectController → Frontend
    ↓           ↓              ↓               ↓
member added activity log   response build  JSON Response
    ↓           ↓              ↓               ↓
relationship audit trail    MemberResponse  HTTP 201
    ↓           ↓              ↓               ↓
successful   project activity success format client update
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/ProjectServiceImpl.java**
```java
addProjectMember() method
↓ log.info("Adding member {} to project {} with role {}", userEmail, projectId, role)
↓ Project project = projectRepository.findById(projectId).orElseThrow(...)
↓ validateProjectManagementPermissions(userId, project) → PM/ADMIN only
↓ User user = userRepository.findByEmailAndIsActiveTrue(userEmail).orElseThrow(...)
↓ Optional<ProjectMember> existing = projectMemberRepository.findByProjectIdAndUserId(projectId, user.getId())
↓ if (existing.isPresent()) throw new RuntimeException("User already member")
↓ ProjectMember member = ProjectMember.builder()
    .project(project).user(user).role(ProjectRole.valueOf(role))
    .permissions(request.permissions).joinedAt(LocalDateTime.now())
    .invitedBy(userId).status(MemberStatus.ACTIVE).build()
↓ ProjectMember savedMember = projectMemberRepository.save(member)
↓ activityService.logProjectActivity(projectId, "MEMBER_ADDED", userId, user.getName())
↓ notificationService.sendProjectInvitation(user.getEmail(), project.getName()) → async
↓ return savedMember
```

---

## 🎯 **SENARYO 4: PROJECT TIMELINE - Proje Zaman Çizelgesi**

### **📥 Frontend Request**
```json
GET /api/v1/projects/550e8400-e29b-41d4-a716-446655440003/timeline?startDate=2024-01-01&endDate=2024-12-31
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → ProjectController → ProjectServiceImpl → TaskRepository → PostgreSQL
    ↓           ↓                    ↓                   ↓               ↓
GET timeline @GetMapping        getProjectTimeline()  findTasksInRange() DATE range query
    ↓           ↓                    ↓                   ↓               ↓
date params path variable       timeline build      JPA query        tasks table
    ↓           ↓                    ↓                   ↓               ↓
start/end   projectId extract   date filtering      WHERE clause     date filtering

PostgreSQL → MilestoneRepository → ProjectServiceImpl → TimelineBuilder → ProjectController
     ↓              ↓                     ↓                  ↓                ↓
task results    milestone query      data aggregation   timeline creation response build
     ↓              ↓                     ↓                  ↓                ↓
date sorted     milestone entities   combine data       chronological     TimelineResponse
     ↓              ↓                     ↓                  ↓                ↓
ORDER BY date   milestone dates      timeline events    time ordering     HTTP 200

Frontend ← JSON Response ← ApiResponse ← TimelineResponse ← Timeline Object
    ↓           ↓              ↓              ↓                ↓
client display success format  standard wrap  timeline data   structured data
    ↓           ↓              ↓              ↓                ↓
Gantt chart    HTTP 200       response body  events array    visual timeline
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/ProjectServiceImpl.java**
```java
getProjectTimeline() method
↓ log.debug("Building timeline for project {} between {} and {}", projectId, startDate, endDate)
↓ validateProjectAccess(userId, projectId)
↓ List<Task> tasks = taskRepository.findTasksInDateRange(projectId, startDate, endDate)
↓ List<Milestone> milestones = milestoneRepository.findByProjectIdAndDateBetween(projectId, startDate, endDate)
↓ List<ProjectEvent> events = eventRepository.findByProjectIdAndDateBetween(projectId, startDate, endDate)

↓ List<TimelineItem> timelineItems = new ArrayList<>()
↓ timelineItems.addAll(tasks.stream().map(this::taskToTimelineItem).collect(Collectors.toList()))
↓ timelineItems.addAll(milestones.stream().map(this::milestoneToTimelineItem).collect(Collectors.toList()))
↓ timelineItems.addAll(events.stream().map(this::eventToTimelineItem).collect(Collectors.toList()))

↓ timelineItems.sort(Comparator.comparing(TimelineItem::getDate))

↓ ProjectTimeline timeline = ProjectTimeline.builder()
    .projectId(projectId).startDate(startDate).endDate(endDate)
    .items(timelineItems).totalItems(timelineItems.size())
    .build()
↓ return timeline
```

**2️⃣ Repository/TaskRepository.java**
```java
@Query("""
    SELECT t FROM Task t 
    WHERE t.project.id = :projectId 
    AND ((t.startDate BETWEEN :startDate AND :endDate) 
         OR (t.dueDate BETWEEN :startDate AND :endDate)
         OR (t.startDate <= :startDate AND t.dueDate >= :endDate))
    ORDER BY t.startDate ASC, t.dueDate ASC
""")
↓ List<Task> findTasksInDateRange parameters
↓ Complex date range query covering overlapping periods
↓ PostgreSQL: multiple date conditions with OR logic
↓ Handles tasks that start before, end after, or overlap the range
↓ Ordered by start date then due date for timeline display
```

---

## 🎯 **SENARYO 5: PROJECT REPORTS - Proje Raporları**

### **📥 Frontend Request**
```json
GET /api/v1/projects/550e8400-e29b-41d4-a716-446655440003/reports/performance?period=MONTHLY
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → ProjectController → ProjectServiceImpl → ReportService → Multiple Repositories
    ↓           ↓                    ↓                   ↓                ↓
GET report  @GetMapping        generateReport()     buildReport()    parallel queries
    ↓           ↓                    ↓                   ↓                ↓
report type path variable      report validation   data collection  repository calls
    ↓           ↓                    ↓                   ↓                ↓
PERFORMANCE projectId extract  permission check    metrics calc     database hits

TaskRepository → TimeTrackingRepository → ProjectMemberRepository → BudgetRepository
      ↓                  ↓                        ↓                     ↓
task metrics        time analytics           member productivity    budget tracking
      ↓                  ↓                        ↓                     ↓
completion rates    hours worked             individual stats       cost analysis
      ↓                  ↓                        ↓                     ↓
velocity calc       overtime tracking        performance scores     budget vs actual

All Data → ReportService → ProjectReport → ProjectController → Frontend
    ↓           ↓               ↓               ↓               ↓
aggregation report generation  DTO creation   response build  JSON Response
    ↓           ↓               ↓               ↓               ↓
data merge  statistical calc  report object  success format  HTTP 200
    ↓           ↓               ↓               ↓               ↓
analytics   chart data        complete report ApiResponse     client charts
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/ReportServiceImpl.java**
```java
generatePerformanceReport() method
↓ log.info("Generating performance report for project {} with period {}", projectId, period)
↓ LocalDate startDate = calculatePeriodStart(period) → last month/quarter/year
↓ LocalDate endDate = LocalDate.now()

// Parallel metrics calculation
↓ CompletableFuture<TaskMetrics> taskMetrics = CompletableFuture.supplyAsync(() -> 
    calculateTaskMetrics(projectId, startDate, endDate))
↓ CompletableFuture<TimeMetrics> timeMetrics = CompletableFuture.supplyAsync(() -> 
    calculateTimeMetrics(projectId, startDate, endDate))
↓ CompletableFuture<ProductivityMetrics> productivityMetrics = CompletableFuture.supplyAsync(() -> 
    calculateProductivityMetrics(projectId, startDate, endDate))
↓ CompletableFuture<BudgetMetrics> budgetMetrics = CompletableFuture.supplyAsync(() -> 
    calculateBudgetMetrics(projectId, startDate, endDate))

↓ CompletableFuture.allOf(taskMetrics, timeMetrics, productivityMetrics, budgetMetrics).join()

↓ ProjectPerformanceReport report = ProjectPerformanceReport.builder()
    .projectId(projectId).period(period).startDate(startDate).endDate(endDate)
    .taskMetrics(taskMetrics.get()).timeMetrics(timeMetrics.get())
    .productivityMetrics(productivityMetrics.get()).budgetMetrics(budgetMetrics.get())
    .generatedAt(LocalDateTime.now()).generatedBy(userId)
    .build()
↓ return report
```

Bu Projects modülü, proje yaşam döngüsünün tüm aşamalarını yöneten kapsamlı bir proje yönetim sistemidir.
