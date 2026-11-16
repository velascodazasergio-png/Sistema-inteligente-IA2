package com.mafe.incidentai.service;

// Event published when an article changes in a way that can affect the vector index (RF03):
// edit, activation/deactivation or verification. KnowledgeIndexingListener handles it after the commit.
public record KnowledgeArticleChangedEvent(Long articleId) {
}
