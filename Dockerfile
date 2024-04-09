FROM maven:3.8.4-openjdk-17

ADD ./repository /usr/share/maven/ref/repository

# clone zest-dipri
WORKDIR /root
RUN git clone https://github.com/isefuzz/zest-dipri.git && cd zest-dipri