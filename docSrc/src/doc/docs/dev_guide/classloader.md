## Purpose

!!! summary 
    Allows loading classes from Projector, Projector agent and Intellij Platform without the use of reflection

Default AppClassLoader of Projector Server doesn't know anything about JB IDE and so cannot load its classes as they were loaded by different classloader. Similar, IDE ClassLoader doesn't know anything about Projector classes (and can be gathered only after IDE startup). 

The idea behind ProjectorClassLoader is simple - delegate classloading to the right classloader. If we think classname determines a class from Intellij Platform then we should load it via IDE ClassLoader. If not, then we fall back to load it via AppClassLoader. 

To specify how classes should be loaded with ProjectorClassLoader you need to call one of the `forceLoadBy*` methods: 

Method Name                         | ClassLoader for specified class(es)
---                                 | ---
`forceLoadByPlatform`               | AppClassLoader 
`forceLoadByProjectorClassLoader`   | ProjectorClassLoader (gather bytecode from any of AppClassLoader or IDE ClassLoader and then define class ourselves) 
`forceLoadByIdea` | IDE ClassLoader (PathClassloader or UrlClassLoader, depending on IDE version)

!!! note 
    Another way to specify ClassLoader for class is to apply `UseProjectorLoader` annotation.

## Initializing ProjectorClassLoader
To initialize ProjectorClassLoader, call function 
```kotlin
package org.jetbrains.projector.server.core.classloader

fun initClassLoader(classLoader: ClassLoader): ClassLoader
``` 
 somewhere at the entry point of Projector to correctly set up an instance of ProjectorClassLoader: assign IDE ClassLoader when IDE is initialized, define classes that should be force loaded via AppClassLoader or ProjectorClassLoader.
 
!!! important
    Don't forget to load some other class (which will be the actual entry point for the Projector/Projector agent logic) with returned ClassLoader (if current ClassLoader is not ProjectorClassLoader) to start using ProjectorClassLoader
 
Currently, function is used at:
  
Class                                              | Method         | Reason
---                                                | ---            | ---
`org.jetbrains.projector.server.ProjectorLauncher` | `main(args: Array<String>)` | Default entry point
`org.jetbrains.projector.server.ProjectorLauncher` | `runProjectorServer(): Boolean` | Entry point for CWM
`org.jetbrains.projector.agent.MainAgent` | `agentmain(args: String?, instrumentation: Instrumentation)` | Projector Plugin entry point

## Support for agent classes

Agent classes are now loaded in the same manner as regular Projector classes. However, if in the future ProjectorClassLoader will be set as a System ClassLoader, then ProjectorClassLoader needs to use `appendToClassPathForInstrumentation(String jarPath)` method to get the location of agent jar file. It's already implemented, so no extra work to support loading classes from agent JAR file is required.
