#!/bin/bash
set -e
VERSION=${1:-latest}
IMAGE=vieterp/backend-hrm

echo "Building $IMAGE:$VERSION ..."
docker build -t $IMAGE:$VERSION -f Dockerfile ..
echo "Built: $IMAGE:$VERSION"

echo "Pushing $IMAGE:$VERSION ..."
docker push $IMAGE:$VERSION
echo "Done."