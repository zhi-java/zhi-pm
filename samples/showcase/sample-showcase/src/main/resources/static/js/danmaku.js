// ============================================================
// 弹幕直播模块
// ============================================================
const DANMAKU_USERS = ['alice', 'bob', 'charlie'];
const DANMAKU_ROOM = 'live-room-1';
let danmakuSendAs = '';
let danmakuHls = null;
let danmakuLikeCount = 0;

const DANMAKU_CHANNELS = [
    { name: 'Live Stream', url: 'https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master_1660.m3u8' },
];

function render_danmaku() {
    const panel = document.getElementById('panel-danmaku');
    panel.innerHTML = `
        <div class="danmaku-area" id="danmaku-screen">
            <video id="danmaku-video" muted autoplay playsinline></video>
            <div class="danmaku-layer" id="danmaku-layer"></div>
            <div class="danmaku-overlay">
                <div style="display:flex;align-items:center;gap:10px">
                    <span class="live-badge">LIVE</span>
                    <select class="danmaku-channel-select" id="danmaku-channel" onchange="danmakuSwitchChannel()">
                        ${DANMAKU_CHANNELS.map((c, i) => `<option value="${i}">${c.name}</option>`).join('')}
                    </select>
                    <span style="font-size:13px;color:rgba(255,255,255,0.6)" id="danmaku-viewer-info">0 人观看</span>
                </div>
                <div class="danmaku-overlay-right">
                    <span class="like-btn" onclick="danmakuLike()">
                        <i data-lucide="heart" style="width:16px;height:16px"></i>
                        <span id="danmaku-like-count">0</span>
                    </span>
                </div>
            </div>
        </div>
        <div class="card" style="margin-top:16px;margin-bottom:0">
            <div class="card-title" style="font-size:15px;margin-bottom:12px"><i data-lucide="users" style="width:18px;height:18px"></i> 用户连接</div>
            <div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:16px" id="danmaku-user-bar"></div>
            <div style="border-top:1px solid var(--border);padding-top:12px">
                <div style="display:flex;gap:8px;align-items:center;margin-bottom:8px">
                    <span style="font-size:13px;font-weight:500;color:var(--text-secondary)">发送身份：</span>
                    <div id="danmaku-send-as" style="display:flex;gap:4px"></div>
                </div>
                <div class="form-row" style="margin-bottom:0">
                    <input class="input" id="danmaku-content" placeholder="发送弹幕..." onkeydown="if(event.key==='Enter')danmakuSend()">
                    <button class="btn btn-primary" onclick="danmakuSend()"><i data-lucide="send" style="width:14px;height:14px"></i> 发送</button>
                </div>
            </div>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-top:16px">
            <div class="card" style="margin-bottom:0">
                <div class="card-title" style="font-size:15px">禁言管理</div>
                <div id="danmaku-muted" style="margin-bottom:10px"><span style="color:var(--text-secondary);font-size:13px">无禁言用户</span></div>
                <div style="display:flex;gap:6px;align-items:center">
                    <select class="select" id="danmaku-mute-user" style="max-width:120px">
                        ${DANMAKU_USERS.map(u => `<option value="${u}">${u}</option>`).join('')}
                    </select>
                    <button class="btn btn-sm btn-danger" onclick="danmakuMute()">禁言</button>
                    <button class="btn btn-sm btn-ghost" onclick="danmakuUnmute()">解除</button>
                </div>
            </div>
            <div class="card" style="margin-bottom:0">
                <div class="card-title" style="font-size:15px">房间信息</div>
                <div style="font-size:13px;color:var(--text-secondary);line-height:1.8">
                    <div>房间 ID: <strong>${DANMAKU_ROOM}</strong></div>
                    <div>限速: 每用户 5 条/秒</div>
                    <div>内容过滤: spam、ads 等敏感词</div>
                    <div>在线人数: <span id="danmaku-room-count">-</span></div>
                </div>
            </div>
        </div>
    `;
    lucide.createIcons();
    initDanmakuVideo();
    renderDanmakuUserBar();
    renderDanmakuSendAs();
    danmakuRefreshMuted();
    danmakuRefreshCount();
}

function initDanmakuVideo() {
    const video = document.getElementById('danmaku-video');
    if (!video) return;
    const streamUrl = DANMAKU_CHANNELS[0].url;
    if (Hls.isSupported()) {
        danmakuHls = new Hls({ liveSyncDurationCount: 3, liveMaxLatencyDurationCount: 6 });
        danmakuHls.loadSource(streamUrl);
        danmakuHls.attachMedia(video);
        danmakuHls.on(Hls.Events.MANIFEST_PARSED, () => video.play().catch(() => {}));
        danmakuHls.on(Hls.Events.ERROR, (_, data) => {
            if (data.fatal) {
                console.warn('HLS fatal error:', data.type, data.details);
                showDanmakuFallback();
            }
        });
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        video.src = streamUrl;
        video.addEventListener('loadedmetadata', () => video.play().catch(() => {}));
    } else {
        showDanmakuFallback();
    }
}

function showDanmakuFallback() {
    const screen = document.getElementById('danmaku-screen');
    if (!screen || screen.querySelector('.danmaku-video-fallback')) return;
    const fb = document.createElement('div');
    fb.className = 'danmaku-video-fallback';
    fb.innerHTML = '<div style="font-size:40px;margin-bottom:8px">&#x1F4E1;</div><div>直播流加载失败</div><div style="font-size:12px;margin-top:4px">请检查网络或更换频道</div>';
    screen.insertBefore(fb, screen.firstChild);
}

function danmakuSwitchChannel() {
    const sel = document.getElementById('danmaku-channel');
    const idx = parseInt(sel.value);
    const ch = DANMAKU_CHANNELS[idx];
    const video = document.getElementById('danmaku-video');
    if (!video || !ch) return;
    const fb = document.querySelector('.danmaku-video-fallback');
    if (fb) fb.remove();
    if (danmakuHls) {
        danmakuHls.loadSource(ch.url);
        danmakuHls.attachMedia(video);
    } else {
        video.src = ch.url;
    }
    video.play().catch(() => {});
}

const DANMAKU_USER_COLORS = {
    alice: { bg: '#FF6B6B', light: 'rgba(255,107,107,0.15)' },
    bob: { bg: '#4ECDC4', light: 'rgba(78,205,196,0.15)' },
    charlie: { bg: '#C780FF', light: 'rgba(199,128,255,0.15)' }
};

function renderDanmakuUserBar() {
    const bar = document.getElementById('danmaku-user-bar');
    if (!bar) return;
    bar.innerHTML = DANMAKU_USERS.map(u => {
        const key = 'danmaku-' + u;
        const client = Store.wsClients[key];
        const online = client && client.connected;
        const colors = DANMAKU_USER_COLORS[u] || { bg: '#86868b', light: 'rgba(134,134,139,0.15)' };
        return `
            <div class="user-card ${online ? 'online' : ''}" style="min-width:140px;padding:10px 14px;cursor:pointer;border-color:${online ? colors.bg : 'var(--border)'}" onclick="danmakuToggleUser('${u}')">
                <div class="user-avatar" style="width:32px;height:32px;font-size:13px;background:${online ? colors.bg : 'var(--text-secondary)'}">${u[0].toUpperCase()}</div>
                <div>
                    <div style="font-size:13px;font-weight:500">${u.charAt(0).toUpperCase() + u.slice(1)}</div>
                    <div style="font-size:11px;color:${online ? colors.bg : 'var(--text-secondary)'}">${online ? '已连接' : '离线'}</div>
                </div>
            </div>
        `;
    }).join('');
}

function renderDanmakuSendAs() {
    const container = document.getElementById('danmaku-send-as');
    if (!container) return;
    const connectedUsers = DANMAKU_USERS.filter(u => Store.wsClients['danmaku-' + u]?.connected);
    if (connectedUsers.length === 0) {
        danmakuSendAs = '';
        container.innerHTML = '<span style="font-size:12px;color:var(--text-secondary)">请先连接用户</span>';
        return;
    }
    if (!connectedUsers.includes(danmakuSendAs)) danmakuSendAs = connectedUsers[0];
    container.innerHTML = connectedUsers.map(u => `
        <button class="btn btn-sm ${danmakuSendAs === u ? 'btn-primary' : 'btn-ghost'}" onclick="danmakuSetSendAs('${u}')">${u}</button>
    `).join('');
}

function danmakuSetSendAs(userId) {
    danmakuSendAs = userId;
    renderDanmakuSendAs();
}

function danmakuToggleUser(userId) {
    const key = 'danmaku-' + userId;
    const existing = Store.wsClients[key];
    if (existing && existing.connected) {
        existing.disconnect();
        delete Store.wsClients[key];
        renderDanmakuUserBar();
        renderDanmakuSendAs();
        danmakuRefreshCount();
        return;
    }
    const client = new WsClient('danmaku', `${WS_BASE}/ws?access_token=${userId}-token`, userId);
    client.on('danmaku.message', (msg) => {
        const content = msg.payload?.content || '';
        const sender = msg.from || 'unknown';
        spawnDanmaku(`${sender}: ${content}`);
    });
    client.on('room.joined', (msg) => {
        showToast(`${userId} 已加入 ${msg.payload?.roomId || DANMAKU_ROOM}`, 'success');
        danmakuRefreshCount();
    });
    client.on('danmaku.error', (msg) => {
        showToast(`[${userId}] 弹幕错误: ${msg.payload?.message || '未知'}`, 'error');
    });
    client.onOpen(() => {
        client.send('room.join', { roomId: DANMAKU_ROOM }, { roomId: DANMAKU_ROOM });
        renderDanmakuUserBar();
        renderDanmakuSendAs();
        danmakuRefreshCount();
    });
    Store.wsClients[key] = client;
    client.connect();
    renderDanmakuUserBar();
}

function spawnDanmaku(text) {
    const layer = document.getElementById('danmaku-layer');
    if (!layer) return;
    const el = document.createElement('div');
    el.className = 'danmaku-item';
    const colonIdx = text.indexOf(': ');
    let userClass = '';
    let userLabel = '';
    let content = text;
    if (colonIdx > 0) {
        const username = text.substring(0, colonIdx).toLowerCase();
        content = text.substring(colonIdx + 2);
        userClass = ` danmaku-user-${username}`;
        userLabel = `<span class="danmaku-user${userClass}">${text.substring(0, colonIdx)}</span>`;
    }
    el.innerHTML = `${userLabel}<span class="danmaku-text">${content}</span>`;
    el.style.top = (10 + Math.random() * 60) + '%';
    const duration = 8 + Math.random() * 4;
    el.style.animationDuration = duration + 's';
    layer.appendChild(el);
    setTimeout(() => el.remove(), duration * 1000);
}

function danmakuSend() {
    const input = document.getElementById('danmaku-content');
    const content = input.value.trim();
    if (!content) return;
    if (!danmakuSendAs) {
        showToast('请先连接用户', 'warning');
        return;
    }
    const client = Store.wsClients['danmaku-' + danmakuSendAs];
    if (!client || !client.connected) {
        showToast('当前用户未连接', 'warning');
        return;
    }
    client.send('danmaku.send', { content, contentType: 'text' }, { roomId: DANMAKU_ROOM });
    spawnDanmaku(`${danmakuSendAs}: ${content}`);
    input.value = '';
}

function danmakuLike() {
    danmakuLikeCount++;
    const el = document.getElementById('danmaku-like-count');
    if (el) el.textContent = danmakuLikeCount;
}

async function danmakuMute() {
    const userId = document.getElementById('danmaku-mute-user').value;
    if (!userId) return;
    await api(`/api/danmaku/rooms/${DANMAKU_ROOM}/mute/${userId}`, { method: 'POST' });
    showToast(`已禁言 ${userId}`, 'warning');
    danmakuRefreshMuted();
}

async function danmakuUnmute() {
    const userId = document.getElementById('danmaku-mute-user').value;
    if (!userId) return;
    await api(`/api/danmaku/rooms/${DANMAKU_ROOM}/unmute/${userId}`, { method: 'POST' });
    showToast(`已解除禁言 ${userId}`, 'success');
    danmakuRefreshMuted();
}

async function danmakuRefreshMuted() {
    const data = await api(`/api/danmaku/rooms/${DANMAKU_ROOM}/muted`);
    const el = document.getElementById('danmaku-muted');
    if (!el || !data) return;
    const users = data.mutedUsers || [];
    el.innerHTML = users.length
        ? users.map(u => `<span class="badge badge-warning" style="margin-right:4px;margin-bottom:4px;display:inline-flex">${u}</span>`).join('')
        : '<span style="color:var(--text-secondary);font-size:13px">无禁言用户</span>';
}

async function danmakuRefreshCount() {
    const data = await api(`/api/presence/rooms/${DANMAKU_ROOM}/count`);
    const count = data?.count || 0;
    const viewerInfo = document.getElementById('danmaku-viewer-info');
    if (viewerInfo) viewerInfo.textContent = `${count} 人观看`;
    const roomCount = document.getElementById('danmaku-room-count');
    if (roomCount) roomCount.textContent = count;
}
