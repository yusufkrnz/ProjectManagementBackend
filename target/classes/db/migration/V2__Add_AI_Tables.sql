-- AI Module Database Schema
-- This script creates tables for AI-powered diagram generation system

-- Enable pgvector extension for vector similarity search
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm; -- For full-text search

-- AI Documents table
CREATE TABLE ai_documents (
    id UUID PRIMARY KEY,
    original_filename VARCHAR(500) NOT NULL,
    stored_filename VARCHAR(500) NOT NULL,
    title VARCHAR(500),
    description TEXT,
    file_path VARCHAR(1000) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(200),
    uploaded_by UUID NOT NULL,
    extracted_text TEXT,
    total_pages INTEGER,
    total_chunks INTEGER DEFAULT 0,
    content_hash VARCHAR(64),
    language_code VARCHAR(5) DEFAULT 'tr',
    quality_score FLOAT,
    
    -- Processing status fields (from BaseProcessor)
    processing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    progress_percentage INTEGER NOT NULL DEFAULT 0,
    processed_by UUID,
    processing_metadata JSONB,
    
    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID,
    updated_by UUID,
    encryption_key_id VARCHAR(100),
    hash_version VARCHAR(50),
    
    CONSTRAINT fk_documents_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id),
    CONSTRAINT fk_documents_processed_by FOREIGN KEY (processed_by) REFERENCES users(id)
);

-- Document domain tags (AI-generated categorization)
CREATE TABLE document_domain_tags (
    document_id UUID NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (document_id, tag),
    CONSTRAINT fk_domain_tags_document FOREIGN KEY (document_id) REFERENCES ai_documents(id) ON DELETE CASCADE
);

-- Document user tags (user-defined tags)
CREATE TABLE document_user_tags (
    document_id UUID NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (document_id, tag),
    CONSTRAINT fk_user_tags_document FOREIGN KEY (document_id) REFERENCES ai_documents(id) ON DELETE CASCADE
);

-- Document chunks with vector embeddings
CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    chunk_text TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    embedding vector(384), -- HuggingFace all-MiniLM-L6-v2 output size
    page_number INTEGER,
    section_title VARCHAR(500),
    start_position INTEGER,
    end_position INTEGER,
    token_count INTEGER,
    confidence_score FLOAT,
    content_type VARCHAR(100),
    technical_level VARCHAR(50),
    language_detected VARCHAR(5) DEFAULT 'tr',
    
    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID,
    updated_by UUID,
    encryption_key_id VARCHAR(100),
    hash_version VARCHAR(50),
    
    CONSTRAINT fk_chunks_document FOREIGN KEY (document_id) REFERENCES ai_documents(id) ON DELETE CASCADE
);

-- Generated diagrams table
CREATE TABLE generated_diagrams (
    id UUID PRIMARY KEY,
    source_document_id UUID NOT NULL,
    generated_by UUID NOT NULL,
    diagram_type VARCHAR(50) NOT NULL,
    plant_uml_code TEXT NOT NULL,
    svg_content TEXT,
    png_file_path VARCHAR(1000),
    diagram_title VARCHAR(500),
    diagram_description TEXT,
    user_rating INTEGER CHECK (user_rating >= 1 AND user_rating <= 5),
    user_feedback TEXT,
    is_public BOOLEAN DEFAULT FALSE,
    view_count BIGINT DEFAULT 0,
    download_count BIGINT DEFAULT 0,
    generation_time_ms BIGINT,
    llm_model_used VARCHAR(200),
    prompt_used TEXT,
    complexity_score FLOAT,
    accuracy_score FLOAT,
    
    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID,
    updated_by UUID,
    encryption_key_id VARCHAR(100),
    hash_version VARCHAR(50),
    
    CONSTRAINT fk_diagrams_document FOREIGN KEY (source_document_id) REFERENCES ai_documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_diagrams_generated_by FOREIGN KEY (generated_by) REFERENCES users(id)
);

-- Diagram tags
CREATE TABLE diagram_tags (
    diagram_id UUID NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (diagram_id, tag),
    CONSTRAINT fk_diagram_tags_diagram FOREIGN KEY (diagram_id) REFERENCES generated_diagrams(id) ON DELETE CASCADE
);

-- Create indexes for performance

-- Document indexes
CREATE INDEX idx_documents_uploaded_by ON ai_documents(uploaded_by);
CREATE INDEX idx_documents_processing_status ON ai_documents(processing_status);
CREATE INDEX idx_documents_file_type ON ai_documents(file_type);
CREATE INDEX idx_documents_created_at ON ai_documents(created_at DESC);
CREATE INDEX idx_documents_content_hash ON ai_documents(content_hash);
CREATE INDEX idx_documents_active ON ai_documents(is_active) WHERE is_active = true;

-- Document tags indexes
CREATE INDEX idx_domain_tags_tag ON document_domain_tags(tag);
CREATE INDEX idx_user_tags_tag ON document_user_tags(tag);

-- Document chunks indexes
CREATE INDEX idx_chunks_document_id ON document_chunks(document_id);
CREATE INDEX idx_chunks_page_number ON document_chunks(page_number);
CREATE INDEX idx_chunks_content_type ON document_chunks(content_type);
CREATE INDEX idx_chunks_confidence ON document_chunks(confidence_score DESC);

-- ⭐ MOST IMPORTANT: Vector similarity index for pgvector
CREATE INDEX idx_chunks_embedding ON document_chunks USING ivfflat (embedding vector_cosine_ops);

-- Generated diagrams indexes
CREATE INDEX idx_diagrams_source_document ON generated_diagrams(source_document_id);
CREATE INDEX idx_diagrams_generated_by ON generated_diagrams(generated_by);
CREATE INDEX idx_diagrams_type ON generated_diagrams(diagram_type);
CREATE INDEX idx_diagrams_public ON generated_diagrams(is_public) WHERE is_public = true;
CREATE INDEX idx_diagrams_rating ON generated_diagrams(user_rating DESC);
CREATE INDEX idx_diagrams_views ON generated_diagrams(view_count DESC);
CREATE INDEX idx_diagrams_created_at ON generated_diagrams(created_at DESC);

-- Diagram tags indexes
CREATE INDEX idx_diagram_tags_tag ON diagram_tags(tag);

-- Full-text search indexes
CREATE INDEX idx_documents_text_search ON ai_documents USING gin(to_tsvector('turkish', COALESCE(extracted_text, '')));
CREATE INDEX idx_chunks_text_search ON document_chunks USING gin(to_tsvector('turkish', chunk_text));

-- Composite indexes for common queries
CREATE INDEX idx_documents_user_status ON ai_documents(uploaded_by, processing_status, created_at DESC);
CREATE INDEX idx_chunks_document_page ON document_chunks(document_id, page_number, chunk_index);
CREATE INDEX idx_diagrams_user_type ON generated_diagrams(generated_by, diagram_type, created_at DESC);

-- Add constraints for data integrity
ALTER TABLE ai_documents ADD CONSTRAINT chk_file_size_positive CHECK (file_size > 0);
ALTER TABLE ai_documents ADD CONSTRAINT chk_progress_range CHECK (progress_percentage >= 0 AND progress_percentage <= 100);
ALTER TABLE document_chunks ADD CONSTRAINT chk_chunk_index_positive CHECK (chunk_index >= 0);
ALTER TABLE document_chunks ADD CONSTRAINT chk_confidence_range CHECK (confidence_score IS NULL OR (confidence_score >= 0.0 AND confidence_score <= 1.0));
ALTER TABLE generated_diagrams ADD CONSTRAINT chk_view_count_positive CHECK (view_count >= 0);
ALTER TABLE generated_diagrams ADD CONSTRAINT chk_download_count_positive CHECK (download_count >= 0);

-- Create triggers for updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_ai_documents_updated_at BEFORE UPDATE ON ai_documents FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_document_chunks_updated_at BEFORE UPDATE ON document_chunks FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_generated_diagrams_updated_at BEFORE UPDATE ON generated_diagrams FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert initial test data (optional)
-- This can be removed in production
INSERT INTO ai_documents (id, original_filename, stored_filename, file_path, file_type, file_size, uploaded_by, processing_status)
SELECT 
    gen_random_uuid(),
    'test_document.pdf',
    'test_document_' || gen_random_uuid() || '.pdf',
    '/uploads/test_document.pdf',
    'PDF',
    1024000,
    u.id,
    'PENDING'
FROM users u LIMIT 1;

-- Create views for common queries
CREATE VIEW v_document_statistics AS
SELECT 
    d.uploaded_by,
    COUNT(*) as total_documents,
    COUNT(CASE WHEN d.processing_status = 'COMPLETED' THEN 1 END) as completed_documents,
    COUNT(CASE WHEN d.processing_status = 'FAILED' THEN 1 END) as failed_documents,
    SUM(d.file_size) as total_file_size,
    SUM(d.total_chunks) as total_chunks,
    AVG(d.quality_score) as avg_quality_score
FROM ai_documents d
WHERE d.is_active = true
GROUP BY d.uploaded_by;

CREATE VIEW v_popular_tags AS
SELECT 
    'domain' as tag_type,
    ddt.tag,
    COUNT(*) as usage_count
FROM document_domain_tags ddt
INNER JOIN ai_documents d ON ddt.document_id = d.id
WHERE d.is_active = true
GROUP BY ddt.tag
UNION ALL
SELECT 
    'user' as tag_type,
    dut.tag,
    COUNT(*) as usage_count
FROM document_user_tags dut
INNER JOIN ai_documents d ON dut.document_id = d.id
WHERE d.is_active = true
GROUP BY dut.tag
ORDER BY usage_count DESC;

-- Grant permissions (adjust as needed for your user)
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO your_app_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO your_app_user;
