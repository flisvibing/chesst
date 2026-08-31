import { mountNav, renderFooter } from './common.js';
mountNav('home');
document.getElementById('footer-mount').innerHTML = renderFooter();
