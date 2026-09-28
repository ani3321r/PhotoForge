package PhotoForge.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import PhotoForge.backend.domain.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID>{
  Optional<RefreshToken> findByToken(String token);

  void deleteByUserId(UUID userId);
}
