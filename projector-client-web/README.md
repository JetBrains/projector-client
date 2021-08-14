# projector-client-web
A web client for Projector.

Contents:
* [Running](#running).
* [Building](#building).
* [Notes](#notes) &mdash; tested browsers.
* [Page parameters](#page-parameters) &mdash; set up the connection.
* [Shortcuts](#shortcuts) &mdash; currently they are needed for debug purposes.

## Running
The latest commit from master is built and deployed on our site. [More info](https://jetbrains.github.io/projector-client/mkdocs/latest/ij_user_guide/accessing/#latest-client)

The latest version can be downloaded at the [Artifacts page](https://github.com/JetBrains/projector-client/releases).

## Building
The client needs only static files. The following script will build the client files in the `projector-client-web/build/distributions` dir:
```shell script
./gradlew :projector-client-web:browserProductionWebpack
```

After building, you can run the HTML page: `projector-client-web/build/distributions/index.html`.

## Notes
Tested browsers:
- Chromium.
- Firefox.

## Page parameters
You can set some settings in query parameters like this: `index.html?host=localhost&port=8887&wss`. Actual list of parameters can be found in the `ParamsProvider.kt` file. Here we describe them.

### Main parameters
Name | Type | Default value | Description
---|---|---|---
`host` | String | Host of the web page | Set the host of `projector-server` to connect.
`port` | String | Port of the web page | Set the port of `projector-server` to connect.
`path` | String | Path of the web page | Set the path of `projector-server` to connect.
`wss` | Presence | Protocol of the web page | Enable security of WebSocket connection.
`notSecureWarning` | Boolean | `true` | Enable warning saying that the context is not [secure](https://developer.mozilla.org/en-US/docs/Web/Security/Secure_Contexts).
`token` | String? | Not present | Set a password which will be checked by the server on the connection.
`inputMethod` | String? | `ime` | `ime` supports Input Method Editors and Dead Keys. `mobileOnlyButtons` – enable overlay controls handy for mobile devices. `mobile` – enable overlay controls and a virtual keyboard toggle handy for mobile devices. `legacy` doesn't support input methods.
`ideWindow` | Int? | Not present | Specify the IDE window ID to show. The first ID is `0`. If not presented, all IDE windows are shown.
`layout` | Enum (String) | `jsDefault` | Specify keyboard layout. Possible values: `jsDefault`, `frAzerty`.

### Debug/test parameters
Name | Type | Default value | Description 
---|---|---|---
`clipping` | Presence | Not present | Show borders of clipping areas via red and blue lines.
`logUnsupportedEvents` | Presence | Not present | Log unsupported events received from server to browser console.
`doubleBuffering` | Boolean | `true` | Enable double buffering for rendering to on-screen surfaces.
`enableCompression` | Presence | Not present | Use compression for sending and receiving WebSocket messages.
`toClientFormat` | String | `jsonManual` | Sets format of data from server to client: `json`, `jsonManual`, `protoBuf`.
`imageTtl` | Double | `60_000.0` | Set caching time of unused images in ms.
`flushDelay` | Int? | `1` | Set buffering time of events from client in ms. If the value is not integer, unbuffered mode is used: every client event is sent to the server immediately.
`showTextWidth` | Presence | Not present | Show near-text lines of browser width and desired width.
`showSentReceived` | Presence | Not present | Show blinking indicators in the corner of the screen when events were sent or received.
`showPing` | Presence | Not present | Show some info of simple ping to and from server.
`pingAverageCount` | Int? | Not present | Activate displaying average ping of this number of iterations.
`backgroundColor` | String | `#282` (green) | Set color of area where there are no windows.
`userScalingRatio` | Double | `1.0` | Set scaling ratio.
`pingInterval` | Int | `1000` | Set interval of pinging in ms.
`showProcessingTime` | Presence | Not present | Log processing time of server messages to browser console.
`repaintArea` | Presence | Not present | Enable ability to see repainted areas, use a shortcut to toggle (more info below).
`speculativeTyping` | Presence | Not present | Enable rendering symbols in Editor not waiting for draw events from server.
`repaintInterval` | Int | `333` | Set interval of repainting that is needed to paint loaded images in ms.
`cacheSize` | Int | `5M` | Set size of cache for images in Chars.
`blockClosing` | Boolean | `true` | Enable blocking of accidental closing of the web page
`relayServerId` | String? | Not present | Identifier of Projector server to connect to for relay connection. Warning: Static files must be accessed via https when relay is used.
`speculativeTypingLatency` | Int | `0` | Sets latency before key press event is sent to server if speculative symbol for the event was drawn.

## Shortcuts
- `Ctrl + F10` prints statistics to the browser console. Example:  
```
[INFO] :: ClientStats :: Stats:

simple ping average:                    20.84 (19 iterations)
to-client message size average:         3K bytes (185 iterations)
to-client compression ratio average:    1.00 (185 iterations)
draw event count average:               56 (185 iterations)
decompressing time average:             0.00 ms (185 iterations)
decoding time average:                  12.51 ms (185 iterations)
drawing time average:                   3.11 ms (185 iterations)
other processing time average:          0.20 ms (185 iterations)
total (sum) time average:               15.83 ms (185 iterations)

to-client message size rate:    30K bytes per second (21.7 seconds)
draw event count rate:          477 per second (21.7 seconds)
decompressing time rate:        0.04 ms per second (21.7 seconds)
decoding time rate:             106.65 ms per second (21.7 seconds)
drawing time rate:              26.52 ms per second (21.7 seconds)
other processing time rate:     1.72 ms per second (21.7 seconds)
total (sum) time rate:          134.92 ms per second (21.7 seconds)

Stats are reset!
```
- `Ctrl + F11` toggles showing repainted areas (if `repaintArea` query param is enabled).
