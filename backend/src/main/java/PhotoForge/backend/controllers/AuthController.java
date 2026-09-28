package PhotoForge.backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import PhotoForge.backend.dto.AuthResponse;
import PhotoForge.backend.dto.LoginRequest;
import PhotoForge.backend.dto.RefreshTokenRequest;
import PhotoForge.backend.dto.RegisterRequest;
import PhotoForge.backend.dto.UserResponse;
import PhotoForge.backend.services.AuthService;
import PhotoForge.backend.services.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService authService;
  private final UserService userService;

  public AuthController(AuthService authService, UserService userService) {
    this.authService = authService;
    this.userService = userService;
  }

  @PostMapping("/register")
public AuthResponse register(
    @Valid @RequestBody RegisterRequest request
) {
    return authService.register(request);
}

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return ResponseEntity.ok(authService.refresh(request.refreshToken()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
    if (request != null && request.refreshToken() != null) {
        authService.logout(request.refreshToken());
    }
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserDetails userDetails){
    return ResponseEntity.ok(userService.getUserResponseByEmail(userDetails.getUsername()));
  }

}