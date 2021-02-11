# PWP Database

This folder contains everything related to the database used in this project.

The purpose of this part is to form the database structure for the whole Book Club project. The database designed is
very simple and very self explanatory. All related tables are constrained with foreign keys and all *ON xxx* constraints
and *ENGINE* definitions are in place for all of the tables.

The database is designed with the latest [MariaDB](https://mariadb.org/) in mind, but it might work on any MySQL server.
The current deployment is done via docker.

# Setup

## Requirements:

* docker *[Get Docker](https://docs.docker.com/get-docker/)*
* docker-compose *[Install Docker Compose](https://docs.docker.com/compose/install/)*

#### Tests and generated classes

* maven
* java 15

## Creating and running the database

The database is run in a [MariaDB docker image](https://hub.docker.com/_/mariadb) that can be managed with simple
scripts found in this part root.

* Scripts
    * _*start.sh*_ - starts (and creates) the database
    * _*stop.sh*_ - stops the database
    * _*reset.sh*_ - stops the database if running and __deletes all related resources__ in docker

### Credential set-up is done in the [setup](setup) folder with:

* __deployment.sql__ - Passwords for users in database and anything pre-setup you don't want in the repo
* __book-club-root.txt__ - Root password for the MariaDB server

This is just for testing though

## Testing and using the database

#### Testing

Run the database using [start.sh](start.sh)

Go to [communicator](communicator) and do:
> mvn install

This will generate all communications classes using [jooq](https://www.jooq.org/) for Kotlin with Java 15. This needs to
connect to the empty database in order to generate all classes, pojos, daos etc. from the actual database. DDL
generation refused to work for the database init script for some reason.

Then go to [tests](communicator/test) and do:
> mvn test

This will run all tests for the database using the previously generated classes

#### Usage

Kotlin/Java

* Run the database using [start.sh](start.sh)
* Generate classes
* Add generated classes as a dependency

## TODO

- [x] Basic skeleton of the database
- [ ] Tests for the database
- [ ] Revision of the main schema
- [ ] Full tests for the database
- [ ] Commit to final schema
- [ ] Generate mock data to populate the database