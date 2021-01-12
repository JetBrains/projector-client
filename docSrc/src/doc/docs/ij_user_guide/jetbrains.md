# JetBrains IDEs

Current [JetBrains IDEs](https://www.jetbrains.com/products/#type=ide) use Swing to draw GUI. The same is true for other IntelliJ-based apps like [Android Studio](https://developer.android.com/studio/). Therefore, Projector that is a technology for rendering Swing GUI over the network is a good way to run and access these apps remotely. It should be compatible with all existing JetBrains IDEs. This section of documentation describes how Projector is adapted to IntelliJ-based apps.

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

![](https://hsto.org/webt/bn/rf/rk/bnrfrkzogrdfp5sxs6t5g7hllc4.jpeg)

*IntelliJ IDEA on Android tablet (Huawei MediaPad M5)*

## Quick start

You should decide how you run a remote IDE and how you access it. This info is given at [servers](running.md) and [clients](accessing.md) pages respectively.

TODO: give tl;dr (docker and bundled client).
