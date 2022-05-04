Current [JetBrains IDEs](https://www.jetbrains.com/products/#type=ide) use Swing to draw GUI. The same is true for other IntelliJ-based apps like [Android Studio](https://developer.android.com/studio/). Therefore, Projector that is a technology for rendering Swing GUI over the network is a good way to run and access these apps remotely. It should be compatible with all existing JetBrains IDEs (but we have a special file with a list of IDEs that we've tested with Projector [here](https://github.com/JetBrains/projector-installer/blob/master/projector_installer/compatible_ide.json)). This section of documentation describes how Projector is adapted to IntelliJ-based apps.

*[GUI]: Graphical User Interface

## Use cases

We believe there are many cases when you may want to use Projector. Let's enumerate some of them to inspire you:

1. Running the code from IDE located **near** the **runtime** (whatever it is) or **near** the **database** to reduce roundtrips;
1. **High-security** zones and corporate environments;
1. Really **huge projects**;
1. No source code on a laptop;
1. **Slow** or **hot** users’ **laptops**;
1. **Thin** clients and **cheap** **hardware** like Android tablets;
1. Running the IDE in a GNU/Linux environment on Windows machines or even on exotic operating systems like ChromeOS;
1. The ability to turn off your computer, while your app continues to work on the server;
1. Slow internet connection;
1. **Remote debugging** on a server-side (devtest, devprod);
1. VM or Docker images with debug sources and pre-configured IDE.
1. Any configuration that requires remote access.

Don't understand some items? We have a [special list](../about/usecases.md) where we describe them more comprehensively.

![](https://hsto.org/webt/bn/rf/rk/bnrfrkzogrdfp5sxs6t5g7hllc4.jpeg)

*IntelliJ IDEA on Android tablet (Huawei MediaPad M5)*

## Quick start

You should decide how you run a remote IDE and how you access it. This info is given comprehensively at [servers](running.md) and [clients](accessing.md) pages respectively.

The easiest way to try the project is to use [Docker Images](running.md#docker).

If you can't use Docker for some reasons (for example, due to any security limitations), try this [installation script](running.md#installer).

If you can't even use this installation script, you need to dive deeper and understand how it works under the hood. You can check [README](https://github.com/JetBrains/projector-server).

As for the client-side, just open the URL (something like [https://127.0.0.1:8887](https://127.0.0.1:8887)) in the browser, and that’s it.

## Client-side

Currently, you can use a web browser to connect to the IDE. The experience is very similar to using any interactive website. You can use a fullscreen mode in your browser to achieve an almost desktop-like experience.

We also have an Electron-based app ([projector-launcher](https://github.com/JetBrains/projector-client/tree/master/projector-launcher)) that wraps a web page and can run Projector Web Client.

If there is a demand from users, in the next stages we may build separate native applications.

## Server-side

You can use the Projector as a set of Docker images ([projector-docker](https://github.com/JetBrains/projector-docker)), or as a standalone distribution ([projector-installer](https://github.com/JetBrains/projector-installer)). A standalone distribution is currently available for GNU/Linux hosts only.

Dockerfiles are public and Open Source, so you can verify them with your security team.

Also, we have the third way to run Projector: via Projector IDE Plugin ([projector-plugin](https://plugins.jetbrains.com/plugin/16015-projector)).

## VPN and SSH tunnels

The Projector should run on popular VPN solutions like OpenVPN. It uses the HTTP and WebSocket protocols to connect to the browser. You shouldn't have any problems with that.

Also, you can use the following SSH command to redirect these ports through a plain SSH tunnel :

```
ssh -i key.pem -L 8887:127.0.0.1:8887 user@server
```

You can try this on the Amazon cloud.

## Competitors

The сlosest competitors to the Projector are X Window System, VNC, and RDP.

One the one hand, the Projector is much more responsive. Unlike all these alternatives, it uses extended and precise information about applications written in Java (which all JetBrains IDEs are) and sends it over the network. For example, that allows rendering crisp, pixel-perfect vector fonts. And it doesn't matter if your connection is slow or not, your text always is perfect, because the fonts are all in a vector format.

On the other hand, X11, VNC, and RDP are well-known solutions, and system administrators know exactly how to run them in a corporate environment.

Also, we have a [special documentation page](../about/comparison.md) where we list advantages of Projector.

## Is it open-source?

Now, everything is free and open-source software:

* [Dockerfiles](https://github.com/JetBrains/projector-docker) (Apache License 2.0);
* [Installer](https://github.com/JetBrains/projector-installer) (Apache License 2.0);
* [Server](https://github.com/JetBrains/projector-server) (GNU GPL v2.0 + Classpath Exception);
* [Client](https://github.com/JetBrains/projector-client) (MIT License).

## Known problems

The server side is supported as a Docker Image and as a local distribution for Linux servers. Local installations of the server on Windows and Mac are not a subject for the current stage of the project. However, technically it is quite possible.

We still have some inconsistencies in shortcuts. For example, a known problem is that we cannot use Ctrl+W to close a source file, because it closes a tab in the browser. The only action Projector can do here is to ask the user if they really want to close that tab. In the future, we can create separate native apps instead of the browser client to fix this.

There's a known limitation on using plugins with a custom rendering provided by Chromium Embedded Framework because it bypasses the standard rendering pipeline of Java. It's a hard problem, probably we can fix this, but not at this stage. Now we have a smart workaround for the Markdown component: we render Markdown HTML locally on a client. Actually, it even improved the experience of writing markdown a lot.

We cannot render separate external applications. For example, if you run the Android Emulator by activating a Run/Debug configuration, and expect to see GUI, it's impossible to render it with the Projector. In the future, it may be solved by combining with VNC technologies, but it's definitely not a target of the current stage.
