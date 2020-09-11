self.addEventListener('install', function (e) {
  e.waitUntil(
      caches.open('projector-store').then(function (cache) {
        return cache.addAll([]);
      })
  );
});

self.addEventListener('fetch', function (e) {
  console.log(e.request.url);
  e.respondWith(fetch(e.request));
});
