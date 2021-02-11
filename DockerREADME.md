First generate the ApiReplay jar and move it.
  `mvn package`
  `cp ./target/replay_maven-1.0-jar-with-dependencies.jar ./replay.jar`
Also set up your ./data/ directory to be copied into the container.

Build the docker image with
  `docker build . -t apireplay`

Run the image with
  `docker run -p 7022:22 -d --name apireplayctnr apireplay`

The above binds port 7022 to the container's port 22. 
You can SSH into the container with
  `ssh replay@localhost -p 7022`
Use the password "replay".

You should then be able to run the jar using the following
  `java -jar ./replay.jar -h <HOST> -d "./data/" -u fakeUser -p fakePassword`
Without adding any more networking, your host should be addressable as 172.17.0.1, eg.
  `java -jar ./replay.jar -h http://172.17.0.1:7878 -d "./data/" -u fakeUser -p fakePassword`

