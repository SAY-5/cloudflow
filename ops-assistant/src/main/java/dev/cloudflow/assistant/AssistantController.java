package dev.cloudflow.assistant;

import dev.cloudflow.assistant.AssistantService.AssistResult;
import dev.cloudflow.assistant.remediation.RemediationService;
import dev.cloudflow.assistant.remediation.RemediationService.Remediation;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** The ops-assistant RAG and remediation endpoints. */
@RestController
@RequestMapping("/v1")
public class AssistantController {

  private final AssistantService assistantService;
  private final RemediationService remediationService;

  public AssistantController(
      AssistantService assistantService, RemediationService remediationService) {
    this.assistantService = assistantService;
    this.remediationService = remediationService;
  }

  public record AssistRequest(@NotBlank String question) {}

  @PostMapping("/assist")
  public AssistResult assist(@RequestBody AssistRequest request) {
    return assistantService.assist(request.question());
  }

  @PostMapping("/remediate")
  public Remediation remediate(@RequestBody AssistRequest request) {
    return remediationService
        .suggest(request.question())
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "no matching runbook for that question"));
  }
}
