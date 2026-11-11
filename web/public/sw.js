const CACHE = 'jfzip-v1';
const SCOPE = self.registration.scope;
const INDEX = new URL('./index.html', SCOPE).href;

const SHELL = [
  new URL('./', SCOPE).href,
  INDEX,
  new URL('./manifest.webmanifest', SCOPE).href,
  new URL('./icons/icon.svg', SCOPE).href,
  new URL('./icons/favicon-32.png', SCOPE).href,
  new URL('./icons/icon-192.png', SCOPE).href,
  new URL('./icons/icon-512.png', SCOPE).href,
  new URL('./icons/icon-maskable-512.png', SCOPE).href,
  new URL('./icons/apple-touch-icon.png', SCOPE).href,
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches
      .open(CACHE)
      .then((cache) => cache.addAll(SHELL))
      .then(() => self.skipWaiting()),
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(keys.filter((key) => key !== CACHE).map((key) => caches.delete(key))),
      )
      .then(() => self.clients.claim()),
  );
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;
  if (url.pathname.endsWith('/sw.js')) return;

  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((response) => {
          if (response.ok) {
            const copy = response.clone();
            caches.open(CACHE).then((cache) => cache.put(INDEX, copy));
            return response;
          }
          return caches.match(INDEX).then((cached) => cached || response);
        })
        .catch(() => caches.match(INDEX)),
    );
    return;
  }

  event.respondWith(
    caches.match(request).then((cached) => {
      if (cached) return cached;
      return fetch(request).then((response) => {
        if (response.ok) {
          const copy = response.clone();
          caches.open(CACHE).then((cache) => cache.put(request, copy));
        }
        return response;
      });
    }),
  );
});
