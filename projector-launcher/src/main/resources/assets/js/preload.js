/*
 * MIT License
 *
 * Copyright (c) 2019-2023 JetBrains s.r.o.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

const {ipcRenderer, contextBridge} = require("electron");

contextBridge.exposeInMainWorld("api", {
  send: (channel, ...data) => {
    const allowedChannels = [
      "projector-connect",
      "projector-dom-ready",
      "toolboxinfo-ok",
      "projector-set-url"
    ];

    if (allowedChannels.includes(channel)) {
      ipcRenderer.send(channel, ...data);
    }
  },

  receive: (channel, cb) => {
    const allowedChannels = [
      "projector-set-url",
    ];
    if (allowedChannels.includes(channel)) {
      ipcRenderer.on(channel, (event, ...args) => cb(...args));
    }
  },
});

function domReady(fn) {
  if (document.readyState === "complete" || document.readyState === "interactive") {
    setTimeout(fn, 1);
  }
  else {
    document.addEventListener("DOMContentLoaded", fn);
  }
}

domReady(function () {
  console.log("DOM loaded")
  const {ipcRenderer} = require('electron')
  ipcRenderer.send("projector-dom-ready");
  console.log("projector-dom-ready sent")
})
console.log("Preload started")
