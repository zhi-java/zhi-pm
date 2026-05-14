import puppeteer from 'puppeteer';
import { mkdir } from 'fs/promises';

const BASE = 'http://localhost:8088';
const DIR = 'docs/screenshots';

const pages = [
    { name: 'landing',   path: '/',             desc: '首页' },
    { name: 'presence',  path: '/presence.html', desc: '在线状态' },
    { name: 'push',      path: '/push.html',     desc: '消息推送' },
    { name: 'danmaku',   path: '/danmaku.html',  desc: '直播弹幕' },
    { name: 'chat',      path: '/chat.html',     desc: '聊天室' },
    { name: 'orders',    path: '/orders.html',   desc: '订单追踪' },
    { name: 'api',       path: '/api.html',      desc: 'API 接入' },
    { name: 'admin',     path: '/admin.html',    desc: '管理后台' },
    { name: 'metrics',   path: '/metrics.html',  desc: '实时指标' },
];

async function main() {
    await mkdir(DIR, { recursive: true });

    const browser = await puppeteer.launch({
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox', '--window-size=1440,900']
    });

    const page = await browser.newPage();
    await page.setViewport({ width: 1440, height: 900, deviceScaleFactor: 2 });

    for (const p of pages) {
        const url = BASE + p.path;
        console.log(`Screenshot: ${p.desc} -> ${url}`);
        try {
            await page.goto(url, { waitUntil: 'networkidle2', timeout: 15000 });
            // Wait for lucide icons and animations to settle
            await new Promise(r => setTimeout(r, 1500));
            // For API page, expand first category (already default)
            const file = `${DIR}/${p.name}.png`;
            await page.screenshot({ path: file, fullPage: false });
            console.log(`  Saved: ${file}`);
        } catch (err) {
            console.error(`  Error: ${err.message}`);
        }
    }

    await browser.close();
    console.log('\nDone! Screenshots saved to docs/screenshots/');
}

main().catch(console.error);
