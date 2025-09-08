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


## 📦 Publicar e usar o util no GitHub Packages (Maven)

### 1 Configurar o `pom.xml` do projeto util

```xml
<distributionManagement>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/SEU_USUARIO/SEU_REPOSITORIO</url>
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

