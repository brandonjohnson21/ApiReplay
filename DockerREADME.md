First generate the ApiReplay jar and move it.

  `mvn package`
  
Also set up your ./data/ directory to be copied into the container.

Build the docker image with

  `docker build . -t apireplay`

Run the image with

  `docker run -p 7022:22 -d --name apireplayctnr apireplay`

The above binds port 7022 to the container's port 22. 

You can SSH into the container with

  `ssh replay@localhost -p 7022`
  
Use the password "replay".

Set the environment variables:

*  `export UDL_USR="<INSERT_USERNAME_HERE>"`
  
*  `export UDL_PWD="<INSERT_PASSWORD_HERE>"`
  
Ideally, this is done by passing the environment variables to the container during startup, with K8S handling the sensitive information.

You should then be able to run the jar using the following

  `./replay.sh <HOST>`
  
Without adding any more networking, your host should be addressable as 172.17.0.1, eg.

  `./replay.sh https://test.unifieddatalibrary.com/ `

Exporting/Importing the container:

Export:
docker save apireplay > apireplay.tar
tar cfvz apireplay.tar.tgz apireplay.tar

Import:
tar xfvz apireplay.tar.gz
docker load apireplay.tar
