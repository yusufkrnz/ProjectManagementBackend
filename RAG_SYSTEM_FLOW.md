# 🤖 RAG (Retrieval-Augmented Generation) System Data Flow

## 📊 **GENEL SİSTEM MİMARİSİ**

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│     CLIENT      │    │   SPRING BOOT   │    │   POSTGRESQL    │
│   (Frontend)    │◄──►│    BACKEND      │◄──►│   + pgvector    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                               │
                               ▼
                    ┌─────────────────┐
                    │  HUGGING FACE   │
                    │   API Service   │
                    └─────────────────┘
```

## 🔄 **RAG SİSTEMİ VERİ AKIŞ ADIMLARI**

### **1️⃣ DOKÜMAN YÜKLEMİ VE İŞLEME**

```
PDF Upload (Client)
    │
    ▼
DocumentProcessingService.processDocument()
    │
    ├─► Text Extraction (Apache PDFBox)
    │
    ▼
DocumentChunkingService.chunkDocument()
    │
    ├─► Intelligent Chunking (1000 chars, 200 overlap)
    ├─► Section Detection (Headers, paragraphs)
    ├─► Quality Validation
    │
    ▼
EmbeddingService.embedBatch() → HuggingFace API
    │
    ├─► all-MiniLM-L6-v2 Model (384 dimensions)
    ├─► Batch Processing (5-10 chunks at once)
    │
    ▼
DocumentChunk.embedding → PostgreSQL pgvector
    │
    ▼
✅ Ready for RAG Queries
```

### **2️⃣ RAG QUERY İŞLEME**

```
User Query (Client)
    │
    ▼
RAGController.ragQuery()
    │
    ▼
RAGService.queryWithRAG()
    │
    ├─► 1. Query Embedding
    │   └─► EmbeddingService.embedText()
    │       └─► HuggingFace API
    │
    ├─► 2. Vector Similarity Search  
    │   └─► VectorSearchService.findSimilarContent()
    │       └─► PostgreSQL: cosine similarity (<->)
    │           └─► SELECT * ORDER BY embedding <-> query_vector
    │
    ├─► 3. Context Building
    │   ├─► Rank chunks by relevance
    │   ├─► Optimize for token limit (3000 tokens)
    │   └─► Build context string
    │
    ├─► 4. LLM Generation
    │   └─► LLMService.generateResponse()
    │       └─► HuggingFace LLM API (Llama-2-7b)
    │           └─► Prompt: Context + User Query
    │
    └─► 5. Response Formatting
        ├─► Add source references
        ├─► Calculate confidence scores
        ├─► Generate suggested questions
        └─► Return RAGQueryResponse
```

## 🏗️ **KLASÖR VE SINIF YAPISI**

```
src/main/java/com/yusufkurnaz/ProjectManagementBackend/
│
├── AI/
│   ├── Controller/
│   │   ├── RAGController.java              ← 🎯 RAG API endpoints
│   │   ├── DocumentController.java         ← PDF upload endpoints
│   │   └── SearchController.java           ← Vector search endpoints
│   │
│   ├── Service/
│   │   ├── RAGService.java                 ← 🧠 Ana RAG interface
│   │   ├── DocumentChunkingService.java    ← 📄 Chunking interface
│   │   ├── VectorSearchService.java        ← 🔍 Vector search interface
│   │   └── impl/
│   │       ├── RAGServiceImpl.java         ← 🤖 RAG implementation
│   │       ├── DocumentChunkingServiceImpl.java ← ✂️ Chunking logic
│   │       └── VectorSearchServiceImpl.java ← 🎯 Similarity search
│   │
│   ├── Entity/
│   │   ├── Document.java                   ← 📋 PDF metadata
│   │   └── DocumentChunk.java              ← 🧩 Text chunks + vectors
│   │
│   ├── Repository/
│   │   ├── DocumentRepository.java         ← 📚 Document CRUD
│   │   └── DocumentChunkRepository.java    ← 🔍 Vector queries (pgvector)
│   │
│   └── Dto/
│       ├── request/
│       │   └── RAGQueryRequest.java        ← 📝 RAG query params
│       └── response/
│           └── RAGQueryResponse.java       ← 📋 RAG response format
│
├── Integration/
│   ├── HuggingFace/
│   │   ├── Service/
│   │   │   ├── EmbeddingService.java       ← 🎯 Vector generation
│   │   │   └── LLMService.java             ← 🤖 Text generation
│   │   └── Config/
│   │       └── HuggingFaceConfig.java      ← ⚙️ API configuration
│   │
│   └── PlantUML/
│       └── Service/
│           └── DiagramGenerationService.java ← 📊 Diagram creation
│
└── Common/
    ├── Config/
    │   └── AsyncConfiguration.java          ← ⚡ Async processing
    └── Dto/
        └── ApiResponse.java                 ← 📡 Standard API response
```

## 🔗 **VERİ AKIŞ DETAYLARI**

### **A) PDF → Chunks → Vectors**
```
1. DocumentProcessingService
   ├── PDF Text Extraction
   ├── Metadata Creation
   └── Async Chunking Trigger

2. DocumentChunkingService  
   ├── Smart Text Splitting
   ├── Section Detection
   ├── Overlap Management
   └── Quality Validation

3. EmbeddingService (Async)
   ├── Batch Text Processing
   ├── HuggingFace API Call
   ├── 384-dim Vector Creation
   └── pgvector Storage
```

### **B) Query → Context → Response**
```
1. RAGController
   ├── Request Validation
   ├── User Authentication
   └── Service Delegation

2. RAGService
   ├── Query Vectorization
   ├── Similarity Search
   ├── Context Optimization
   └── LLM Integration

3. VectorSearchService
   ├── Cosine Similarity Calc
   ├── Domain Filtering
   ├── Result Ranking
   └── Chunk Selection

4. LLMService
   ├── Prompt Engineering
   ├── Context Injection
   ├── Response Generation
   └── Quality Assessment
```

## 🚀 **PERFORMANS OPTİMİZASYONLARI**

### **Embedding Generation**
- ✅ Batch processing (5-10 chunks)
- ✅ Async processing (@Async)
- ✅ Fallback mechanisms
- ✅ Error handling

### **Vector Search**
- ✅ pgvector native indexing
- ✅ Cosine similarity (<-> operator)
- ✅ Domain tag filtering
- ✅ Result limit controls

### **Context Window**
- ✅ Token limit optimization (3000)
- ✅ Chunk relevance ranking
- ✅ Smart truncation
- ✅ Quality preservation

## 📋 **API ENDPOINTS**

```bash
# 1. RAG Query (Genel)
POST /api/v1/ai/rag/query
{
  "query": "Spring Boot nedir?",
  "domainTags": ["java", "backend"],
  "maxChunks": 5,
  "minSimilarity": 0.3
}

# 2. Document-Specific RAG
POST /api/v1/ai/rag/query/document/{documentId}
{
  "query": "Bu dokümandaki ana konular neler?"
}

# 3. RAG with Diagram
POST /api/v1/ai/rag/query/diagram?diagramType=class
{
  "query": "User entity için class diagram oluştur"
}

# 4. Conversational RAG
POST /api/v1/ai/rag/query/conversational
{
  "query": "Peki ya security nasıl?",
  "conversationHistory": ["Spring Boot nedir?", "..."]
}
```

## 🎯 **BAŞARI KRİTERLERİ**

- ✅ **PDF Upload & Processing**: Working
- ✅ **Text Chunking**: Smart chunking with overlap
- ✅ **Vector Generation**: HuggingFace integration
- ✅ **Vector Storage**: PostgreSQL + pgvector
- ✅ **Similarity Search**: Cosine similarity queries
- ✅ **RAG Service**: Complete pipeline
- ✅ **API Endpoints**: RESTful interface
- ✅ **Async Processing**: Performance optimization

## 🔧 **KALAN GÖREVLER**

1. **Configuration**: application.properties RAG settings
2. **Testing**: Integration tests for RAG pipeline
3. **Monitoring**: Performance metrics and logging
4. **Documentation**: API documentation with examples
5. **Error Handling**: Comprehensive error scenarios
