# 🚀 SERVICE DATA FLOW GUIDE
## Frontend'den Backend'e Veri Akış Rehberi

Bu dokümanda her servis için frontend'den gelen verinin backend'de hangi klasörlerde, hangi sınıflarda işlendiği ve nasıl response döndüğü açıklanmaktadır.

---

## 🤖 **1. RAG (Retrieval-Augmented Generation) SERVICE**

### **📥 Frontend Request**
```javascript
POST /api/v1/ai/rag/query
{
  "query": "Spring Boot nedir?",
  "domainTags": ["java", "backend"],
  "maxChunks": 5,
  "minSimilarity": 0.3
}
```

### **🔄 Backend Data Flow**

```
Frontend Request
    │
    ▼
📁 AI/Controller/RAGController.java
    │ ├─► @PostMapping("/query")
    │ ├─► Request validation (@Valid RAGQueryRequest)
    │ ├─► Authentication check (UUID userId)
    │
    ▼
📁 AI/Service/impl/RAGServiceImpl.java
    │ ├─► queryWithRAG() method
    │ ├─► Parameter normalization
    │
    ▼
📁 Integration/HuggingFace/Service/impl/EmbeddingServiceImpl.java
    │ ├─► embedText() - Query'yi vector'e çevir
    │ ├─► HuggingFace API call
    │ ├─► 384-dimensional vector return
    │
    ▼
📁 AI/Service/impl/VectorSearchServiceImpl.java
    │ ├─► findSimilarContent() method
    │ ├─► floatArrayToString() - Vector format
    │
    ▼
📁 AI/Repository/DocumentChunkRepository.java
    │ ├─► findSimilarChunks() native query
    │ ├─► PostgreSQL pgvector: ORDER BY embedding <-> query_vector
    │ ├─► Return List<DocumentChunk>
    │
    ▼
📁 AI/Service/impl/RAGServiceImpl.java
    │ ├─► rankChunksByRelevance() - Relevance scoring
    │ ├─► optimizeContextWindow() - Token limit check
    │ ├─► buildContext() - Context string creation
    │
    ▼
📁 Integration/HuggingFace/Service/impl/LLMServiceStub.java
    │ ├─► generateResponse() method
    │ ├─► Prompt engineering with context
    │ ├─► HuggingFace LLM API call
    │
    ▼
📁 AI/Dto/response/RAGQueryResponse.java
    │ ├─► Response object creation
    │ ├─► Source chunks mapping
    │ ├─► Metadata addition
    │ ├─► Confidence score calculation
    │
    ▼
📁 AI/Controller/RAGController.java
    │ ├─► ApiResponse wrapper
    │ ├─► HTTP 200 OK
    │
    ▼
Frontend Response
```

### **📤 Backend Response**
```json
{
  "success": true,
  "data": {
    "originalQuery": "Spring Boot nedir?",
    "response": "Spring Boot, Java tabanlı...",
    "confidenceScore": 0.85,
    "qualityScore": 0.78,
    "sourceChunks": [...],
    "metadata": {...}
  }
}
```

---

## 📄 **2. DOCUMENT PROCESSING SERVICE**

### **📥 Frontend Request**
```javascript
POST /api/v1/ai/documents/upload
FormData: {
  file: PDF_FILE,
  tags: ["java", "tutorial"]
}
```

### **🔄 Backend Data Flow**

```
Frontend Multipart Request
    │
    ▼
📁 AI/Controller/DocumentController.java
    │ ├─► @PostMapping("/upload")
    │ ├─► MultipartFile validation
    │ ├─► User authentication
    │
    ▼
📁 AI/Service/impl/DocumentProcessingServiceImpl.java
    │ ├─► processDocument() method
    │ ├─► File validation (size, type)
    │ ├─► Content hash generation
    │
    ▼
📁 AI/Entity/Document.java
    │ ├─► Document entity creation
    │ ├─► Metadata setting
    │ ├─► Status: PROCESSING
    │
    ▼
📁 AI/Service/impl/DocumentProcessingServiceImpl.java
    │ ├─► extractTextFromFile() - PDF text extraction
    │ ├─► Apache PDFBox integration
    │
    ▼
📁 AI/Service/impl/DocumentChunkingServiceImpl.java
    │ ├─► chunkDocument() method
    │ ├─► Smart text splitting (1000 chars, 200 overlap)
    │ ├─► Section detection
    │ ├─► Quality validation
    │
    ▼
📁 AI/Entity/DocumentChunk.java (Multiple instances)
    │ ├─► Chunk entities creation
    │ ├─► Text content storage
    │ ├─► Index and position tracking
    │
    ▼
📁 AI/Service/impl/DocumentChunkingServiceImpl.java
    │ ├─► generateEmbeddingsAsync() - @Async method
    │ ├─► Batch processing chunks
    │
    ▼
📁 Integration/HuggingFace/Service/impl/EmbeddingServiceImpl.java
    │ ├─► embedBatch() method
    │ ├─► HuggingFace API batch call
    │ ├─► 384-dim vectors for all chunks
    │
    ▼
📁 AI/Entity/DocumentChunk.java
    │ ├─► setEmbeddingFromFloatArray()
    │ ├─► Vector storage in pgvector format
    │
    ▼
📁 AI/Repository/DocumentChunkRepository.java
    │ ├─► saveAll() chunks with vectors
    │ ├─► PostgreSQL batch insert
    │
    ▼
📁 AI/Entity/Document.java
    │ ├─► completeProcessing()
    │ ├─► Status: COMPLETED
    │ ├─► Total chunks count update
    │
    ▼
📁 AI/Controller/DocumentController.java
    │ ├─► DocumentResponse creation
    │ ├─► Success response
    │
    ▼
Frontend Response
```

---

## 🔍 **3. VECTOR SEARCH SERVICE**

### **📥 Frontend Request**
```javascript
POST /api/v1/ai/search/similarity
{
  "query": "mikroservis mimarisi",
  "domainTags": ["architecture"],
  "minSimilarityScore": 0.4,
  "limit": 10
}
```

### **🔄 Backend Data Flow**

```
Frontend Request
    │
    ▼
📁 AI/Controller/SearchController.java
    │ ├─► @PostMapping("/similarity")
    │ ├─► SimilaritySearchRequest validation
    │
    ▼
📁 AI/Service/impl/VectorSearchServiceImpl.java
    │ ├─► findSimilarContent() method
    │ ├─► Query text processing
    │
    ▼
📁 Integration/HuggingFace/Service/impl/EmbeddingServiceImpl.java
    │ ├─► embedText() - Query vectorization
    │ ├─► HuggingFace embedding API
    │
    ▼
📁 AI/Service/impl/VectorSearchServiceImpl.java
    │ ├─► floatArrayToString() conversion
    │ ├─► Domain tags array preparation
    │
    ▼
📁 AI/Repository/DocumentChunkRepository.java
    │ ├─► findSimilarChunks() native query
    │ ├─► pgvector cosine similarity: <->
    │ ├─► Domain tag filtering
    │ ├─► LIMIT clause
    │
    ▼
PostgreSQL Database
    │ ├─► Vector similarity calculation
    │ ├─► Index-based fast search
    │ ├─► Sorted results by similarity
    │
    ▼
📁 AI/Service/impl/VectorSearchServiceImpl.java
    │ ├─► Results ranking
    │ ├─► Similarity score calculation
    │
    ▼
📁 AI/Controller/SearchController.java
    │ ├─► SimilaritySearchResponse creation
    │ ├─► Search metadata addition
    │
    ▼
Frontend Response
```

---

## 🎨 **4. CANVAS SERVICE**

### **📥 Frontend Request**
```javascript
POST /api/v1/canvas
{
  "name": "Project Architecture",
  "description": "System design canvas",
  "workspaceId": "uuid-workspace"
}
```

### **🔄 Backend Data Flow**

```
Frontend Request
    │
    ▼
📁 IdeaWorkspace/Controller/CanvasController.java
    │ ├─► @PostMapping("/")
    │ ├─► CreateCanvasRequest validation
    │
    ▼
📁 IdeaWorkspace/Service/impl/CanvasServiceImpl.java
    │ ├─► createCanvas() method
    │ ├─► Workspace validation
    │ ├─► User validation
    │
    ▼
📁 Workspace/Repository/WorkspaceRepository.java
    │ ├─► findById() workspace check
    │ ├─► Access validation
    │
    ▼
📁 Common/Repository/UserRepository.java
    │ ├─► findById() user check
    │
    ▼
📁 IdeaWorkspace/Entity/CanvasBoard.java
    │ ├─► Canvas entity creation
    │ ├─► Default Excalidraw data
    │ ├─► Metadata setting
    │
    ▼
📁 IdeaWorkspace/Repository/CanvasBoardRepository.java
    │ ├─► save() canvas entity
    │
    ▼
📁 IdeaWorkspace/Entity/CanvasCollaborator.java
    │ ├─► Creator collaborator entity
    │ ├─► ADMIN permission
    │ ├─► ACTIVE status
    │
    ▼
📁 IdeaWorkspace/Repository/CanvasCollaboratorRepository.java
    │ ├─► save() collaborator entity
    │
    ▼
📁 IdeaWorkspace/Service/impl/CanvasServiceImpl.java
    │ ├─► Canvas response preparation
    │
    ▼
📁 IdeaWorkspace/Controller/CanvasController.java
    │ ├─► CanvasResponse creation
    │
    ▼
Frontend Response
```

---

## 📁 **5. CANVAS FILE SERVICE**

### **📥 Frontend Request**
```javascript
POST /api/v1/canvas/{canvasId}/files
FormData: {
  file: IMAGE_FILE
}
```

### **🔄 Backend Data Flow**

```
Frontend Multipart Request
    │
    ▼
📁 IdeaWorkspace/Controller/CanvasFileController.java
    │ ├─► @PostMapping("/{canvasId}/files")
    │ ├─► Path variable validation
    │ ├─► MultipartFile validation
    │
    ▼
📁 IdeaWorkspace/Service/impl/CanvasFileServiceImpl.java
    │ ├─► uploadFile() method
    │ ├─► Canvas access validation
    │
    ▼
📁 IdeaWorkspace/Service/impl/CanvasServiceImpl.java
    │ ├─► validateCanvasAccess()
    │ ├─► User permission check
    │
    ▼
📁 IdeaWorkspace/Repository/CanvasBoardRepository.java
    │ ├─► findByIdWithAccess() query
    │
    ▼
📁 IdeaWorkspace/Service/impl/CanvasFileServiceImpl.java
    │ ├─► validateFile() - Type, size checks
    │ ├─► calculateFileHash() - SHA-256
    │ ├─► Duplicate detection
    │
    ▼
📁 IdeaWorkspace/Repository/CanvasFileRepository.java
    │ ├─► findByFileHashAndIsActiveTrue()
    │ ├─► Duplicate check
    │
    ▼
📁 IdeaWorkspace/Service/impl/CanvasFileServiceImpl.java
    │ ├─► generateUniqueFilename()
    │ ├─► saveFileToDisk() - File system
    │
    ▼
File System (/uploads/canvas/)
    │ ├─► Physical file storage
    │ ├─► UUID-based filename
    │
    ▼
📁 IdeaWorkspace/Entity/CanvasFile.java
    │ ├─► File entity creation
    │ ├─► Metadata storage
    │ ├─► URL generation
    │
    ▼
📁 IdeaWorkspace/Repository/CanvasFileRepository.java
    │ ├─► save() file entity
    │
    ▼
📁 IdeaWorkspace/Service/impl/CanvasFileServiceImpl.java
    │ ├─► Image dimension detection (if image)
    │ ├─► Response preparation
    │
    ▼
Frontend Response
```

---

## 🏷️ **6. DOCUMENT TAGGING SERVICE**

### **📥 Frontend Request**
```javascript
POST /api/v1/ai/documents/{documentId}/tags/generate
{
  "maxTags": 5,
  "useAI": true
}
```

### **🔄 Backend Data Flow**

```
Frontend Request
    │
    ▼
📁 AI/Controller/DocumentController.java
    │ ├─► @PostMapping("/{documentId}/tags/generate")
    │ ├─► Path variable validation
    │
    ▼
📁 AI/Service/impl/DocumentTaggingServiceImpl.java
    │ ├─► generateTags() method
    │ ├─► Document validation
    │
    ▼
📁 AI/Repository/DocumentRepository.java
    │ ├─► findById() document check
    │
    ▼
📁 AI/Service/impl/DocumentTaggingServiceImpl.java
    │ ├─► extractDocumentContent()
    │ ├─► Content preparation for AI
    │
    ▼
📁 Integration/HuggingFace/Service/impl/LLMServiceStub.java
    │ ├─► generateResponse() for tagging
    │ ├─► AI-powered tag extraction
    │
    ▼
📁 AI/Service/impl/DocumentTaggingServiceImpl.java
    │ ├─► parseTags() - AI response parsing
    │ ├─► validateTags() - Quality check
    │ ├─► mergeSimilarTags() - Deduplication
    │
    ▼
📁 AI/Entity/DocumentDomainTag.java (Multiple instances)
    │ ├─► Tag entities creation
    │ ├─► Confidence scores
    │ ├─► Tag categories
    │
    ▼
📁 AI/Repository/DocumentDomainTagRepository.java
    │ ├─► saveAll() tag entities
    │
    ▼
📁 AI/Service/impl/DocumentTaggingServiceImpl.java
    │ ├─► Response preparation
    │ ├─► Tag statistics
    │
    ▼
Frontend Response
```

---

## 📊 **GENEL KLASÖR YAPISI VE SORUMLULUKLAR**

### **🏗️ Architecture Layers**

```
📁 Controller Layer (REST Endpoints)
    ├─► Request validation
    ├─► Authentication check
    ├─► Response formatting
    └─► HTTP status management

📁 Service Layer (Business Logic)
    ├─► Core business rules
    ├─► Data processing
    ├─► Integration orchestration
    └─► Transaction management

📁 Repository Layer (Data Access)
    ├─► Database queries
    ├─► Entity CRUD operations
    ├─► Native SQL for complex queries
    └─► Caching strategies

📁 Entity Layer (Data Models)
    ├─► Database table mapping
    ├─► Relationship definitions
    ├─► Validation rules
    └─► Business logic methods

📁 DTO Layer (Data Transfer)
    ├─► Request/Response objects
    ├─► Data validation
    ├─► Serialization rules
    └─► API documentation

📁 Integration Layer (External APIs)
    ├─► HuggingFace API calls
    ├─► PlantUML integration
    ├─► GitHub API integration
    └─► Error handling
```

### **🔄 Common Data Flow Pattern**

```
1. Frontend Request
   ↓
2. Controller (Validation + Auth)
   ↓
3. Service (Business Logic)
   ↓
4. Repository (Database/External API)
   ↓
5. Entity/Response Creation
   ↓
6. Controller Response Formatting
   ↓
7. Frontend Response
```

Bu rehber her serviste verinin nasıl aktığını, hangi sınıfların sorumlu olduğunu ve sequence'ı göstermektedir. Her modül kendi sorumluluğunu yerine getirerek SOLID prensiplerine uygun bir yapı oluşturmaktadır.
