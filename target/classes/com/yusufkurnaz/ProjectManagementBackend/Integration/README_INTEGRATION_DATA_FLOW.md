# 🔗 INTEGRATION MODULE - SENARYO BAZLI VERİ AKIŞ DOKÜMANTASYONU

## 📁 **KLASÖR YAPISI**
```
Integration/
├── HuggingFace/        ← AI model integrations
├── PlantUML/          ← Diagram generation
├── GitHub/            ← Git repository integration
├── Controller/        ← Integration endpoints
├── Service/           ← Integration services
├── Repository/        ← Integration data
├── Entity/            ← Integration entities
├── Config/            ← Integration configurations
├── Dto/              ← Integration DTOs
└── Exception/         ← Integration exceptions
```

---

## 🎯 **SENARYO 1: HUGGINGFACE EMBEDDING - Text'i Vector'e Çevirme**

### **📥 Internal Service Call**
```java
// AI modülünden çağrı
embeddingService.embedText("Spring Boot mikroservis mimarisinde nasıl kullanılır?");
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
AI Module → EmbeddingService → HuggingFaceConfig → RestTemplate → HuggingFace API
    ↓            ↓                   ↓                ↓              ↓
embedText()  Turkish text      API configuration  HTTP Client    POST request
    ↓            ↓                   ↓                ↓              ↓
method call  input validation   API key/URL       request build  feature-extraction
    ↓            ↓                   ↓                ↓              ↓
String param text processing    config values     headers set    all-MiniLM-L6-v2

HuggingFace API → HTTP Response → RestTemplate → EmbeddingService → AI Module
       ↓               ↓             ↓               ↓               ↓
   JSON array      response body   response parse  float[] array   vector data
       ↓               ↓             ↓               ↓               ↓
   384 dimensions  HTTP 200        JSON to Object  embedding       return value
       ↓               ↓             ↓               ↓               ↓
   float values    success status  deserialization processed data  business logic
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ HuggingFace/Service/impl/EmbeddingServiceImpl.java**
```java
embedText() method
↓ if (text == null || text.trim().isEmpty()) return new float[384] // Zero vector
↓ log.debug("Generating embedding for text: '{}'", text.substring(0, min(50)))
↓ String url = apiUrl + "/pipeline/feature-extraction/" + embeddingModel
↓ HttpHeaders headers = new HttpHeaders()
↓ headers.setContentType(MediaType.APPLICATION_JSON)
↓ headers.setBearerAuth(apiKey)
↓ Map<String, Object> requestBody = Map.of("inputs", text, "options", Map.of("wait_for_model", true))
↓ HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers)
↓ ResponseEntity<float[][]> response = restTemplate.exchange(url, HttpMethod.POST, request, float[][].class)
↓ if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null)
↓ return response.getBody()[0] // First embedding vector
```

**2️⃣ HuggingFace/Config/HuggingFaceConfig.java**
```java
Configuration properties
↓ @Value("${app.huggingface.api-key}") private String apiKey
↓ @Value("${app.huggingface.api-url}") private String apiUrl  
↓ @Value("${app.huggingface.embedding-model}") private String embeddingModel
↓ @Value("${app.huggingface.timeout}") private Long timeout
↓ @Value("${app.huggingface.max-retries}") private Integer maxRetries

@Bean RestTemplate restTemplate()
↓ RestTemplate template = new RestTemplate()
↓ template.setRequestFactory(new HttpComponentsClientHttpRequestFactory())
↓ template.getInterceptors().add(new LoggingInterceptor())
↓ return template
```

---

## 🎯 **SENARYO 2: HUGGINGFACE LLM - AI Cevap Üretme**

### **📥 Internal Service Call**
```java
// RAG servisinden çağrı
llmService.generateResponse("Context: Spring Boot is... \nQuestion: What is Spring Boot?");
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
RAG Service → LLMService → HuggingFaceConfig → RestTemplate → HuggingFace API
     ↓           ↓             ↓                ↓              ↓
generateResponse() prompt      API config      HTTP Client    POST request
     ↓           ↓             ↓                ↓              ↓
context+question prompt eng.   model settings  request build  text-generation
     ↓           ↓             ↓                ↓              ↓
String input    template apply config values   headers set    Llama-2-7b-chat

HuggingFace API → HTTP Response → RestTemplate → LLMService → RAG Service
       ↓               ↓             ↓             ↓            ↓
   JSON response   response body   response parse AI response  generated text
       ↓               ↓             ↓             ↓            ↓
   generated text  HTTP 200        JSON to String text extract return value
       ↓               ↓             ↓             ↓            ↓
   AI answer       success status  deserialization clean text  business logic
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ HuggingFace/Service/impl/LLMServiceStub.java**
```java
generateResponse() method
↓ log.info("Generating LLM response for prompt length: {}", prompt.length())
↓ String enhancedPrompt = buildPromptTemplate(prompt)
↓ Map<String, Object> requestBody = Map.of(
    "inputs", enhancedPrompt,
    "parameters", Map.of(
        "max_length", maxTokens,
        "temperature", temperature,
        "do_sample", true
    ))
↓ HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers)
↓ ResponseEntity<String> response = restTemplate.exchange(llmUrl, HttpMethod.POST, request, String.class)
↓ return extractTextFromResponse(response.getBody())
```

**2️⃣ Prompt Engineering**
```java
buildPromptTemplate() method
↓ return String.format("""
    <s>[INST] <<SYS>>
    Sen yardımcı bir AI asistanısın. Türkçe ve doğru cevaplar ver.
    <</SYS>>
    
    %s [/INST]
    """, prompt)
```

---

## 🎯 **SENARYO 3: PLANTUML DIAGRAM - Class Diagram Oluşturma**

### **📥 Internal Service Call**
```java
// RAG servisinden çağrı
diagramService.generateDiagram("User class with fields: id, name, email", "class");
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
RAG Service → DiagramGenerationService → PlantUMLConfig → PlantUML Library → Diagram
     ↓              ↓                      ↓               ↓                 ↓
generateDiagram()  class diagram         config values   PlantUML engine   SVG/PNG
     ↓              ↓                      ↓               ↓                 ↓
text description   parse requirements    library config  UML processing    image data
     ↓              ↓                      ↓               ↓                 ↓
String input       diagram type detect   settings        code generation   binary output

PlantUML → File System → DiagramGenerationService → RAG Service
    ↓          ↓               ↓                      ↓
image file  save to disk   return URL             diagram info
    ↓          ↓               ↓                      ↓
PNG/SVG     /uploads/diagrams/ file path           response DTO
    ↓          ↓               ↓                      ↓
binary data physical storage  URL generation       client access
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ PlantUML/Service/impl/DiagramGenerationServiceImpl.java**
```java
generateDiagram() method
↓ log.info("Generating {} diagram from description: {}", diagramType, description)
↓ String plantUMLCode = convertToPlantUMLCode(description, diagramType)
↓ if (plantUMLCode == null) return null
↓ String diagramUrl = generateDiagramImage(plantUMLCode)
↓ saveDiagramToDatabase(plantUMLCode, diagramUrl, diagramType)
↓ return plantUMLCode

convertToPlantUMLCode() method
↓ switch (diagramType) {
    case "class": return generateClassDiagram(description)
    case "sequence": return generateSequenceDiagram(description)
    case "component": return generateComponentDiagram(description)
  }

generateClassDiagram() method  
↓ StringBuilder uml = new StringBuilder("@startuml\n")
↓ // Parse description for classes, fields, methods
↓ Pattern classPattern = Pattern.compile("(\\w+)\\s+class")
↓ // Extract class information and relationships
↓ uml.append("class User {\n  +Long id\n  +String name\n  +String email\n}\n")
↓ uml.append("@enduml")
↓ return uml.toString()
```

**2️⃣ PlantUML Image Generation**
```java
generateDiagramImage() method
↓ SourceStringReader reader = new SourceStringReader(plantUMLCode)
↓ ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
↓ reader.outputImage(outputStream, new FileFormatOption(FileFormat.PNG))
↓ byte[] imageBytes = outputStream.toByteArray()
↓ String filename = UUID.randomUUID() + ".png"
↓ Path imagePath = Paths.get(uploadDir, filename)
↓ Files.write(imagePath, imageBytes)
↓ return "/api/v1/diagrams/" + filename
```

---

## 🎯 **SENARYO 4: GITHUB INTEGRATION - Repository Analizi**

### **📥 Frontend Request**
```json
POST /api/v1/integration/github/analyze
{
  "repositoryUrl": "https://github.com/spring-projects/spring-boot",
  "branch": "main",
  "analysisType": "structure"
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → GitHubController → GitHubAnalysisService → GitHubConfig → GitHub API
    ↓           ↓                    ↓                    ↓             ↓
AnalyzeReq  @PostMapping        analyzeRepository()   API config    GET requests
    ↓           ↓                    ↓                    ↓             ↓
JSON Body   @Valid check        URL validation       token/URL     repository data
    ↓           ↓                    ↓                    ↓             ↓
repo URL    parameter extract   permission check     auth headers  file structure

GitHub API → RestTemplate → GitHubAnalysisService → Repository Analysis → Database
     ↓            ↓               ↓                       ↓                ↓
JSON response HTTP client    process response       analyze structure   save results
     ↓            ↓               ↓                       ↓                ↓
repo metadata response parse   extract file tree     detect patterns     INSERT
     ↓            ↓               ↓                       ↓                ↓
files/dirs   deserialization   build hierarchy       tech stack detect  analysis data

Database → GitHubAnalysisService → GitHubController → Frontend
    ↓              ↓                      ↓              ↓
saved analysis  return results       response build   JSON Response
    ↓              ↓                      ↓              ↓
entity created  AnalysisResponse    success wrap     HTTP 200
    ↓              ↓                      ↓              ↓
ID generated    analysis details    ApiResponse      client display
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ GitHub/Service/impl/GitHubAnalysisServiceImpl.java**
```java
analyzeRepository() method
↓ log.info("Analyzing GitHub repository: {}", repositoryUrl)
↓ validateRepositoryUrl(repositoryUrl)
↓ String[] parts = extractOwnerAndRepo(repositoryUrl) → ["spring-projects", "spring-boot"]
↓ GitHubRepositoryInfo repoInfo = fetchRepositoryInfo(parts[0], parts[1])
↓ List<GitHubFileInfo> fileStructure = fetchFileStructure(parts[0], parts[1], branch)
↓ TechStackAnalysis techStack = analyzeTechStack(fileStructure)
↓ ProjectStructureAnalysis structure = analyzeProjectStructure(fileStructure)
↓ GitHubAnalysis analysis = GitHubAnalysis.builder()
    .repositoryUrl(repositoryUrl).repoInfo(repoInfo).techStack(techStack)
    .structure(structure).analysisType(analysisType).build()
↓ return gitHubAnalysisRepository.save(analysis)
```

**2️⃣ GitHub API Calls**
```java
fetchRepositoryInfo() method
↓ String url = githubApiUrl + "/repos/" + owner + "/" + repo
↓ HttpHeaders headers = new HttpHeaders()
↓ headers.setBearerAuth(githubToken)
↓ HttpEntity<?> entity = new HttpEntity<>(headers)
↓ ResponseEntity<GitHubRepositoryInfo> response = restTemplate.exchange(url, HttpMethod.GET, entity, GitHubRepositoryInfo.class)
↓ return response.getBody()

fetchFileStructure() method
↓ String url = githubApiUrl + "/repos/" + owner + "/" + repo + "/git/trees/" + branch + "?recursive=1"
↓ ResponseEntity<GitHubTreeResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, GitHubTreeResponse.class)
↓ return response.getBody().getTree().stream()
    .map(this::convertToFileInfo)
    .collect(Collectors.toList())
```

---

## 🎯 **SENARYO 5: INTEGRATION ERROR HANDLING - API Hata Yönetimi**

### **📥 HuggingFace API Hatası**
```java
// HuggingFace API'den 503 Service Unavailable
RestClientException: 503 Service Unavailable
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
HuggingFace API → RestTemplate → EmbeddingService → Integration Exception → Global Handler
       ↓              ↓              ↓                     ↓                     ↓
503 Error       HTTP Exception   catch block         throw custom          @ExceptionHandler
       ↓              ↓              ↓                     ↓                     ↓
Service down    RestClientException error handling    IntegrationException  handle exception
       ↓              ↓              ↓                     ↓                     ↓
API timeout     exception details   log error         custom message        error response

Global Handler → Retry Mechanism → Circuit Breaker → Fallback → Client Response
      ↓               ↓                  ↓             ↓            ↓
error format      @Retryable        @CircuitBreaker  fallback     JSON error
      ↓               ↓                  ↓             ↓            ↓
ApiResponse.error retry 3 times     circuit open    zero vector  HTTP 503
      ↓               ↓                  ↓             ↓            ↓
standard format   exponential delay  fail fast      default data client handling
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Exception/IntegrationExceptionHandler.java**
```java
@ExceptionHandler(RestClientException.class)
↓ log.error("REST client exception in integration: {}", ex.getMessage(), ex)
↓ String errorMessage = "External service temporarily unavailable"
↓ if (ex.getMessage().contains("timeout")) errorMessage = "Service timeout occurred"
↓ if (ex.getMessage().contains("401")) errorMessage = "Authentication failed"
↓ return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
    .body(ApiResponse.error(errorMessage))

@ExceptionHandler(HuggingFaceException.class)  
↓ log.warn("HuggingFace service error: {}", ex.getMessage())
↓ return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
    .body(ApiResponse.error("AI service error: " + ex.getMessage()))
```

**2️⃣ Retry & Circuit Breaker**
```java
@Retryable(value = {RestClientException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
public float[] embedTextWithRetry(String text) {
↓ log.debug("Attempting embedding generation (with retry)")
↓ return embedText(text)
}

@CircuitBreaker(name = "huggingface", fallbackMethod = "fallbackEmbedding")
public float[] embedTextWithCircuitBreaker(String text) {
↓ return embedTextWithRetry(text)
}

public float[] fallbackEmbedding(String text, Exception ex) {
↓ log.warn("Using fallback embedding for text: {}", ex.getMessage())
↓ return new float[384] // Zero vector fallback
}
```

---

## 🔄 **INTEGRATION MODULE BAĞIMLILIKLARI**

### **Diğer Modüllerle İlişki**
```
AI Module → HuggingFace/EmbeddingService (vector generation)
AI Module → HuggingFace/LLMService (text generation)
AI Module → PlantUML/DiagramService (diagram creation)

Projects Module → GitHub/AnalysisService (repo analysis)
Teams Module → GitHub/RepositoryService (team repos)

All Modules → Integration/Exception handling (error management)
All Modules → Integration/Config (external API configs)
```

### **External API Dependencies**
```
HuggingFace API (api-inference.huggingface.co)
├── Embedding Models: all-MiniLM-L6-v2
├── LLM Models: Llama-2-7b-chat
└── Authentication: Bearer token

GitHub API (api.github.com)  
├── Repository data
├── File structure
└── Authentication: Personal access token

PlantUML Engine (Local library)
├── Diagram generation
├── Multiple formats (PNG, SVG)
└── No external dependency
```

Bu Integration modülü, dış servislerle güvenli ve güvenilir entegrasyon sağlar, hata durumlarını yönetir ve fallback mekanizmaları sunar.
