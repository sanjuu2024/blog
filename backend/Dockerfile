FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /build

# Copy the build descriptor first so dependency resolution remains cacheable.
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --uid 10001 --create-home blog

COPY --from=build /build/target/*.jar /app/blog.jar

USER 10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/blog.jar"]
