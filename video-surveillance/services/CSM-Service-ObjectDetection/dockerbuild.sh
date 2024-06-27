docker build --pull -t "lellson/cirrina-examples-object-detection:develop" -f Dockerfile . --no-cache

docker push "lellson/cirrina-examples-object-detection:develop"