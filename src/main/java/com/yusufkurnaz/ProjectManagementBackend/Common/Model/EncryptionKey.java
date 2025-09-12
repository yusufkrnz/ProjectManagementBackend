package com.yusufkurnaz.ProjectManagementBackend.Common.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "encryption_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EncryptionKey extends BaseEntity {

    @Column(name = "key_name", nullable = false, unique = true)
    private String keyName;

    @Column(name = "key_value", nullable = false, columnDefinition = "TEXT")
    private String keyValue;

    @Column(name = "algorithm", nullable = false)
    private String algorithm;

    @Column(name = "key_size", nullable = false)
    private Integer keySize;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "key_type", nullable = false)
    private String keyType; // USER, WORKSPACE, GLOBAL

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Long usageCount = 0L;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
}