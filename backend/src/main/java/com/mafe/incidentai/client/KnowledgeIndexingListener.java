package com.mafe.incidentai.client;

import com.mafe.incidentai.service.KnowledgeArticleChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Mantiene Qdrant sincronizado (RF03): cuando un articulo cambia, pide al flujo 01 de n8n que lo reindexe
// (o que borre su vector si quedo inactivo o sin verificar).
@Component
@RequiredArgsConstructor
public class KnowledgeIndexingListener {

    private final N8nWebhookClient n8nWebhookClient;

    // AFTER_COMMIT: n8n consulta el articulo en Spring Boot, asi que el cambio debe estar guardado.
    // @Async: el flujo genera embeddings y puede tardar; el usuario no espera esa respuesta.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleChanged(KnowledgeArticleChangedEvent event) {
        n8nWebhookClient.triggerKnowledgeIndexing(event.articleId());
    }
}
