package com.travelagency.backend.repository;

import com.travelagency.backend.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de integración para UserRepository.
 *
 * @SpringBootTest: levanta el contexto completo (repositorios + JPA + BD real).
 * @Transactional: cada test corre dentro de una transacción que se hace
 *   ROLLBACK automático al terminar → los datos de prueba no quedan en la BD.
 */
@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired UserRepository userRepository;

    private UserEntity savedUser;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setKeycloakId("kc-test-001");
        user.setFullName("María Test");
        user.setEmail("maria.test.repo@example.cl");
        user.setActive(true);
        savedUser = userRepository.save(user);
        userRepository.flush(); // fuerza el INSERT para que las queries lo encuentren
    }

    // ── findByKeycloakId ──────────────────────────────────────────────────────

    @Test
    void findByKeycloakId_existingId_returnsUser() {
        var result = userRepository.findByKeycloakId("kc-test-001");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("maria.test.repo@example.cl");
    }

    @Test
    void findByKeycloakId_unknownId_returnsEmpty() {
        var result = userRepository.findByKeycloakId("kc-no-existe-xyz");

        assertThat(result).isEmpty();
    }

    // ── findByEmail ───────────────────────────────────────────────────────────

    @Test
    void findByEmail_existingEmail_returnsUser() {
        var result = userRepository.findByEmail("maria.test.repo@example.cl");

        assertThat(result).isPresent();
        assertThat(result.get().getKeycloakId()).isEqualTo("kc-test-001");
    }

    @Test
    void findByEmail_unknownEmail_returnsEmpty() {
        var result = userRepository.findByEmail("no.existe.xyz@test.cl");

        assertThat(result).isEmpty();
    }

    // ── existsByEmail ─────────────────────────────────────────────────────────

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        assertThat(userRepository.existsByEmail("maria.test.repo@example.cl")).isTrue();
    }

    @Test
    void existsByEmail_unknownEmail_returnsFalse() {
        assertThat(userRepository.existsByEmail("fantasma.xyz@test.cl")).isFalse();
    }
}
