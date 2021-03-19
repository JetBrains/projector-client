# Projector Electron Launcher

## Downloading and running

Please check out [documentation](https://jetbrains.github.io/projector-client/mkdocs/latest/ij_user_guide/accessing/#client-app-launcher).

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
        - `brew install --cask --no-quarantine wine-stable`
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
