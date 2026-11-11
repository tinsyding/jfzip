import { render } from 'preact';
import { App } from './app';
import './styles.css';

const root = document.getElementById('app');
if (root) {
  render(<App />, root);
}

if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('./sw.js').catch(() => {
      // The app still works without offline caching.
    });
  });
}
