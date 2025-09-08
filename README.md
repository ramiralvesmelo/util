# 📦 Projeto util

[![Build Status](https://github.com/ramiralvesmelo/util/actions/workflows/maven.yml/badge.svg)](https://github.com/ramiralvesmelo/util/actions/workflows/maven.yml)
[![CI/CD](https://img.shields.io/github/actions/workflow/status/ramiralvesmelo/util/maven.yml?label=CI%2FCD&logo=githubactions)](https://github.com/ramiralvesmelo/util/actions)
[![GitHub Release](https://img.shields.io/github/v/release/ramiralvesmelo/util?logo=github)](https://github.com/ramiralvesmelo/util/releases)
[![Coverage Status](https://img.shields.io/codecov/c/github/ramiralvesmelo/util?logo=codecov)](https://app.codecov.io/gh/ramiralvesmelo/util)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_ramiralvesmelo_util&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_ramiralvesmelo_util&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_ramiralvesmelo_util&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_ramiralvesmelo_util&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_ramiralvesmelo_util&metric=coverage)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_ramiralvesmelo_util&metric=bugs)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_ramiralvesmelo_util&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_ramiralvesmelo_util&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)


[![Java](https://img.shields.io/badge/Java-21-blue.svg?logo=java)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)


O projeto **UTIL** é um módulo de **funcionalidades reutilizáveis**, desenvolvido em **Java 21** (compatível com **Java 24**) e baseado no **Spring Boot 3.3.5**. Ele concentra um conjunto de **classes utilitárias** aplicáveis em diferentes projetos, com o propósito de **centralizar funcionalidades comuns**, **evitar duplicação de código** e **promover boas práticas de desenvolvimento**.


---

## 🚀 Benefícios

* Código **padronizado** e fácil de integrar em projetos Spring Boot ou Java puro
* **Reutilização** de lógica em múltiplos módulos
* **Redução de duplicação** e aumento de manutenibilidade
* Preparado para uso em ambientes **corporativos** com autenticação via Keycloak

---


## 📦 Publicar e usar o util no GitHub Packages (Maven)

### 1 Configurar o `pom.xml` do projeto util

```xml
<distributionManagement>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/ramiralvesmelo/SEU_REPOSITORIO</url>
  </repository>
</distributionManagement>
```

### 2 Configurar credenciais no `~/.m2/settings.xml`

```xml
<servers>
  <server>
    <id>github</id>
    <username>SEU_USUARIO</username>
    <password>${GITHUB_TOKEN}</password>
  </server>
</servers>
```

### 3 Fazer o deploy

```bash
mvn clean deploy
```

### 4️⃣ Usar em outro projeto

Adicione o repositório:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/SEU_USUARIO/SEU_REPOSITORIO</url>
  </repository>
</repositories>
```

E a dependência:

```xml
<dependencies>
  <dependency>
    <groupId>br.com.ramiralvesmelo</groupId>
    <artifactId>util</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```


---

