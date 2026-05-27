package dev.cloudflow.assistant;

import dev.cloudflow.assistant.AssistantService.AssistResult;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The ops-assistant RAG endpoint. */
@RestController
@RequestMapping("/v1")
public class AssistantController {

  private final AssistantService assistantService;

  public AssistantController(AssistantService assistantService) {
    this.assistantService = assistantService;
  }

  public record AssistRequest(@NotBlank String question) {}

  @PostMapping("/assist")
  public AssistResult assist(@RequestBody AssistRequest request) {
    return assistantService.assist(request.question());
  }
}
