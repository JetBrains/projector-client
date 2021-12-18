# Accessing remote app (clients)

To access a remotely run app, you need to use a client. There are multiple variants available.

Client | Supported platforms
---|---
[Web browser](#web-browser) | Any
[Client app ("launcher")](#client-app-launcher) | Windows, Linux, MacOS

## Web browser

### Bundled client

Recent versions of servers have bundled client files. So for example if you start a server on port 9999, you can access it via a browser locally opening <http://localhost:9999/> (or <https://...> if your server is secure).

If your server is located remotely, just use the corresponding host address instead of `localhost`.

### Latest client

The latest client is always available at our site: <https://projector.jetbrains.com/client/latest/?host=HOST&port=PORT>. So this variant is useful if you want to check out the recent update in the client.

Also, you can select the stable version of the client by replacing the field `latest` with the number of the stable version, starting from `v1.4.0`.

For example, _projector.jetbrains.com/client/**v1.4.0**/?host=HOST&port=PORT_.

You should set `HOST` and `PORT` matching your server. For the previous example, it will be <https://projector.jetbrains.com/client/latest/?host=localhost&port=9999>.

!!! info "Secure connection is required for the latest client"
    Your server must be secure when using the latest client because our domain is secure itself. Before connection, make sure that your browser trusts the certificate you use on your server (in this case, connection via the bundled client should succeed). For more info, read [below](#its-not-possible-to-connect-to-insecure-websocket-from-a-secure-web-page).

!!! warning "The latest client can introduce incompatible changes"
    The latest client can be incompatible with the latest release or another release that you use. 

### Known issues

Due to limitations of web browsers, there are some issues in the Web Client. They can be solved via native implementations of the client.

#### iPad as a client

iPads don't support self-signed WebSockets, so if you want to use a browser on iPad as a client, you now have to disable security on the server or use a well-signed certificate on your server.

#### Some hotkeys are intercepted by the browser

For example, `Ctrl+Q` in Windows/Linux or `Cmd+N` in Mac is handled by the browser.

Since some shortcuts close the tab or the window, we implemented a confirmation which is shown when the page is about to close (if `blockClosing` parameter is enabled).

Also, we consider `Ctrl+Q` shortcut as frequently used, so we mapped it to the `F1` button.

It seems that we can't do anything more about that, at least in a normal browser window.

The proposed **workaround** here is to you the feature of browsers called [PWA](https://en.wikipedia.org/wiki/Progressive_web_application). It's a way to install a web page as a separate application. We've tested it in Chrome and in this mode, all the tested shortcuts are handled by Projector, not by the browser. The instructions are as follows: simply create a shortcut by selecting `Menu` | `More Tools` | `Create Shortcut...` and `Open as window`. Instructions with screenshots can be googled, for example, [this one](https://ccm.net/faq/9934-create-a-desktop-shortcut-on-google-chrome). The similar can be achieved in Firefox: for example, [take this](https://www.maketecheasier.com/enable-site-specific-browser-firefox/) instruction.

#### Incomplete clipboard synchronization

There are some limitations with clipboard.

##### To-server

When your clipboard is changed on the client side, the server needs to apply the change on its side.

We implement it on the client side via setting ["paste" listener](https://developer.mozilla.org/en-US/docs/Web/API/Element/paste_event). So clipboard is updated on the server only if you invoke that listener, for example, by hitting Ctrl+V or Ctrl+Shift+V. **If you have an application on the server side with a "paste" button, a click on it can paste outdated information unless the listener wasn't invoked**.

Unfortunately, we can't just continuously get clipboard data from [`window.navigator.clipboard`](https://developer.mozilla.org/en-US/docs/Web/API/Navigator/clipboard) and send it to the server because when it's invoked not from user's context, there will be alert from the browser like "the site wants to read clipboard info, do you grant?".

##### To-client

It's vice versa: when your clipboard is changed on the server side, the client needs to apply the change on its side.

We set the clipboard on the client side via [`window.navigator.clipboard`](https://developer.mozilla.org/en-US/docs/Web/API/Navigator/clipboard) when client is opened using HTTPS or on localhost. If client is opened in insecure context or doesn't support Async Clipboard API we fall back to `document.execCommand("copy")`. If it also didn't work out then we show prompt so that you can manually copy new contents of server clipboard.

We can't use ["copy" listener](https://developer.mozilla.org/en-US/docs/Web/API/Element/copy_event) because when this event is generated, we don't have a message from the server with actual clipboard data yet. Also, this method won't work if you click a "copy" button in your application.

#### It's not possible to connect to insecure WebSocket from a secure web page

This is a limitation of browsers. So for example you can't use client at <https://projector.jetbrains.com/client/> to access an insecure server.

### Advanced options and shortcuts

The web client has some hidden [options](https://github.com/JetBrains/projector-client/tree/master/projector-client-web#page-parameters) and keyboard [shortcuts](https://github.com/JetBrains/projector-client/tree/master/projector-client-web#shortcuts). Please use the links if you want to know more info.

In the future, welcome screen ([PRJ-126](https://youtrack.jetbrains.com/issue/PRJ-126)) and actions panel ([PRJ-248](https://youtrack.jetbrains.com/issue/PRJ-248)) will make these features more explicit.

## Client app ("launcher")

Also, we have a special app called `launcher` that wraps web page and allows to overcome some limitations of web browsers such as **shortcut interception** and **low performance**.

Currently, it's a small app, and it's easy to use. It has just a single text field for the URL you want to connect.

### Downloading

Please check [releases](https://github.com/JetBrains/projector-client/releases) beginning with the `launcher-` prefix. Download the file for your OS.

### Running

After unpacking the archive, run the executable file corresponding to your OS (see below). If you don't want to specify the URL in the GUI every time, you can create a shortcut or run this app from command line passing the URL as the first argument.

#### Windows

Run `projector.exe` file.

#### Linux

Run `projector` file.

#### Mac (Darwin)

Run `projector` app (on other OSes it's visible like `projector.app` dir).

We publish **signed notarized** and **unsigned** version of the app. If you want to run the app **easily**, please use the **signed notarized** version.

If you want to try the unsigned version, Mac doesn't allow unsigned apps to be run easily, and will ask you to put the app to Trash Bin. So you need to select ["Open Anyway" in System Preferences](https://stackoverflow.com/a/59899342) to allow launching. We believe the only advantage of the unsigned version is that it's available right away after the new release, and we need some more time to publish the signed notarized version.
