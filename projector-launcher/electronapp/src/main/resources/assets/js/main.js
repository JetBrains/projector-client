document.querySelector('#connect-button').addEventListener('click', function () {
    let url = document.getElementById("url-text-field").value;
    const {ipcRenderer} = require('electron')
    ipcRenderer.send("projector-connect", url);

});

//$( document ).ready(function() {
    const {ipcRenderer} = require('electron')
    ipcRenderer.on('projector-set-url', (event, arg) => {
        console.log("New URL: "+ arg);
        document.getElementById("url-text-field").value = arg
    })
//});
