# 📦 Projeto util

O **util** é um módulo Java que reúne um conjunto de **classes utilitárias** reutilizáveis em diferentes projetos.
Seu objetivo é centralizar funcionalidades comuns, evitando duplicação de código e promovendo boas práticas de desenvolvimento.

---

## 🚀 Benefícios

* Código **padronizado** e fácil de integrar em projetos Spring Boot ou Java puro
* **Reutilização** de lógica em múltiplos módulos
* **Redução de duplicação** e aumento de manutenibilidade
* Preparado para uso em ambientes **corporativos** com autenticação via Keycloak

---

## 🔑 Funcionalidades principais

### 📧 Validação de e-mails (`EmailUtil`)

* Validação sintática com regex robusta
* Logs de depuração via Lombok `@Slf4j`

### ⚠️ Exceções de negócio (`BusinessException`)

* Exceção customizada com suporte a códigos HTTP (`HttpStatus`)
* Métodos auxiliares para criar exceções comuns (`conflict`, `notFound`)

### 🆔 Geração de identificadores (`OrderNumberUtil`)

* Criação de números de pedido únicos no formato `ORD-{ULID}`
* Baseado em **ULID** (lexicograficamente ordenável e mais legível que UUID)

### 🔐 Integração com segurança (Keycloak) (`KeycloakRoleConverter`)

* Conversão automática de **roles do Keycloak** para `GrantedAuthority` do Spring Security
* Suporte a roles de **realm** e **resource (client-specific)**

---

