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
/**
 * LRU cache for URLs entered by the user
 */

const datalist = document.getElementById("url-data-list");
const projectorLauncherStorageKey = "projector-launcher-urls"
const storage = window.localStorage;

class LRU {
  constructor(max = 10) {
    this.max = max;
    this.cache = new Map();
  }

  get(key) {
    let item = this.cache.get(key);
    if (item) {
      // refresh key
      this.cache.delete(key);
      this.cache.set(key, item);
    }
    return item;
  }

  set(key, val) {
    // refresh key
    if (this.cache.has(key)) {
      this.cache.delete(key);
    }// evict oldest
    else if (this.cache.size === this.max) this.cache.delete(this.first());
    this.cache.set(key, val);
  }

  first() {
    return this.cache.keys().next().value;
  }

  keys() {
    return this.cache.keys();
  }
}

function cacheNewUrlValue(url) {
  urlCache.set(url, url);

  clearChildren(datalist);
  populateDataList();

  storage.setItem(projectorLauncherStorageKey, JSON.stringify(Array.from(urlCache.keys())))
}

function clearChildren(parent) {
  while (parent.childNodes.length > 0) {
    parent.removeChild(parent.firstChild);
  }
}


function populateDataList() {
  // In reverse order to have the latest url on top in the datalist
  for (let key of Array.from(urlCache.keys()).reverse()) {
    addDataListOption(key);
  }
}

function addDataListOption(url) {
  let option = document.createElement('option');
  option.setAttribute('value', url);
  option.innerText = url;
  datalist.appendChild(option);
}

const urlCache = new LRU()

export {urlCache, cacheNewUrlValue, populateDataList, projectorLauncherStorageKey, storage};
