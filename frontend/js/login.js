import { mountNav, renderFooter, api, authStore, toast, ApiError } from './common.js';
mountNav('login');
document.getElementById('footer-mount').innerHTML = renderFooter();

const form = document.getElementById('login-form');
const alertBox = document.getElementById('alert');
const submit = document.getElementById('submit');

function showAlert(msg, type = 'error') {
  alertBox.innerHTML = `<div class="alert alert-${type}">${msg}</div>`;
}

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  alertBox.innerHTML = '';
  submit.disabled = true;
  submit.innerHTML = '<span class="spinner"></span> Signing in…';
  try {
    const identifier = document.getElementById('identifier').value.trim();
    const password = document.getElementById('password').value;
    const res = await api.post('/api/auth/login', { identifier, password });
    authStore.setSession(res.accessToken, res.user);
    toast('Welcome back, ' + res.user.username + '!', 'success');
    setTimeout(() => window.location.href = 'analysis.html', 400);
  } catch (err) {
    if (err instanceof ApiError) showAlert(err.message);
    else showAlert('Something went wrong. Please try again.');
    submit.disabled = false;
    submit.textContent = 'Sign in';
  }
});
