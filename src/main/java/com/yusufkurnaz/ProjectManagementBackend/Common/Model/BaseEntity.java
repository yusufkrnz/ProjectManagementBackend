package com.yusufkurnaz.ProjectManagementBackend.Common.Model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    // Security fields
    @Column(name = "encryption_key_id")
    private String encryptionKeyId;
    
    @Column(name = "hash_version")
    private String hashVersion;
}