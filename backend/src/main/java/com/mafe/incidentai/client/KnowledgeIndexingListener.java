package com.mafe.incidentai.client;

import com.mafe.incidentai.service.KnowledgeArticleChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Keeps Qdrant in sync (RF03): when an article changes, asks n8n workflow 01 to reindex it
// (or to delete its vector if it became inactive or unverified).
@Component
@RequiredArgsConstructor
public class KnowledgeIndexingListener {

    private final N8nWebhookClient n8nWebhookClient;

    // AFTER_COMMIT: n8n reads the article back from Spring Boot, so the change must already be saved.
    // @Async: the workflow generates embeddings and can be slow; the user does not wait for it.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleChanged(KnowledgeArticleChangedEvent event) {
        n8nWebhookClient.triggerKnowledgeIndexing(event.articleId());
    }
}
