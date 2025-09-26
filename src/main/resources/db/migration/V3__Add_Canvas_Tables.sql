-- Canvas Module Database Schema
-- This script creates tables for IdeaWorkspace canvas functionality

-- Canvas Boards table
CREATE TABLE canvas_boards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    workspace_id UUID NOT NULL,
    canvas_data JSONB DEFAULT '{"type":"excalidraw","version":2,"source":"https://excalidraw.com","elements":[],"appState":{"gridSize":null,"viewBackgroundColor":"#ffffff"},"files":{}}',
    thumbnail_url VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    last_accessed_at TIMESTAMP,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    is_template BOOLEAN NOT NULL DEFAULT FALSE,
    view_count BIGINT NOT NULL DEFAULT 0,
    fork_count BIGINT NOT NULL DEFAULT 0,
    
    -- Canvas settings
    canvas_width INTEGER NOT NULL DEFAULT 1920,
    canvas_height INTEGER NOT NULL DEFAULT 1080,
    background_color VARCHAR(7) NOT NULL DEFAULT '#ffffff',
    grid_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Collaboration settings
    collaboration_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    max_collaborators INTEGER NOT NULL DEFAULT 10,
    
    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID,
    updated_by UUID,
    encryption_key_id VARCHAR(255),
    hash_version VARCHAR(255),
    
    CONSTRAINT fk_canvas_boards_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_canvas_boards_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_canvas_boards_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- Canvas Board Tags table (many-to-many relationship)
CREATE TABLE canvas_board_tags (
    canvas_board_id UUID NOT NULL,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (canvas_board_id, tag),
    CONSTRAINT fk_canvas_board_tags_canvas FOREIGN KEY (canvas_board_id) REFERENCES canvas_boards(id) ON DELETE CASCADE
);

-- Canvas Files table
CREATE TABLE canvas_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canvas_board_id UUID NOT NULL,
    original_filename VARCHAR(500) NOT NULL,
    stored_filename VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_url VARCHAR(1000),
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    file_hash VARCHAR(64),
    
    -- Image-specific metadata
    image_width INTEGER,
    image_height INTEGER,
    has_thumbnail BOOLEAN NOT NULL DEFAULT FALSE,
    thumbnail_path VARCHAR(1000),
    
    -- Canvas-specific metadata
    excalidraw_file_id VARCHAR(100),
    canvas_position_x DOUBLE PRECISION,
    canvas_position_y DOUBLE PRECISION,
    canvas_width DOUBLE PRECISION,
    canvas_height DOUBLE PRECISION,
    
    -- Usage tracking
    usage_count BIGINT NOT NULL DEFAULT 0,
    last_used_at TIMESTAMP,
    
    -- File status
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    
    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID,
    updated_by UUID,
    encryption_key_id VARCHAR(255),
    hash_version VARCHAR(255),
    
    CONSTRAINT fk_canvas_files_canvas_board FOREIGN KEY (canvas_board_id) REFERENCES canvas_boards(id) ON DELETE CASCADE,
    CONSTRAINT fk_canvas_files_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_canvas_files_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT chk_canvas_files_status CHECK (status IN ('UPLOADING', 'UPLOADED', 'PROCESSING', 'READY', 'ERROR', 'DELETED'))
);

-- Canvas Collaborators table
CREATE TABLE canvas_collaborators (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canvas_board_id UUID NOT NULL,
    user_id UUID NOT NULL,
    permission VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    invited_by UUID,
    invitation_sent_at TIMESTAMP,
    joined_at TIMESTAMP,
    last_active_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    -- Real-time collaboration metadata
    cursor_position_x DOUBLE PRECISION,
    cursor_position_y DOUBLE PRECISION,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    user_color VARCHAR(7),
    session_id VARCHAR(100),
    
    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID,
    updated_by UUID,
    encryption_key_id VARCHAR(255),
    hash_version VARCHAR(255),
    
    CONSTRAINT fk_canvas_collaborators_canvas_board FOREIGN KEY (canvas_board_id) REFERENCES canvas_boards(id) ON DELETE CASCADE,
    CONSTRAINT fk_canvas_collaborators_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_canvas_collaborators_invited_by FOREIGN KEY (invited_by) REFERENCES users(id),
    CONSTRAINT fk_canvas_collaborators_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_canvas_collaborators_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT chk_canvas_collaborators_permission CHECK (permission IN ('VIEWER', 'EDITOR', 'ADMIN')),
    CONSTRAINT chk_canvas_collaborators_status CHECK (status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'REMOVED')),
    CONSTRAINT uk_canvas_collaborators_canvas_user UNIQUE (canvas_board_id, user_id)
);

-- Indexes for performance optimization

-- Canvas Boards indexes
CREATE INDEX idx_canvas_boards_workspace_id ON canvas_boards(workspace_id);
CREATE INDEX idx_canvas_boards_created_by ON canvas_boards(created_by);
CREATE INDEX idx_canvas_boards_is_public ON canvas_boards(is_public) WHERE is_public = TRUE;
CREATE INDEX idx_canvas_boards_is_template ON canvas_boards(is_template) WHERE is_template = TRUE;
CREATE INDEX idx_canvas_boards_is_active ON canvas_boards(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_canvas_boards_updated_at ON canvas_boards(updated_at DESC);
CREATE INDEX idx_canvas_boards_last_accessed_at ON canvas_boards(last_accessed_at DESC);
CREATE INDEX idx_canvas_boards_view_count ON canvas_boards(view_count DESC);
CREATE INDEX idx_canvas_boards_name_search ON canvas_boards USING gin(to_tsvector('english', name));
CREATE INDEX idx_canvas_boards_description_search ON canvas_boards USING gin(to_tsvector('english', description));

-- Canvas Board Tags indexes
CREATE INDEX idx_canvas_board_tags_tag ON canvas_board_tags(tag);

-- Canvas Files indexes
CREATE INDEX idx_canvas_files_canvas_board_id ON canvas_files(canvas_board_id);
CREATE INDEX idx_canvas_files_file_hash ON canvas_files(file_hash);
CREATE INDEX idx_canvas_files_excalidraw_file_id ON canvas_files(excalidraw_file_id);
CREATE INDEX idx_canvas_files_mime_type ON canvas_files(mime_type);
CREATE INDEX idx_canvas_files_status ON canvas_files(status);
CREATE INDEX idx_canvas_files_created_at ON canvas_files(created_at DESC);
CREATE INDEX idx_canvas_files_usage_count ON canvas_files(usage_count DESC);

-- Canvas Collaborators indexes
CREATE INDEX idx_canvas_collaborators_canvas_board_id ON canvas_collaborators(canvas_board_id);
CREATE INDEX idx_canvas_collaborators_user_id ON canvas_collaborators(user_id);
CREATE INDEX idx_canvas_collaborators_status ON canvas_collaborators(status);
CREATE INDEX idx_canvas_collaborators_is_online ON canvas_collaborators(is_online) WHERE is_online = TRUE;
CREATE INDEX idx_canvas_collaborators_session_id ON canvas_collaborators(session_id) WHERE session_id IS NOT NULL;
CREATE INDEX idx_canvas_collaborators_last_active_at ON canvas_collaborators(last_active_at DESC);

-- Full-text search indexes
CREATE INDEX idx_canvas_boards_fts ON canvas_boards USING gin(
    to_tsvector('english', coalesce(name, '') || ' ' || coalesce(description, ''))
);

-- JSONB indexes for canvas data queries (if needed for advanced search)
CREATE INDEX idx_canvas_boards_canvas_data_elements ON canvas_boards USING gin((canvas_data -> 'elements'));
CREATE INDEX idx_canvas_boards_canvas_data_files ON canvas_boards USING gin((canvas_data -> 'files'));

-- Partial indexes for active records
CREATE INDEX idx_canvas_boards_active_workspace ON canvas_boards(workspace_id, updated_at DESC) 
    WHERE is_active = TRUE;
CREATE INDEX idx_canvas_boards_active_user ON canvas_boards(created_by, updated_at DESC) 
    WHERE is_active = TRUE;
CREATE INDEX idx_canvas_files_active_canvas ON canvas_files(canvas_board_id, created_at DESC) 
    WHERE is_active = TRUE;
CREATE INDEX idx_canvas_collaborators_active_canvas ON canvas_collaborators(canvas_board_id, last_active_at DESC) 
    WHERE is_active = TRUE AND status = 'ACTIVE';

-- Comments for documentation
COMMENT ON TABLE canvas_boards IS 'Canvas boards for IdeaWorkspace - stores Excalidraw data and metadata';
COMMENT ON TABLE canvas_files IS 'Files uploaded to canvas boards - images, documents, etc.';
COMMENT ON TABLE canvas_collaborators IS 'Real-time collaboration permissions and presence for canvas boards';
COMMENT ON TABLE canvas_board_tags IS 'Tags for categorizing and searching canvas boards';

COMMENT ON COLUMN canvas_boards.canvas_data IS 'Excalidraw JSON data stored as JSONB for efficient querying';
COMMENT ON COLUMN canvas_boards.version IS 'Version number for optimistic locking in collaborative editing';
COMMENT ON COLUMN canvas_files.excalidraw_file_id IS 'ID used in Excalidraw files object for referencing uploaded files';
COMMENT ON COLUMN canvas_collaborators.session_id IS 'WebSocket session ID for real-time collaboration';
COMMENT ON COLUMN canvas_collaborators.user_color IS 'Hex color code for user cursor and selections in collaborative editing';
