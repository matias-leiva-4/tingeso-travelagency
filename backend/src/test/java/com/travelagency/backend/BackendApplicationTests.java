package com.travelagency.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
class BackendApplicationTests {

	@Test
	void contextLoads() {
		// Verifica que el contexto de Spring Boot arranca sin excepciones.
		// Este test cubre la carga de todos los beans (@Service, @Repository, etc.)
	}

	@Test
	void main_entryPoint_doesNotThrow() {
		// Llama directamente al método main() para que JaCoCo contabilice esa línea.
		// Spring reutiliza el contexto ya cargado en contextLoads(), así que no hay
		// costo extra significativo.
		assertThatCode(() -> BackendApplication.main(new String[]{}))
				.doesNotThrowAnyException();
	}

}
