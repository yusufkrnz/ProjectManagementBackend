package com.yusufkurnaz.ProjectManagementBackend.Integration.HuggingFace.Service;

import java.util.List;

public interface EmbeddingService {

    /**
     * Generate embedding vector for a single text.
     */
    float[] embedText(String text);

    /**
     * Generate embedding vectors for a batch of texts.
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * Return model name used for embeddings.
     */
    String getModelName();
}
