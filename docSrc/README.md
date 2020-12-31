# Projector Documentation

This module contains sources of Projector documentation.

## Prerequisites

Make sure Java and Python are available.

## Running

The following command will serve the built documentation on <0.0.0.0:8080>:

```shell
./gradlew :docSrc:mkdocsServe
```

## Building

The following command will build the documentation to `docSrc/build/mkdocs/latest`:

```shell
./gradlew :docSrc:mkdocsBuild
```
