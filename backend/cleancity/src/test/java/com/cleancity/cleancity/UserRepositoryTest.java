
        package com.cleancity.cleancity.repository;

import com.cleancity.cleancity.entity.Role;
import com.cleancity.cleancity.entity.User;

import com.cleancity.cleancity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setName("Test Citizen");
        user.setEmail("repositorytest@example.com");
        user.setPassword("testPassword");
        user.setRole(Role.CITIZEN);

        entityManager.persistAndFlush(user);
        entityManager.clear();
    }

    @Test
    void findByEmail_shouldReturnUserWhenEmailExists() {

        Optional<User> result =
                userRepository.findByEmail(
                        "repositorytest@example.com"
                );

        assertThat(result).isPresent();
        assertThat(result.get().getName())
                .isEqualTo("Test Citizen");
        assertThat(result.get().getEmail())
                .isEqualTo("repositorytest@example.com");
    }

    @Test
    void existsByEmail_shouldReturnTrueWhenEmailExists() {

        boolean exists =
                userRepository.existsByEmail(
                        "repositorytest@example.com"
                );

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_shouldReturnFalseWhenEmailDoesNotExist() {

        boolean exists =
                userRepository.existsByEmail(
                        "notfound@example.com"
                );

        assertThat(exists).isFalse();
    }
}
