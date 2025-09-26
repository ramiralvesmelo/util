package br.com.ramiralvesmelo.util.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;

class TransactionConfigTest {

    @Test
    void deveCriarJpaTransactionManagerComTimeout() {
        TransactionConfig cfg = new TransactionConfig();
        EntityManagerFactory emf = mock(EntityManagerFactory.class);

        PlatformTransactionManager tx = cfg.transactionManager(emf);

        assertNotNull(tx);
        assertTrue(tx instanceof JpaTransactionManager);

        JpaTransactionManager jpaTx = (JpaTransactionManager) tx;
        assertEquals(30, jpaTx.getDefaultTimeout());
        assertEquals(emf, jpaTx.getEntityManagerFactory());
    }
}
