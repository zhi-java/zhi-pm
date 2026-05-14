// ============================================================
// 消息推送模块
// ============================================================
const PUSH_USERS = ['alice', 'bob', 'charlie'];
const PUSH_ROOMS = [
    { id: 'live-room-1', name: '直播间 live-room-1' },
    { id: 'group-team', name: '团队群聊' }
];
let pushTargetType = 'user';
let pushSelectedTarget = '';

function render_push() {
    const existing = Store.wsClients['push'];
    const connected = existing && existing.connected;
    const selectedUser = connected ? existing.userId : '';
    const panel = document.getElementById('panel-push');
    panel.innerHTML = `
        <div class="card">
            <div class="card-title"><i data-lucide="send" style="width:20px;height:20px"></i> 消息推送</div>
            <p style="color:var(--text-secondary);margin-bottom:16px;font-size:14px">
                连接用户后选择推送目标发送消息，消息通过 WebSocket 回显
            </p>
            <div class="form-row">
                <select class="select" id="push-user" style="max-width:150px" onchange="pushConnect()">
                    <option value="">请选择监听用户...</option>
                    ${PUSH_USERS.map(u => `<option value="${u}" ${selectedUser === u ? 'selected' : ''}>${u.charAt(0).toUpperCase() + u.slice(1)}</option>`).join('')}
                </select>
                <span id="push-status" class="badge ${connected ? 'badge-success' : 'badge-error'}">${connected ? '已连接: ' + selectedUser : '未连接'}</span>
            </div>
        </div>
        <div class="card">
            <div class="card-title">发送推送</div>
            <div style="display:flex;gap:0;background:var(--input-bg);border-radius:10px;padding:3px;margin-bottom:16px">
                <button class="btn btn-sm push-tab ${pushTargetType === 'user' ? 'btn-primary' : ''}" style="flex:1;border-radius:8px" onclick="pushSwitchType('user')">
                    <i data-lucide="user" style="width:14px;height:14px"></i> 用户
                </button>
                <button class="btn btn-sm push-tab ${pushTargetType === 'room' ? 'btn-primary' : ''}" style="flex:1;border-radius:8px" onclick="pushSwitchType('room')">
                    <i data-lucide="home" style="width:14px;height:14px"></i> 房间
                </button>
                <button class="btn btn-sm push-tab ${pushTargetType === 'broadcast' ? 'btn-primary' : ''}" style="flex:1;border-radius:8px" onclick="pushSwitchType('broadcast')">
                    <i data-lucide="radio" style="width:14px;height:14px"></i> 广播
                </button>
            </div>
            <div id="push-target-area"></div>
            <div class="form-group" style="margin-top:12px">
                <label class="form-label">消息内容</label>
                <input class="input" id="push-message" placeholder="输入要发送的消息..." value="Hello from push module!">
            </div>
            <button class="btn btn-primary" onclick="pushSend()" style="margin-top:4px"><i data-lucide="send" style="width:14px;height:14px"></i> 发送</button>
        </div>
        <div class="log-panel">
            <div class="log-header">收到的消息 <button class="btn btn-sm btn-ghost" onclick="clearLog('push')">清空</button></div>
            <div class="log-body" id="push-log"></div>
        </div>
    `;
    lucide.createIcons();
    pushRenderTarget();
}

function pushSwitchType(type) {
    pushTargetType = type;
    pushSelectedTarget = '';
    document.querySelectorAll('.push-tab').forEach(btn => {
        btn.className = btn.className.replace(/btn-primary/g, '').trim();
    });
    const tabs = document.querySelectorAll('.push-tab');
    const idx = { user: 0, room: 1, broadcast: 2 }[type];
    if (tabs[idx]) tabs[idx].classList.add('btn-primary');
    pushRenderTarget();
}

function pushRenderTarget() {
    const area = document.getElementById('push-target-area');
    if (!area) return;
    if (pushTargetType === 'user') {
        area.innerHTML = `
            <label class="form-label">选择用户</label>
            <div style="display:flex;gap:8px;flex-wrap:wrap">
                ${PUSH_USERS.map(u => `
                    <div class="user-card ${pushSelectedTarget === u ? 'online' : ''}" style="min-width:120px;padding:10px 14px;cursor:pointer" onclick="pushSelectTarget('${u}')">
                        <div class="user-avatar" style="width:32px;height:32px;font-size:13px">${u[0].toUpperCase()}</div>
                        <div style="font-size:13px;font-weight:500">${u.charAt(0).toUpperCase() + u.slice(1)}</div>
                    </div>
                `).join('')}
            </div>
            ${pushSelectedTarget ? `<div style="margin-top:8px;font-size:13px;color:var(--text-secondary)">已选择: <strong>${pushSelectedTarget}</strong></div>` : ''}
        `;
    } else if (pushTargetType === 'room') {
        area.innerHTML = `
            <label class="form-label">选择房间</label>
            <div style="display:flex;gap:8px;flex-wrap:wrap">
                ${PUSH_ROOMS.map(r => `
                    <div class="user-card ${pushSelectedTarget === r.id ? 'online' : ''}" style="min-width:160px;padding:10px 14px;cursor:pointer" onclick="pushSelectTarget('${r.id}')">
                        <div class="user-avatar" style="width:32px;height:32px;font-size:13px;background:var(--warning)"><i data-lucide="home" style="width:16px;height:16px"></i></div>
                        <div>
                            <div style="font-size:13px;font-weight:500">${r.name}</div>
                            <div style="font-size:11px;color:var(--text-secondary)">${r.id}</div>
                        </div>
                    </div>
                `).join('')}
            </div>
            ${pushSelectedTarget ? `<div style="margin-top:8px;font-size:13px;color:var(--text-secondary)">已选择: <strong>${pushSelectedTarget}</strong></div>` : ''}
        `;
        lucide.createIcons();
    } else {
        area.innerHTML = `
            <div style="background:var(--input-bg);border-radius:10px;padding:16px;text-align:center">
                <i data-lucide="radio" style="width:24px;height:24px;color:var(--accent);margin-bottom:4px"></i>
                <div style="font-size:14px;font-weight:500">广播模式</div>
                <div style="font-size:12px;color:var(--text-secondary)">消息将发送给所有已连接的用户</div>
            </div>
        `;
        lucide.createIcons();
    }
}

function pushSelectTarget(target) {
    pushSelectedTarget = target;
    pushRenderTarget();
}

function pushConnect() {
    const userId = document.getElementById('push-user').value;
    if (Store.wsClients['push']) Store.wsClients['push'].disconnect();
    if (!userId) return;
    const client = new WsClient('push', `${WS_BASE}/ws?access_token=${userId}-token`, userId);
    client.on('*', (msg) => {
        const log = document.getElementById('push-log');
        if (log) {
            const entry = document.createElement('div');
            entry.className = 'log-entry';
            entry.innerHTML = `<span class="log-time">${new Date().toLocaleTimeString()}</span><span class="log-type">${msg.type}</span><span class="log-data">${JSON.stringify(msg.payload || {})}</span>`;
            log.prepend(entry);
        }
    });
    Store.wsClients['push'] = client;
    client.connect();
    setTimeout(() => {
        const status = document.getElementById('push-status');
        if (status) {
            status.textContent = client.connected ? `已连接: ${userId}` : '连接中...';
            status.className = client.connected ? 'badge badge-success' : 'badge badge-warning';
        }
    }, 500);
}

async function pushSend() {
    const message = document.getElementById('push-message').value;
    if (!message) { showToast('请输入消息内容', 'warning'); return; }
    const body = JSON.stringify({ message });
    let url = '';
    if (pushTargetType === 'user') {
        if (!pushSelectedTarget) { showToast('请选择目标用户', 'warning'); return; }
        url = `/api/push/users/${pushSelectedTarget}`;
    } else if (pushTargetType === 'broadcast') {
        url = '/api/push/broadcast';
    } else {
        if (!pushSelectedTarget) { showToast('请选择目标房间', 'warning'); return; }
        url = `/api/push/rooms/${pushSelectedTarget}`;
    }
    const result = await api(url, { method: 'POST', body });
    const label = pushTargetType === 'broadcast' ? '所有连接' : pushSelectedTarget;
    showToast(`已发送至 ${label}，${result?.sent || 0} 个连接`, 'success');
}
