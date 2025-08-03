# =========================================================================
# ESTÁGIO 1: Builder - Compila a aplicação em uma imagem nativa
# Utilizamos a imagem oficial do GraalVM que já contém o Maven e as ferramentas nativas.
# =========================================================================
FROM ghcr.io/graalvm/graalvm-community:java21 AS builder

# Define o diretório de trabalho dentro do contêiner
WORKDIR /app

# Copia primeiro o pom.xml para aproveitar o cache do Docker.
# As dependências só serão baixadas novamente se o pom.xml mudar.
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o restante do código fonte do projeto
COPY src ./src

# Executa o build nativo usando o perfil 'native' do Maven.
# Isso gera um executável na pasta /app/target/
# O nome do executável é definido pela propriedade finalName no pom.xml.
RUN mvn -Pnative -DskipTests native:compile -X

# =========================================================================
# ESTÁGIO 2: Imagem Final - Cria a imagem de produção final
# Usamos uma imagem base mínima, pois não precisamos de um JDK completo para rodar o executável.
# =========================================================================
FROM debian:slim-bullseye

# Define o diretório de trabalho
WORKDIR /app

# Copia APENAS o executável compilado do estágio 'builder'.
# Renomeamos para 'application' para simplificar o comando ENTRYPOINT.
# Substitua 'solopreneur-web-platform' pelo `finalName` do seu pom.xml se for diferente.
COPY --from=builder /app/target/solopreneur-web-platform ./application

# Expõe a porta em que a aplicação Spring Boot roda
EXPOSE 8080

# Define o comando que será executado quando o contêiner iniciar.
# Incluímos as flags de memória otimizadas para o seu ambiente de 1GB RAM.
ENTRYPOINT ["/app/application", "-Xms128m", "-Xmx512m"]