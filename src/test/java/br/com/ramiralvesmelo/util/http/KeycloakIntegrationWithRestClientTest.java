package br.com.ramiralvesmelo.util.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import br.com.ramiralvesmelo.util.exception.JwtRestClientException;
import lombok.extern.slf4j.Slf4j;

@Disabled("Desativado temporariamente enquanto ajusto o Keycloak")
@SpringBootTest
@TestPropertySource("classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
class KeycloakIntegrationWithRestClientTest {

    private static final String IMAGE = "quay.io/keycloak/keycloak:24.0.5";

    // ===== lidos do properties =====
    @Value("${app.realm.keycloak.realm}")
    private String realm;

    @Value("${app.realm.keycloak.client.event}")
    private String clientEvent;

    @Value("${app.realm.keycloak.secret.ok}")
    private String secretOk;

    // ===== container gerenciado manualmente (para usar @Value) =====
    private GenericContainer<?> keycloak;

    // ===== endpoints calculados após subir o container =====
    private String baseUrl;
    private String realmBase;
    private String tokenUri;
    private String userInfoUri;
    private String wellKnownUri;

    @BeforeAll
    void setUp() {
        // Cria o container APÓS @Value já ter injetado os valores
        keycloak = new GenericContainer<>(IMAGE)
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("realm-app-api-realm.json", 0700),
                "/opt/keycloak/data/import/realm-app-api-realm.json"
            )
            .withCommand("start-dev --http-port=8080 --import-realm")
            .withExposedPorts(8080)
            .waitingFor(
                Wait.forHttp("/realms/" + realm + "/.well-known/openid-configuration")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(3))
            );

        keycloak.start();

        String host = keycloak.getHost();
        Integer port = keycloak.getMappedPort(8080);

        baseUrl      = "http://" + host + ":" + port;
        realmBase    = baseUrl + "/realms/" + realm;
        tokenUri     = realmBase + "/protocol/openid-connect/token";
        userInfoUri  = realmBase + "/protocol/openid-connect/userinfo";
        wellKnownUri = realmBase + "/.well-known/openid-configuration";

        log.info("""
            Keycloak iniciado com sucesso:
              • baseUrl      : {}
              • realmBase    : {}
              • tokenUri     : {}
              • userInfoUri  : {}
              • wellKnownUri : {}
            """, baseUrl, realmBase, tokenUri, userInfoUri, wellKnownUri);
    }

    @AfterAll
    void tearDown() {
        if (keycloak != null) {
            keycloak.stop();
            log.info("Keycloak encerrado.");
        }
    }

    /* ------------ helpers ------------ */

    private void initRestClient(String clientId, String secret) {
        log.info("""
            Inicializando RestClient:
              • clientId : {}
              • baseUrl  : {}
              • tokenUri : {}
            """, clientId, baseUrl, tokenUri);

        RestClient.init(
            new RestClient.Config()
                .baseUrl(baseUrl)
                .tokenUri(tokenUri)
                .clientId(clientId)
                .clientSecret(secret)
                .scope("")                // opcional
                .connectTimeoutMs(5000)
                .readTimeoutMs(15000)
                .restTemplate(new RestTemplate())
        );
    }

    private String obterAccessTokenUsuario(String username, String password) {
        var rt = new RestTemplate();

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        var form = new org.springframework.util.LinkedMultiValueMap<String, String>();
        form.add("grant_type", "password");
        form.add("client_id", "app-api");    // cliente público com password grant habilitado no realm JSON
        form.add("username", username);
        form.add("password", password);

        ResponseEntity<Map<String, Object>> tokenResp = rt.exchange(
            tokenUri,
            HttpMethod.POST,
            new HttpEntity<>(form, headers),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertEquals(200, tokenResp.getStatusCodeValue(), "Token endpoint deve retornar 200");
        assertNotNull(tokenResp.getBody());
        assertTrue(tokenResp.getBody().containsKey("access_token"));
        return tokenResp.getBody().get("access_token").toString();
    }

    /* ------------ testes ------------ */

    @Test
    void clientCredentials_obterToken_ok() {
        initRestClient(clientEvent, secretOk);

        String token = RestClient.obterAccessToken();
        assertNotNull(token);
        assertTrue(token.contains("."), "Esperado formato JWT (com pontos).");
        assertTrue(token.length() > 20);
    }

    @Test
    void userInfo_com_token_de_usuario_via_password_grant() {
        // 1) Inicializa RestClient (sempre antes de usar RestClient.*)
        initRestClient(clientEvent, secretOk);

        // 2) Obtém token de usuário usando password grant (cliente público "app-api")
        String userAccessToken = obterAccessTokenUsuario("appapi", "123");
        assertNotNull(userAccessToken);

        // 3) Chama /userinfo com ESSE token de usuário (via RestTemplate direto)
        var rt = new RestTemplate();
        var h2 = new HttpHeaders();
        h2.setBearerAuth(userAccessToken);

        var rsp = rt.exchange(
            userInfoUri,
            HttpMethod.GET,
            new HttpEntity<>(h2),
            new ParameterizedTypeReference<Map<String,Object>>() {}
        );
        var userInfo = rsp.getBody();

        assertNotNull(userInfo);
        assertEquals("appapi", String.valueOf(userInfo.get("preferred_username")));
    }

    @Test
    void wellKnown_openid_configuration_via_RestClient_get() {
        initRestClient(clientEvent, secretOk);

        Map<String, Object> conf = RestClient.get(
            wellKnownUri, new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertNotNull(conf);
        assertEquals(tokenUri, conf.get("token_endpoint"));
    }

    @Test
    void clientCredentials_secret_invalido_deve_falhar() {
        initRestClient(clientEvent, "segredo-invalido");

        JwtRestClientException ex = assertThrows(
            JwtRestClientException.class,
            RestClient::obterAccessToken
        );
        assertTrue(ex.getMessage().contains("Falha ao obter token"));
    }
}
