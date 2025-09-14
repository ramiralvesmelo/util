# 📦 Projeto util

[![Build Status](https://github.com/ramiralvesmelo/util/actions/workflows/maven.yml/badge.svg)](https://github.com/ramiralvesmelo/util/actions/workflows/maven.yml)
[![Coverage Status](https://img.shields.io/codecov/c/github/ramiralvesmelo/util?logo=codecov)](https://app.codecov.io/gh/ramiralvesmelo/util)
[![Java](https://img.shields.io/badge/Java-21-blue.svg?logo=java)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_util&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_util&metric=bugs)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_util&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_util&metric=coverage)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_util&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_util&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_util&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_util&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_util&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_util&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=ramiralvesmelo_util&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=ramiralvesmelo_util)


Biblioteca Java com classes utilitárias reutilizáveis, ideal para aplicativos em **Spring Boot**, ou projetos Java genéricos.

> Centraliza funcionalidades comuns, evitando duplicações de código e promovendo boas práticas de desenvolvimento.

---

## Visão Geral

O **util** oferece um conjunto de classes utilitárias para simplificar tarefas comuns, como:

- Formatação de datas, números ou textos  
- Manipulação de arquivos ou recursos  
- Validações frequentes (strings vazias, números nulos, etc.)  
- Outros helpers configuráveis

Ideal para projetos onde você quer manter um padrão, reduzir código repetido e facilitar manutenção.

---

## Funcionalidades

- Simplicidade e reutilização de lógica comum  
- Alta flexibilidade e adaptabilidade  
- Projetado para integração com **Spring Boot**, **Spring Security** e demais stacks Java corporativas  
- Java 21, Spring Boot ≥ 3.3.x compatível

---

## Requisitos

- Java 21 ou superior  
- Maven 3.8.x ou superior  
- (Opcional) Spring Boot 3.3.x, se usar recursos do ecossistema Spring

---


## 📦 Publicar e usar o util no GitHub Packages (Maven)

### 1 Configurar o `pom.xml` do projeto util

```xml
<distributionManagement>
  <repositories>
    <repository>
      <id>github-ramir-util</id>
      <url>https://maven.pkg.github.com/ramiralvesmelo/util</url>
    </repository>
  </repositories>
</distributionManagement>
```

### 2 Configurar credenciais no `~/.m2/settings.xml` ou `%USERPROFILE%\.m2\settings.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">

  <!-- Perfis -->
  <profiles>
    <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>github-ramir-util</id>
          <url>https://maven.pkg.github.com/ramiralvesmelo/util</url>
          <releases>
            <enabled>true</enabled>
          </releases>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>

  <!-- Ativar perfil por padrão -->
  <activeProfiles>
    <activeProfile>github-ramir-util</activeProfile>
  </activeProfiles>

  <!-- Credenciais -->
  <servers>
    <server>
      <id>github-ramir-util</id>
      <username>ramiralvesmelo</username>
      <password>SEU_PERSONAL_ACCESS_TOKEN</password>
    </server>
  </servers>

</settings>

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
    <id>github-ramir-util</id>
    <url>https://maven.pkg.github.com/ramiralvesmelo/util</url>
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

