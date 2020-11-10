function domReady(fn) {
    if (document.readyState === "complete" || document.readyState === "interactive") {
        setTimeout(fn, 1);
    } else {
        document.addEventListener("DOMContentLoaded", fn);
    }
}

domReady(function() {
    console.log("DOM loaded")
    const {ipcRenderer} = require('electron')
    ipcRenderer.send("projector-dom-ready");
    console.log("projector-dom-ready sent")
})
console.log("Preload started")
