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
