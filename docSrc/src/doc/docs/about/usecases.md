We believe there are many cases when you may want to use Projector. Let's enumerate some of them to inspire you:

* Not running a Swing IDE on a high-end laptop, but running it on a server and accessing it via a **thin client**:
    * Usually, **high-end laptops cost more** than a server with comparable performance and a thin client.
    * A thin client not only has a relatively small price but also it **doesn't contain any valuable data**, so a case of its loss is not a big deal.
    * Thin clients are **more mobile**: you aren't restricted to use only x86, you can select ARM and devices that are cooler, more compact, and have longer battery life, for example, you can use iPad and a keyboard instead of MacBook.
* Not **remote debugging** in a local Swing IDE, but accessing a remote Swing IDE doing local debugging.
* Not **coding in nano or vim over ssh** but copying a Swing IDE and Projector there and access it.
* Run your Swing IDE in a **Linux environment but on Windows**: you can simply run it via Projector Server in WSL and use a client in Windows. This is easier than other methods such as installing X Server to WSL or using a visual virtual machine.
* Do **pair programming** sessions remotely: multiple clients can simultaneously connect to the same Swing IDE and interact with it in rotation.
* You can **turn your device off** while your app continues work on a server. For example, if your project is so huge that it takes a night to compile it fully, you can leave this task for a server and take your device with you without risk of overheating in your bag.
