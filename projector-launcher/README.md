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

### Development

If you're a UI developer, and you want to speed up iterations when you constantly change HTML files, here are two new tasks introduced in 1.1.0: `electronRun` and `electronBuildAndRun`.

The problem is, preparing the full dist environment with `electronProductionRun` takes unbelievably long time, therefore you can build only HTML and Kotlin (with `electronBuildAndRun`) or build nothing at all (`electronRun`).

Please note, that the *fastest* way to get to do quick and dirty experiments with HTML and preload JS is to build the project only once (with `electronProductionRun`), then open the command line, change the directory to `build/distributions` and then run command `npx electron .`. You want to do this only if you work on quick fixing HTML. For building Kotlin, dependencies, and everything else `electronBuildAndRun` is still required.
