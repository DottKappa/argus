FROM eclipse-temurin:21-jdk

# Installazione di Maven e delle dipendenze grafiche Linux per JavaFX
RUN apt-get update && apt-get install -y \
    maven \
    libgl1 \
    libglx-mesa0 \
    libgtk-3-0 \
    libxxf86vm1 \
    libglib2.0-0 \
    libxrender1 \
    libxext6 \
    libxi6 \
    libxtst6 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copia e scarica le dipendenze per velocizzare le build successive
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia il resto del sorgente
COPY src ./src

# Compila l'applicazione
RUN mvn clean compile

CMD ["mvn", "javafx:run"]