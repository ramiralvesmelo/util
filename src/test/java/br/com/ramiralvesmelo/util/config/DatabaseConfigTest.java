package br.com.ramiralvesmelo.util.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

class DatabaseConfigTest {

    private final DatabaseConfig config = new DatabaseConfig();

    @Test
    void deveCriarDataSourceParaProfileTest() {
        DataSource ds = config.dataSource();
        assertNotNull(ds, "DataSource não deve ser nulo");
    }

    @Test
    void deveCriarJpaVendorAdapter() {
        JpaVendorAdapter adapter = config.jpaVendorAdapter();
        assertNotNull(adapter);
        assertTrue(adapter instanceof HibernateJpaVendorAdapter, 
            "Adapter deve ser do tipo HibernateJpaVendorAdapter");
    }
}
