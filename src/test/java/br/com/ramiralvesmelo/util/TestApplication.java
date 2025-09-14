package br.com.ramiralvesmelo.util;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.test.context.ActiveProfiles;

@SpringBootApplication
@ActiveProfiles("test")
public class TestApplication {
  // vazio: serve apenas como @SpringBootConfiguration para os testes
}