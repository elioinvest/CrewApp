// Copies www into Crew (../Crew/phone), which serves it to phone browsers from Settings → Phone access.
const fs = require('fs');
const path = require('path');

const src = path.join(__dirname, '..', 'www');
const dest = path.join(__dirname, '..', '..', 'Crew', 'phone');
if (!fs.existsSync(path.dirname(dest))) { console.error(`Crew not found at ${path.dirname(dest)}`); process.exit(1); }
fs.rmSync(dest, { recursive: true, force: true });
fs.cpSync(src, dest, { recursive: true });
console.log(`Copied www to ${dest}`);
