FROM maven:3.8.4-openjdk-17

COPY ./zest-dipri /root/zest-dipri
COPY ./repository /usr/share/maven/ref/repository
COPY ./plot_data.tar.gz /root/zest-dipri/result-handle

WORKDIR /root/zest-dipri
RUN cd ./result-handle && tar -xzvf ./plot_data.tar.gz