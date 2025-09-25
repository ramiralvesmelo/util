package br.com.ramiralvesmelo.util.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindException;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.ramiralvesmelo.util.core.exception.BusinessException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(new BoomController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }

    @RestController
    @RequestMapping("/boom")
    static class BoomController {

        @GetMapping("/illegal")
        public void illegal() { throw new IllegalArgumentException("param inválido"); }

        @GetMapping("/constraint")
        public void constraint() { throw new ConstraintViolationException("erro", java.util.Set.of()); }

        @PostMapping(path="/not-readable", consumes = MediaType.APPLICATION_JSON_VALUE)
        public void notReadable(@RequestBody String body) { /* nunca chamado: falha no parse do JSON */ }

        @PostMapping("/valid-body")
        public void validBody(@RequestBody @Valid SampleDto dto) { /* validação dispara quando body {} */ }

        @GetMapping("/type-mismatch")
        public void typeMismatch(@RequestParam("id") int id) { /* id=abc -> mismatch */ }

        @GetMapping("/bind-ex")
        public void bindEx() throws Exception {
            // forçar BindException para cobrir o mesmo handler de 400
            BindException be = new BindException(new Object(), "obj");
            be.reject("code", "msg");
            throw be;
        }

        @GetMapping("/missing-param")
        public void missing(@RequestParam("q") String q) { /* ausente -> MissingServletRequestParameterException */ }

        @GetMapping("/not-found")
        public void notFound() { throw new NoSuchElementException("não achei"); }

        @GetMapping("/method-only-get")
        public void onlyGet() { /* chamar via POST gera 405 */ }

        @PostMapping(path="/unsupported", consumes = MediaType.APPLICATION_JSON_VALUE)
        public void unsupported(@RequestBody String body) { /* chamar com text/plain -> 415 */ }

        @GetMapping("/opt-lock-spring")
        public void optSpring() { throw new OptimisticLockingFailureException("desatualizado"); }

        @GetMapping("/opt-lock-jpa")
        public void optJpa() { throw new OptimisticLockException("desatualizado JPA"); }

        @GetMapping("/business")
        public void business() { throw new BusinessException("Regra de negócio falhou"); }

        @GetMapping("/generic")
        public void generic() { throw new RuntimeException("boom"); }
    }

    @Getter @Setter
    static class SampleDto {
        @NotBlank(message = "name obrigatório")
        private String name;
    }

    @Test @DisplayName("400 - IllegalArgumentException")
    void illegalArgument() throws Exception {
        mockMvc.perform(get("/boom/illegal"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("param inválido"));
    }

    @Test @DisplayName("400 - ConstraintViolationException")
    void constraintViolation() throws Exception {
        mockMvc.perform(get("/boom/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro inválido."));
    }

    @Test @DisplayName("400 - JSON malformado")
    void notReadableJson() throws Exception {
        mockMvc.perform(post("/boom/not-readable")
                        .content("{invalidJson")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("JSON inválido ou malformado."));
    }

    @Test @DisplayName("400 - @Valid body inválido")
    void validBodyViolation() throws Exception {
        mockMvc.perform(post("/boom/valid-body")
                        .content("{}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro inválido."));
    }

    @Test @DisplayName("400 - TypeMismatch em query param")
    void typeMismatch() throws Exception {
        mockMvc.perform(get("/boom/type-mismatch").param("id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro inválido."));
    }

    @Test @DisplayName("400 - BindException explícita")
    void bindException() throws Exception {
        mockMvc.perform(get("/boom/bind-ex"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro inválido."));
    }

    @Test @DisplayName("400 - Parâmetro obrigatório ausente")
    void missingParam() throws Exception {
        mockMvc.perform(get("/boom/missing-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Parâmetro obrigatório ausente")));
    }

    @Test @DisplayName("404 - NoSuchElementException")
    void notFound() throws Exception {
        mockMvc.perform(get("/boom/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Recurso não encontrado."));
    }

    @Test @DisplayName("405 - Método não permitido")
    void methodNotAllowed() throws Exception {
        mockMvc.perform(post("/boom/method-only-get"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test @DisplayName("415 - MediaType não suportado")
    void unsupportedMediaType() throws Exception {
        mockMvc.perform(post("/boom/unsupported")
                        .content("x")
                        .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test @DisplayName("500 - OptimisticLockingFailureException (Spring)")
    void optimisticLockSpring() throws Exception {
        mockMvc.perform(get("/boom/opt-lock-spring"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Registro desatualizado. Recarregue os dados e tente novamente."));
    }

    @Test @DisplayName("500 - OptimisticLockException (JPA)")
    void optimisticLockJpa() throws Exception {
        mockMvc.perform(get("/boom/opt-lock-jpa"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Registro desatualizado. Recarregue os dados e tente novamente."));
    }

    @Test @DisplayName("409 - BusinessException")
    void businessException() throws Exception {
        mockMvc.perform(get("/boom/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Regra de negócio falhou"));
    }

    @Test @DisplayName("500 - Genérica")
    void genericException() throws Exception {
        mockMvc.perform(get("/boom/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("boom"));
    }
}
