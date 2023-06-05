# Reactive Quarkus Workshop - Dead Letter Queue - Lab

This repository includes the lab assignment on implementing a dead letter queue mitigation strategy for use with the SmallRye Apache Kafka Connector.

## Getting started

In this exercise, you'll be working on a couple of integration tests that exercise the correct behavior of our Kafka-based messaging components. You'll find a `docker-compose.yaml` in folder `docker` which sets up a locally running Kafka cluster that comprises a single broker. Launching the cluster can be done by issuing

*Linux / MacOS*

```bash
$ cd docker
$ docker-compose up
```

*Windows*

```bash
$ cd docker
$ docker-compose up
```

from within the root folder.

## Useful resources

* SmallRye Reactive Messaging - Kafka Connector ([guide](https://quarkus.io/guides/kafka-reactive-getting-started)): Connect to Kafka with Reactive Messaging

## Docker CLI Hints

*Starting containers using `docker-compose`*

```bash
$ docker-compose up
```

*Stopping containers using `docker-compose`*

```bash
$ docker-compose stop
```

*Removing containers using `docker-compose`*

```bash
$ docker-compose rm -f
```
