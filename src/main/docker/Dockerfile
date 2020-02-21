FROM adoptopenjdk:11-jre-hotspot
#FROM docker-prod.registry.kroger.com/library/java-openjre:11-latest

EXPOSE 8080
WORKDIR /opt/app

# Example: docker build --build-arg JAR_NAME=foo.jar .
ARG JAR_NAME=service.jar

#ENV JAR_NAME=${JAR_NAME}
ENV SPRING_PROFILES_ACTIVE=dev

COPY target/${JAR_NAME} /opt/app/service.jar

CMD ["java","-jar","/opt/app/service.jar"]
