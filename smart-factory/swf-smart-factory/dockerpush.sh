mvn clean package

docker build --pull -t "lellson/swf-sonataflow-smart-factory:develop" -f ./src/main/docker/Dockerfile.jvm . --no-cache

docker push lellson/swf-sonataflow-smart-factory:develop