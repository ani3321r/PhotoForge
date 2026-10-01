package PhotoForge.backend.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record ImportPhotoRequest(
  @NotEmpty List<String> imagekitFileIds
) {
  
}
