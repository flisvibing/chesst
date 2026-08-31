import { mountNav, renderFooter, api, toast, ApiError, escapeHtml } from './common.js';
mountNav('register');
document.getElementById('footer-mount').innerHTML = renderFooter();

const form = document.getElementById('register-form');
const alertBox = document.getElementById('alert');
const submit = document.getElementById('submit');

function showAlert(msg, type = 'error') {
  alertBox.innerHTML = `<div class="alert alert-${type}">${escapeHtml(msg)}</div>`;
}

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  alertBox.innerHTML = '';
  const username = document.getElementById('username').value.trim();
  const email = document.getElementById('email').value.trim();
  const password = document.getElementById('password').value;
  const confirm = document.getElementById('confirm').value;
  if (password !== confirm) { showAlert('Passwords do not match.'); return; }
  submit.disabled = true;
  submit.innerHTML = '<span class="spinner"></span> Creating…';
  try {
    await api.post('/api/auth/register', { username, email, password, confirmPassword: confirm }, { auth: false });
    form.style.display = 'none';
    document.querySelector('.auth-card').insertAdjacentHTML('beforeend', `
      <div id="verify-step">
        <div class="alert alert-info">
          Account created. We sent a 6-digit verification code to <strong>${escapeHtml(email)}</strong>.
          Enter it below to activate your account.
        </div>
        <form id="verify-form">
          <div class="form-group">
            <label for="code">Verification code</label>
            <input class="input" id="code" type="text" inputmode="numeric" maxlength="6" required pattern="[0-9]{6}">
          </div>
          <button class="btn btn-primary" id="verify-submit" style="width:100%;">Verify & sign in</button>
        </form>
        <p class="auth-foot">Didn't get it? <a href="#" id="resend">Resend code</a></p>
      </div>
    `);
    wireVerify(email);
  } catch (err) {
    if (err instanceof ApiError) showAlert(err.message);
    else showAlert('Something went wrong. Please try again.');
    submit.disabled = false;
    submit.textContent = 'Create account';
  }
});

function wireVerify(email) {
  const verifyForm = document.getElementById('verify-form');
  const verifySubmit = document.getElementById('verify-submit');
  verifyForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const code = document.getElementById('code').value.trim();
    verifySubmit.disabled = true;
    verifySubmit.innerHTML = '<span class="spinner"></span> Verifying…';
    try {
      await api.post('/api/auth/verify-email', { email, code }, { auth: false });
      toast('Email verified! You can sign in now.', 'success');
      setTimeout(() => window.location.href = 'login.html', 800);
    } catch (err) {
      verifySubmit.disabled = false;
      verifySubmit.textContent = 'Verify & sign in';
      showAlert(err.message || 'Verification failed.');
    }
  });
  document.getElementById('resend').addEventListener('click', async (e) => {
    e.preventDefault();
    try {
      await api.post('/api/auth/resend-verification', { email }, { auth: false });
      toast('A new code has been sent.', 'success');
    } catch (err) { toast(err.message || 'Could not resend.', 'error'); }
  });
}
