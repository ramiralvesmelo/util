package br.com.ramiralvesmelo.util.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

class RestTemplateConfigTest {

    @Test
    void deveCriarRestTemplateComFactoryHttpComponents() {
        RestTemplateConfig cfg = new RestTemplateConfig();

        RestTemplate rt = cfg.restTemplate(1234, 5678);

        assertNotNull(rt);
        ClientHttpRequestFactory f = rt.getRequestFactory();
        assertNotNull(f);
        assertTrue(f instanceof HttpComponentsClientHttpRequestFactory);
    }
}
