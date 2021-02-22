# PWP Database

This folder contains everything related to the database used in this project.

The purpose of this part is to form the database structure for the whole Book Club project. The database designed is
very simple and very self-explanatory. All related tables are constrained with foreign keys and all *ON xxx* constraints
and *ENGINE* definitions are in place for all the tables.

The database is designed with the latest [MariaDB](https://mariadb.org/) in mind, but it might work on any MySQL server.
A sample deployment is done via docker for testing and communication class generation.

# Setup

## Requirements:

* docker *[Get Docker](https://docs.docker.com/get-docker/)*
* docker-compose *[Install Docker Compose](https://docs.docker.com/compose/install/)*

#### Tests and generated classes

* maven
* java 11

## Creating and running the database

The database is run in a [MariaDB docker image](https://hub.docker.com/_/mariadb) that can be managed with simple
scripts found in this part root.

* Scripts
    * _*start.sh*_ - starts (and creates) the database
    * _*stop.sh*_ - stops the database
    * _*reset.sh*_ - stops the database if running and __deletes all related resources__ in docker

### Set-up is done in the [setup](setup) folder with:

All the stuff in here is passed into the docker container and is run on the ___FIRST___ run of the server after reset.
Example of using docker secrets for root password is in the [compose file](docker-compose.yml).

* __init.sql__ - Main database schema
* __xxx.sql__ - Anything you want to run (Runs in alphabetic order)

This is just for testing though

## Testing and using the database

#### Testing

Tests are not very extensive. The tests test some simple, but important features of the database.

Run the database using [start.sh](start.sh)

Go to [communicator](communicator) and do:
> mvn compile

This will generate all communications classes using [jooq](https://www.jooq.org/) for Kotlin with Java 11. This needs to
connect to the empty database in order to generate communications classes from the actual database. DDL
generation refused to work for the database init script for some reason.

Then go to [tests](communicator/generated) and do:
> mvn test

This will run all tests for the database using the previously generated classes

#### Usage

Kotlin/Java

* Run the database using [start.sh](start.sh)
* Generate classes
* Add generated classes as a dependency
