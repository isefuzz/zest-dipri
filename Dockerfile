FROM maven:3.8.6-openjdk-8

WORKDIR /root

# clone zest-dipri
RUN git clone https://github.com/isefuzz/zest-dipri.git && cd zest-dipri && mvn clean install