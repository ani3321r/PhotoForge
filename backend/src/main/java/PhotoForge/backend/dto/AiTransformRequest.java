package PhotoForge.backend.dto;

import PhotoForge.backend.domain.AiTransformType;
import jakarta.validation.constraints.NotNull;

public record AiTransformRequest(
  @NotNull AiTransformType type,
  String prompt,
  Integer width,
  Integer height,
  String focusObject
) {
  
}
