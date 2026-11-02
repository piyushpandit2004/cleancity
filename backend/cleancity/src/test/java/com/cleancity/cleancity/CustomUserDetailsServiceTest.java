package com.cleancity.cleancity;

import com.cleancity.cleancity.entity.Role;
import com.cleancity.cleancity.entity.User;
import com.cleancity.cleancity.repository.UserRepository;
import com.cleancity.cleancity.security.CustomUserDetailsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User user;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);
        user.setName("Test Citizen");
        user.setEmail("citizen@test.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.CITIZEN);
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetailsWhenUserExists() {

        when(userRepository.findByEmail("citizen@test.com"))
                .thenReturn(Optional.of(user));

        UserDetails result =
                customUserDetailsService.loadUserByUsername(
                        "citizen@test.com"
                );

        assertThat(result).isNotNull();
        assertThat(result.getUsername())
                .isEqualTo("citizen@test.com");
        assertThat(result.getPassword())
                .isEqualTo("encodedPassword");
    }

    @Test
    void loadUserByUsername_shouldThrowExceptionWhenUserDoesNotExist() {

        when(userRepository.findByEmail("unknown@test.com"))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(
                        "unknown@test.com"
                )
        );

        assertThat(exception.getMessage())
                .isEqualTo("User not found");
    }

    @Test
    void loadUserByUsername_shouldReturnCorrectRole() {

        when(userRepository.findByEmail("citizen@test.com"))
                .thenReturn(Optional.of(user));

        UserDetails result =
                customUserDetailsService.loadUserByUsername(
                        "citizen@test.com"
                );

        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CITIZEN");
    }
}
