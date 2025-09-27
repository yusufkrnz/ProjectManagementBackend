package com.yusufkurnaz.ProjectManagementBackend.AI.Service.impl;

import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.Document;
import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.DocumentChunk;
import com.yusufkurnaz.ProjectManagementBackend.AI.Repository.DocumentChunkRepository;
import com.yusufkurnaz.ProjectManagementBackend.AI.Repository.DocumentRepository;
import com.yusufkurnaz.ProjectManagementBackend.AI.Service.DocumentChunkingService;
import com.yusufkurnaz.ProjectManagementBackend.Integration.HuggingFace.Service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Implementation of DocumentChunkingService
 * Handles intelligent document chunking with overlap and context preservation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DocumentChunkingServiceImpl implements DocumentChunkingService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;

    @Value("${app.ai.chunk.default-size:1000}")
    private int defaultChunkSize;

    @Value("${app.ai.chunk.overlap-size:200}")
    private int defaultOverlapSize;

    @Value("${app.ai.chunk.min-size:100}")
    private int minChunkSize;

    @Value("${app.ai.chunk.max-size:2000}")
    private int maxChunkSize;

    // Patterns for detecting section boundaries
    private static final Pattern SECTION_PATTERN = Pattern.compile(
            "(?i)(^|\\n)\\s*(\\d+\\.\\s+|[A-Z][A-Z\\s]{2,}|BÖLÜM|CHAPTER|SECTION|§)", 
            Pattern.MULTILINE
    );

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile(
            "(?<=[.!?])\\s+(?=[A-Z])", 
            Pattern.MULTILINE
    );

    @Override
    public List<DocumentChunk> chunkDocument(UUID documentId) {
        log.info("Chunking document: {}", documentId);
        
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        if (document.getExtractedText() == null || document.getExtractedText().trim().isEmpty()) {
            log.warn("Document has no extracted text: {}", documentId);
            return Collections.emptyList();
        }

        // Delete existing chunks
        deleteDocumentChunks(documentId);

        // Get optimal chunk size for this document
        int chunkSize = getOptimalChunkSize(document);
        
        // Chunk the text
        List<DocumentChunk> chunks = chunkText(document.getExtractedText(), chunkSize, defaultOverlapSize);
        
        // Set document reference and additional metadata
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            chunk.setDocument(document);
            chunk.setChunkIndex(i);
            chunk.setCreatedBy(document.getUploadedBy());
            chunk.setUpdatedBy(document.getUploadedBy());
        }

        // Extract section titles
        extractSectionTitles(chunks);

        // Save chunks
        List<DocumentChunk> savedChunks = chunkRepository.saveAll(chunks);
        
        // Update document chunk count
        document.setTotalChunks(savedChunks.size());
        documentRepository.save(document);

        // Generate embeddings asynchronously
        generateEmbeddingsAsync(savedChunks);

        log.info("Successfully chunked document {} into {} chunks", documentId, savedChunks.size());
        return savedChunks;
    }

    @Override
    public List<DocumentChunk> chunkText(String text, int maxChunkSize, int overlapSize) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<DocumentChunk> chunks = new ArrayList<>();
        String cleanText = cleanText(text);
        
        // First, try to split by sections
        List<String> sections = splitBySections(cleanText);
        
        for (String section : sections) {
            if (section.length() <= maxChunkSize) {
                // Section fits in one chunk
                chunks.add(createChunk(section, chunks.size()));
            } else {
                // Section needs to be split further
                chunks.addAll(splitLargeSection(section, maxChunkSize, overlapSize, chunks.size()));
            }
        }

        // Optimize chunks (merge small ones, etc.)
        chunks = optimizeChunks(chunks);

        // Calculate token counts and confidence scores
        for (DocumentChunk chunk : chunks) {
            chunk.setTokenCount(estimateTokenCount(chunk.getChunkText()));
            chunk.setConfidenceScore(calculateChunkQuality(chunk.getChunkText()));
        }

        return chunks;
    }

    @Override
    public List<DocumentChunk> rechunkDocument(UUID documentId, int newChunkSize, int newOverlapSize) {
        log.info("Re-chunking document {} with new parameters: chunk={}, overlap={}", 
                documentId, newChunkSize, newOverlapSize);
        
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        // Delete existing chunks
        deleteDocumentChunks(documentId);

        // Re-chunk with new parameters
        return chunkText(document.getExtractedText(), newChunkSize, newOverlapSize);
    }

    @Override
    public int getOptimalChunkSize(Document document) {
        // Determine optimal chunk size based on document characteristics
        if (document.getTotalPages() != null) {
            if (document.getTotalPages() > 100) {
                return Math.min(maxChunkSize, defaultChunkSize + 500); // Larger chunks for long docs
            } else if (document.getTotalPages() < 10) {
                return Math.max(minChunkSize, defaultChunkSize - 300); // Smaller chunks for short docs
            }
        }

        // Check content type
        String text = document.getExtractedText();
        if (text != null) {
            if (text.contains("class ") || text.contains("function ") || text.contains("def ")) {
                return 800; // Code documents need smaller chunks
            }
            if (text.contains("§") || text.contains("madde") || text.contains("article")) {
                return 1200; // Legal documents can have larger chunks
            }
        }

        return defaultChunkSize;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunk> getDocumentChunks(UUID documentId) {
        return chunkRepository.findByDocumentIdOrderByChunkIndex(documentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunk> getChunksByPage(UUID documentId, Integer pageNumber) {
        return chunkRepository.findByDocumentIdAndPageNumberOrderByChunkIndex(documentId, pageNumber);
    }

    @Override
    public void deleteDocumentChunks(UUID documentId) {
        List<DocumentChunk> chunks = chunkRepository.findByDocumentIdOrderByChunkIndex(documentId);
        chunkRepository.deleteAll(chunks);
        log.info("Deleted {} chunks for document {}", chunks.size(), documentId);
    }

    @Override
    public void updateChunkEmbeddings(UUID documentId) {
        List<DocumentChunk> chunks = getDocumentChunks(documentId);
        generateEmbeddingsAsync(chunks);
    }

    @Override
    @Transactional(readOnly = true)
    public ChunkStatistics getChunkStatistics(UUID documentId) {
        List<DocumentChunk> chunks = getDocumentChunks(documentId);
        
        if (chunks.isEmpty()) {
            return new ChunkStatistics(0, 0, 0, 0, 0.0, 0, 0);
        }

        int totalChunks = chunks.size();
        int totalSize = chunks.stream().mapToInt(c -> c.getChunkText().length()).sum();
        int averageSize = totalSize / totalChunks;
        int minSize = chunks.stream().mapToInt(c -> c.getChunkText().length()).min().orElse(0);
        int maxSize = chunks.stream().mapToInt(c -> c.getChunkText().length()).max().orElse(0);
        int emptyChunks = (int) chunks.stream().filter(c -> c.getChunkText().trim().isEmpty()).count();
        
        double averageQuality = chunks.stream()
                .filter(c -> c.getConfidenceScore() != null)
                .mapToDouble(DocumentChunk::getConfidenceScore)
                .average().orElse(0.0);

        return new ChunkStatistics(
                totalChunks, averageSize, minSize, maxSize, 
                0.0, emptyChunks, (int) (averageQuality * 100)
        );
    }

    @Override
    public boolean validateChunkQuality(DocumentChunk chunk) {
        String text = chunk.getChunkText();
        
        // Check minimum length
        if (text.length() < minChunkSize) {
            return false;
        }
        
        // Check if chunk is mostly whitespace
        if (text.trim().length() < text.length() * 0.5) {
            return false;
        }
        
        // Check if chunk has meaningful content (not just numbers/symbols)
        long letterCount = text.chars().filter(Character::isLetter).count();
        return letterCount > text.length() * 0.3;
    }

    @Override
    public List<DocumentChunk> optimizeChunks(List<DocumentChunk> chunks) {
        List<DocumentChunk> optimized = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk current = chunks.get(i);
            
            // Skip very small chunks by merging with next if possible
            if (current.getChunkText().length() < minChunkSize && i < chunks.size() - 1) {
                DocumentChunk next = chunks.get(i + 1);
                if (current.getChunkText().length() + next.getChunkText().length() <= maxChunkSize) {
                    // Merge chunks
                    String mergedText = current.getChunkText() + "\n\n" + next.getChunkText();
                    DocumentChunk merged = DocumentChunk.builder()
                            .chunkText(mergedText)
                            .chunkIndex(current.getChunkIndex())
                            .pageNumber(current.getPageNumber())
                            .sectionTitle(current.getSectionTitle())
                            .startPosition(current.getStartPosition())
                            .endPosition(next.getEndPosition())
                            .build();
                    optimized.add(merged);
                    i++; // Skip next chunk as it's been merged
                    continue;
                }
            }
            
            optimized.add(current);
        }
        
        return optimized;
    }

    @Override
    public void extractSectionTitles(List<DocumentChunk> chunks) {
        for (DocumentChunk chunk : chunks) {
            String title = extractSectionTitle(chunk.getChunkText());
            if (title != null && !title.trim().isEmpty()) {
                chunk.setSectionTitle(title);
            }
        }
    }

    // Private helper methods
    private String cleanText(String text) {
        return text
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private List<String> splitBySections(String text) {
        // Try to split by section headers
        String[] sections = SECTION_PATTERN.split(text);
        List<String> result = new ArrayList<>();
        
        for (String section : sections) {
            if (!section.trim().isEmpty()) {
                result.add(section.trim());
            }
        }
        
        // If no sections found, return the whole text
        if (result.isEmpty()) {
            result.add(text);
        }
        
        return result;
    }

    private List<DocumentChunk> splitLargeSection(String section, int maxChunkSize, int overlapSize, int startIndex) {
        List<DocumentChunk> chunks = new ArrayList<>();
        
        // Try to split by sentences first
        String[] sentences = SENTENCE_BOUNDARY.split(section);
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = startIndex;
        
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() <= maxChunkSize) {
                if (currentChunk.length() > 0) {
                    currentChunk.append(" ");
                }
                currentChunk.append(sentence);
            } else {
                // Save current chunk
                if (currentChunk.length() > 0) {
                    chunks.add(createChunk(currentChunk.toString(), chunkIndex++));
                }
                
                // Start new chunk with overlap
                currentChunk = new StringBuilder();
                if (overlapSize > 0 && !chunks.isEmpty()) {
                    String lastChunk = chunks.get(chunks.size() - 1).getChunkText();
                    if (lastChunk.length() > overlapSize) {
                        currentChunk.append(lastChunk.substring(lastChunk.length() - overlapSize));
                        currentChunk.append(" ");
                    }
                }
                currentChunk.append(sentence);
            }
        }
        
        // Add final chunk
        if (currentChunk.length() > 0) {
            chunks.add(createChunk(currentChunk.toString(), chunkIndex));
        }
        
        return chunks;
    }

    private DocumentChunk createChunk(String text, int index) {
        return DocumentChunk.builder()
                .chunkText(text.trim())
                .chunkIndex(index)
                .languageDetected("tr")
                .build();
    }

    private String extractSectionTitle(String text) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 5 && line.length() < 100) {
                // Check if line looks like a title
                if (line.matches("^\\d+\\.\\s+.+") || 
                    line.matches("^[A-Z][A-Z\\s]{2,}") ||
                    line.contains("BÖLÜM") || 
                    line.contains("CHAPTER")) {
                    return line;
                }
            }
        }
        return null;
    }

    private int estimateTokenCount(String text) {
        // Rough estimation: 1 token ≈ 4 characters for Turkish
        return text.length() / 4;
    }

    private float calculateChunkQuality(String text) {
        float quality = 1.0f;
        
        // Penalize very short chunks
        if (text.length() < minChunkSize) {
            quality -= 0.3f;
        }
        
        // Penalize chunks with too much whitespace
        long whitespaceCount = text.chars().filter(Character::isWhitespace).count();
        if (whitespaceCount > text.length() * 0.5) {
            quality -= 0.2f;
        }
        
        // Reward chunks with proper sentence structure
        if (text.contains(".") && text.contains(" ")) {
            quality += 0.1f;
        }
        
        return Math.max(0.0f, Math.min(1.0f, quality));
    }

    /**
     * Generate embeddings for chunks asynchronously
     * Uses batch processing for better performance
     */
    @Async("embeddingTaskExecutor")
    private void generateEmbeddingsAsync(List<DocumentChunk> chunks) {
        log.info("Starting async embedding generation for {} chunks", chunks.size());
        
        try {
            // Batch process chunks for better performance
            List<String> texts = chunks.stream()
                    .map(DocumentChunk::getChunkText)
                    .toList();
            
            // Use batch embedding for efficiency
            List<float[]> embeddings = embeddingService.embedBatch(texts);
            
            // Update chunks with embeddings
            for (int i = 0; i < chunks.size() && i < embeddings.size(); i++) {
                DocumentChunk chunk = chunks.get(i);
                float[] embedding = embeddings.get(i);
                
                if (embedding != null && embedding.length > 0) {
                    chunk.setEmbeddingFromFloatArray(embedding);
                    chunkRepository.save(chunk);
                    log.debug("Generated embedding for chunk {} (dimension: {})", 
                            chunk.getId(), embedding.length);
                } else {
                    log.warn("Failed to generate embedding for chunk {}", chunk.getId());
                }
            }
            
            log.info("Completed async embedding generation for {} chunks", chunks.size());
            
        } catch (Exception e) {
            log.error("Async embedding generation failed for {} chunks: {}", 
                    chunks.size(), e.getMessage(), e);
            
            // Fallback to individual embedding generation
            generateEmbeddingsIndividually(chunks);
        }
    }
    
    /**
     * Fallback method for individual embedding generation
     */
    private void generateEmbeddingsIndividually(List<DocumentChunk> chunks) {
        log.info("Fallback: Generating embeddings individually for {} chunks", chunks.size());
        
        for (DocumentChunk chunk : chunks) {
            try {
                float[] embedding = embeddingService.embedText(chunk.getChunkText());
                if (embedding != null && embedding.length > 0) {
                    chunk.setEmbeddingFromFloatArray(embedding);
                    chunkRepository.save(chunk);
                }
            } catch (Exception e) {
                log.error("Error generating individual embedding for chunk {}: {}", 
                        chunk.getId(), e.getMessage());
            }
        }
    }
}

