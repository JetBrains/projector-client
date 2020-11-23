/*
 * MIT License
 *
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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
window.onload = function() {
  document.getElementById("url-text-field").focus();
};

// Cache used URLs
const storage = window.localStorage;
const datalist = document.getElementById("url-data-list");
const projectorLauncherStorageKey = "projector-launcher-urls"
const results = (storage.getItem(projectorLauncherStorageKey)) ? JSON.parse(storage.getItem(projectorLauncherStorageKey)) : [];

// Add cached URLs to the combobox datalist at start up
results.forEach(result => addDataListOption(result))

function addDataListOption(url) {
  let option = document.createElement('option');
  option.setAttribute('value', url);
  option.innerText = url;
  datalist.appendChild(option);
}

document.querySelector('#connect-button').addEventListener('click', function () {
  connect()
});

document.querySelector('#url-text-field').addEventListener('keypress', function (e) {
  if (e.key === 'Enter') {
    connect()
  }
});

function connect() {
  let url = document.getElementById("url-text-field").value;
  if (isEmpty(url)) {
    return;
  }

  const {ipcRenderer} = require('electron')
  ipcRenderer.send("projector-connect", url);

  cacheNewUrlValue(url);
}

function cacheNewUrlValue(url) {
  if (results.indexOf(url) < 0) {
    results.push(url);
    storage.setItem(projectorLauncherStorageKey, JSON.stringify(results));
    addDataListOption(url)
  }
}

//$( document ).ready(function() {
const {ipcRenderer} = require('electron')
ipcRenderer.on('projector-set-url', (event, arg) => {
  console.log("New URL: " + arg);
  document.getElementById("url-text-field").value = arg
})
//});

function isEmpty(str) {
  return (!str || 0 === str.length);
}