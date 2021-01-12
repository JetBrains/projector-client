# Running remote app (server)

To run an app with Projector, you need to create a server. There are multiple variants available.

Variant | Supported platforms | [Mode](#mode)
---|---|---
installer | Linux | Headless
Docker | Any | Headless
IDE Plugin | Any | GUI

## Mode

There are two ways of how the server is run:

- **GUI** mode – you can see the app where you launch it, and also connect to it remotely at the same time.
- **Headless** mode – you can't see the app, you only can connect remotely.

### Differences

You may wonder what to choose. Let's describe some differences that should help you decide.

- Currently, headless mode is available **only on Linux** (or inside Docker). On the other hand, GUI mode is available everywhere.
- Headless mode doesn't require **Desktop Environment** nor **XServer** nor **virtual framebuffer** to be available on the device, while GUI mode in Linux obviously does.
- Headless mode can adapt to the connected client machine better, especially when the client has different screen parameters (DPI, resolution).

## installer

TODO: https://github.com/JetBrains/projector-installer

## Docker

TODO: https://github.com/JetBrains/projector-docker

## IDE Plugin

TODO: https://github.com/JetBrains/projector-server/tree/master/projector-plugin
