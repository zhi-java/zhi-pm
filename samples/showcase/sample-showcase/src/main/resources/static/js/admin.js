// ============================================================
// 管理后台模块
// ============================================================
let adminAutoRefresh = null;
let adminPrevStats = {};
let adminEvents = [];
const ADMIN_MAX_EVENTS = 40;

function render_admin() {
    if (adminAutoRefresh) clearInterval(adminAutoRefresh);
    const panel = document.getElementById('panel-admin');
    panel.innerHTML = `
        <!-- 顶部状态栏 -->
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:20px">
            <div style="display:flex;align-items:center;gap:12px">
                <div style="width:40px;height:40px;border-radius:12px;background:rgba(0,122,255,0.12);display:flex;align-items:center;justify-content:center">
                    <i data-lucide="shield" style="width:20px;height:20px;color:var(--accent)"></i>
                </div>
                <div>
                    <div style="font-weight:700;font-size:18px">管理后台</div>
                    <div class="ops-pulse" style="color:var(--success)"><span class="ops-pulse-dot"></span> 实时监控中</div>
                </div>
            </div>
            <div style="display:flex;gap:8px">
                <button class="btn btn-sm btn-ghost" onclick="adminRefreshAll()"><i data-lucide="refresh-cw" style="width:13px;height:13px"></i> 全部刷新</button>
            </div>
        </div>

        <!-- 健康状态 -->
        <div id="admin-health" class="ops-health" style="margin-bottom:20px"></div>

        <!-- 统计卡片 -->
        <div id="admin-stats" style="display:grid;grid-template-columns:repeat(4, 1fr);gap:14px;margin-bottom:20px"></div>

        <!-- 主内容: 连接 + 房间 -->
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:20px">
            <div class="card" style="margin-bottom:0">
                <div class="ops-section">
                    <div class="ops-section-icon" style="background:rgba(0,122,255,0.12)">
                        <i data-lucide="link" style="width:16px;height:16px;color:var(--accent)"></i>
                    </div>
                    <div>
                        <div class="ops-section-title">活跃连接</div>
                        <div class="ops-section-subtitle" id="admin-conn-count">-</div>
                    </div>
                    <div class="ops-section-actions">
                        <button class="btn btn-sm btn-ghost" onclick="adminLoadConnections()"><i data-lucide="refresh-cw" style="width:12px;height:12px"></i></button>
                    </div>
                </div>
                <div id="admin-conn-list" style="display:flex;flex-direction:column;gap:8px;max-height:340px;overflow-y:auto"></div>
            </div>
            <div class="card" style="margin-bottom:0">
                <div class="ops-section">
                    <div class="ops-section-icon" style="background:rgba(255,149,0,0.12)">
                        <i data-lucide="home" style="width:16px;height:16px;color:var(--warning)"></i>
                    </div>
                    <div>
                        <div class="ops-section-title">活跃房间</div>
                        <div class="ops-section-subtitle" id="admin-room-count">-</div>
                    </div>
                    <div class="ops-section-actions">
                        <button class="btn btn-sm btn-ghost" onclick="adminLoadRooms()"><i data-lucide="refresh-cw" style="width:12px;height:12px"></i></button>
                    </div>
                </div>
                <div id="admin-room-list" style="display:flex;flex-direction:column;gap:8px;max-height:340px;overflow-y:auto"></div>
            </div>
        </div>

        <!-- 底部: 广播 + 事件流 -->
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
            <div class="card" style="margin-bottom:0">
                <div class="ops-section">
                    <div class="ops-section-icon" style="background:rgba(52,199,89,0.12)">
                        <i data-lucide="megaphone" style="width:16px;height:16px;color:var(--success)"></i>
                    </div>
                    <div class="ops-section-title">广播消息</div>
                </div>
                <div class="ops-broadcast-area">
                    <textarea id="admin-broadcast-msg" placeholder="输入广播消息，将发送至所有已连接的客户端..."></textarea>
                </div>
                <div style="margin-top:10px;display:flex;gap:8px">
                    <button class="btn btn-primary" onclick="adminBroadcast()" style="flex:1"><i data-lucide="send" style="width:14px;height:14px"></i> 发送广播</button>
                </div>
            </div>
            <div class="card" style="margin-bottom:0">
                <div class="ops-section">
                    <div class="ops-section-icon" style="background:rgba(199,128,255,0.12)">
                        <i data-lucide="activity" style="width:16px;height:16px;color:#C780FF"></i>
                    </div>
                    <div>
                        <div class="ops-section-title">实时事件</div>
                        <div class="ops-section-subtitle" id="admin-event-count">0 条事件</div>
                    </div>
                    <div class="ops-section-actions">
                        <button class="btn btn-sm btn-ghost" onclick="adminClearEvents()">清空</button>
                    </div>
                </div>
                <div id="admin-event-stream" class="ops-event-stream"></div>
            </div>
        </div>
    `;
    lucide.createIcons();
    adminPrevStats = {};
    adminEvents = [];
    adminRefreshAll();
    adminAutoRefresh = setInterval(adminRefreshAll, 4000);
}

function adminAddEvent(type, text) {
    adminEvents.unshift({ time: new Date(), type, text });
    if (adminEvents.length > ADMIN_MAX_EVENTS) adminEvents.pop();
    const stream = document.getElementById('admin-event-stream');
    const countEl = document.getElementById('admin-event-count');
    if (stream) {
        const el = document.createElement('div');
        el.className = 'ops-event';
        el.innerHTML = `
            <span class="ops-event-time">${new Date().toLocaleTimeString()}</span>
            <span class="ops-event-type ${type}">${type}</span>
            <span class="ops-event-data">${text}</span>
        `;
        stream.prepend(el);
        if (stream.children.length > ADMIN_MAX_EVENTS) stream.lastChild.remove();
    }
    if (countEl) countEl.textContent = `${adminEvents.length} 条事件`;
}

function adminClearEvents() {
    adminEvents = [];
    const stream = document.getElementById('admin-event-stream');
    if (stream) stream.innerHTML = '<div style="text-align:center;color:var(--text-secondary);padding:20px;font-size:12px">暂无事件</div>';
    const countEl = document.getElementById('admin-event-count');
    if (countEl) countEl.textContent = '0 条事件';
}

function adminAnimateNumber(el, newVal) {
    if (!el) return;
    const oldVal = parseInt(el.textContent) || 0;
    if (oldVal !== newVal) {
        el.textContent = newVal;
        el.classList.remove('changed');
        void el.offsetWidth;
        el.classList.add('changed');
    }
}

async function adminRefreshAll() {
    await adminLoadStats();
    await adminLoadConnections();
    await adminLoadRooms();
}

async function adminLoadStats() {
    const stats = await api('/admin/api/stats');
    if (!stats) return;
    const container = document.getElementById('admin-stats');
    const healthEl = document.getElementById('admin-health');
    if (!container) return;
    const items = [
        { icon: 'link', label: '活跃连接', value: stats.activeConnections || 0, color: 'var(--accent)', prevKey: 'activeConnections' },
        { icon: 'users', label: '在线用户', value: stats.onlineUsers || 0, color: 'var(--success)', prevKey: 'onlineUsers' },
        { icon: 'home', label: '活跃房间', value: stats.activeRooms || 0, color: 'var(--warning)', prevKey: 'activeRooms' },
        { icon: 'check-circle', label: '推送成功率', value: ((stats.pushSuccessRate || 1) * 100), color: '#C780FF', prevKey: 'pushSuccessRate', isPercent: true }
    ];
    container.innerHTML = items.map(item => {
        const prev = adminPrevStats[item.prevKey];
        const curr = item.isPercent ? parseFloat(item.value.toFixed(1)) : item.value;
        let trend = 'flat';
        let delta = '';
        if (prev !== undefined) {
            const diff = item.isPercent ? (curr - prev).toFixed(1) : (curr - prev);
            if (diff > 0) { trend = 'up'; delta = `+${diff}`; }
            else if (diff < 0) { trend = 'down'; delta = `${diff}`; }
        }
        return `
            <div class="ops-stat">
                <div class="ops-stat-icon" style="background:${item.color}18">
                    <i data-lucide="${item.icon}" style="width:18px;height:18px;color:${item.color}"></i>
                </div>
                <div class="ops-stat-label">${item.label}</div>
                <div class="ops-stat-value ops-number-animate" style="color:${item.color}">${item.isPercent ? curr.toFixed(1) + '%' : curr}</div>
                ${delta ? `<span class="ops-stat-trend ${trend}">${trend === 'up' ? '&#9650;' : '&#9650;'} ${delta}</span>` : '<span class="ops-stat-trend flat">- 持平</span>'}
            </div>
        `;
    }).join('');
    lucide.createIcons();
    // 检测变化并记录事件
    if (adminPrevStats.activeConnections !== undefined) {
        const connDelta = (stats.activeConnections || 0) - adminPrevStats.activeConnections;
        if (connDelta > 0) adminAddEvent('connect', `+${connDelta} 个新连接`);
        else if (connDelta < 0) adminAddEvent('disconnect', `${connDelta} 个连接断开`);
    }
    adminPrevStats = {
        activeConnections: stats.activeConnections || 0,
        onlineUsers: stats.onlineUsers || 0,
        activeRooms: stats.activeRooms || 0,
        pushSuccessRate: (stats.pushSuccessRate || 1) * 100
    };
    // 健康状态
    if (healthEl) {
        const connCount = stats.activeConnections || 0;
        const successRate = (stats.pushSuccessRate || 1) * 100;
        healthEl.innerHTML = `
            <div class="ops-health-item">
                <span class="ops-health-dot ${connCount > 0 ? 'healthy' : 'warning'}"></span>
                <div>
                    <div class="ops-health-label">WebSocket</div>
                    <div class="ops-health-value">${connCount > 0 ? '正常运行' : '无连接'}</div>
                </div>
            </div>
            <div class="ops-health-item">
                <span class="ops-health-dot ${successRate >= 95 ? 'healthy' : successRate >= 80 ? 'warning' : 'error'}"></span>
                <div>
                    <div class="ops-health-label">推送服务</div>
                    <div class="ops-health-value">${successRate.toFixed(1)}% 成功率</div>
                </div>
            </div>
            <div class="ops-health-item">
                <span class="ops-health-dot healthy"></span>
                <div>
                    <div class="ops-health-label">服务状态</div>
                    <div class="ops-health-value">运行中</div>
                </div>
            </div>
        `;
    }
}

async function adminLoadConnections() {
    const conns = await api('/admin/api/connections');
    const container = document.getElementById('admin-conn-list');
    const countEl = document.getElementById('admin-conn-count');
    if (!container || !conns) return;
    if (countEl) countEl.textContent = `${conns.length} 个活跃连接`;
    if (conns.length === 0) {
        container.innerHTML = '<div style="text-align:center;color:var(--text-secondary);padding:32px;font-size:13px"><i data-lucide="wifi-off" style="width:24px;height:24px;display:block;margin:0 auto 8px;opacity:0.4"></i>暂无活跃连接</div>';
        lucide.createIcons();
        return;
    }
    const userColors = { alice: '#FF6B6B', bob: '#4ECDC4', charlie: '#C780FF' };
    container.innerHTML = conns.map(c => {
        const color = userColors[c.userId] || '#86868b';
        const connectedTime = c.connectedAt ? new Date(c.connectedAt).toLocaleTimeString() : '-';
        const heartbeatTime = c.lastHeartbeatAt ? new Date(c.lastHeartbeatAt).toLocaleTimeString() : '-';
        const isRecent = c.lastHeartbeatAt && (Date.now() - new Date(c.lastHeartbeatAt).getTime() < 10000);
        return `
            <div class="ops-entity-card">
                <div class="user-avatar" style="width:34px;height:34px;font-size:13px;background:${color};flex-shrink:0">${(c.userId || '?')[0].toUpperCase()}</div>
                <div style="flex:1;min-width:0">
                    <div style="display:flex;align-items:center;gap:6px;margin-bottom:2px">
                        <span style="font-weight:600;font-size:13px">${c.userId || 'unknown'}</span>
                        <span style="width:6px;height:6px;border-radius:50%;background:${isRecent ? 'var(--success)' : 'var(--warning)'};flex-shrink:0"></span>
                        <span style="font-family:monospace;font-size:10px;color:var(--text-secondary)">${c.sessionId?.substring(0, 8)}...</span>
                    </div>
                    <div style="font-size:11px;color:var(--text-secondary)">
                        <i data-lucide="clock" style="width:10px;height:10px;vertical-align:middle"></i> ${connectedTime}
                        &nbsp;|&nbsp;
                        <i data-lucide="heart-pulse" style="width:10px;height:10px;vertical-align:middle"></i> ${heartbeatTime}
                    </div>
                </div>
                <button class="btn btn-sm btn-danger" onclick="adminKick('${c.sessionId}')" style="flex-shrink:0;opacity:0.8" onmouseenter="this.style.opacity=1" onmouseleave="this.style.opacity=0.8">
                    <i data-lucide="user-x" style="width:12px;height:12px"></i>
                </button>
            </div>
        `;
    }).join('');
    lucide.createIcons();
}

async function adminLoadRooms() {
    const rooms = await api('/admin/api/rooms');
    const container = document.getElementById('admin-room-list');
    const countEl = document.getElementById('admin-room-count');
    if (!container || !rooms) return;
    if (countEl) countEl.textContent = `${rooms.length} 个活跃房间`;
    if (rooms.length === 0) {
        container.innerHTML = '<div style="text-align:center;color:var(--text-secondary);padding:32px;font-size:13px"><i data-lucide="home" style="width:24px;height:24px;display:block;margin:0 auto 8px;opacity:0.4"></i>暂无活跃房间</div>';
        lucide.createIcons();
        return;
    }
    const roomColors = ['#007AFF', '#FF9500', '#34C759', '#C780FF', '#FF6B6B'];
    container.innerHTML = rooms.map((r, i) => {
        const color = roomColors[i % roomColors.length];
        return `
            <div class="ops-entity-card">
                <div style="width:34px;height:34px;border-radius:10px;background:${color}15;display:flex;align-items:center;justify-content:center;flex-shrink:0">
                    <i data-lucide="home" style="width:16px;height:16px;color:${color}"></i>
                </div>
                <div style="flex:1;min-width:0">
                    <div style="font-weight:600;font-size:13px">${r.roomId}</div>
                    <div style="font-size:11px;color:var(--text-secondary)">${r.memberCount} 个成员在线</div>
                </div>
                <button class="btn btn-sm btn-ghost" onclick="adminViewMembers('${r.roomId}')" style="flex-shrink:0">
                    <i data-lucide="eye" style="width:12px;height:12px"></i>
                </button>
            </div>
        `;
    }).join('');
    lucide.createIcons();
}

async function adminKick(sessionId) {
    if (!confirm(`确认踢出连接 ${sessionId?.substring(0, 12)}...?`)) return;
    await api(`/admin/api/connections/${sessionId}`, { method: 'DELETE' });
    showToast('连接已踢出', 'warning');
    adminAddEvent('kick', `踢出连接 ${sessionId?.substring(0, 12)}...`);
    adminLoadConnections();
    adminLoadStats();
}

async function adminViewMembers(roomId) {
    const data = await api(`/admin/api/rooms/${roomId}/members`);
    if (data) {
        const members = data.map(m => m.userId).join(', ');
        showToast(`房间 ${roomId}: ${members}`, 'info');
        adminAddEvent('room', `查看房间 ${roomId} 成员: ${members}`);
    }
}

async function adminBroadcast() {
    const msg = document.getElementById('admin-broadcast-msg')?.value;
    if (!msg) return;
    const result = await api('/admin/api/broadcast', { method: 'POST', body: JSON.stringify({ type: 'admin.broadcast', payload: { message: msg } }) });
    const sent = result?.sent || 0;
    showToast(`广播已发送至 ${sent} 个连接`, 'success');
    adminAddEvent('broadcast', `"${msg.substring(0, 40)}..." -> ${sent} 个连接`);
    document.getElementById('admin-broadcast-msg').value = '';
}
