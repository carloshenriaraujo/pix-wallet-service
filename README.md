# scaffold java

`[INSIRA DESCRICAO DO PROJETO AQUI]`

# Pré Requisitos

Para que seja possível rodar essa aplicação é necessário atender alguns requisitos básicos.

- Java 8+
- Maven 3.3+

# Compilando e inicializando

Assim como todo projeto *Maven*, é necessário primeiramente realizarmos a geração dos fontes. Conforme o exemplo abaixo:

```bash
mvn clean install
```

`[INSIRA PARTICILARIDADES DO PROJETO AQUI]`

Certifique-se de o repositório do Maven está corretamente configurado. Após os fontes terem sido gerados, basta executar
o comando abaixo para inicializar a aplicação:

```
$ java -jar NAME-SNAPSHOT.jar
```

Ou, se tiver importado por meio do IntelliJ, via classe `Application.java`. Para validar se a aplicação inicializou com
sucesso é necessario chamar o endpoint do *actuator* através do *link* abaixo:

```
http://localhost:8080/actuator/health
```

# Mostrando endpoints expostos

Assim que o projeto estiver sendo executado, é possível verificar as APIs expostas acessando a URL:

```
http://localhost:8080/swagger-ui.html
```

# Documentação

Abaixo uma lista de informações relevantes para iniciar o projeto:

## start

- Em caso de utilização local ...
- [Aplicação de referência](colocar o link de referência)
- [Documentação de referência](colocar link da documentação de referência)

## Arquitetural

- [Modelo arquitetural](Caso você esteja seguindo algum padrão descrever aqui)

![image](https://user-images.githubusercontent.com/38960317/174696081-5e271e30-7aac-4dae-a33d-b3e9b64f79a7.png)

- [Stack de tecnologias](precisamos definir uma stack padrão e colocar o link aqui)
- [Scaffolding](Precisamos definir um Scaffolding para utilização em novos projetos)

## Package para produção

- [Pipeline](Orientações para gerar versão para produção)
