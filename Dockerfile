FROM openjdk:11-jre-slim

RUN ln -s /usr/local/openjdk-11/bin/java /bin/java

RUN apt update && apt install openssh-server sudo -y

RUN useradd -rm -d /home/replay -s /bin/bash -g root -G sudo -u 1000 replay

RUN  echo 'replay:replay' | chpasswd

RUN service ssh start

COPY ./replay.jar /home/replay/replay.jar
COPY ./data /home/replay/data

EXPOSE 22

CMD ["/usr/sbin/sshd","-D"]
