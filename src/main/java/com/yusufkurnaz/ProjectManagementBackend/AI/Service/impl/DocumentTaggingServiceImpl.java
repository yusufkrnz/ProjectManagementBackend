package com.yusufkurnaz.ProjectManagementBackend.AI.Service.impl;

import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.Document;
import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.DocumentDomainTag;
import com.yusufkurnaz.ProjectManagementBackend.AI.Repository.DocumentDomainTagRepository;
import com.yusufkurnaz.ProjectManagementBackend.AI.Repository.DocumentRepository;
import com.yusufkurnaz.ProjectManagementBackend.AI.Service.DocumentTaggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of DocumentTaggingService
 * Provides comprehensive document tagging and domain categorization
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DocumentTaggingServiceImpl implements DocumentTaggingService {

    private final DocumentDomainTagRepository tagRepository;
    private final DocumentRepository documentRepository;

    // Predefined domain categories for Turkish healthcare and software domains
    private static final Set<String> VALID_DOMAINS = Set.of(
            "sağlık", "healthcare", "tıp", "medicine", "hastane", "hospital",
            "yazılım", "software", "teknoloji", "technology", "bilgisayar", "computer",
            "finans", "finance", "bankacılık", "banking", "muhasebe", "accounting",
            "eğitim", "education", "öğretim", "teaching", "akademik", "academic",
            "hukuk", "law", "legal", "yasal", "mevzuat", "regulation",
            "pazarlama", "marketing", "satış", "sales", "müşteri", "customer",
            "insan kaynakları", "hr", "personel", "staff", "işe alım", "recruitment",
            "üretim", "manufacturing", "endüstri", "industry", "fabrika", "factory"
    );

    @Override
    public DocumentDomainTag addTag(UUID documentId, String tag, UUID userId) {
        return addTag(documentId, tag, null, userId);
    }

    @Override
    public DocumentDomainTag addTag(UUID documentId, String tag, String description, UUID userId) {
        log.info("Adding tag '{}' to document {} by user {}", tag, documentId, userId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        String normalizedTag = normalizeTag(tag);
        
        // Check if tag already exists for this document
        if (tagRepository.documentHasTag(documentId, normalizedTag)) {
            throw new RuntimeException("Tag already exists for this document: " + normalizedTag);
        }

        DocumentDomainTag domainTag = DocumentDomainTag.builder()
                .document(document)
                .tag(normalizedTag)
                .description(description)
                .taggedBy(userId)
                .tagSource(DocumentDomainTag.TagSource.USER)
                .confidenceScore(1.0f) // User tags have full confidence
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        DocumentDomainTag savedTag = tagRepository.save(domainTag);
        
        // Track tag usage for recommendations
        trackTagUsage(userId, normalizedTag);
        
        log.info("Tag '{}' added successfully to document {}", normalizedTag, documentId);
        return savedTag;
    }

    @Override
    public List<DocumentDomainTag> addMultipleTags(UUID documentId, List<String> tags, UUID userId) {
        return tags.stream()
                .map(tag -> addTag(documentId, tag, userId))
                .collect(Collectors.toList());
    }

    @Override
    public void removeTag(UUID documentId, String tag, UUID userId) {
        log.info("Removing tag '{}' from document {} by user {}", tag, documentId, userId);

        String normalizedTag = normalizeTag(tag);
        
        List<DocumentDomainTag> existingTags = tagRepository.findByDocumentIdAndIsActiveTrue(documentId);
        
        existingTags.stream()
                .filter(domainTag -> normalizedTag.equals(domainTag.getTag()))
                .forEach(domainTag -> {
                    domainTag.setIsActive(false);
                    domainTag.setUpdatedBy(userId);
                    tagRepository.save(domainTag);
                });

        log.info("Tag '{}' removed from document {}", normalizedTag, documentId);
    }

    @Override
    public void removeAllTags(UUID documentId, UUID userId) {
        log.info("Removing all tags from document {} by user {}", documentId, userId);

        List<DocumentDomainTag> tags = tagRepository.findByDocumentIdAndIsActiveTrue(documentId);
        
        tags.forEach(tag -> {
            tag.setIsActive(false);
            tag.setUpdatedBy(userId);
        });

        tagRepository.saveAll(tags);
        log.info("All tags removed from document {}", documentId);
    }

    @Override
    public List<DocumentDomainTag> generateAutoTags(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        String content = extractDocumentContent(document);
        return generateAutoTagsFromContent(content);
    }

    @Override
    public List<DocumentDomainTag> generateAutoTagsFromContent(String content) {
        log.info("Generating auto tags from content (length: {})", content.length());

        // For now, return empty list. In real implementation, this would use NLP/AI
        // TODO: Implement actual AI-based tag generation from content analysis
        return Collections.emptyList();
    }

    @Override
    public void updateTagConfidence(UUID tagId, Float newConfidence) {
        DocumentDomainTag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("Tag not found: " + tagId));

        tag.setConfidenceScore(newConfidence);
        tagRepository.save(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentDomainTag> getDocumentTags(UUID documentId) {
        return tagRepository.findByDocumentIdAndIsActiveTrue(documentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDocumentTagNames(UUID documentId) {
        return tagRepository.findByDocumentIdAndIsActiveTrue(documentId)
                .stream()
                .map(DocumentDomainTag::getTag)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findDocumentsByTag(String tag) {
        String normalizedTag = normalizeTag(tag);
        return tagRepository.findDocumentsByTag(normalizedTag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findDocumentsByTags(List<String> tags, boolean requireAll) {
        List<String> normalizedTags = tags.stream()
                .map(this::normalizeTag)
                .collect(Collectors.toList());

        if (requireAll) {
            return tagRepository.findDocumentsByAllTags(normalizedTags, normalizedTags.size());
        } else {
            return tagRepository.findDocumentsByTagsIn(normalizedTags);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getPopularTags(int limit) {
        return tagRepository.findPopularTags(PageRequest.of(0, limit))
                .stream()
                .map(result -> (String) result[0])
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTrendingTags(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return tagRepository.findTrendingTags(since)
                .stream()
                .limit(limit)
                .map(result -> (String) result[0])
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getRelatedTags(String tag, int limit) {
        String normalizedTag = normalizeTag(tag);
        return tagRepository.findRelatedTags(normalizedTag)
                .stream()
                .limit(limit)
                .map(result -> (String) result[0])
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getUserDomainPreferences(UUID userId, int limit) {
        return tagRepository.findUserDomainPreferences(userId)
                .stream()
                .limit(limit)
                .map(result -> (String) result[0])
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getDocumentCountByTag(String tag) {
        String normalizedTag = normalizeTag(tag);
        return tagRepository.countDocumentsByTag(normalizedTag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> recommendTagsForDocument(UUID documentId, int limit) {
        // Get document content and analyze for tag suggestions
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        String content = extractDocumentContent(document);
        return recommendTagsForContent(content, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> recommendTagsForContent(String content, int limit) {
        // Simple keyword-based recommendation
        List<String> recommendations = new ArrayList<>();
        
        String lowerContent = content.toLowerCase();
        
        for (String domain : VALID_DOMAINS) {
            if (lowerContent.contains(domain.toLowerCase())) {
                recommendations.add(domain);
                if (recommendations.size() >= limit) break;
            }
        }

        return recommendations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> recommendTagsForUser(UUID userId, int limit) {
        // Get user's most used tags and suggest related ones
        List<String> userTags = getUserDomainPreferences(userId, 5);
        
        Set<String> recommendations = new HashSet<>();
        
        for (String tag : userTags) {
            List<String> relatedTags = getRelatedTags(tag, 3);
            recommendations.addAll(relatedTags);
            
            if (recommendations.size() >= limit) break;
        }

        return new ArrayList<>(recommendations).subList(0, Math.min(recommendations.size(), limit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findSimilarDocumentsInDomain(UUID documentId, List<String> domainTags, int limit) {
        List<String> normalizedTags = domainTags.stream()
                .map(this::normalizeTag)
                .collect(Collectors.toList());

        List<Document> similarDocs = tagRepository.findDocumentsForSimilaritySearch(normalizedTags);
        
        // Filter out the source document and limit results
        return similarDocs.stream()
                .filter(doc -> !doc.getId().equals(documentId))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> extractDomainFromContent(String content) {
        return recommendTagsForContent(content, 10);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValidDomainTag(String tag) {
        String normalizedTag = normalizeTag(tag);
        return VALID_DOMAINS.contains(normalizedTag) || 
               VALID_DOMAINS.stream().anyMatch(domain -> domain.contains(normalizedTag) || normalizedTag.contains(domain));
    }

    @Override
    public void mergeTag(String oldTag, String newTag, UUID userId) {
        // TODO: Implement tag merging functionality
        log.info("Merging tag '{}' into '{}' by user {}", oldTag, newTag, userId);
    }

    @Override
    public void deleteTag(String tag, UUID userId) {
        // TODO: Implement tag deletion functionality
        log.info("Deleting tag '{}' by user {}", tag, userId);
    }

    @Override
    public void cleanupUnusedTags() {
        // TODO: Implement cleanup of unused tags
        log.info("Cleaning up unused tags");
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> suggestTagCorrections(String tag) {
        // Simple suggestion based on edit distance
        return VALID_DOMAINS.stream()
                .filter(domain -> calculateEditDistance(tag.toLowerCase(), domain.toLowerCase()) <= 2)
                .collect(Collectors.toList());
    }

    @Override
    public void updateUserDomainPreferences(UUID userId, List<String> preferredTags) {
        // TODO: Store user preferences in separate table
        log.info("Updating domain preferences for user {} with {} tags", userId, preferredTags.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getUserPreferredDomains(UUID userId) {
        return getUserDomainPreferences(userId, 10);
    }

    @Override
    public void trackTagUsage(UUID userId, String tag) {
        // TODO: Implement tag usage tracking for analytics
        log.debug("Tracking tag usage: user={}, tag={}", userId, tag);
    }

    @Override
    public void batchTagDocuments(List<UUID> documentIds, List<String> tags, UUID userId) {
        for (UUID documentId : documentIds) {
            addMultipleTags(documentId, tags, userId);
        }
    }

    @Override
    public void batchRemoveTagsFromDocuments(List<UUID> documentIds, String tag, UUID userId) {
        for (UUID documentId : documentIds) {
            removeTag(documentId, tag, userId);
        }
    }

    @Override
    public List<DocumentDomainTag> batchGenerateAutoTags(List<UUID> documentIds) {
        return documentIds.stream()
                .flatMap(docId -> generateAutoTags(docId).stream())
                .collect(Collectors.toList());
    }

    // Export/Import methods - TODO: Implement
    @Override
    @Transactional(readOnly = true)
    public List<DocumentDomainTag> exportUserTags(UUID userId) {
        return tagRepository.findTagsByUser(userId);
    }

    @Override
    public void importUserTags(UUID userId, List<DocumentDomainTag> tags) {
        // TODO: Implement tag import
    }

    @Override
    @Transactional(readOnly = true)
    public String exportTagsAsJson(UUID userId) {
        // TODO: Implement JSON export
        return "{}";
    }

    @Override
    public void importTagsFromJson(UUID userId, String jsonData) {
        // TODO: Implement JSON import
    }

    // Private helper methods
    private String normalizeTag(String tag) {
        return tag.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private String extractDocumentContent(Document document) {
        // TODO: Extract content from document chunks
        return document.getTitle() + " " + document.getDescription();
    }


    private int calculateEditDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                    );
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }
}
