FROM maven:3.8.4-openjdk-17

ADD ../zest-dipri /root/zest-dipri
ADD ./repository /usr/share/maven/ref/repository

# clone zest-dipri
WORKDIR /root/zest-dipri