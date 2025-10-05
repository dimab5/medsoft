export async function initRouter() {
  const root = document.getElementById('app');

  async function loadPage(path) {
    let html = '';
    let cssPath = '';
    let scriptModule;

    if (path === '/' || path === '/reception') {
      html = await fetch('/src/reception/template.html').then(r => r.text());
      cssPath = '/src/reception/style.css';
      scriptModule = await import('/src/reception/app.js');
    } else if (path === '/chief') {
      html = await fetch('/src/chief/template.html').then(r => r.text());
      cssPath = '/src/chief/style.css';
      scriptModule = await import('/src/chief/app.js');
    } else {
      html = '<h2>404 — Страница не найдена</h2>';
    }

    root.innerHTML = html;

    if (scriptModule?.initPage) scriptModule.initPage();
  }

  await loadPage(window.location.pathname);

  window.onpopstate = () => loadPage(window.location.pathname);
}
