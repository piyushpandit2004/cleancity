package  com.cleancity.cleancity;

import com.cleancity.cleancity.dto.LoginRequestDto;
import com.cleancity.cleancity.dto.RegisterRequestDto;
import com.cleancity.cleancity.entity.Role;
import com.cleancity.cleancity.entity.User;
import com.cleancity.cleancity.repository.UserRepository;
import com.cleancity.cleancity.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private RegisterRequestDto registerRequest;
    private LoginRequestDto loginRequest;

    private User user;

    @BeforeEach
    void setUp() {

        registerRequest = new RegisterRequestDto();
        registerRequest.setName("Test User");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequestDto();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.CITIZEN);
    }

    // TEST 1: Successful registration
    @Test
    void registerUser_ShouldRegisterUserSuccessfully() {

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = userService.registerUser(registerRequest);

        assertNotNull(savedUser);
        assertEquals("Test User", savedUser.getName());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("encodedPassword", savedUser.getPassword());
        assertEquals(Role.CITIZEN, savedUser.getRole());

        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    // TEST 2: Registration with an existing email
    @Test
    void registerUser_ShouldThrowExceptionWhenEmailAlreadyExists() {

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(registerRequest)
        );

        assertEquals(
                "Email is already registered",
                exception.getMessage()
        );

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));
    }

    // TEST 3: Successful login
    @Test
    void loginUser_ShouldLoginSuccessfully() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password123", "encodedPassword"))
                .thenReturn(true);

        User loggedInUser = userService.loginUser(loginRequest);

        assertNotNull(loggedInUser);
        assertEquals("Test User", loggedInUser.getName());
        assertEquals("test@example.com", loggedInUser.getEmail());
        assertEquals(Role.CITIZEN, loggedInUser.getRole());

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "encodedPassword");
    }

    // TEST 4: Login with an incorrect password
    @Test
    void loginUser_ShouldThrowExceptionWhenPasswordIsIncorrect() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("wrongPassword", "encodedPassword"))
                .thenReturn(false);

        loginRequest.setPassword("wrongPassword");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.loginUser(loginRequest)
        );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("wrongPassword", "encodedPassword");
    }

    // TEST 5: Login with an email that does not exist
    @Test
    void loginUser_ShouldThrowExceptionWhenEmailDoesNotExist() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.loginUser(loginRequest)
        );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(userRepository).findByEmail("test@example.com");

        verifyNoInteractions(passwordEncoder);
    }
}
