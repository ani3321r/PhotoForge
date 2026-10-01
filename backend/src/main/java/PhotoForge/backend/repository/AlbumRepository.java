package PhotoForge.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import PhotoForge.backend.domain.Album;

public interface AlbumRepository extends JpaRepository<PhotoForge.backend.domain.Album, UUID>{
  List<Album> findByUserIdOrderByUpdatedAtDesc(UUID userId);
  Optional<Album> findByIdAndUserId(UUID id, UUID userId);

  @Query ("""
      SELECT COUNT(ap) FROM AlbumPhoto ap
      WHERE ap.album.id = :albumId
      """)
  long countPhotosByAlbumId(@Param ("albumId") UUID albumId);
}
