export async function initRouter() {
  const root = document.getElementById('app');

  async function loadPage(path) {
    let html = '';
    let cssPath = '';
    let scriptModule;

    if (path === '/' || path === '/reception') {
      html = await fetch('/src/reception/template.html').then(r => r.text());
      scriptModule = await import('/src/reception/app.js');
    } else if (path === '/chief') {
      html = await fetch('/src/chief/template.html').then(r => r.text());
      scriptModule = await import('/src/chief/app.js');
    }
    else if (path === '/doctor') {
        html = await fetch('/src/doctor/template.html').then(r => r.text());
        scriptModule = await import('/src/doctor/app.js');
    }
    else {
      html = '<h2>404 — Страница не найдена</h2>';
    }

    root.innerHTML = html;

    if (scriptModule?.initPage) scriptModule.initPage();
  }

  await loadPage(window.location.pathname);

  window.onpopstate = () => loadPage(window.location.pathname);
}
