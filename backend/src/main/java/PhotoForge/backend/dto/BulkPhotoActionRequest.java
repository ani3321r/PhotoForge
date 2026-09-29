package PhotoForge.backend.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

public record BulkPhotoActionRequest(
  @NotEmpty List<UUID> photoIds
) {
  
}
