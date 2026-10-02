package PhotoForge.backend.dto;

import PhotoForge.backend.domain.AiTransformType;

public record AiTransformPreviewResponse(
  String previewUrl,
  AiTransformType type,
  String transformation
) {
  
}
