# Environnement local de l'arbitre harness-game (voir .github/workflows/harness-game.yml), sans GitHub.
# Reproduit les outils que le workflow trouve sur le runner ubuntu-latest : Java 21 (temurin), Gradle 9.5.0,
# git, gh, jq, python3.
#
# Construction : docker build -t omni-harness .
# Usage        : ./harness-docker.sh all --json rapport.json      (voir harness-docker.sh)

FROM eclipse-temurin:21-jdk-noble

ARG GRADLE_VERSION=9.5.0
ARG GH_VERSION=2.90.0
# Fourni par BuildKit (amd64 ou arm64) : choisit le binaire gh de la bonne architecture.
ARG TARGETARCH

RUN apt-get update \
 && apt-get install -y --no-install-recommends git jq python3 unzip curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL "https://github.com/cli/cli/releases/download/v${GH_VERSION}/gh_${GH_VERSION}_linux_${TARGETARCH}.tar.gz" \
      | tar -xz -C /tmp \
 && mv "/tmp/gh_${GH_VERSION}_linux_${TARGETARCH}/bin/gh" /usr/local/bin/gh \
 && rm -rf "/tmp/gh_${GH_VERSION}_linux_${TARGETARCH}"

RUN curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip \
 && unzip -q /tmp/gradle.zip -d /opt \
 && ln -s "/opt/gradle-${GRADLE_VERSION}/bin/gradle" /usr/local/bin/gradle \
 && rm /tmp/gradle.zip

# Le dépôt est monté dans /work (le propriétaire des fichiers hôte n'est pas root) : git doit l'accepter.
RUN git config --system --add safe.directory '*'

WORKDIR /work
ENTRYPOINT ["./harness.sh"]
