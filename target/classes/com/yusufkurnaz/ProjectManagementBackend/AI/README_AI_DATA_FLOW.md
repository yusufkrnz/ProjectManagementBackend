# 🤖 AI MODULE - SENARYO BAZLI VERİ AKIŞ DOKÜMANTASYONU

## 📁 **KLASÖR YAPISI**
```
AI/
├── Controller/     ← REST API endpoints
├── Service/        ← Business logic
├── Repository/     ← Database access
├── Entity/         ← JPA entities
├── Config/         ← Configuration classes
└── Dto/           ← Data transfer objects
    ├── request/    ← Request DTOs
    └── response/   ← Response DTOs
```

---

## 🎯 **SENARYO 1: RAG QUERY - "Spring Boot nedir?" Sorusu**

### **📥 Frontend Request**
```json
POST /api/v1/ai/rag/query
{
  "query": "Spring Boot nedir?",
  "domainTags": ["java", "backend"],
  "maxChunks": 5,
  "minSimilarity": 0.3
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → RAGController → RAGServiceImpl → EmbeddingService → VectorSearchService → DocumentChunkRepository → PostgreSQL
    ↓           ↓              ↓               ↓                 ↓                    ↓                      ↓
RAGQueryReq  Validation   queryWithRAG()  embedText()    findSimilarContent()  findSimilarChunks()   pgvector query
    ↓           ↓              ↓               ↓                 ↓                    ↓                      ↓
JSON Body   @Valid check  Parameter norm. HuggingFace API  Vector conversion    Native SQL            cosine similarity
    ↓           ↓              ↓               ↓                 ↓                    ↓                      ↓
Request DTO userId extract Long startTime  float[] vector   floatArrayToString() ORDER BY <->         List<DocumentChunk>
                            
← ← ← ← ← ← ← ← ← ← ← ← ← ← ← GERİ DÖNÜŞ ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ←

PostgreSQL → DocumentChunk → VectorSearchService → RAGServiceImpl → LLMService → RAGServiceImpl → RAGController → Frontend
    ↓             ↓               ↓                     ↓              ↓              ↓               ↓             ↓
Chunk data    Entity mapping   rankByRelevance()   buildContext()  generateResp() Response build  ApiResponse   JSON Response
    ↓             ↓               ↓                     ↓              ↓              ↓               ↓             ↓
Raw DB rows   DocumentChunk   Similarity scoring   Context string  AI cevabı      RAGQueryResp   HTTP 200      Client UI
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Controller/RAGController.java**
```java
@PostMapping("/query") 
↓ @Valid RAGQueryRequest validation
↓ Authentication.getName() → UUID userId  
↓ ragService.queryWithRAG(request.query, userId, request.domainTags, ...)
↓ try-catch error handling
↓ ResponseEntity.ok(ApiResponse.success(response))
```

**2️⃣ Service/impl/RAGServiceImpl.java**
```java
queryWithRAG() method
↓ long startTime = System.currentTimeMillis()
↓ maxChunks = maxChunks != null ? maxChunks : defaultMaxChunks
↓ vectorSearchService.findSimilarContent(query, domainTags, minSimilarity, maxChunks * 2)
↓ if (relevantChunks.isEmpty()) return error
↓ rankChunksByRelevance(query, relevantChunks)
↓ optimizeContextWindow(rankedChunks, maxContextTokens)
↓ buildContext(optimizedChunks) → String context
↓ generateLLMResponse(query, context)
↓ RAGQueryResponse.success(query, llmResponse, optimizedChunks, responseTime, metadata)
```

**3️⃣ Service/impl/VectorSearchServiceImpl.java**
```java
findSimilarContent() method
↓ float[] queryEmbedding = embeddingService.embedText(queryText)
↓ String embeddingString = floatArrayToString(queryEmbedding)
↓ String[] domainTagsArray = domainTags.toArray()
↓ documentChunkRepository.findSimilarChunks(embeddingString, domainTagsArray, limit)
↓ return List<DocumentChunk>
```

**4️⃣ Repository/DocumentChunkRepository.java**
```java
@Query(value = """
    SELECT dc.* FROM document_chunks dc
    INNER JOIN ai_documents d ON dc.document_id = d.id
    WHERE dc.embedding IS NOT NULL 
    AND d.is_active = true
    ORDER BY dc.embedding <-> CAST(:queryEmbedding AS vector) 
    LIMIT :limit
""")
↓ PostgreSQL pgvector extension
↓ Cosine similarity calculation: embedding <-> query_vector
↓ Return sorted List<DocumentChunk>
```

---

## 🎯 **SENARYO 2: PDF UPLOAD - "Spring Boot Tutorial.pdf" Yükleme**

### **📥 Frontend Request**
```javascript
POST /api/v1/ai/documents/upload
FormData: {
  file: "Spring Boot Tutorial.pdf" (2.5MB),
  tags: ["java", "tutorial", "backend"]
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → DocumentController → DocumentProcessingService → Document Entity → DocumentRepository → Database
    ↓            ↓                      ↓                        ↓               ↓                  ↓
Multipart    @PostMapping         processDocument()        Builder.build()   save()            INSERT
Request      /upload              validateFile()           metadata set      transaction       ai_documents
    ↓            ↓                      ↓                        ↓               ↓                  ↓
PDF File     MultipartFile        extractTextFromFile()    Status.PROCESSING  JPA persist      row created
    ↓            ↓                      ↓                        ↓               ↓                  ↓
Binary Data  validation           Apache PDFBox            Document entity    flush()           ID generated

↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ASYNC CHUNKING BAŞLAR ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓

DocumentProcessingService → DocumentChunkingService → DocumentChunk Entities → DocumentChunkRepository → Database
         ↓                         ↓                        ↓                         ↓                    ↓
    chunkDocument()           chunkText()              Builder.build() x N        saveAll()           BATCH INSERT
         ↓                         ↓                        ↓                         ↓                    ↓
    extracted text           smart splitting           chunk entities           batch operation      document_chunks
         ↓                         ↓                        ↓                         ↓                    ↓
    "Spring Boot is..."      1000 char chunks         List<DocumentChunk>       JPA batch           multiple rows

↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ASYNC EMBEDDING BAŞLAR ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓ ↓

DocumentChunkingService → EmbeddingService → HuggingFace API → EmbeddingService → DocumentChunk → Database
         ↓                      ↓                ↓                    ↓               ↓            ↓
@Async method            embedBatch()      POST request        response array    setEmbedding()   UPDATE
         ↓                      ↓                ↓                    ↓               ↓            ↓
generateEmbeddingsAsync()  List<String>    feature-extraction   float[][] arrays  vector data   embedding column
         ↓                      ↓                ↓                    ↓               ↓            ↓
batch processing         chunk texts      all-MiniLM-L6-v2     384-dim vectors   pgvector      ready for RAG
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Controller/DocumentController.java**
```java
@PostMapping("/upload")
↓ @RequestParam MultipartFile file validation
↓ Authentication.getName() → UUID userId
↓ List<String> userTags = Arrays.asList(tags.split(","))
↓ documentProcessingService.processDocument(file, userId, userTags)
↓ DocumentResponse.fromEntity(savedDocument)
↓ ResponseEntity.ok(ApiResponse.success(response))
```

**2️⃣ Service/impl/DocumentProcessingServiceImpl.java**
```java
processDocument() method
↓ log.info("Processing document: {} for user: {}", filename, userId)
↓ validateFile(file) → size, type, content checks
↓ Document.builder().originalFilename().storedFilename().filePath()...build()
↓ userTags.forEach(document::addUserTag)
↓ document.startProcessing() → Status.PROCESSING
↓ extractTextFromFile(file) → Apache PDFBox
↓ document.setExtractedText(extractedText)
↓ chunkDocument(document, extractedText) → DocumentChunkingService
↓ document.completeProcessing() → Status.COMPLETED
↓ documentRepository.save(document)
```

**3️⃣ Service/impl/DocumentChunkingServiceImpl.java**
```java
chunkDocument() method
↓ log.info("Chunking document: {}", documentId)
↓ deleteDocumentChunks(documentId) → cleanup existing
↓ int chunkSize = getOptimalChunkSize(document)
↓ chunkText(extractedText, chunkSize, defaultOverlapSize)
↓ for (chunk : chunks) { chunk.setDocument(document); chunk.setChunkIndex(i); }
↓ extractSectionTitles(chunks)
↓ List<DocumentChunk> savedChunks = chunkRepository.saveAll(chunks)
↓ document.setTotalChunks(savedChunks.size())
↓ generateEmbeddingsAsync(savedChunks) → @Async method
```

**4️⃣ @Async generateEmbeddingsAsync()**
```java
@Async("embeddingTaskExecutor")
↓ log.info("Starting async embedding generation for {} chunks", chunks.size())
↓ List<String> texts = chunks.stream().map(DocumentChunk::getChunkText).toList()
↓ List<float[]> embeddings = embeddingService.embedBatch(texts)
↓ for (int i = 0; i < chunks.size(); i++) {
    chunk.setEmbeddingFromFloatArray(embeddings.get(i));
    chunkRepository.save(chunk);
  }
↓ log.info("Completed async embedding generation")
```

---

## 🎯 **SENARYO 3: VECTOR SEARCH - "mikroservis mimarisi" Arama**

### **📥 Frontend Request**
```json
POST /api/v1/ai/search/similarity
{
  "query": "mikroservis mimarisi",
  "domainTags": ["architecture", "java"],
  "minSimilarityScore": 0.4,
  "limit": 10
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → SearchController → VectorSearchService → EmbeddingService → HuggingFace → EmbeddingService
    ↓           ↓                    ↓                   ↓                ↓              ↓
JSON Req    @PostMapping       findSimilarContent()   embedText()      API Call      float[] array
    ↓           ↓                    ↓                   ↓                ↓              ↓
SimilarityReq  validation       query processing      "mikroservis..."  POST          384 dimensions
    ↓           ↓                    ↓                   ↓                ↓              ↓
Request DTO    userId extract    parameter prep       Turkish text     embedding      query vector

VectorSearchService → DocumentChunkRepository → PostgreSQL → DocumentChunkRepository → VectorSearchService
        ↓                      ↓                   ↓                   ↓                      ↓
floatArrayToString()      findSimilarChunks()   pgvector query    ResultSet mapping      List<DocumentChunk>
        ↓                      ↓                   ↓                   ↓                      ↓
"[0.1,0.2,...]"           @Query native        cosine similarity   JPA mapping           entity objects
        ↓                      ↓                   ↓                   ↓                      ↓
vector format             SQL execution        <-> operator        DocumentChunk         sorted results

SearchController → SimilaritySearchResponse → ApiResponse → Frontend
       ↓                    ↓                     ↓            ↓
buildSearchResponse()   response mapping      success wrap   JSON Response
       ↓                    ↓                     ↓            ↓
metadata addition       chunk details         HTTP 200       UI display
       ↓                    ↓                     ↓            ↓
search time calc        similarity scores     standard format client rendering
```

### **1. Controller Layer → Service Layer**
```
📁 Controller/RAGController.java
    ├── @PostMapping("/query")
    ├── @Valid RAGQueryRequest validation
    ├── Authentication.getName() → userId
    │
    ▼ ragService.queryWithRAG() çağrısı
    │
📁 Service/impl/RAGServiceImpl.java
    ├── Parameter normalization
    ├── Query validation
    └── Multi-step processing başlatır
```

### **2. Service Layer İç Koordinasyonu**
```
📁 Service/impl/RAGServiceImpl.java
    │
    ├─► vectorSearchService.getTextEmbedding(query)
    │   └── Integration/HuggingFace'e gider
    │
    ├─► vectorSearchService.findSimilarContent()
    │   └── Repository layer'a gider
    │
    ├─► rankChunksByRelevance()
    │   └── Internal processing
    │
    ├─► optimizeContextWindow()
    │   └── Token limit kontrolü
    │
    ├─► buildContext()
    │   └── String concatenation
    │
    └─► llmService.generateResponse()
        └── Integration/HuggingFace'e gider
```

### **3. Repository Layer Veritabanı İşlemleri**
```
📁 Repository/DocumentChunkRepository.java
    │
    ├── findSimilarChunks() native query
    │   ├── PostgreSQL pgvector extension
    │   ├── Cosine similarity: embedding <-> query_vector
    │   ├── Domain tag filtering
    │   └── LIMIT clause
    │
    └── Return: List<DocumentChunk>
```

### **4. Entity Layer Veri Modeli**
```
📁 Entity/DocumentChunk.java
    │
    ├── @Column embedding (vector(384))
    ├── getEmbeddingAsFloatArray() method
    ├── setEmbeddingFromFloatArray() method
    └── Business logic methods
```

### **5. DTO Layer Response Oluşturma**
```
📁 Dto/response/RAGQueryResponse.java
    │
    ├── Static factory methods
    │   ├── success() method
    │   ├── error() method
    │   └── calculateConfidence() private
    │
    ├── Nested classes
    │   ├── SourceChunk
    │   ├── DiagramInfo
    │   └── QueryMetadata
    │
    └── Return formatted response
```

---

## 📄 **DOCUMENT PROCESSING VERİ AKIŞI**

### **1. File Upload Flow**
```
📁 Controller/DocumentController.java
    ├── @PostMapping("/upload")
    ├── MultipartFile validation
    │
    ▼
📁 Service/impl/DocumentProcessingServiceImpl.java
    ├── processDocument() method
    ├── File validation (size, type, hash)
    ├── Entity creation
    │
    ▼
📁 Entity/Document.java
    ├── Builder pattern creation
    ├── Status: PROCESSING
    ├── Metadata setting
    │
    ▼
📁 Repository/DocumentRepository.java
    └── save() method
```

### **2. Text Extraction & Chunking**
```
📁 Service/impl/DocumentProcessingServiceImpl.java
    ├── extractTextFromFile() - Apache PDFBox
    │
    ▼ chunkDocument() call
    │
📁 Service/impl/DocumentChunkingServiceImpl.java
    ├── chunkDocument() method
    ├── Smart text splitting (1000 chars, 200 overlap)
    ├── Section detection with regex patterns
    ├── Quality validation
    │
    ▼ Multiple DocumentChunk creation
    │
📁 Entity/DocumentChunk.java
    ├── Builder pattern instances
    ├── Text content storage
    ├── Index and position tracking
```

### **3. Async Embedding Generation**
```
📁 Service/impl/DocumentChunkingServiceImpl.java
    ├── @Async generateEmbeddingsAsync()
    ├── Batch processing (5-10 chunks)
    │
    ▼ Integration call
    │
📁 Integration/HuggingFace/Service/EmbeddingService
    ├── embedBatch() method
    ├── API call to HuggingFace
    ├── 384-dimensional vectors
    │
    ▼ Vector storage
    │
📁 Entity/DocumentChunk.java
    ├── setEmbeddingFromFloatArray()
    ├── PostgreSQL vector format conversion
    │
    ▼
📁 Repository/DocumentChunkRepository.java
    └── saveAll() batch insert
```

---

## 🔍 **VECTOR SEARCH VERİ AKIŞI**

### **1. Similarity Search**
```
📁 Controller/SearchController.java
    ├── @PostMapping("/similarity")
    ├── SimilaritySearchRequest validation
    │
    ▼
📁 Service/impl/VectorSearchServiceImpl.java
    ├── findSimilarContent() method
    ├── Query embedding generation
    ├── Vector format conversion
    │
    ▼
📁 Repository/DocumentChunkRepository.java
    ├── findSimilarChunks() native SQL
    ├── pgvector cosine similarity
    ├── Domain filtering
    │
    ▼
📁 Entity/DocumentChunk.java
    └── Lazy loading with Document relationship
```

### **2. Cosine Similarity Calculation**
```
📁 Service/impl/VectorSearchServiceImpl.java
    │
    ├── calculateSimilarity() method
    │   ├── Dot product calculation
    │   ├── Magnitude calculations
    │   └── Cosine formula: dot/(mag1*mag2)
    │
    ├── floatArrayToString() conversion
    │   └── PostgreSQL vector format
    │
    └── stringToFloatArray() parsing
        └── Database vector to float[]
```

---

## 🏷️ **DOCUMENT TAGGING VERİ AKIŞI**

### **1. AI-Powered Tagging**
```
📁 Controller/DocumentController.java
    ├── @PostMapping("/{documentId}/tags/generate")
    │
    ▼
📁 Service/impl/DocumentTaggingServiceImpl.java
    ├── generateTags() method
    ├── Document validation
    ├── Content extraction
    │
    ▼ AI processing
    │
📁 Integration/HuggingFace/Service/LLMService
    ├── Tag extraction prompt
    ├── AI response parsing
    │
    ▼ Tag processing
    │
📁 Service/impl/DocumentTaggingServiceImpl.java
    ├── parseTags() method
    ├── validateTags() quality check
    ├── mergeSimilarTags() deduplication
    │
    ▼
📁 Entity/DocumentDomainTag.java
    ├── Tag entity creation
    ├── Confidence scores
    ├── Category assignment
    │
    ▼
📁 Repository/DocumentDomainTagRepository.java
    └── saveAll() batch insert
```

---

## ⚙️ **CONFIG LAYER YAPISI**

### **Configuration Classes**
```
📁 Config/AIConfiguration.java
    ├── Bean definitions
    ├── Service configurations
    └── Integration settings

📁 Config/AsyncConfiguration.java
    ├── @EnableAsync
    ├── TaskExecutor beans
    └── Async method configurations

📁 Config/FileStorageConfiguration.java
    ├── File upload settings
    ├── Storage paths
    └── File type validations
```

---

## 🔄 **KATMANLAR ARASI İLETİŞİM**

### **Dependency Flow**
```
Controller → Service → Repository → Entity
    │           │           │
    ↓           ↓           ↓
   DTO      Integration   Database
```

### **Error Handling Flow**
```
Exception (Any Layer)
    │
    ▼
📁 Common/Exceptions/GlobalExceptionHandler
    ├── Catch and format
    ├── Log error details
    └── Return ApiResponse.error()
```

### **Transaction Management**
```
📁 Service/impl/*ServiceImpl.java
    ├── @Transactional annotations
    ├── Read-only transactions
    ├── Rollback conditions
    └── Isolation levels
```

---

## 📊 **PERFORMANS OPTİMİZASYONLARI**

### **Async Processing**
```
📁 Service/impl/DocumentChunkingServiceImpl.java
    ├── @Async methods
    ├── CompletableFuture usage
    └── Non-blocking operations
```

### **Batch Operations**
```
📁 Repository/*Repository.java
    ├── saveAll() batch inserts
    ├── Native queries for performance
    └── Pagination support
```

### **Caching Strategy**
```
📁 Entity/*.java
    ├── @Cacheable annotations
    ├── Cache eviction policies
    └── Cache key strategies
```

Bu dokümantasyon AI modülünün iç yapısını ve veri akışını detaylı olarak açıklamaktadır.
