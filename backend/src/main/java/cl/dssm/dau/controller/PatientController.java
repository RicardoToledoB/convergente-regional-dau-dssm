package cl.dssm.dau.controller;

import cl.dssm.dau.dto.PatientEventResponse;
import cl.dssm.dau.service.PatientEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientEventService patientEventService;

    /**
     * Retorna, en orden cronológico, el arreglo de eventos DAU asociados al paciente.
     * Si no existen eventos retorna [] con HTTP 200.
     */
    @GetMapping("/{rut}")
    public List<PatientEventResponse> getPatientEvents(@PathVariable String rut) {
        return patientEventService.findEventsByRut(rut);
    }
}
