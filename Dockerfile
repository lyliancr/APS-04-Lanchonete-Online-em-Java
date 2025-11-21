FROM maven:3.8.4-openjdk-11 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY web ./web
RUN mvn clean package

FROM tomcat:8.5-jre8

# Instala o unzip para inspeção do .war
RUN apt-get update && apt-get install -y unzip

# Copia o .war gerado pelo Maven
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war
COPY context.xml /usr/local/tomcat/conf/
EXPOSE 8080
CMD ["catalina.sh", "run"] 