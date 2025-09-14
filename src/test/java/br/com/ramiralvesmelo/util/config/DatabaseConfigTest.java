package br.com.ramiralvesmelo.util.config;



import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DatabaseConfig.class)
@ActiveProfiles("test")
class DatabaseConfigTest {

  @Autowired
  DataSource dataSource;

  @Test
  void deveCriarDataSourceH2EmMemoriaNoProfileTest() throws Exception {
    assertThat(dataSource).isNotNull();

    try (Connection c = dataSource.getConnection()) {
      DatabaseMetaData md = c.getMetaData();
      assertThat(md.getURL()).contains("jdbc:h2:mem:testdb");
      assertThat(md.getDatabaseProductName()).containsIgnoringCase("H2");
    }
  }
}
