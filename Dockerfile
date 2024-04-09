FROM maven:3.8.4-openjdk-17

COPY ./ /root/zest-dipri
COPY ./repository /usr/share/maven/ref/repository

# clone zest-dipri
WORKDIR /root/zest-dipri
