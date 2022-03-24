Some options can help you to customize the server.

## Handling algorithm

Firstly, an option is searched in Java system properties, and if not found, it's searched in environment variables. If it is not found in both places, the default value is used.

### Specifying Java system properties

Java system properties are special storage of any Java process. It can be changed in runtime, but its initial items can also be set in Java process run arguments.

Example: you want to set `ORG_JETBRAINS_PROJECTOR_P1` option to `value1` and `ORG_JETBRAINS_PROJECTOR_P2` option to `value2`. Add two run arguments: `-DORG_JETBRAINS_PROJECTOR_P1=value1` and `-DORG_JETBRAINS_PROJECTOR_P2=value2`.

So an argument starts with `-D` and separates the option and its value with `=`.

Also, in IntelliJ-based IDEs, there is a special file in **Help | Custom VM Options...** that contains run arguments applied to the IDE on start, one argument per line. You can add options there too.

!!! important
    Don't forget to restart the IDE to apply the options from the **Custom VM Options** file.

### Specifying environment variables

This is a more common way of setting up apps. For example, for Linux, you can do `export ORG_JETBRAINS_PROJECTOR_P1=value1`.

## Applying

### For projector-installer

Most options will be asked during config creation and then are automatically passed as Java system properties to the IDE process. Others can be set manually via system properties or environment variables.

### For projector-docker

You can pass environment variables via `-e` parameter of `docker run` command easily. Also, system properties in Custom VM options are available too.

### For projector-plugin

Most options are available to set up in the UI. Some of them can be changed in the runtime, just change them and click the **Save** button in the Projector dialog window.

Others can be set manually via system properties or environment variables.

## List of parameters

### Enable auto keymap setting

Name | Type | Default value
---|---|---
`ORG_JETBRAINS_PROJECTOR_SERVER_AUTO_KEYMAP` | Boolean | `true`

When a client connects to the server, it reports its OS. By default, the server switches the keymap to match the last connected client's OS.

If you use a custom keymap or, for some other reason, don't want to switch keymaps automatically, set this setting to `false`.

!!! note
    If you disable this setting, the keymap won't be reverted back to what it has been set to automatically. Please set it back manually in IDE Settings/Preferences.

### Force keyboard modifiers mode

Name | Type | Default value
---|---|---
`ORG_JETBRAINS_PROJECTOR_SERVER_MAC_KEYBOARD` | Boolean? | `null`

Set to `true` to make keyboard modifiers like "Option(Alt)+Minus gives Dash" work for Mac clients. Set to `false` to disable this.

By default, it's dynamic (`null`): it's based on the last connected client OS.

### Assigning connection password

Name | Type | Default value
---|---|---
`ORG_JETBRAINS_PROJECTOR_SERVER_HANDSHAKE_TOKEN` | String? | `null` (no password)
`ORG_JETBRAINS_PROJECTOR_SERVER_RO_HANDSHAKE_TOKEN` | String? | `null` (no password)

You can set a password that will be validated on the connection start on the server. There are two variants of access: full (**read/write**) when you can control UI via mouse and keyboard and **read-only** when you can only watch.  
On the server-side, provide the first setting containing the password for full access and the second setting for read-only access. On the client-side, specify the password in query parameters like this: <http://localhost:8080/?token=mySecretPassword> (as mentioned in [web client options](https://github.com/JetBrains/projector-client/tree/master/projector-client-web#page-parameters)).

!!! warning
    If you don't set passwords, by default, they are equal to `null`. If **rw** and **ro** passwords are the same, the server gives full access to clients with a correct password. This also means that if you assign only one password, another type of access will be available without a password.

!!! note
    For now, "no password" (`null`) and "empty password" (`""`) are different values.

### Setting host and port to listen to

Name | Type | Default value
---|---|---
`ORG_JETBRAINS_PROJECTOR_SERVER_HOST` | String | `"0.0.0.0"` (wildcard – all addresses)
`ORG_JETBRAINS_PROJECTOR_SERVER_PORT` | Int | `8887`

No explanation is needed here: you can set up the host and the port where the server is available.

### Making the connection secure

Name | Type | Default value
---|---|---
`ORG_JETBRAINS_PROJECTOR_SERVER_SSL_PROPERTIES_PATH` | String? | `null`

To enable SSL for the server, as a value of this setting, specify a path to a file with SSL properties. Example of such a file:

```properties
STORE_TYPE=JKS
FILE_PATH=/path-to/keystore.jks
STORE_PASSWORD=mypassword
KEY_PASSWORD=mypassword
```

If you do everything right, the server launch log will contain something like `WebSocket SSL is enabled: /path-to/properties.file`. If it logs `WebSocket SSL is disabled` instead, something is wrong. Maybe the setting can't be found, or there is an exception parsing the properties file (it will be logged).  
After that, enable SSL on the client-side by adding the `wss` query parameter like this: <https://localhost:8080/?wss>. Make sure that your browser trusts the certificate you use.

### Adjust scrolling speed

Name | Type | Default value
---|---|---
`ORG_JETBRAINS_PROJECTOR_SERVER_PIXEL_PER_UNIT` | Int | `100`

If you want faster scrolling, you should decrease the value, so for example, the value of `50` means the scrolling will be two times faster.

!!! bug
    Actually, we want to get rid of this option in the future, determining scrolling speed automatically, synced with the client. More details: [PRJ-272](https://youtrack.jetbrains.com/issue/PRJ-272).

### Disabling WebSocket server

Name | Type | Default value
---|---|---
`ORG_JETBRAINS_PROJECTOR_SERVER_ENABLE_WS_SERVER` | Boolean | `true`

Setting this option to `false` will disable the built-in WebSocket server.  
Additional transports can be added at runtime via `ProjectorServer.addTransport`

!!! warning
    With default Projector distribution, this will leave you with no way to connect to it. This option is intended primarily for use with alternative embedding approaches.

### Disabling IDE and plugins updates

Name | Type | Default value
---|---|---
`ORG_JETBRAINS_PROJECTOR_SERVER_DISABLE_IDEA_UPDATES` | Boolean | `true`

Setting this option to `false` will enable the platform and plugins updates.

### Disabling attaching to the IDE

Name | Type | Default value
---|---|---
`ORG_JETBRAINS_PROJECTOR_SERVER_ATTACH_TO_IDE` | Boolean | `true`

Setting this option to `false` will disable integrations of Projector with the IDE. By default (`true`), Projector decides it automatically: it attaches if it's able to do so.

## Difference between env vars and system props

The advantage of specifying a system property is that it's not passed to a child process automatically.

So, for example, passwords won't be passed to a child process: this is more secure, and also it makes Projector development inside Projector easier.

Just because of this inheritance, firstly, system props are checked that can't be inherited, and only if they aren't specified, env vars are checked that can be inherited and not expected to be used.
