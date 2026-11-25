FROM eclipse-temurin:21-jdk

RUN apt-get update && apt-get install -y --no-install-recommends \
    maven \
    xvfb \
    x11vnc \
    novnc \
    websockify \
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

# Utente non-root con directory di lavoro e home scrivibile
RUN useradd -m -u 10001 -s /bin/sh appuser
WORKDIR /app
RUN chown -R appuser:appuser /app

COPY --chown=appuser:appuser pom.xml .
RUN mvn -o dependency:go-offline || mvn dependency:go-offline

COPY --chown=appuser:appuser src ./src
RUN mvn -o clean compile

USER appuser
ENV HOME=/app
ENV DISPLAY=:99

CMD ["sh", "-c", "Xvfb :99 -screen 0 1280x800x24 -ac & sleep 1 && x11vnc -display :99 -localhost -nopw -forever -shared -rfbport 5900 & websockify --web=/usr/share/novnc/ 0.0.0.0:6080 localhost:5900 & mvn -o javafx:run"]