package com.backendlearninglab.spring.security;

import com.backendlearninglab.spring.repository.SpringDataUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service loading users from the database for Spring Security's AuthenticationManager.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SpringDataUserRepository userRepository;

    public CustomUserDetailsService(SpringDataUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .map(CustomUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user found registered with email: " + email));
    }
}
