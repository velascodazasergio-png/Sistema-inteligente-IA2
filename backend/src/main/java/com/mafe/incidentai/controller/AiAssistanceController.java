package com.mafe.incidentai.controller;

import com.mafe.incidentai.security.AccessPolicy;
import com.mafe.incidentai.domain.enums.QueryStatus;
import com.mafe.incidentai.dto.*;
import com.mafe.incidentai.service.AiAssistanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

// REST controller that orchestrates incident analysis with n8n, stores responses, ratings and query history
@RestController
@RequestMapping("/api/v1/ai-assistance")
@RequiredArgsConstructor
@Tag(
    name = "AI Assistance (n8n Integration)",
    description = "Endpoints to trigger the analysis in n8n, store responses with their sources, rate suggestions and read the history"
)
public class AiAssistanceController {

    private final AiAssistanceService aiAssistanceService;
    private final AccessPolicy accessPolicy;

    // 1. Triggers the analysis of an incident through the n8n webhook and records the AiQuery (PENDING or COMMUNICATION_ERROR)
    @Operation(
        summary = "Request AI incident analysis (Trigger to n8n)",
        description = "Records the AiQuery, moves the incident to IN_AI_ANALYSIS, calls n8n over HTTP and marks COMMUNICATION_ERROR on a timeout or error."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Request accepted and saved in the database"),
        @ApiResponse(responseCode = "400", description = "Invalid input (for example, an empty description)"),
        @ApiResponse(responseCode = "404", description = "Incident not found"),
        @ApiResponse(responseCode = "409", description = "The incident is closed (CLOSED)")
    })
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestAiAssistance(
            @Valid @RequestBody AiAssistanceRequestDTO request) {
        accessPolicy.checkIncident(request.incidentId());
        Map<String, Object> result = aiAssistanceService.requestAiAssistance(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    // 2. Receives the n8n callback and saves the AiResponse with its ResponseSource entries and similarityScore in cascade
    @Operation(
        summary = "Register AI response and sources (Callback webhook from n8n)",
        description = "Saves the n8n response (AiResponse) together with its sources (ResponseSource) and their similarity. "
                + "Marks the AiQuery as COMPLETED when there are sources, or NO_RESULTS when n8n found no relevant documents. "
                + "If app.callback.token is set, the X-Callback-Token header is required."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "AiResponse and its sources saved"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Missing or wrong callback token"),
        @ApiResponse(responseCode = "404", description = "Original AiQuery not found"),
        @ApiResponse(responseCode = "409", description = "The query already has a response")
    })
    @PostMapping("/responses")
    public ResponseEntity<Map<String, Object>> registerAiResponse(
            @Valid @RequestBody AiResponseDTO responseDto) {
        Map<String, Object> result = aiAssistanceService.registerAiResponse(responseDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // 2b. Receives from n8n the report of a failure while processing the query (embeddings, Qdrant or LLM)
    @Operation(
        summary = "Report an AI processing failure (Callback webhook from n8n)",
        description = "Marks the query as FAILED and stores the failure code and message. If app.callback.token is set, the X-Callback-Token header is required."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Failure recorded"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Missing or wrong callback token"),
        @ApiResponse(responseCode = "404", description = "AiQuery not found"),
        @ApiResponse(responseCode = "409", description = "The query already has a response or the ID does not match")
    })
    @PostMapping("/queries/{aiQueryId}/failure")
    public ResponseEntity<Map<String, Object>> reportFailure(
            @PathVariable Long aiQueryId,
            @Valid @RequestBody AiFailureReportDTO report) {
        return ResponseEntity.ok(aiAssistanceService.reportFailure(aiQueryId, report));
    }

    // 3. Records the rating of a response generated by the AI
    @Operation(
        summary = "Rate an AI response",
        description = "Lets a technician or user rate from 1 to 5 how useful an AI-generated response was."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Rating recorded"),
        @ApiResponse(responseCode = "400", description = "Score outside 1 to 5 or incomplete data"),
        @ApiResponse(responseCode = "404", description = "AI response or evaluating user not found"),
        @ApiResponse(responseCode = "409", description = "The AI response was already rated")
    })
    @PostMapping("/responses/{aiResponseId}/ratings")
    public ResponseEntity<RatingResponseDTO> rateAiResponse(
            @PathVariable Long aiResponseId,
            @Valid @RequestBody CreateRatingRequestDTO request) {
        // Only an administrator can rate on behalf of someone else; everyone else rates as themselves
        if (!accessPolicy.isAdmin()) {
            accessPolicy.checkIncident(aiAssistanceService.findIncidentIdOfResponse(aiResponseId));
            request = new CreateRatingRequestDTO(accessPolicy.user().id(), request.score(), request.helpful(),
                    request.feedbackComment());
        }
        RatingResponseDTO response = aiAssistanceService.rateAiResponse(aiResponseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 4. Returns the full history of AI queries (AiQuery) for an incident
    @Operation(
        summary = "Get AI query history for an incident",
        description = "Returns every query sent to n8n for an incident, with its responses, sources, similarities and ratings."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "History of the incident"),
        @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    @GetMapping("/incidents/{incidentId}/history")
    public ResponseEntity<List<AiQueryHistoryResponseDTO>> getIncidentAiHistory(
            @PathVariable Long incidentId) {
        accessPolicy.checkIncident(incidentId);
        List<AiQueryHistoryResponseDTO> history = aiAssistanceService.getIncidentAiHistory(incidentId);
        return ResponseEntity.ok(history);
    }

    // 5. Returns the global history of AI queries
    @Operation(
        summary = "Get global AI query history",
        description = "Returns every AI query, newest first. Optional filter by status."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Global history"),
        @ApiResponse(responseCode = "400", description = "Invalid query status")
    })
    @GetMapping("/history")
    public ResponseEntity<List<AiQueryHistoryResponseDTO>> getAiHistory(
            @RequestParam(required = false) QueryStatus status) {
        List<AiQueryHistoryResponseDTO> history = aiAssistanceService.getAiHistory(status);
        // An employee only sees the queries of their own incidents
        if (accessPolicy.isEmployee()) {
            Set<Long> own = accessPolicy.ownIncidentIds();
            history = history.stream().filter(q -> own.contains(q.incidentId())).toList();
        }
        return ResponseEntity.ok(history);
    }
}
