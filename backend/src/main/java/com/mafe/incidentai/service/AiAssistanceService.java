package com.mafe.incidentai.service;

import com.mafe.incidentai.client.N8nWebhookClient;
import com.mafe.incidentai.domain.entity.*;
import com.mafe.incidentai.domain.enums.IncidentStatus;
import com.mafe.incidentai.domain.enums.QueryStatus;
import com.mafe.incidentai.dto.*;
import com.mafe.incidentai.exception.BusinessRuleException;
import com.mafe.incidentai.exception.ResourceNotFoundException;
import com.mafe.incidentai.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Servicio que orquesta la comunicacion con n8n, el guardado en cadena de respuestas IA, calificaciones e historial
@Service
@RequiredArgsConstructor
public class AiAssistanceService {

    private final IncidentRepository incidentRepository;
    private final AiQueryRepository aiQueryRepository;
    private final AiResponseRepository aiResponseRepository;
    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final N8nWebhookClient n8nWebhookClient;

    // 1. Registra la consulta IA en estado PENDING, dispara el webhook de n8n y refleja COMMUNICATION_ERROR si falla
    @Transactional
    public Map<String, Object> requestAiAssistance(AiAssistanceRequestDTO request) {
        Incident incident = incidentRepository.findById(request.incidentId())
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with ID: " + request.incidentId()));

        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new BusinessRuleException("Cannot request AI assistance for a CLOSED incident");
        }

        // Construimos el prompt consolidado que queda registrado para auditoria en la tabla ai_queries
        String consolidatedPrompt = String.format(
                "[%s | Priority: %s] %s - %s. Additional Context: %s",
                request.categoryName(),
                request.priority() != null ? request.priority() : incident.getPriority().name(),
                request.incidentTitle(),
                request.detailedDescription(),
                request.additionalPromptContext() != null ? request.additionalPromptContext() : "None"
        );

        AiQuery aiQuery = new AiQuery();
        aiQuery.setIncident(incident);
        aiQuery.setSentPrompt(consolidatedPrompt);
        aiQuery.setQueryStatus(QueryStatus.PENDING);

        // Guardamos primero la consulta en estado PENDING para obtener su ID autogenerado
        AiQuery savedQuery = aiQueryRepository.save(aiQuery);

        // Solo un incidente abierto pasa a IN_AI_ANALYSIS. Consultar a la IA sobre uno ya asignado o resuelto
        // no lo reabre: si no, cada consulta bajaria el porcentaje de resolucion del dashboard.
        if (incident.getStatus() == IncidentStatus.OPEN) {
            incident.setStatus(IncidentStatus.IN_AI_ANALYSIS);
            incidentRepository.save(incident);
        }

        // Disparamos la peticion HTTP hacia el webhook de n8n
        boolean deliveredToN8n = n8nWebhookClient.triggerIncidentAnalysis(savedQuery.getId(), request);

        // Si hubo timeout o error de conexion con n8n, reflejamos el estado COMMUNICATION_ERROR en la base de datos
        if (!deliveredToN8n) {
            savedQuery.setQueryStatus(QueryStatus.COMMUNICATION_ERROR);
            savedQuery.setFailureCode("N8N_UNREACHABLE");
            savedQuery.setFailureMessage("The AI service (n8n) did not accept the request or timed out");
            aiQueryRepository.save(savedQuery);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", deliveredToN8n
                ? "AI assistance request successfully sent to n8n"
                : "AI query registered, but communication with n8n failed or timed out");
        response.put("aiQueryId", savedQuery.getId());
        response.put("incidentId", incident.getId());
        response.put("queryStatus", savedQuery.getQueryStatus().name());
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    // 2. Recibe y guarda en cadena (CascadeType.ALL) la respuesta de n8n: AiResponse, ResponseSource y similarityScore
    @Transactional
    public Map<String, Object> registerAiResponse(AiResponseDTO responseDto) {
        AiQuery aiQuery = aiQueryRepository.findById(responseDto.aiQueryId())
                .orElseThrow(() -> new ResourceNotFoundException("AI Query not found with ID: " + responseDto.aiQueryId()));

        // Validamos que esta consulta no tenga ya una respuesta registrada (relacion 1:1)
        if (aiResponseRepository.findByAiQueryId(aiQuery.getId()).isPresent()) {
            throw new BusinessRuleException("An AI response is already registered for AI Query ID: " + aiQuery.getId());
        }

        AiResponse aiResponse = new AiResponse();
        aiResponse.setAiQuery(aiQuery);
        aiResponse.setSuggestedDiagnosis(responseDto.suggestedDiagnosis());
        aiResponse.setSolutionSteps(responseDto.solutionSteps());
        aiResponse.setConfidenceScore(responseDto.confidenceScore());
        aiResponse.setProcessingTimeMs(responseDto.processingTimeMs());

        // Guardado en cadena de las fuentes documentales y sus puntajes de similitud (similarityScore)
        if (responseDto.sources() != null) {
            for (AiResponseDTO.SourceDTO sourceDto : responseDto.sources()) {
                ResponseSource source = new ResponseSource();
                source.setSourceTitle(sourceDto.sourceTitle());
                source.setReferenceUrl(sourceDto.referenceUrl());
                source.setCitedSnippet(sourceDto.citedSnippet());
                source.setSimilarityScore(sourceDto.similarityScore());

                // Si n8n envio el ID de un KnowledgeArticle interno, validamos que exista y este ACTIVO y VERIFICADO (Restriccion RF02)
                if (sourceDto.knowledgeArticleId() != null) {
                    KnowledgeArticle article = knowledgeArticleRepository.findById(sourceDto.knowledgeArticleId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Knowledge article not found with ID: " + sourceDto.knowledgeArticleId()));

                    if (!Boolean.TRUE.equals(article.getActive()) || !Boolean.TRUE.equals(article.getVerified())) {
                        throw new BusinessRuleException(
                                "Cannot use an inactive or unverified knowledge article as a RAG source (ID: " + article.getId() + ")");
                    }
                    source.setKnowledgeArticle(article);
                }

                // Sincroniza ambos lados de la relacion bidireccional 1:N
                aiResponse.addSource(source);
            }
        }

        // Persistimos AiResponse y en cascada todas sus ResponseSource
        AiResponse savedResponse = aiResponseRepository.save(aiResponse);

        // Una respuesta valida reemplaza un error previo (p. ej. timeout del webhook)
        aiQuery.setFailureCode(null);
        aiQuery.setFailureMessage(null);
        // COMPLETED si hubo fuentes; NO_RESULTS si n8n no encontro documentos relevantes
        aiQuery.setQueryStatus(savedResponse.getSources().isEmpty() ? QueryStatus.NO_RESULTS : QueryStatus.COMPLETED);
        aiQueryRepository.save(aiQuery);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "AI response and sources registered successfully");
        result.put("aiResponseId", savedResponse.getId());
        result.put("aiQueryId", aiQuery.getId());
        result.put("registeredSources", savedResponse.getSources().size());
        result.put("queryStatus", aiQuery.getQueryStatus().name());
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    // Registra el fallo que reporta n8n: la consulta pasa a FAILED con su codigo y mensaje.
    // Solo aplica a consultas sin respuesta; asi un fallo tardio no pisa una respuesta valida.
    @Transactional
    public Map<String, Object> reportFailure(Long aiQueryId, AiFailureReportDTO report) {
        if (report.aiQueryId() != null && !report.aiQueryId().equals(aiQueryId)) {
            throw new BusinessRuleException("aiQueryId in the body does not match the URL");
        }
        AiQuery aiQuery = aiQueryRepository.findById(aiQueryId)
                .orElseThrow(() -> new ResourceNotFoundException("AI Query not found with ID: " + aiQueryId));
        if (aiQuery.getAiResponse() != null
                || aiQuery.getQueryStatus() == QueryStatus.COMPLETED
                || aiQuery.getQueryStatus() == QueryStatus.NO_RESULTS) {
            throw new BusinessRuleException("AI Query " + aiQueryId + " already has a response");
        }

        aiQuery.setQueryStatus(QueryStatus.FAILED);
        aiQuery.setFailureCode(report.errorCode().trim());
        // Mensaje de n8n + indicacion de que revisar, deducida del detalle tecnico (que no se guarda)
        String hint = AiFailureHints.hint(report.errorCode(), report.technicalDetail());
        String message = hint == null ? report.message().trim() : report.message().trim() + " " + hint;
        aiQuery.setFailureMessage(message.length() > 500 ? message.substring(0, 500) : message);
        aiQueryRepository.save(aiQuery);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "AI failure registered");
        result.put("aiQueryId", aiQuery.getId());
        result.put("queryStatus", aiQuery.getQueryStatus().name());
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    // 3. Registra la calificacion (Rating) de un usuario/tecnico sobre una respuesta de la IA
    @Transactional
    public RatingResponseDTO rateAiResponse(Long aiResponseId, CreateRatingRequestDTO request) {
        AiResponse aiResponse = aiResponseRepository.findById(aiResponseId)
                .orElseThrow(() -> new ResourceNotFoundException("AI Response not found with ID: " + aiResponseId));

        if (ratingRepository.findByAiResponseId(aiResponseId).isPresent()) {
            throw new BusinessRuleException("This AI response has already been rated");
        }

        User evaluator = userRepository.findById(request.evaluatorUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Evaluator user not found with ID: " + request.evaluatorUserId()));

        Rating rating = new Rating();
        rating.setAiResponse(aiResponse);
        rating.setEvaluatorUser(evaluator);
        rating.setScore(request.score());
        rating.setHelpful(request.helpful());
        rating.setFeedbackComment(request.feedbackComment());

        Rating savedRating = ratingRepository.save(rating);

        return new RatingResponseDTO(
                savedRating.getId(),
                aiResponse.getId(),
                evaluator.getId(),
                evaluator.getFullName(),
                savedRating.getScore(),
                savedRating.getHelpful(),
                savedRating.getFeedbackComment(),
                savedRating.getRatedAt()
        );
    }

    // Incidente al que pertenece una respuesta de la IA (para validar quien puede calificarla)
    @Transactional(readOnly = true)
    public Long findIncidentIdOfResponse(Long aiResponseId) {
        return aiResponseRepository.findById(aiResponseId)
                .map(r -> r.getAiQuery().getIncident().getId())
                .orElseThrow(() -> new ResourceNotFoundException("AI Response not found with ID: " + aiResponseId));
    }

    // 4. Consulta el historial de interacciones con la IA (AiQuery) para un incidente especifico
    @Transactional(readOnly = true)
    public List<AiQueryHistoryResponseDTO> getIncidentAiHistory(Long incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResourceNotFoundException("Incident not found with ID: " + incidentId);
        }

        return aiQueryRepository.findByIncidentIdOrderByQueriedAtDesc(incidentId)
                .stream()
                .map(this::mapToHistoryDTO)
                .toList();
    }

    // 5. Historial global de consultas a la IA, con filtro opcional por estado
    @Transactional(readOnly = true)
    public List<AiQueryHistoryResponseDTO> getAiHistory(QueryStatus status) {
        List<AiQuery> queries = status != null
                ? aiQueryRepository.findByQueryStatusOrderByQueriedAtDesc(status)
                : aiQueryRepository.findAllByOrderByQueriedAtDesc();
        return queries.stream().map(this::mapToHistoryDTO).toList();
    }

    // Transforma una entidad AiQuery y su arbol de relaciones (AiResponse -> ResponseSource / Rating) a DTO
    private AiQueryHistoryResponseDTO mapToHistoryDTO(AiQuery query) {
        AiResponse response = query.getAiResponse();
        AiQueryHistoryResponseDTO.AiResponseSummaryDTO responseSummary = null;

        if (response != null) {
            List<AiQueryHistoryResponseDTO.SourceSummaryDTO> sourceSummaries = response.getSources() != null
                    ? response.getSources().stream()
                    .map(source -> new AiQueryHistoryResponseDTO.SourceSummaryDTO(
                            source.getId(),
                            source.getKnowledgeArticle() != null ? source.getKnowledgeArticle().getId() : null,
                            source.getSourceTitle(),
                            source.getReferenceUrl(),
                            source.getCitedSnippet(),
                            source.getSimilarityScore()
                    ))
                    .toList()
                    : Collections.emptyList();

            Rating rating = response.getRating();
            AiQueryHistoryResponseDTO.RatingSummaryDTO ratingSummary = rating != null
                    ? new AiQueryHistoryResponseDTO.RatingSummaryDTO(
                            rating.getId(),
                            rating.getScore(),
                            rating.getHelpful(),
                            rating.getFeedbackComment(),
                            rating.getRatedAt()
                    )
                    : null;

            responseSummary = new AiQueryHistoryResponseDTO.AiResponseSummaryDTO(
                    response.getId(),
                    response.getSuggestedDiagnosis(),
                    response.getSolutionSteps(),
                    response.getConfidenceScore(),
                    response.getProcessingTimeMs(),
                    response.getRespondedAt(),
                    sourceSummaries,
                    ratingSummary
            );
        }

        return new AiQueryHistoryResponseDTO(
                query.getId(),
                query.getIncident().getId(),
                query.getIncident().getTitle(),
                query.getSentPrompt(),
                query.getQueryStatus(),
                query.getQueriedAt(),
                query.getFailureCode(),
                query.getFailureMessage(),
                responseSummary
        );
    }
}
