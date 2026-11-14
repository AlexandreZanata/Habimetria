// Proofs remain in the fragment/DOM only, never query strings or web storage.
const fragment = location.hash.slice(1);
if (fragment) {
  history.replaceState(null, '', location.pathname);
  const verification = /^verificar=([A-Za-z0-9_-]{43})$/.exec(fragment);
  const recovery = /^recuperar=([A-Za-z0-9_-]{43})$/.exec(fragment);
  if (recovery) {
    const field = document.getElementById('recovery-code');
    if (field) field.value = recovery[1];
    else if (location.pathname !== '/painel/redefinir') location.replace('/painel/redefinir#recuperar=' + recovery[1]);
  } else if (verification) {
    const field = document.getElementById('verification-code');
    if (field) field.value = verification[1];
    else if (location.pathname !== '/painel/verificar') location.replace('/painel/verificar#verificar=' + verification[1]);
  } else if (/^[A-Za-z0-9_-]{43}$/.test(fragment)) {
    const field = document.getElementById('codigo');
    if (field) {
      document.getElementById('ativacao').open = true;
      field.value = fragment;
    } else location.replace('/painel/entrar#' + fragment);
  }
}
