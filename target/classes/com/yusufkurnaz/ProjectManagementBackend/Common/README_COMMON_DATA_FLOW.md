# 🏗️ COMMON MODULE - SENARYO BAZLI VERİ AKIŞ DOKÜMANTASYONU

## 📁 **KLASÖR YAPISI**
```
Common/
├── Controller/     ← Test endpoints
├── Service/        ← Shared services
├── Repository/     ← User repository
├── Model/          ← Base entities (User, BaseEntity)
├── Config/         ← Security, JWT, CORS configs
├── Dto/           ← Shared DTOs
├── Exceptions/    ← Global exception handling
└── Util/          ← Utility classes
```

---

## 🎯 **SENARYO 1: USER LOGIN - JWT Token Alma**

### **📥 Frontend Request**
```json
POST /api/v1/auth/login
{
  "email": "user@example.com",
  "password": "password123"
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → AuthController → AuthService → UserRepository → PostgreSQL → UserRepository → AuthService
    ↓           ↓              ↓              ↓               ↓              ↓               ↓
LoginReq    @PostMapping   authenticate()  findByEmail()   SELECT query   User entity    validation
    ↓           ↓              ↓              ↓               ↓              ↓               ↓
JSON Body   @Valid check   password check  JPA query      user table     Optional<User>  password match
    ↓           ↓              ↓              ↓               ↓              ↓               ↓
Request DTO validation     BCrypt compare  database hit   row data       entity mapping  boolean result

AuthService → JwtService → TokenResponse → AuthController → ApiResponse → Frontend
    ↓            ↓             ↓              ↓               ↓             ↓
generateToken()  createJWT()   build response  success wrap   HTTP 200     JSON Response
    ↓            ↓             ↓              ↓               ↓             ↓
user details    JWT payload   access token   response DTO   standard wrap  client storage
    ↓            ↓             ↓              ↓               ↓             ↓
claims set      sign token    refresh token  metadata       success flag  localStorage
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Controller/AuthController.java**
```java
@PostMapping("/login")
↓ @Valid LoginRequest validation
↓ email format, password length checks
↓ authService.authenticate(request.email, request.password)
↓ if (user == null) throw AuthenticationException
↓ String token = jwtService.generateToken(user)
↓ TokenResponse.builder().accessToken(token).user(UserResponse.from(user)).build()
↓ ResponseEntity.ok(ApiResponse.success(response))
```

**2️⃣ Service/impl/AuthServiceImpl.java**
```java
authenticate() method
↓ log.info("Authentication attempt for: {}", email)
↓ Optional<User> userOpt = userRepository.findByEmailAndIsActiveTrue(email)
↓ if (userOpt.isEmpty()) return null
↓ User user = userOpt.get()
↓ boolean matches = passwordEncoder.matches(password, user.getPassword())
↓ if (!matches) { user.incrementFailedAttempts(); return null; }
↓ user.updateLastLogin()
↓ userRepository.save(user)
↓ return user
```

**3️⃣ Repository/UserRepository.java**
```java
@Query("SELECT u FROM User u WHERE u.email = :email AND u.isActive = true")
↓ JPA query execution
↓ PostgreSQL: SELECT * FROM users WHERE email = ? AND is_active = true
↓ ResultSet mapping to User entity
↓ Optional<User> return
```

---

## 🎯 **SENARYO 2: JWT AUTHENTICATION FILTER - Request Doğrulama**

### **📥 Her API Request'te Otomatik**
```
GET /api/v1/ai/documents
Headers: {
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIs..."
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
HTTP Request → JwtAuthenticationFilter → JwtService → UserRepository → SecurityContext → Target Controller
     ↓                  ↓                    ↓             ↓                ↓                    ↓
Bearer Token       doFilterInternal()   validateToken()  findById()      setAuthentication()  @PreAuthorize
     ↓                  ↓                    ↓             ↓                ↓                    ↓
Authorization      extract token        JWT validation   user lookup     Spring Security      method access
     ↓                  ↓                    ↓             ↓                ↓                    ↓
Header value       token parsing        signature check  database query  authentication obj   permission check
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Config/JwtAuthenticationFilter.java**
```java
doFilterInternal() method
↓ String authHeader = request.getHeader("Authorization")
↓ if (authHeader == null || !authHeader.startsWith("Bearer ")) { chain.doFilter(); return; }
↓ String token = authHeader.substring(7)
↓ String userEmail = jwtService.extractUsername(token)
↓ if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null)
↓ User user = userRepository.findByEmail(userEmail).orElse(null)
↓ if (jwtService.isTokenValid(token, user))
↓ UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
↓ SecurityContextHolder.getContext().setAuthentication(authToken)
↓ filterChain.doFilter(request, response)
```

**2️⃣ Service/JwtService.java**
```java
validateToken() method
↓ try { Claims claims = Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token).getBody(); }
↓ String username = claims.getSubject()
↓ Date expiration = claims.getExpiration()
↓ return username.equals(user.getEmail()) && !isTokenExpired(expiration)
```

---

## 🎯 **SENARYO 3: GLOBAL EXCEPTION HANDLING - Hata Yönetimi**

### **📥 Herhangi Bir Hata Durumu**
```java
// Herhangi bir Controller'da exception fırlatıldığında
throw new RuntimeException("Document not found: " + documentId);
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Any Controller → Exception Thrown → GlobalExceptionHandler → ApiResponse → Frontend
      ↓               ↓                      ↓                   ↓            ↓
Business Logic    RuntimeException    @ExceptionHandler       error format   JSON Response
      ↓               ↓                      ↓                   ↓            ↓
Service call      error condition     catch exception        error message   HTTP 400/500
      ↓               ↓                      ↓                   ↓            ↓
Database op       validation fail     log error details      standard wrap   client display
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Exceptions/GlobalExceptionHandler.java**
```java
@ExceptionHandler(RuntimeException.class)
↓ log.error("Runtime exception occurred: {}", ex.getMessage(), ex)
↓ String errorMessage = ex.getMessage()
↓ ApiResponse<Object> response = ApiResponse.error(errorMessage)
↓ return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)

@ExceptionHandler(ValidationException.class)
↓ log.warn("Validation exception: {}", ex.getMessage())
↓ return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()))

@ExceptionHandler(AccessDeniedException.class)
↓ log.warn("Access denied: {}", ex.getMessage())
↓ return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied"))
```

---

## 🎯 **SENARYO 4: CORS CONFIGURATION - Cross-Origin İstekleri**

### **📥 Frontend'den Cross-Origin Request**
```javascript
fetch('http://localhost:8080/api/v1/ai/documents', {
  method: 'GET',
  headers: {
    'Origin': 'http://localhost:3000',
    'Authorization': 'Bearer token...'
  }
});
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Browser Preflight → CorsConfiguration → CorsFilter → Target Controller → Response → Browser
       ↓                 ↓                ↓              ↓                ↓           ↓
OPTIONS request      allowed origins   filter check   actual request   add headers  CORS success
       ↓                 ↓                ↓              ↓                ↓           ↓
CORS headers         configuration     validation     method execution  CORS headers client access
       ↓                 ↓                ↓              ↓                ↓           ↓
Origin check         whitelist check   pass/reject    business logic   response     no CORS error
```

### **🔍 Detaylý Sınıf İçi İşlemler**

**1️⃣ Config/CorsConfiguration.java**
```java
@Bean CorsConfigurationSource corsConfigurationSource()
↓ CorsConfiguration configuration = new CorsConfiguration()
↓ configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:3000", "https://myapp.com"))
↓ configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"))
↓ configuration.setAllowedHeaders(Arrays.asList("*"))
↓ configuration.setAllowCredentials(true)
↓ UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource()
↓ source.registerCorsConfiguration("/**", configuration)
↓ return source
```

---

## 🎯 **SENARYO 5: BASE ENTITY AUDIT - Otomatik Timestamp**

### **📥 Herhangi Bir Entity Kaydı**
```java
// Herhangi bir service'de entity kaydı
Document document = Document.builder().title("Test").build();
documentRepository.save(document);
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Entity Save → JPA Lifecycle → BaseEntity → AuditingEntityListener → Database
     ↓             ↓              ↓               ↓                      ↓
repository.save()  @PrePersist   audit fields    @CreatedDate          INSERT with
     ↓             ↓              ↓               ↓                      ↓
JPA operation     lifecycle hook  createdAt      current timestamp      timestamps
     ↓             ↓              ↓               ↓                      ↓
entity instance   before save    updatedAt      LocalDateTime.now()    audit trail
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Model/BaseEntity.java**
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
↓ @CreatedDate @Column(name = "created_at") private LocalDateTime createdAt
↓ @LastModifiedDate @Column(name = "updated_at") private LocalDateTime updatedAt
↓ @CreatedBy @Column(name = "created_by") private UUID createdBy
↓ @LastModifiedBy @Column(name = "updated_by") private UUID updatedBy

@PrePersist protected void onCreate()
↓ if (createdAt == null) createdAt = LocalDateTime.now()
↓ if (updatedAt == null) updatedAt = LocalDateTime.now()

@PreUpdate protected void onUpdate()
↓ updatedAt = LocalDateTime.now()
```

---

## 🔄 **COMMON MODULE BAĞIMLILIKLARI**

### **Diğer Modüllerle İlişki**
```
AI Module → Common/Model/User (createdBy, updatedBy)
AI Module → Common/Dto/ApiResponse (response wrapping)
AI Module → Common/Exceptions/GlobalExceptionHandler (error handling)

IdeaWorkspace → Common/Model/User (canvas ownership)
IdeaWorkspace → Common/Config/JwtAuthenticationFilter (authentication)

Integration → Common/Exceptions (error handling)
Integration → Common/Config/RestTemplate (HTTP clients)

Message → Common/Model/User (message senders/receivers)
Teams → Common/Model/User (team members)
Projects → Common/Model/User (project participants)
```

### **Shared Utilities Flow**
```
Any Module → Common/Util/ValidationUtil → validation logic → return boolean
Any Module → Common/Util/DateUtil → date operations → return formatted date
Any Module → Common/Util/EncryptionUtil → encrypt/decrypt → return processed data
```

Bu Common modülü tüm diğer modüllerin temel altyapısını sağlar ve cross-cutting concerns'leri yönetir.
