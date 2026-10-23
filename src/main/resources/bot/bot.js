// Bot sterowany z moda (Fabric). Komunikacja: JSON, jedna linia na wiadomosc, przez stdin/stdout.
const mineflayer = require('mineflayer');
const readline = require('readline');

let bot = null;
let autoRules = true;
let password = null;
let loggedIn = false;

function out(type, text) {
  process.stdout.write(JSON.stringify({ type, text: String(text) }) + '\n');
}
const log = (t) => out('log', t);

function itemName(item) {
  if (!item) return '';
  let n = item.displayName || item.name || '';
  try {
    if (item.customName) {
      const c = typeof item.customName === 'string' ? item.customName : JSON.stringify(item.customName);
      n += ' ' + c.replace(/\\u00a7./g, '').replace(/[{}"\[\]]/g, ' ');
    }
  } catch (e) {}
  return n;
}

function describeWindow(win) {
  if (!win) return log('[bot] Brak otwartego okna.');
  let title = win.title;
  try { title = typeof title === 'string' ? title : JSON.stringify(title); } catch (e) {}
  log('[bot] Okno: ' + String(title).replace(/§./g, ''));
  win.slots.forEach((item, i) => {
    if (item) log('  slot ' + i + ': ' + itemName(item).trim() + ' x' + item.count);
  });
}

function tryAcceptRules(win) {
  if (!autoRules || !win) return false;
  const re = /(akceptuj|zaakceptuj|akceptuje|accept|zgadzam|agree|regulamin)/i;
  for (let i = 0; i < win.slots.length; i++) {
    const it = win.slots[i];
    if (it && re.test(itemName(it))) {
      log('[bot] Auto-akceptacja: klikam slot ' + i + ' (' + itemName(it).trim() + ')');
      setTimeout(() => bot && bot.clickWindow(i, 0, 0).catch((e) => log('[bot] blad klikniecia: ' + e.message)), 600);
      return true;
    }
  }
  return false;
}

function connect(opts) {
  if (bot) return log('[bot] Juz polaczony/laczenie.');
  autoRules = opts.autoRules !== false;
  password = opts.password || null;
  loggedIn = false;
  const options = {
    host: opts.host,
    port: opts.port || 25565,
    username: opts.username || 'Bot',
    auth: 'offline',
  };
  if (opts.version) options.version = opts.version;
  log('[bot] Lacze z ' + options.host + ':' + options.port + ' jako ' + options.username + '...');
  bot = mineflayer.createBot(options);

  bot.once('login', () => out('state', 'connected'));
  bot.once('spawn', () => log('[bot] Zespawnowany.'));

  bot.on('messagestr', (msg) => {
    const clean = msg.replace(/§./g, '');
    if (clean.trim()) log(clean);
    // automatyczne logowanie/rejestracja, jesli podano haslo
    if (password && !loggedIn) {
      if (/\/register|zarejestruj/i.test(clean)) {
        loggedIn = true;
        setTimeout(() => bot && bot.chat('/register ' + password + ' ' + password), 800);
      } else if (/\/login|zaloguj/i.test(clean)) {
        loggedIn = true;
        setTimeout(() => bot && bot.chat('/login ' + password), 800);
      }
    }
  });

  bot.on('windowOpen', (win) => {
    describeWindow(win);
    tryAcceptRules(win);
  });
  bot.on('kicked', (reason) => log('[bot] Wyrzucony: ' + (typeof reason === 'string' ? reason : JSON.stringify(reason))));
  bot.on('error', (err) => log('[bot] Blad: ' + err.message));
  bot.on('end', (reason) => {
    log('[bot] Rozlaczono (' + reason + ').');
    out('state', 'disconnected');
    bot = null;
  });
}

const rl = readline.createInterface({ input: process.stdin });
rl.on('line', (line) => {
  let m;
  try { m = JSON.parse(line); } catch (e) { return; }
  switch (m.cmd) {
    case 'connect': connect(m); break;
    case 'chat': if (bot) bot.chat(m.text); else log('[bot] Nie polaczony.'); break;
    case 'click':
      if (!bot || !bot.currentWindow) { log('[bot] Brak otwartego okna.'); break; }
      bot.clickWindow(m.slot, 0, 0).then(() => log('[bot] Klik slot ' + m.slot)).catch((e) => log('[bot] blad: ' + e.message));
      break;
    case 'window': describeWindow(bot && bot.currentWindow); break;
    case 'closeWindow': if (bot && bot.currentWindow) bot.closeWindow(bot.currentWindow); break;
    case 'autoRules': autoRules = !!m.value; log('[bot] Auto-regulamin: ' + (autoRules ? 'wl' : 'wyl')); break;
    case 'disconnect': if (bot) bot.quit(); setTimeout(() => process.exit(0), 300); break;
  }
});
rl.on('close', () => { if (bot) bot.quit(); process.exit(0); });
process.on('uncaughtException', (e) => log('[bot] Wyjatek: ' + e.message));
