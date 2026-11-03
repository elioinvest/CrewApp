// Copies the browser files the app uses (xterm.js) from node_modules into www/vendor, so the app
// needs no bundler: www/ is the whole app, served by Capacitor's WebView.
const fs = require('fs');
const path = require('path');

const files = [
  ['@xterm/xterm/lib/xterm.js', 'xterm.js'],
  ['@xterm/xterm/css/xterm.css', 'xterm.css'],
  ['@xterm/addon-fit/lib/addon-fit.js', 'addon-fit.js'],
  ['@xterm/addon-webgl/lib/addon-webgl.js', 'addon-webgl.js'],
];
const out = path.join(__dirname, '..', 'www', 'vendor');
fs.mkdirSync(out, { recursive: true });
for (const [from, to] of files) fs.copyFileSync(path.join(__dirname, '..', 'node_modules', from), path.join(out, to));
console.log(`Copied ${files.length} files to www/vendor`);
