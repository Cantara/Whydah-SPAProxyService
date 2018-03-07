#!/bin/bash
# This file will build a Docker image, create a data volume container and run an application container
# from the image. Use it to test that your Docker configuration builds successfully.

# Change these
PROJECT_NAME=Whydah-SPAProxyService
IMAGE_NAME=whydah-spaproxy
IMAGE_NAME_DATA=whydah-spaproxy-data

sudo docker rm -v -f $IMAGE_NAME
sudo docker build -t $IMAGE_NAME .

sudo docker create --name $IMAGE_NAME_DATA $IMAGE_NAME

sudo docker run -d -p 8086:8086 --name $IMAGE_NAME --volumes-from $IMAGE_NAME_DATA $IMAGE_NAME


echo "Starting instance. Do 'sudo docker exec -it $IMAGE_NAME bash' to get shell"

wget http://localhost:8086/Whydah-SPAProxyService/health

