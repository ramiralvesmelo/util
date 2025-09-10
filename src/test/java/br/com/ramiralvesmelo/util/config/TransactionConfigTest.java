package br.com.ramiralvesmelo.util.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    TransactionConfig.class,
    TransactionConfigTest.TestBeans.class // <-- inclui o EMF mock no contexto
})
class TransactionConfigTest {

    // ===== Teste unitário puro (sem contexto) =====
    @Test
    void deveCriarJpaTransactionManagerComTimeout30() {
        // arrange
        EntityManagerFactory emf = Mockito.mock(EntityManagerFactory.class);
        TransactionConfig config = new TransactionConfig();

        // act
        PlatformTransactionManager tx = config.transactionManager(emf);

        // assert
        assertThat(tx).isInstanceOf(JpaTransactionManager.class);
        JpaTransactionManager jpaTx = (JpaTransactionManager) tx;
        assertThat(jpaTx.getEntityManagerFactory()).isSameAs(emf);
        assertThat(jpaTx.getDefaultTimeout()).isEqualTo(30);
    }

    // ===== Beans auxiliares para o contexto Spring =====
    @Configuration
    static class TestBeans {
        @Bean
        EntityManagerFactory entityManagerFactory() {
            // mock suficiente para o TransactionManager
            return Mockito.mock(EntityManagerFactory.class);
        }
    }

    // ===== Teste com contexto Spring (bean registrado) =====
    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void contextoDeveExporBeanPlatformTransactionManager() {
        assertThat(transactionManager).isNotNull();
        assertThat(transactionManager).isInstanceOf(JpaTransactionManager.class);
    }
}
