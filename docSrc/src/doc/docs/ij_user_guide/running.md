# Running remote app (servers)

To run an app with Projector, you need to create a server. There are multiple variants available.

Variant | Supported platforms | [Mode](#mode)
---|---|---
[Installer](#installer) | Linux | Headless
[Docker](#docker) | Any | Headless
[IDE Plugin](#ide-plugin) | Any | GUI

## Mode

There are two ways of how the server is run:

- **GUI** mode – you can see the app where you launch it, and also connect to it remotely at the same time.
- **Headless** mode – you can't see the app, you only can connect to it remotely.

### Differences

You may wonder what to choose. Let's describe some differences that should help you decide.

- Currently, headless mode is available **only on Linux** (or inside Docker) – we have a request to fix it: [PRJ-75](https://youtrack.jetbrains.com/issue/PRJ-75). On the other hand, GUI mode is available everywhere.
- Headless mode doesn't require **Desktop Environment** nor **X-Server** nor **virtual framebuffer** to be available on the device, while GUI mode in Linux obviously does. This makes Projector be able to run easily on headless machines such as servers in clouds.
- Headless mode can adapt to the connected client machine better, especially when the client has different screen parameters (DPI, resolution).

## Installer

Installer is a console application that allows to install, configure, and run JetBrains IDEs with Projector. Currently it works on Linux or in [WSL](https://docs.microsoft.com/windows/wsl/). BSD is supported too. The IDE will be run **headlessly**.

You can find instructions in the corresponding repo: <https://github.com/JetBrains/projector-installer>.

## Docker

We have a sample Docker script that allows you to generate Docker images containing JetBrains IDEs and Projector inside. This allows you to run IDEs anywhere where Docker is available. The IDE will be run **headlessly** and isolated inside Docker.

You can find instructions in the corresponding repo: <https://github.com/JetBrains/projector-docker>.

## IDE Plugin

Another way of running Projector is in **GUI** mode. To perform this with JetBrains IDEs, we have a plugin for IDEs. You need to install the plugin. After that, the plugin will provide controls to start the screen sharing. This can be useful when you have a workstation with GUI, but need to have an option to leave the workplace and resume the work remotely. With IDE plugin that supports GUI, you don't need to restart already opened IDE.

You can find instructions in the corresponding repo: <https://github.com/JetBrains/projector-server/tree/master/projector-plugin>.
