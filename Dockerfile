FROM gradle:7.3-jdk17 as build
RUN microdnf install jq unzip

COPY . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle jar --no-daemon
ENV RAM_MB=256

FROM bellsoft/liberica-openjdk-alpine:17

COPY --from=build /home/gradle/src/build/libs/*.jar /home/strategy/strategy.jar
ADD tester-linux/app-linux.tar.gz /home/strategy/.

CMD ["bash", "entrypoint.sh"]

