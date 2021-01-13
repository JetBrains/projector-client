Here we formulate the pros of Projector:

* Since our goal is to support only AWT apps, Projector is **more compact** than more general remote access software like RDP or VNC. You don't even have to install special apps anywhere: on a server-side, you need only JRE, but there is one because your app is a Java app; on a client-side, you need only a Web browser, and you have it on almost any device.
* Since Projector Server knows about **AWT components**, some of them can be serialized in a special way to be shown on a client **natively**. The example is the Markdown Preview of JetBrains IDEs.
* Projector supports **simultaneous client connections** to the same application instance.
