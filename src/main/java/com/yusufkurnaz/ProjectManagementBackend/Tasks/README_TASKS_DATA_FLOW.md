# ✅ TASKS MODULE - SENARYO BAZLI VERİ AKIŞ DOKÜMANTASYONU

## 📁 **KLASÖR YAPISI**
```
Tasks/
├── Controller/     ← Task REST endpoints
├── Service/        ← Task business logic  
├── Repository/     ← Task data access
├── Entities/       ← Task entities
├── Dto/           ← Task DTOs
└── Exception/     ← Task exceptions
```

---

## 🎯 **SENARYO 1: CREATE TASK - Yeni Görev Oluşturma**

### **📥 Frontend Request**
```json
POST /api/v1/tasks
{
  "title": "Implement user authentication API",
  "description": "Create JWT-based authentication endpoints with refresh token support",
  "projectId": "550e8400-e29b-41d4-a716-446655440003",
  "assigneeId": "550e8400-e29b-41d4-a716-446655440004",
  "priority": "HIGH",
  "dueDate": "2024-02-15",
  "estimatedHours": 16,
  "tags": ["backend", "security", "api"]
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → TaskController → TaskServiceImpl → ProjectRepository → PostgreSQL
    ↓           ↓                ↓                  ↓                ↓
CreateTaskReq @PostMapping   createTask()       findById()       SELECT query
    ↓           ↓                ↓                  ↓                ↓
JSON Body   @Valid check     validation         project check    projects table
    ↓           ↓                ↓                  ↓                ↓
task data   userId extract   permission check   entity fetch     row data

TaskServiceImpl → UserRepository → Task Entity → TaskRepository → Database
       ↓              ↓              ↓              ↓               ↓
validateAssignee() findById()    Builder.build()  save()         INSERT
       ↓              ↓              ↓              ↓               ↓
assignee check    user lookup    task creation   JPA persist    tasks table
       ↓              ↓              ↓              ↓               ↓
user validation   entity fetch   metadata set    transaction    row created

Database → NotificationService → ActivityService → TaskController → Frontend
    ↓              ↓                    ↓               ↓               ↓
task created   task assignment      activity log    response build  JSON Response
    ↓              ↓                    ↓               ↓               ↓
entity saved   async notification   audit trail     TaskResponse    HTTP 201
    ↓              ↓                    ↓               ↓               ↓
ID generated   email/push notify    project activity success format client update
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Controller/TaskController.java**
```java
@PostMapping("/")
↓ @Valid CreateTaskRequest validation
↓ Authentication.getName() → UUID creatorId
↓ taskService.createTask(request, creatorId)
↓ TaskResponse.fromEntity(savedTask)
↓ ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
```

**2️⃣ Service/impl/TaskServiceImpl.java**
```java
createTask() method
↓ log.info("Creating task '{}' in project {} by user {}", title, projectId, creatorId)
↓ Project project = projectRepository.findById(projectId).orElseThrow(...)
↓ validateTaskCreationPermissions(creatorId, project)
↓ User assignee = userRepository.findById(request.assigneeId).orElseThrow(...)
↓ validateAssigneeProjectMembership(assignee.getId(), projectId)
↓ Task task = Task.builder()
    .title(request.title).description(request.description).project(project)
    .assignee(assignee).creator(creator).priority(TaskPriority.valueOf(request.priority))
    .status(TaskStatus.TODO).dueDate(request.dueDate)
    .estimatedHours(request.estimatedHours).createdAt(LocalDateTime.now()).build()
↓ request.tags.forEach(task::addTag)
↓ Task savedTask = taskRepository.save(task)
↓ notificationService.sendTaskAssignmentNotification(assignee, savedTask) → async
↓ activityService.logTaskActivity(savedTask.getId(), "TASK_CREATED", creatorId)
↓ return savedTask
```

---

## 🎯 **SENARYO 2: UPDATE TASK STATUS - Görev Durumu Güncelleme**

### **📥 Frontend Request**
```json
PUT /api/v1/tasks/550e8400-e29b-41d4-a716-446655440005/status
{
  "status": "IN_PROGRESS",
  "comment": "Started working on authentication endpoints"
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → TaskController → TaskServiceImpl → TaskRepository → PostgreSQL
    ↓           ↓                ↓                ↓               ↓
StatusUpdateReq @PutMapping   updateTaskStatus() findById()     SELECT query
    ↓           ↓                ↓                ↓               ↓
JSON Body   path variable    status validation  task lookup    tasks table
    ↓           ↓                ↓                ↓               ↓
new status  taskId extract   permission check  entity fetch   row data

TaskServiceImpl → Task Entity → TaskRepository → Database → TimeTracking
       ↓              ↓              ↓              ↓              ↓
validateStatusChange() setStatus()   save()        UPDATE      start tracking
       ↓              ↓              ↓              ↓              ↓
business rules    status update   JPA persist   row updated   time entry
       ↓              ↓              ↓              ↓              ↓
workflow check    entity change   transaction   status change auto tracking

Database → TaskHistory → NotificationService → TaskController → Frontend
    ↓           ↓              ↓                    ↓               ↓
update success status history  status notification response build JSON Response
    ↓           ↓              ↓                    ↓               ↓
version++      audit record   async notify        TaskResponse   HTTP 200
    ↓           ↓              ↓                    ↓               ↓
optimistic lock history table  team notification  success format client update
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/TaskServiceImpl.java**
```java
updateTaskStatus() method
↓ log.info("Updating task {} status from {} to {}", taskId, currentStatus, newStatus)
↓ Task task = taskRepository.findById(taskId).orElseThrow(...)
↓ validateTaskUpdatePermissions(userId, task)
↓ TaskStatus oldStatus = task.getStatus()
↓ validateStatusTransition(oldStatus, TaskStatus.valueOf(newStatus))
↓ task.setStatus(TaskStatus.valueOf(newStatus))
↓ task.setUpdatedBy(userId)
↓ task.setUpdatedAt(LocalDateTime.now())
↓ if (request.comment != null) task.addComment(userId, request.comment)

// Status-specific logic
↓ if (newStatus.equals("IN_PROGRESS")) {
    timeTrackingService.startTimeTracking(taskId, userId)
  } else if (newStatus.equals("COMPLETED")) {
    timeTrackingService.stopTimeTracking(taskId, userId)
    task.setCompletedAt(LocalDateTime.now())
  }

↓ Task savedTask = taskRepository.save(task)
↓ taskHistoryService.recordStatusChange(taskId, oldStatus, newStatus, userId, request.comment)
↓ notificationService.sendTaskStatusNotification(task, oldStatus, newStatus)
↓ return savedTask
```

**2️⃣ Status Transition Validation**
```java
validateStatusTransition() method
↓ Map<TaskStatus, List<TaskStatus>> allowedTransitions = Map.of(
    TaskStatus.TODO, List.of(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED),
    TaskStatus.IN_PROGRESS, List.of(TaskStatus.COMPLETED, TaskStatus.BLOCKED, TaskStatus.TODO),
    TaskStatus.COMPLETED, List.of(TaskStatus.TODO), // Reopen
    TaskStatus.BLOCKED, List.of(TaskStatus.IN_PROGRESS, TaskStatus.TODO)
  )
↓ if (!allowedTransitions.get(currentStatus).contains(newStatus))
    throw new InvalidStatusTransitionException("Cannot transition from " + currentStatus + " to " + newStatus)
```

---

## 🎯 **SENARYO 3: TASK ASSIGNMENT - Görev Atama**

### **📥 Frontend Request**
```json
PUT /api/v1/tasks/550e8400-e29b-41d4-a716-446655440005/assign
{
  "assigneeId": "550e8400-e29b-41d4-a716-446655440006",
  "reason": "Better expertise in authentication systems"
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → TaskController → TaskServiceImpl → UserRepository → PostgreSQL
    ↓           ↓                ↓                ↓               ↓
AssignTaskReq @PutMapping    assignTask()     findById()       SELECT query
    ↓           ↓                ↓                ↓               ↓
JSON Body   path variable   assignment logic  user lookup     users table
    ↓           ↓                ↓                ↓               ↓
new assignee taskId extract  validation       entity fetch    row data

TaskServiceImpl → ProjectMemberRepository → Task Entity → TaskRepository → Database
       ↓                  ↓                     ↓              ↓               ↓
validateMembership()  findByProjectAndUser()  setAssignee()  save()         UPDATE
       ↓                  ↓                     ↓              ↓               ↓
membership check      member lookup          assignee change JPA persist    row updated
       ↓                  ↓                     ↓              ↓               ↓
project access        entity validation      entity update   transaction    assignment done

Database → TaskAssignmentHistory → NotificationService → TaskController → Frontend
    ↓              ↓                       ↓                    ↓               ↓
update success assignment history     assignment notification response build JSON Response
    ↓              ↓                       ↓                    ↓               ↓
assignee changed  audit record           async notify         TaskResponse   HTTP 200
    ↓              ↓                       ↓                    ↓               ↓
relationship     history table          email/push           success format  client update
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/TaskServiceImpl.java**
```java
assignTask() method
↓ log.info("Assigning task {} from {} to {}", taskId, currentAssigneeId, newAssigneeId)
↓ Task task = taskRepository.findById(taskId).orElseThrow(...)
↓ validateTaskAssignmentPermissions(userId, task) → PM/Creator/Current assignee only
↓ User newAssignee = userRepository.findById(request.assigneeId).orElseThrow(...)
↓ validateProjectMembership(newAssignee.getId(), task.getProject().getId())
↓ User oldAssignee = task.getAssignee()
↓ task.setAssignee(newAssignee)
↓ task.setUpdatedBy(userId)
↓ task.setUpdatedAt(LocalDateTime.now())
↓ Task savedTask = taskRepository.save(task)
↓ taskAssignmentHistoryService.recordAssignmentChange(taskId, oldAssignee, newAssignee, userId, request.reason)
↓ if (oldAssignee != null) 
    notificationService.sendTaskUnassignmentNotification(oldAssignee, savedTask)
↓ notificationService.sendTaskAssignmentNotification(newAssignee, savedTask)
↓ return savedTask
```

---

## 🎯 **SENARYO 4: TASK FILTERING & SEARCH - Görev Arama**

### **📥 Frontend Request**
```json
GET /api/v1/tasks/search?projectId=550e8400-e29b-41d4-a716-446655440003&status=IN_PROGRESS&assigneeId=550e8400-e29b-41d4-a716-446655440004&priority=HIGH&dueDate=2024-02-15&q=authentication
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → TaskController → TaskServiceImpl → TaskRepository → PostgreSQL
    ↓           ↓                ↓                ↓               ↓
GET params  @GetMapping      searchTasks()    findTasksByCriteria() COMPLEX query
    ↓           ↓                ↓                ↓               ↓
filter params parameter extract search criteria JPA Criteria API WHERE clauses
    ↓           ↓                ↓                ↓               ↓
multiple filters validation    filter building  dynamic query   multiple conditions

PostgreSQL → TaskRepository → TaskServiceImpl → TaskController → Frontend
     ↓              ↓                ↓               ↓               ↓
filtered results List<Task>      result processing response build JSON Response
     ↓              ↓                ↓               ↓               ↓
JOIN operations  entity mapping   pagination       SearchResponse  HTTP 200
     ↓              ↓                ↓               ↓               ↓
ORDER BY        JPA results      metadata         page info       client display
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Repository/TaskRepository.java**
```java
@Query("""
    SELECT DISTINCT t FROM Task t 
    LEFT JOIN FETCH t.assignee 
    LEFT JOIN FETCH t.project 
    WHERE (:projectId IS NULL OR t.project.id = :projectId)
    AND (:status IS NULL OR t.status = :status)
    AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)
    AND (:priority IS NULL OR t.priority = :priority)
    AND (:dueDate IS NULL OR t.dueDate <= :dueDate)
    AND (:searchTerm IS NULL OR 
         LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
         LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    ORDER BY t.priority DESC, t.dueDate ASC, t.createdAt DESC
""")
↓ Page<Task> findTasksByCriteria(parameters, Pageable pageable)
↓ Dynamic query with multiple optional filters
↓ PostgreSQL: complex WHERE clause with NULL checks
↓ LIKE operations for text search
↓ Multiple JOIN FETCH for eager loading
↓ Pagination and sorting support
```

---

## 🎯 **SENARYO 5: TASK DEPENDENCIES - Görev Bağımlılıkları**

### **📥 Frontend Request**
```json
POST /api/v1/tasks/550e8400-e29b-41d4-a716-446655440005/dependencies
{
  "dependentTaskId": "550e8400-e29b-41d4-a716-446655440007",
  "dependencyType": "BLOCKS"
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → TaskController → TaskServiceImpl → TaskRepository → PostgreSQL
    ↓           ↓                ↓                ↓               ↓
DependencyReq @PostMapping   addDependency()   findById()      SELECT queries
    ↓           ↓                ↓                ↓               ↓
JSON Body   path variable    dependency logic  task lookup     tasks table
    ↓           ↓                ↓                ↓               ↓
dependency data taskId extract validation      entity fetch    row data

TaskServiceImpl → Cycle Detection → TaskDependency Entity → TaskDependencyRepository
       ↓                ↓                    ↓                        ↓
validateDependency() detectCycle()      Builder.build()            save()
       ↓                ↓                    ↓                        ↓
business rules      graph traversal     dependency creation        JPA persist
       ↓                ↓                    ↓                        ↓
same project        DFS algorithm       relationship setup         transaction

Database → Task Status Update → NotificationService → TaskController → Frontend
    ↓              ↓                    ↓                    ↓               ↓
dependency added status check         dependency notification response build JSON Response
    ↓              ↓                    ↓                    ↓               ↓
relationship    blocked status        async notify         DependencyResp  HTTP 201
    ↓              ↓                    ↓                    ↓               ↓
constraint added auto blocking        team notification    success format  client update
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/TaskServiceImpl.java**
```java
addDependency() method
↓ log.info("Adding dependency: task {} {} task {}", taskId, dependencyType, dependentTaskId)
↓ Task task = taskRepository.findById(taskId).orElseThrow(...)
↓ Task dependentTask = taskRepository.findById(dependentTaskId).orElseThrow(...)
↓ validateDependencyCreation(task, dependentTask)
↓ if (wouldCreateCycle(task, dependentTask, dependencyType))
    throw new CircularDependencyException("Dependency would create a cycle")
↓ TaskDependency dependency = TaskDependency.builder()
    .task(task).dependentTask(dependentTask)
    .dependencyType(DependencyType.valueOf(dependencyType))
    .createdBy(userId).createdAt(LocalDateTime.now()).build()
↓ TaskDependency savedDependency = taskDependencyRepository.save(dependency)
↓ if (dependencyType.equals("BLOCKS") && !dependentTask.getStatus().equals(TaskStatus.BLOCKED))
    updateTaskStatus(dependentTaskId, "BLOCKED", "Blocked by dependency", userId)
↓ return savedDependency
```

**2️⃣ Cycle Detection Algorithm**
```java
wouldCreateCycle() method
↓ Set<UUID> visited = new HashSet<>()
↓ Set<UUID> recursionStack = new HashSet<>()
↓ return hasCycleDFS(dependentTask.getId(), task.getId(), visited, recursionStack)

hasCycleDFS() method
↓ visited.add(currentTaskId)
↓ recursionStack.add(currentTaskId)
↓ List<TaskDependency> dependencies = taskDependencyRepository.findByTaskId(currentTaskId)
↓ for (TaskDependency dep : dependencies) {
    UUID nextTaskId = dep.getDependentTask().getId()
    if (nextTaskId.equals(targetTaskId)) return true // Cycle found
    if (!visited.contains(nextTaskId) && hasCycleDFS(nextTaskId, targetTaskId, visited, recursionStack))
      return true
  }
↓ recursionStack.remove(currentTaskId)
↓ return false
```

Bu Tasks modülü, görev yaşam döngüsünü, bağımlılıkları ve iş akışlarını yöneten kapsamlı bir görev yönetim sistemidir.
