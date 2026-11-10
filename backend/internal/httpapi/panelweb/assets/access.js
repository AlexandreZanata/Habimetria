const code = location.hash.slice(1);
if (code) {
  history.replaceState(null, '', location.pathname);
  if (/^[A-Za-z0-9_-]{43}$/.test(code)) {
    document.getElementById('ativacao').open = true;
    document.getElementById('codigo').value = code;
  }
}
