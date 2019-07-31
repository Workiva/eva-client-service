#!/usr/bin/env bash

mkdir -p ./compose_remote
cd ./compose_remote

# eva
if [ ! -d "./eva" ]; then
  mkdir eva && cd ./eva
  git init && git remote add origin git@github.com:Workiva/eva.git
  git fetch --depth=1 --tags
  echo Checking out compose for eva at $(git describe --tags `git rev-list --tags --max-count=1`)
  git checkout $(git describe --tags `git rev-list --tags --max-count=1`) -- docker/docker-compose.yml docker/eva-db
else
  echo "Using Existing EVA Git Repo!"
  cd ./eva && git fetch origin
  echo Checking out compose for eva at $(git describe --tags `git rev-list --tags --max-count=1`)
  git checkout $(git describe --tags `git rev-list --tags --max-count=1`) -- docker/docker-compose.yml docker/eva-db
fi
cd ..
echo $PWD
cp ./eva/docker/docker-compose.yml ./local-compose-eva.yml
echo local-compose-eva.yaml cloned to compose_remote dir

# eva-catalog
if [ ! -d "./eva-catalog" ]; then
  mkdir eva-catalog && cd ./eva-catalog
  git init && git remote add origin git@github.com:Workiva/eva-catalog.git
  git fetch --depth=1 --tags
  echo Checking out compose for eva-catalog at $(git describe --tags `git rev-list --tags --max-count=1`)
  git checkout $(git describe --tags `git rev-list --tags --max-count=1`) -- docker/docker-compose.yml
else
  echo "Using Existing Eva-Catalog Git Repo!"
  cd ./eva-catalog && git fetch origin
  echo Checking out compose for eva-catalog at $(git describe --tags `git rev-list --tags --max-count=1`)
  git checkout $(git describe --tags `git rev-list --tags --max-count=1`) -- docker/docker-compose.yml
fi
cd ..
echo $PWD
cp ./eva-catalog/docker/docker-compose.yml ./local-compose-eva-catalog.yml
echo local-compose-eva-catalog.yaml cloned to compose_remote dir
