package com.mafe.incidentai.service;

// Evento publicado cuando cambia un articulo que puede afectar al indice vectorial (RF03):
// edicion, activacion/desactivacion o verificacion. Lo atiende KnowledgeIndexingListener tras el commit.
public record KnowledgeArticleChangedEvent(Long articleId) {
}
