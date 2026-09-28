package PhotoForge.backend.services;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import PhotoForge.backend.domain.User;
import PhotoForge.backend.dto.UserResponse;
import PhotoForge.backend.exceptions.UnauthorizedException;
import PhotoForge.backend.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class UserService {
    
  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public UserResponse getUserResponseByEmail(String email) {
    return toUserResponse(getByEmail(email));
  }

  public User getByEmail(String email) {
    return userRepository.findByEmail(email.toLowerCase())
      .orElseThrow(() -> new UnauthorizedException("User not found"));
  }

  public Optional<String> findEmailById(UUID userId) {
    return userRepository.findById(userId).map(User::getEmail);
  }

  public boolean existsById(UUID userId) {
    return userRepository.existsById(userId);
  }

  public UserResponse toUserResponse(User user) {
    return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName());
  }
}