# Projector Electron Launcher
## Downloading
Please check [releases](https://github.com/JetBrains/projector-client/releases) beginning with the `launcher-` prefix. Download the file for your OS.

## Running
### Windows
Run `projector.exe` file.

### Linux
Run `projector` file.

### Mac
Run `projector` app (on other OSes it's visible like `projector.app` dir).

Mac doesn't allow unsigned apps to be run easily, and will ask you to put the app to Trash Bin. So you need to select [Open Anyway in System Preferences](https://stackoverflow.com/a/59899342/6639500) to allow launching.
 
## Using sources
### Prerequisites
- Install Java: <https://bell-sw.com/pages/downloads>
- Install Node.js and NPM: <https://nodejs.org/en/download>
    - You have to use 12.16.1 version
    - You can try use Node Version Manager
        - Linux and Mac: <https://github.com/nvm-sh/nvm>
        - Windows: <https://github.com/coreybutler/nvm-windows>
- If your OS is not Windows you must install Wine:
    - Mac: 
        - `brew cask install xquartz`
        - `brew cask install wine-stable`
    - Ubuntu, Debian: `apt install wine-stable`
    - ArchLinux: `pacman -S wine`
    - Fedora:
        - Fedora 33: `dnf config-manager --add-repo https://dl.winehq.org/wine-builds/fedora/33/winehq.repo`
        - Fedora 32: `dnf config-manager --add-repo https://dl.winehq.org/wine-builds/fedora/32/winehq.repo`
        - `dnf install winehq-stable` 

### Building from source
```shell script
./gradlew :projector-launcher:dist
```

After that, executables will be generated in the `build/electronOut` dir.

### Running from source
```shell script
./gradlew :projector-launcher:electronProductionRun
```
