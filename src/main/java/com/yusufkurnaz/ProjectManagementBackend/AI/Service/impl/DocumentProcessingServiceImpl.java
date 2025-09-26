package com.yusufkurnaz.ProjectManagementBackend.AI.Service.impl;

import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.Document;
import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.DocumentChunk;
import com.yusufkurnaz.ProjectManagementBackend.AI.Repository.DocumentRepository;
import com.yusufkurnaz.ProjectManagementBackend.AI.Service.DocumentProcessingService;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.FileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of DocumentProcessingService
 * Follows SOLID principles
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DocumentProcessingServiceImpl implements DocumentProcessingService {

    private final DocumentRepository documentRepository;

    @Override
    public Document processDocument(MultipartFile file, UUID userId, List<String> userTags) {
        log.info("Processing document: {} for user: {}", file.getOriginalFilename(), userId);
        
        try {
            // Step 1: Validate file
            validateFile(file);
            
            // Step 2: Create document entity
            Document document = Document.builder()
                    .originalFilename(file.getOriginalFilename())
                    .storedFilename(generateStoredFilename(file.getOriginalFilename()))
                    .filePath("/uploads/" + generateStoredFilename(file.getOriginalFilename()))
                    .fileType(FileType.fromFileName(file.getOriginalFilename()))
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .uploadedBy(userId)
                    .contentHash(generateContentHash(file))
                    .build();
            
            // Step 3: Add user tags
            userTags.forEach(document::addUserTag);
            
            // Step 4: Start processing
            document.startProcessing();
            
            // Step 5: Extract text
            String extractedText = extractTextFromFile(file);
            document.setExtractedText(extractedText);
            
            // Step 6: Create chunks (simplified for now)
            List<DocumentChunk> chunks = chunkDocument(document, extractedText);
            document.getChunks().addAll(chunks);
            document.updateChunksCount();
            
            // Step 7: Complete processing
            document.completeProcessing();
            
            Document savedDocument = documentRepository.save(document);
            
            log.info("Document processed successfully: {}", savedDocument.getId());
            return savedDocument;
            
        } catch (Exception e) {
            log.error("Error processing document: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Document processing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Document reprocessDocument(UUID documentId) {
        // TODO: Implement reprocessing logic
        throw new UnsupportedOperationException("Reprocessing not implemented yet");
    }

    @Override
    public String extractTextFromFile(MultipartFile file) {
        try {
            FileType fileType = FileType.fromFileName(file.getOriginalFilename());
            
            return switch (fileType) {
                case PDF -> extractTextFromPDF(file);
                case TXT -> new String(file.getBytes());
                default -> throw new UnsupportedOperationException("File type not supported: " + fileType);
            };
            
        } catch (Exception e) {
            log.error("Error extracting text from file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Text extraction failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DocumentChunk> chunkDocument(Document document, String extractedText) {
        List<DocumentChunk> chunks = new ArrayList<>();
        
        // Simple chunking strategy - split by sentences/paragraphs
        String[] paragraphs = extractedText.split("\\n\\s*\\n");
        int chunkIndex = 0;
        
        for (String paragraph : paragraphs) {
            if (paragraph.trim().length() > 50) { // Skip very short paragraphs
                DocumentChunk chunk = DocumentChunk.builder()
                        .document(document)
                        .chunkText(paragraph.trim())
                        .chunkIndex(chunkIndex++)
                        .tokenCount(estimateTokenCount(paragraph))
                        .confidenceScore(1.0f) // Default confidence
                        .build();
                
                chunks.add(chunk);
            }
        }
        
        log.info("Created {} chunks for document: {}", chunks.size(), document.getOriginalFilename());
        return chunks;
    }

    @Override
    public void generateEmbeddingsForDocument(UUID documentId) {
        // TODO: Implement embedding generation (async)
        log.info("Embedding generation requested for document: {}", documentId);
    }

    @Override
    public Document addDomainTags(UUID documentId, List<String> domainTags) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        
        domainTags.forEach(document::addDomainTag);
        return documentRepository.save(document);
    }

    @Override
    public Document addUserTags(UUID documentId, UUID userId, List<String> userTags) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        
        if (!document.getUploadedBy().equals(userId)) {
            throw new SecurityException("Access denied to document: " + documentId);
        }
        
        userTags.forEach(document::addUserTag);
        return documentRepository.save(document);
    }

    @Override
    public Document getDocumentStatus(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
    }

    @Override
    public List<Document> getUserDocuments(UUID userId, String fileType, String processingStatus, int page, int size) {
        // TODO: Implement with pagination and filtering
        return documentRepository.findByUploadedByAndIsActiveTrueOrderByCreatedAtDesc(
                userId, org.springframework.data.domain.PageRequest.of(page, size)
        ).getContent();
    }

    @Override
    public void deleteDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        
        if (!document.getUploadedBy().equals(userId)) {
            throw new SecurityException("Access denied to document: " + documentId);
        }
        
        document.setIsActive(false);
        documentRepository.save(document);
        
        log.info("Document soft deleted: {} by user: {}", documentId, userId);
    }

    @Override
    public Object getDocumentStatistics(UUID userId) {
        return documentRepository.getDocumentStatisticsByUser(userId);
    }

    @Override
    public List<Document> searchDocuments(String searchText, List<String> domainTags, UUID userId) {
        if (domainTags != null && !domainTags.isEmpty()) {
            return documentRepository.findByDomainTags(domainTags.toArray(new String[0]));
        } else {
            return documentRepository.searchByTextContent(searchText);
        }
    }

    @Override
    public List<String> getPopularDomainTags(int limit) {
        return documentRepository.findPopularDomainTags(limit).stream()
                .map(result -> (String) ((Object[]) result)[0])
                .toList();
    }

    @Override
    public void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        if (file.getSize() > 50 * 1024 * 1024) { // 50MB limit
            throw new IllegalArgumentException("File size exceeds 50MB limit");
        }
        
        try {
            FileType.fromFileName(file.getOriginalFilename());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported file type: " + file.getOriginalFilename());
        }
    }

    // Private helper methods
    
    private String extractTextFromPDF(MultipartFile file) throws IOException {
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    private String generateStoredFilename(String originalFilename) {
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        return UUID.randomUUID().toString() + extension;
    }
    
    private String generateContentHash(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(file.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString(); // Fallback
        }
    }
    
    private int estimateTokenCount(String text) {
        // Simple estimation: ~4 characters per token
        return text.length() / 4;
    }
}
