This document describes the ecosystem as a whole. If you want to understand how Projector works and maybe even change something, please check this document out.

## TL;DR

Projector is a way to run Swing applications without any windowing system and accessing them either locally or remotely.

## The system

Here you can find some info on how this works.

### AWT implementation

The Foundation stone of Projector is an implementation of AWT. It allows running Swing applications headlessly but without `HeadlessException`s. Also, it intercepts some AWT calls and remembers them. The most calls are invocations of `Graphics2D` methods. This module is called `projector-awt`.

!!! warning "Update is needed"
    This documentation is a bit deprecated because it doesn't mention that currently we also can run Swing apps with Projector without custom AWT. This allows running apps with GUI, but simultaneously share them via Projector.

### Server and client

To run an application with our AWT and access it, we provide a server and a client.

The module of the server implementation is called `projector-server`. It provides a launcher that sets AWT system properties and fields and is compatible with the most of Swing apps.

Also, it extracts remembered AWT calls from `projector-awt` and can send them on a client. This implementation uses WebSocket and a serialization protocol which is located in the `projector-common` module. A client can execute the received calls on its side. The only client implemented now is for Web browsers, it's available in the `projector-client-web` module.

Finally, the client can send its events such as mouse clicks or window resizes in the reverse direction. `projector-server` translates these events to AWT.

### Client components

Some Swing apps contain content that can't be easily represented as `Graphics2D` method calls. For example, it can be an embedded heavyweight component. In such cases, it's sometimes possible to serialize this content another way.

The only client component implemented now is Markdown Preview in JetBrains IDEs.

## Repositories
Here we enumerate repos related to this project and describe their content.

### Main

* [projector-client](https://github.com/JetBrains/projector-client):
    * `projector-common` &mdash; protocol and other common code for clients and a server.
    * `projector-client-common` &mdash; common code for clients.
    * `projector-client-web` &mdash; a client implementation for Web browsers.
* [projector-server](https://github.com/JetBrains/projector-server):
    * `projector-awt` &mdash; AWT implementation.
    * `projector-server` &mdash; an application server for running and remote accessing a Swing app.

### Examples and utils

* [projector-demo](https://github.com/JetBrains/projector-demo) &mdash; a sample app showing how to run any Swing app with Projector.
* [projector-docker](https://github.com/JetBrains/projector-docker) &mdash; scripts for building Docker images containing JetBrains IDEs and Projector Server.
* [projector-installer](https://github.com/JetBrains/projector-installer) &mdash; a utility for installing, configuring, and running JetBrains IDEs with `projector-server` on Linux or in WSL, available at PyPI.

### Obsolete

* [projector-markdown-plugin](https://github.com/JetBrains/projector-markdown-plugin) &mdash; Markdown Preview for JetBrains IDEs which can be used with Projector. Now we managed to bundle it to projector-server, so you don't need to install it separately anymore.

## How to use in another project

You can find some hints in the corresponding repos.

You can use [JitPack](https://jitpack.io/) to make a dependency on our project. Also, you can clone a project and build it yourself.

We show these ways in example repos.
