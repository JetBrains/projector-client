# projector-client-web
A web client for Projector.

## Building
```shell script
./gradlew :projector-client-web:browserProductionWebpack
```

This will build the client files in the `projector-client-web/build/distributions` dir.

## Running
After building, you can run the HTML page: `projector-client-web/build/distributions/index.html`.

## Notes
Tested browsers:
- Chromium.
- Firefox.

You can set connection parameters like this: `index.html?host=localhost&port=8887`. Supported parameters can be found in the `ParamsProvider.kt` file.

Shortcuts:
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
