FROM gradle:7.3-jdk17
RUN microdnf install jq unzip

ENV MOUNT_POINT="/opt/mount-point"
ENV SOLUTION_CODE_PATH="/opt/client/solution"
COPY . $SOLUTION_CODE_PATH
WORKDIR $SOLUTION_CODE_PATH
CMD ["bash", "entrypoint.sh"]

# TODO: wtf doesnt work?
# RUN mvn dependency:go-offline
RUN gradle build

# TODO
ENV RAM_MB=256