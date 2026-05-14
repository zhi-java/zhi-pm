// ============================================================
// 在线状态模块
// ============================================================
const PRESENCE_PORT = location.port || '8088';
const PRESENCE_WS = `${WS_BASE}/ws`;

function render_presence() {
    const panel = document.getElementById('panel-presence');
    panel.innerHTML = `
        <div class="card" style="margin-bottom:12px">
            <div class="card-title"><i data-lucide="activity" style="width:20px;height:20px"></i> 在线状态</div>
            <p style="color:var(--text-secondary);font-size:14px">
                每个用户独立窗口，点击「连接」建立 WebSocket，可单独发送消息或广播
            </p>
        </div>
        <div class="card-grid" style="grid-template-columns:repeat(auto-fit, minmax(320px, 1fr))">
            <div class="stat-card"><div class="stat-label">连接数</div><div class="stat-value" id="presence-stat-conn">-</div></div>
            <div class="stat-card"><div class="stat-label">在线用户</div><div class="stat-value" id="presence-stat-users">-</div></div>
        </div>
        <div id="presence-user-windows" style="display:grid;grid-template-columns:repeat(auto-fit, minmax(360px, 1fr));gap:16px;margin-top:16px"></div>
        <div class="card" style="margin-top:16px">
            <div class="card-title"><i data-lucide="radio" style="width:20px;height:20px"></i> 广播消息</div>
            <div class="form-row">
                <input class="input" id="presence-broadcast-msg" placeholder="广播消息给所有已连接用户..." value="系统通知：服务器将于今晚 22:00 维护">
                <button class="btn btn-ghost" onclick="presenceBroadcast()"><i data-lucide="radio" style="width:14px;height:14px"></i> 广播</button>
            </div>
        </div>
    `;
    lucide.createIcons();
    renderPresenceUserWindows();
    presenceRefreshStats();
}

function renderPresenceUserWindows() {
    const users = ['alice', 'bob', 'charlie'];
    const container = document.getElementById('presence-user-windows');
    if (!container) return;
    container.innerHTML = users.map(u => {
        const client = Store.wsClients['presence-' + u];
        const online = client && client.connected;
        return `
            <div class="card" style="margin-bottom:0;border:${online ? '2px solid var(--success)' : '1px solid var(--border)'}">
                <div style="display:flex;align-items:center;gap:12px;margin-bottom:12px">
                    <div class="user-avatar" style="background:${online ? 'var(--success)' : 'var(--text-secondary)'}">${u[0].toUpperCase()}</div>
                    <div style="flex:1">
                        <div style="font-weight:600;font-size:15px">${u.charAt(0).toUpperCase() + u.slice(1)}</div>
                        <div style="font-size:12px;color:var(--text-secondary)">${online ? '已连接 WebSocket' : '未连接'}</div>
                    </div>
                    <button class="btn ${online ? 'btn-danger' : 'btn-primary'} btn-sm" onclick="presenceToggleUser('${u}')">
                        ${online ? '断开' : '连接'}
                    </button>
                </div>
                <div class="form-row" style="margin-bottom:8px">
                    <input class="input" id="presence-msg-${u}" placeholder="发送给 ${u}..." value="你好 ${u}！" ${online ? '' : 'disabled'}>
                    <button class="btn btn-primary btn-sm" onclick="presencePushTo('${u}')" ${online ? '' : 'disabled'}><i data-lucide="send" style="width:12px;height:12px"></i></button>
                </div>
                <div style="max-height:120px;overflow-y:auto;background:var(--log-bg);border-radius:8px;padding:8px;font-size:12px;font-family:'SF Mono',monospace" id="presence-log-${u}">
                    <div style="color:var(--text-secondary)">等待消息...</div>
                </div>
            </div>
        `;
    }).join('');
    lucide.createIcons();
}

function presenceToggleUser(userId) {
    const key = 'presence-' + userId;
    if (Store.wsClients[key] && Store.wsClients[key].connected) {
        Store.wsClients[key].disconnect();
        delete Store.wsClients[key];
    } else {
        const client = new WsClient('presence', `${PRESENCE_WS}?access_token=${userId}-token`, userId);
        client.on('*', (msg) => {
            renderPresenceLog(userId, msg);
        });
        Store.wsClients[key] = client;
        client.connect();
    }
    setTimeout(() => { renderPresenceUserWindows(); presenceRefreshStats(); }, 500);
}

async function presencePushTo(userId) {
    const input = document.getElementById('presence-msg-' + userId);
    const message = input ? input.value : '';
    if (!message) return;
    await api(`/api/push/users/${userId}`, { method: 'POST', body: JSON.stringify({ message }) });
    showToast(`已推送给 ${userId}`, 'success');
    const log = document.getElementById('presence-log-' + userId);
    if (log) {
        const entry = document.createElement('div');
        entry.className = 'log-entry';
        entry.innerHTML = `<span class="log-time">${new Date().toLocaleTimeString()}</span><span class="log-type" style="color:var(--success)">SENT</span><span class="log-data">${message.substring(0, 60)}</span>`;
        log.prepend(entry);
    }
}

async function presenceBroadcast() {
    const message = document.getElementById('presence-broadcast-msg').value;
    if (!message) return;
    const result = await api(`/api/push/broadcast`, { method: 'POST', body: JSON.stringify({ message }) });
    showToast(`广播已发送至 ${result?.sent || 0} 个连接`, 'success');
    ['alice', 'bob', 'charlie'].forEach(u => {
        const log = document.getElementById('presence-log-' + u);
        if (log && Store.wsClients['presence-' + u]?.connected) {
            const entry = document.createElement('div');
            entry.className = 'log-entry';
            entry.innerHTML = `<span class="log-time">${new Date().toLocaleTimeString()}</span><span class="log-type" style="color:var(--warning)">BROADCAST</span><span class="log-data">${message.substring(0, 60)}</span>`;
            log.prepend(entry);
        }
    });
}

async function presenceRefreshStats() {
    const count = await api('/api/presence/connections/count');
    if (count) document.getElementById('presence-stat-conn').textContent = count.count || 0;
    const users = ['alice', 'bob', 'charlie'];
    let onlineCount = 0;
    for (const u of users) {
        const status = await api(`/api/presence/users/${u}/online`);
        if (status && status.online) onlineCount++;
    }
    document.getElementById('presence-stat-users').textContent = onlineCount;
}

function renderPresenceLog(userId, msg) {
    const log = document.getElementById('presence-log-' + userId);
    if (!log) return;
    if (log.children.length === 1 && log.children[0].textContent === '等待消息...') log.innerHTML = '';
    const entry = document.createElement('div');
    entry.className = 'log-entry';
    entry.innerHTML = `<span class="log-time">${new Date().toLocaleTimeString()}</span><span class="log-type">${msg.type}</span><span class="log-data">${JSON.stringify(msg.payload || {}).substring(0, 80)}</span>`;
    log.prepend(entry);
}

function clearLog(module) {
    const log = document.getElementById(module + '-log');
    if (log) log.innerHTML = '';
}
