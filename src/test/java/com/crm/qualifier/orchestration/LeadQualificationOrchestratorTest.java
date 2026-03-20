package com.crm.qualifier.orchestration;

import com.crm.qualifier.cache.ComplianceCache;
import com.crm.qualifier.domain.*;
import com.crm.qualifier.service.*;
import com.crm.qualifier.service.ComplianceBureauService.ComplianceResponse;
import com.crm.qualifier.service.ComplianceBureauService.ComplianceStatus;
import com.crm.qualifier.service.JudicialService.JudicialResponse;
import com.crm.qualifier.service.JudicialService.JudicialStatus;
import com.crm.qualifier.service.RegistryService.RegistryResponse;
import com.crm.qualifier.service.RegistryService.RegistryStatus;
import com.crm.qualifier.validation.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Lead Qualification Orchestrator Tests")
class LeadQualificationOrchestratorTest {

    private static final Lead TEST_LEAD = new Lead(
        "123456789", LocalDate.of(1990, 5, 15), "John", "Doe", "john@example.com"
    );

    @TempDir
    Path tempDir;

    private ComplianceCache createCache() {
        return new ComplianceCache(tempDir.resolve("cache.json"), Duration.ofHours(24));
    }

    @Test
    @DisplayName("Should APPROVE when all validations pass and score > 60")
    void shouldApproveWhenAllPass() {
        RegistryService registryService = new RegistryService() {
            @Override
            public RegistryResponse check(String nationalId, String firstName, String lastName, LocalDate birthdate) {
                return new RegistryResponse(RegistryStatus.MATCH, firstName, lastName, birthdate);
            }
        };
        JudicialService judicialService = new JudicialService() {
            @Override
            public JudicialResponse check(String nationalId) {
                return new JudicialResponse(JudicialStatus.CLEAN);
            }
        };
        ComplianceBureauService complianceService = new ComplianceBureauService() {
            @Override
            public ComplianceResponse check(String nationalId) {
                return new ComplianceResponse(ComplianceStatus.CLEAR);
            }
        };
        QualificationScoreService scoreService = new QualificationScoreService() {
            @Override
            public int generateScore() {
                return 85;
            }
        };

        LeadQualificationOrchestrator orchestrator = buildOrchestrator(
            registryService, judicialService, complianceService, scoreService
        );

        PipelineResult result = orchestrator.qualify(TEST_LEAD);

        assertEquals(QualificationStatus.APPROVED, result.status());
        assertTrue(result.getProspect().isPresent());
        assertEquals(85, result.getProspect().get().qualificationScore());
        assertEquals(4, result.validationResults().size());
    }

    @Test
    @DisplayName("Should REJECT when registry returns MISMATCH")
    void shouldRejectOnRegistryMismatch() {
        RegistryService registryService = new RegistryService() {
            @Override
            public RegistryResponse check(String nationalId, String firstName, String lastName, LocalDate birthdate) {
                return new RegistryResponse(RegistryStatus.MISMATCH, "Unknown", lastName, birthdate);
            }
        };
        JudicialService judicialService = new JudicialService() {
            @Override
            public JudicialResponse check(String nationalId) {
                return new JudicialResponse(JudicialStatus.CLEAN);
            }
        };

        LeadQualificationOrchestrator orchestrator = buildOrchestrator(
            registryService, judicialService, clearComplianceService(), fixedScoreService(85)
        );

        PipelineResult result = orchestrator.qualify(TEST_LEAD);

        assertEquals(QualificationStatus.REJECTED, result.status());
        assertTrue(result.getProspect().isEmpty());
        // Should have 2 results (registry + judicial ran in parallel)
        assertEquals(2, result.validationResults().size());
    }

    @Test
    @DisplayName("Should REJECT when judicial has records")
    void shouldRejectOnJudicialRecords() {
        RegistryService registryService = new RegistryService() {
            @Override
            public RegistryResponse check(String nationalId, String firstName, String lastName, LocalDate birthdate) {
                return new RegistryResponse(RegistryStatus.MATCH, firstName, lastName, birthdate);
            }
        };
        JudicialService judicialService = new JudicialService() {
            @Override
            public JudicialResponse check(String nationalId) {
                return new JudicialResponse(JudicialStatus.HAS_RECORDS);
            }
        };

        LeadQualificationOrchestrator orchestrator = buildOrchestrator(
            registryService, judicialService, clearComplianceService(), fixedScoreService(85)
        );

        PipelineResult result = orchestrator.qualify(TEST_LEAD);

        assertEquals(QualificationStatus.REJECTED, result.status());
        assertEquals(2, result.validationResults().size());
    }

    @Test
    @DisplayName("Should return MANUAL_REVIEW when compliance service is down")
    void shouldReturnManualReviewWhenComplianceDown() {
        ComplianceBureauService failingService = new ComplianceBureauService() {
            @Override
            public ComplianceResponse check(String nationalId) {
                throw new RuntimeException("Compliance Bureau service unavailable");
            }
        };

        LeadQualificationOrchestrator orchestrator = buildOrchestrator(
            matchRegistryService(), cleanJudicialService(), failingService, fixedScoreService(85)
        );

        PipelineResult result = orchestrator.qualify(TEST_LEAD);

        assertEquals(QualificationStatus.MANUAL_REVIEW, result.status());
        assertTrue(result.getProspect().isEmpty());
        assertEquals(3, result.validationResults().size());
    }

    @Test
    @DisplayName("Should REJECT when compliance returns FLAGGED")
    void shouldRejectOnComplianceFlagged() {
        ComplianceBureauService flaggedService = new ComplianceBureauService() {
            @Override
            public ComplianceResponse check(String nationalId) {
                return new ComplianceResponse(ComplianceStatus.FLAGGED);
            }
        };

        LeadQualificationOrchestrator orchestrator = buildOrchestrator(
            matchRegistryService(), cleanJudicialService(), flaggedService, fixedScoreService(85)
        );

        PipelineResult result = orchestrator.qualify(TEST_LEAD);

        assertEquals(QualificationStatus.REJECTED, result.status());
        assertEquals(3, result.validationResults().size());
    }

    @Test
    @DisplayName("Should REJECT when score <= 60")
    void shouldRejectOnLowScore() {
        LeadQualificationOrchestrator orchestrator = buildOrchestrator(
            matchRegistryService(), cleanJudicialService(), clearComplianceService(), fixedScoreService(45)
        );

        PipelineResult result = orchestrator.qualify(TEST_LEAD);

        assertEquals(QualificationStatus.REJECTED, result.status());
        assertEquals(4, result.validationResults().size());
    }

    @Test
    @DisplayName("Should REJECT when score is exactly 60")
    void shouldRejectOnScoreExactly60() {
        LeadQualificationOrchestrator orchestrator = buildOrchestrator(
            matchRegistryService(), cleanJudicialService(), clearComplianceService(), fixedScoreService(60)
        );

        PipelineResult result = orchestrator.qualify(TEST_LEAD);

        assertEquals(QualificationStatus.REJECTED, result.status());
    }

    @Test
    @DisplayName("Should APPROVE when score is exactly 61")
    void shouldApproveOnScore61() {
        LeadQualificationOrchestrator orchestrator = buildOrchestrator(
            matchRegistryService(), cleanJudicialService(), clearComplianceService(), fixedScoreService(61)
        );

        PipelineResult result = orchestrator.qualify(TEST_LEAD);

        assertEquals(QualificationStatus.APPROVED, result.status());
        assertTrue(result.getProspect().isPresent());
        assertEquals(61, result.getProspect().get().qualificationScore());
    }

    @RepeatedTest(5)
    @DisplayName("Integration: Should run full pipeline with real services (multiple outcomes)")
    void shouldRunFullPipelineIntegration() {
        RegistryService registryService = new RegistryService();
        JudicialService judicialService = new JudicialService();
        ComplianceBureauService complianceService = new ComplianceBureauService();
        QualificationScoreService scoreService = new QualificationScoreService();

        LeadQualificationOrchestrator orchestrator = buildOrchestrator(
            registryService, judicialService, complianceService, scoreService
        );

        PipelineResult result = orchestrator.qualify(TEST_LEAD);

        assertNotNull(result);
        assertNotNull(result.status());
        assertNotNull(result.validationResults());
        assertFalse(result.validationResults().isEmpty());

        // Verify status-prospect consistency
        if (result.status() == QualificationStatus.APPROVED) {
            assertTrue(result.getProspect().isPresent());
        } else {
            assertTrue(result.getProspect().isEmpty());
        }
    }

    // --- Helper methods to build orchestrator from services ---

    private LeadQualificationOrchestrator buildOrchestrator(
            RegistryService registryService,
            JudicialService judicialService,
            ComplianceBureauService complianceService,
            QualificationScoreService scoreService) {
        return new LeadQualificationOrchestrator(
            new RegistryValidator(registryService),
            new JudicialRecordsValidator(judicialService),
            new ComplianceBureauValidator(complianceService, createCache()),
            new QualificationScoreValidator(scoreService)
        );
    }

    private RegistryService matchRegistryService() {
        return new RegistryService() {
            @Override
            public RegistryResponse check(String nationalId, String firstName, String lastName, LocalDate birthdate) {
                return new RegistryResponse(RegistryStatus.MATCH, firstName, lastName, birthdate);
            }
        };
    }

    private JudicialService cleanJudicialService() {
        return new JudicialService() {
            @Override
            public JudicialResponse check(String nationalId) {
                return new JudicialResponse(JudicialStatus.CLEAN);
            }
        };
    }

    private ComplianceBureauService clearComplianceService() {
        return new ComplianceBureauService() {
            @Override
            public ComplianceResponse check(String nationalId) {
                return new ComplianceResponse(ComplianceStatus.CLEAR);
            }
        };
    }

    private QualificationScoreService fixedScoreService(int score) {
        return new QualificationScoreService() {
            @Override
            public int generateScore() {
                return score;
            }
        };
    }
}
