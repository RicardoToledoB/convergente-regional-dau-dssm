package cl.dssm.dau.controller;

import cl.dssm.dau.dto.ApiResponse;
import cl.dssm.dau.dto.DauIngestionResponse;
import cl.dssm.dau.service.DauIngestionService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integration/dau")
@RequiredArgsConstructor
public class DauIntegrationController {
    private final DauIngestionService service;

    @PostMapping("/eventos")
    public ResponseEntity<ApiResponse<DauIngestionResponse>> receiveEvent(
            @RequestBody JsonNode payload,
            @RequestHeader(value = "X-FILENAME", required = false) String fileName,
            HttpServletRequest request) {
        var result = service.ingest(payload, fileName, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Evento DAU recibido", result));
    }
}
