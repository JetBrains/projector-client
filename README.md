# projector-client
[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![codecov](https://codecov.io/gh/JetBrains/projector-client/branch/master/graph/badge.svg?token=XH5BF4QZH5)](https://codecov.io/gh/JetBrains/projector-client)
[![Tests status badge](https://github.com/JetBrains/projector-client/workflows/Tests/badge.svg)](https://github.com/JetBrains/projector-client/actions)
[![Build status badge](https://github.com/JetBrains/projector-client/workflows/Builds/badge.svg)](https://github.com/JetBrains/projector-client/actions)

Common and client-related code for running Swing applications remotely.

[Documentation](https://jetbrains.github.io/projector-client/mkdocs/latest/) | [Issue tracker](https://youtrack.jetbrains.com/issues/PRJ)

## The state of Projector

The development of JetBrains Projector as its own standalone product has been suspended. That said, Projector remains an important part of [JetBrains Gateway](https://www.jetbrains.com/remote-development/gateway/), which is the primary remote development tool for JetBrains IDEs. We will focus our efforts on improving and developing Projector in this limited scenario.

Our goal is to provide a rich, full-featured remote development experience with a look and feel that is equal to or better than what you get when working with IDEs locally. The only way to get everything you’re used to having when working locally (low latency, low network traffic, user-defined and OS-specific shortcuts, themes, settings migrations, ssh-agent/port forwarding, and other things) is by installing a dedicated client-side application. The standalone version of Projector is not capable of meeting these goals.

As a result, we no longer recommend using the standalone version of JetBrains Projector or merely making tweaks to incorporate it into your existing IDE servers. We won’t provide user support or quick-fixes for issues that arise when these setups are used. If you have the option to switch from Projector to Gateway, we strongly recommend you do so.

[Learn more about JetBrains Gateway](https://www.jetbrains.com/remote-development/gateway/)

## Modules
To learn more about modules, check out README files inside them:
* [projector-common](projector-common/README.md).
* [projector-client-common](projector-client-common/README.md).
* [projector-client-web](projector-client-web/README.md).
* [projector-launcher](projector-launcher/README.md).

## License
[MIT](LICENSE.txt).
