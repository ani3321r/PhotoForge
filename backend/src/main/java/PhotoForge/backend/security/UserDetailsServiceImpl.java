package PhotoForge.backend.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import PhotoForge.backend.repository.UserRepository;

@Service 
public class UserDetailsServiceImpl implements UserDetailsService{
  private final UserRepository userRepository;

  public UserDetailsServiceImpl(UserRepository userRepository){
    this.userRepository = userRepository;
  }

  @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
      return userRepository.findByEmail(email.toLowerCase())
        .map(user -> org.springframework.security.core.userdetails.User.builder()
          .username(user.getEmail())
          .password(user.getPasswordHash())
          .authorities("ROLE_USER")
          .build())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
