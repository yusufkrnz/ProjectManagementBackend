package com.yusufkurnaz.ProjectManagementBackend.AI.Service;

import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.DocumentDomainTag;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for document tagging and domain categorization
 * Enables users to tag documents and discover content through tags
 */
public interface DocumentTaggingService {

    // Manual tagging by users
    DocumentDomainTag addTag(UUID documentId, String tag, UUID userId);
    
    DocumentDomainTag addTag(UUID documentId, String tag, String description, UUID userId);
    
    List<DocumentDomainTag> addMultipleTags(UUID documentId, List<String> tags, UUID userId);
    
    void removeTag(UUID documentId, String tag, UUID userId);
    
    void removeAllTags(UUID documentId, UUID userId);

    // AI-powered auto-tagging
    List<DocumentDomainTag> generateAutoTags(UUID documentId);
    
    List<DocumentDomainTag> generateAutoTagsFromContent(String content);
    
    void updateTagConfidence(UUID tagId, Float newConfidence);

    // Tag queries and discovery
    List<DocumentDomainTag> getDocumentTags(UUID documentId);
    
    List<String> getDocumentTagNames(UUID documentId);
    
    List<com.yusufkurnaz.ProjectManagementBackend.AI.Entity.Document> findDocumentsByTag(String tag);
    
    List<com.yusufkurnaz.ProjectManagementBackend.AI.Entity.Document> findDocumentsByTags(List<String> tags, boolean requireAll);

    // Tag statistics and analytics
    List<String> getPopularTags(int limit);
    
    List<String> getTrendingTags(int days, int limit);
    
    List<String> getRelatedTags(String tag, int limit);
    
    List<String> getUserDomainPreferences(UUID userId, int limit);
    
    Long getDocumentCountByTag(String tag);

    // Tag recommendations
    List<String> recommendTagsForDocument(UUID documentId, int limit);
    
    List<String> recommendTagsForContent(String content, int limit);
    
    List<String> recommendTagsForUser(UUID userId, int limit);

    // Domain-specific search support
    List<com.yusufkurnaz.ProjectManagementBackend.AI.Entity.Document> findSimilarDocumentsInDomain(
            UUID documentId, List<String> domainTags, int limit);
    
    List<String> extractDomainFromContent(String content);
    
    boolean isValidDomainTag(String tag);

    // Tag management
    void mergeTag(String oldTag, String newTag, UUID userId);
    
    void deleteTag(String tag, UUID userId);
    
    void cleanupUnusedTags();
    
    List<String> suggestTagCorrections(String tag);

    // User preferences and personalization
    void updateUserDomainPreferences(UUID userId, List<String> preferredTags);
    
    List<String> getUserPreferredDomains(UUID userId);
    
    void trackTagUsage(UUID userId, String tag);

    // Batch operations
    void batchTagDocuments(List<UUID> documentIds, List<String> tags, UUID userId);
    
    void batchRemoveTagsFromDocuments(List<UUID> documentIds, String tag, UUID userId);
    
    List<DocumentDomainTag> batchGenerateAutoTags(List<UUID> documentIds);

    // Export and import
    List<DocumentDomainTag> exportUserTags(UUID userId);
    
    void importUserTags(UUID userId, List<DocumentDomainTag> tags);
    
    String exportTagsAsJson(UUID userId);
    
    void importTagsFromJson(UUID userId, String jsonData);
}
