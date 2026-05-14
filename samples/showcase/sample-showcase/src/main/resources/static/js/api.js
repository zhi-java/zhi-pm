// ============================================================
// API 接入文档模块
// ============================================================

const API_CATEGORIES = [
    {
        id: 'push', name: '消息推送', icon: 'send', color: '#34C759',
        apis: [
            {
                method: 'POST', path: '/api/push/users/{userId}',
                summary: '向指定用户推送消息',
                params: [{ name: 'userId', in: 'path', type: 'string', required: true, desc: '目标用户 ID' }],
                body: { type: 'custom.event', payload: { message: 'Hello!' } },
                example: `curl -X POST http://localhost:8088/api/push/users/alice \\
  -H "Content-Type: application/json" \\
  -d '{"type":"custom.event","payload":{"message":"Hello!"}}'`
            },
            {
                method: 'POST', path: '/api/push/rooms/{roomId}',
                summary: '向指定房间推送消息',
                params: [{ name: 'roomId', in: 'path', type: 'string', required: true, desc: '目标房间 ID' }],
                body: { type: 'room.event', payload: { message: 'Room broadcast' } },
                example: `curl -X POST http://localhost:8088/api/push/rooms/general \\
  -H "Content-Type: application/json" \\
  -d '{"type":"room.event","payload":{"message":"Room broadcast"}}'`
            },
            {
                method: 'POST', path: '/api/push/broadcast',
                summary: '向所有连接广播消息',
                params: [],
                body: { type: 'broadcast', payload: { message: 'Hello everyone!' } },
                example: `curl -X POST http://localhost:8088/api/push/broadcast \\
  -H "Content-Type: application/json" \\
  -d '{"type":"broadcast","payload":{"message":"Hello everyone!"}}'`
            }
        ]
    },
    {
        id: 'presence', name: '在线状态', icon: 'activity', color: '#007AFF',
        apis: [
            {
                method: 'GET', path: '/api/presence/connections/count',
                summary: '获取当前连接数',
                params: [],
                example: `curl http://localhost:8088/api/presence/connections/count`
            },
            {
                method: 'GET', path: '/api/presence/users/{userId}/online',
                summary: '检查用户是否在线',
                params: [{ name: 'userId', in: 'path', type: 'string', required: true, desc: '用户 ID' }],
                example: `curl http://localhost:8088/api/presence/users/alice/online`
            },
            {
                method: 'GET', path: '/api/presence/rooms/{roomId}/count',
                summary: '获取房间在线人数',
                params: [{ name: 'roomId', in: 'path', type: 'string', required: true, desc: '房间 ID' }],
                example: `curl http://localhost:8088/api/presence/rooms/general/count`
            }
        ]
    },
    {
        id: 'danmaku', name: '弹幕', icon: 'radio', color: '#FF3B30',
        apis: [
            {
                method: 'POST', path: '/api/danmaku/rooms/{roomId}',
                summary: '发送弹幕消息',
                params: [{ name: 'roomId', in: 'path', type: 'string', required: true, desc: '房间 ID' }],
                body: { userId: 'alice', content: 'Hello danmaku!' },
                example: `curl -X POST http://localhost:8088/api/danmaku/rooms/live-001 \\
  -H "Content-Type: application/json" \\
  -d '{"userId":"alice","content":"Hello danmaku!"}'`
            },
            {
                method: 'POST', path: '/api/danmaku/rooms/{roomId}/mute/{userId}',
                summary: '禁言用户',
                params: [
                    { name: 'roomId', in: 'path', type: 'string', required: true, desc: '房间 ID' },
                    { name: 'userId', in: 'path', type: 'string', required: true, desc: '用户 ID' }
                ],
                example: `curl -X POST http://localhost:8088/api/danmaku/rooms/live-001/mute/bob`
            },
            {
                method: 'POST', path: '/api/danmaku/rooms/{roomId}/unmute/{userId}',
                summary: '解除禁言',
                params: [
                    { name: 'roomId', in: 'path', type: 'string', required: true, desc: '房间 ID' },
                    { name: 'userId', in: 'path', type: 'string', required: true, desc: '用户 ID' }
                ],
                example: `curl -X POST http://localhost:8088/api/danmaku/rooms/live-001/unmute/bob`
            },
            {
                method: 'GET', path: '/api/danmaku/rooms/{roomId}/muted',
                summary: '获取已禁言用户列表',
                params: [{ name: 'roomId', in: 'path', type: 'string', required: true, desc: '房间 ID' }],
                example: `curl http://localhost:8088/api/danmaku/rooms/live-001/muted`
            }
        ]
    },
    {
        id: 'chat', name: '聊天', icon: 'message-square', color: '#C780FF',
        apis: [
            {
                method: 'GET', path: '/api/chat/conversations/{conversationId}/history',
                summary: '获取聊天历史记录',
                params: [
                    { name: 'conversationId', in: 'path', type: 'string', required: true, desc: '会话 ID' },
                    { name: 'limit', in: 'query', type: 'int', required: false, desc: '返回条数，默认 50' }
                ],
                example: `curl "http://localhost:8088/api/chat/conversations/conv-alice-bob/history?limit=20"`
            },
            {
                method: 'GET', path: '/api/chat/conversations/{conversationId}/unread/{userId}',
                summary: '获取未读消息数',
                params: [
                    { name: 'conversationId', in: 'path', type: 'string', required: true, desc: '会话 ID' },
                    { name: 'userId', in: 'path', type: 'string', required: true, desc: '用户 ID' }
                ],
                example: `curl http://localhost:8088/api/chat/conversations/conv-alice-bob/unread/alice`
            }
        ]
    },
    {
        id: 'orders', name: '订单', icon: 'shopping-cart', color: '#FF9500',
        apis: [
            {
                method: 'POST', path: '/api/orders',
                summary: '创建订单',
                params: [],
                body: { userId: 'alice', product: 'MacBook Pro', amount: 14999.00 },
                example: `curl -X POST http://localhost:8088/api/orders \\
  -H "Content-Type: application/json" \\
  -d '{"userId":"alice","product":"MacBook Pro","amount":14999.00}'`
            },
            {
                method: 'POST', path: '/api/orders/{orderId}/status',
                summary: '更新订单状态',
                params: [{ name: 'orderId', in: 'path', type: 'string', required: true, desc: '订单 ID' }],
                body: { status: 'PAID' },
                example: `curl -X POST http://localhost:8088/api/orders/A1B2C3D4/status \\
  -H "Content-Type: application/json" \\
  -d '{"status":"PAID"}'`
            },
            {
                method: 'GET', path: '/api/orders/{orderId}',
                summary: '获取订单详情',
                params: [{ name: 'orderId', in: 'path', type: 'string', required: true, desc: '订单 ID' }],
                example: `curl http://localhost:8088/api/orders/A1B2C3D4`
            },
            {
                method: 'GET', path: '/api/orders/user/{userId}',
                summary: '获取用户订单列表',
                params: [{ name: 'userId', in: 'path', type: 'string', required: true, desc: '用户 ID' }],
                example: `curl http://localhost:8088/api/orders/user/alice`
            }
        ]
    },
    {
        id: 'admin', name: '管理', icon: 'shield', color: '#86868b',
        apis: [
            {
                method: 'GET', path: '/admin/api/stats',
                summary: '获取网关统计数据',
                params: [],
                example: `curl http://localhost:8088/admin/api/stats`
            },
            {
                method: 'GET', path: '/admin/api/connections',
                summary: '列出所有活跃连接',
                params: [],
                example: `curl http://localhost:8088/admin/api/connections`
            },
            {
                method: 'DELETE', path: '/admin/api/connections/{sessionId}',
                summary: '踢出指定连接',
                params: [{ name: 'sessionId', in: 'path', type: 'string', required: true, desc: '会话 ID' }],
                example: `curl -X DELETE http://localhost:8088/admin/api/connections/abc123`
            },
            {
                method: 'GET', path: '/admin/api/rooms',
                summary: '列出所有活跃房间',
                params: [],
                example: `curl http://localhost:8088/admin/api/rooms`
            },
            {
                method: 'GET', path: '/admin/api/rooms/{roomId}/members',
                summary: '获取房间成员列表',
                params: [{ name: 'roomId', in: 'path', type: 'string', required: true, desc: '房间 ID' }],
                example: `curl http://localhost:8088/admin/api/rooms/general/members`
            },
            {
                method: 'POST', path: '/admin/api/broadcast',
                summary: '广播消息到所有客户端',
                params: [],
                body: { type: 'admin.broadcast', payload: { message: 'System notice' } },
                example: `curl -X POST http://localhost:8088/admin/api/broadcast \\
  -H "Content-Type: application/json" \\
  -d '{"type":"admin.broadcast","payload":{"message":"System notice"}}'`
            }
        ]
    }
];

let apiActiveTab = 'push';
let apiExpandedCards = {};

function render_api() {
    const panel = document.getElementById('panel-api');
    panel.innerHTML = `
        <div style="display:flex;align-items:center;gap:12px;margin-bottom:20px">
            <div style="width:40px;height:40px;border-radius:10px;background:rgba(0,122,255,0.1);display:flex;align-items:center;justify-content:center">
                <i data-lucide="code-2" style="width:20px;height:20px;color:var(--accent)"></i>
            </div>
            <div>
                <div style="font-weight:700;font-size:18px">API 接入文档</div>
                <div style="font-size:12px;color:var(--text-secondary)">WebSocket 网关 REST 接口一览，点击 Try it 在线调试</div>
            </div>
        </div>
        <div id="api-tabs" style="display:flex;gap:6px;margin-bottom:16px;flex-wrap:wrap"></div>
        <div id="api-content"></div>
    `;
    lucide.createIcons();
    apiExpandedCards = {};
    renderApiTabs();
    renderApiContent();
}

function renderApiTabs() {
    const container = document.getElementById('api-tabs');
    container.innerHTML = API_CATEGORIES.map(cat => `
        <button class="btn btn-sm ${apiActiveTab === cat.id ? 'btn-primary' : 'btn-ghost'}"
                onclick="apiSwitchTab('${cat.id}')" style="gap:6px">
            <i data-lucide="${cat.icon}" style="width:14px;height:14px"></i>
            ${cat.name}
            <span style="background:${apiActiveTab === cat.id ? 'rgba(255,255,255,0.2)' : 'var(--border)'};padding:1px 6px;border-radius:4px;font-size:10px">${getCatApiCount(cat)}</span>
        </button>
    `).join('');
    lucide.createIcons();
}

function getCatApiCount(cat) {
    return cat.apis.length;
}

function apiSwitchTab(id) {
    apiActiveTab = id;
    apiExpandedCards = {};
    renderApiTabs();
    renderApiContent();
}

function renderApiContent() {
    const cat = API_CATEGORIES.find(c => c.id === apiActiveTab);
    if (!cat) return;
    const container = document.getElementById('api-content');
    container.innerHTML = `
        <div style="background:var(--card-bg);border-radius:12px;border:0.5px solid var(--border);box-shadow:var(--card-shadow);overflow:hidden">
            <div style="padding:16px 20px;border-bottom:0.5px solid var(--border);display:flex;align-items:center;gap:10px">
                <div style="width:28px;height:28px;border-radius:7px;background:${cat.color}15;display:flex;align-items:center;justify-content:center">
                    <i data-lucide="${cat.icon}" style="width:16px;height:16px;color:${cat.color}"></i>
                </div>
                <div style="font-weight:600;font-size:15px">${cat.name}</div>
                <span style="font-size:12px;color:var(--text-secondary)">${cat.apis.length} 个接口</span>
            </div>
            <div style="display:flex;flex-direction:column">
                ${cat.apis.map((api, i) => renderApiCard(api, i, cat)).join('')}
            </div>
        </div>
    `;
    lucide.createIcons();
}

function renderApiCard(api, index, cat) {
    const key = cat.id + '-' + index;
    const expanded = apiExpandedCards[key];
    const methodColors = { GET: '#34C759', POST: '#007AFF', PUT: '#FF9500', DELETE: '#FF3B30', PATCH: '#C780FF' };
    const methodColor = methodColors[api.method] || '#86868b';

    return `
        <div style="border-bottom:0.5px solid var(--border);${index === cat.apis.length - 1 ? 'border-bottom:none' : ''}">
            <div style="padding:14px 20px;display:flex;align-items:center;gap:12px;cursor:pointer;transition:background 0.1s"
                 onclick="apiToggleCard('${key}')" onmouseenter="this.style.background='var(--sidebar-hover)'" onmouseleave="this.style.background='transparent'">
                <span style="background:${methodColor}15;color:${methodColor};font-size:11px;font-weight:700;padding:3px 8px;border-radius:5px;min-width:52px;text-align:center;font-family:'SF Mono','Fira Code',monospace">${api.method}</span>
                <span style="font-family:'SF Mono','Fira Code',monospace;font-size:13px;color:var(--text-primary);flex:1">${api.path}</span>
                <span style="font-size:12px;color:var(--text-secondary)">${api.summary}</span>
                <i data-lucide="${expanded ? 'chevron-up' : 'chevron-down'}" style="width:16px;height:16px;color:var(--text-secondary);flex-shrink:0;transition:transform 0.2s"></i>
            </div>
            ${expanded ? renderApiDetail(api, key) : ''}
        </div>
    `;
}

function renderApiDetail(api, key) {
    let html = '<div style="padding:0 20px 16px;animation:fadeIn 0.2s ease">';

    // Parameters
    if (api.params && api.params.length > 0) {
        html += `
            <div style="margin-bottom:14px">
                <div style="font-size:12px;font-weight:600;color:var(--text-secondary);margin-bottom:8px;text-transform:uppercase;letter-spacing:0.4px">Parameters</div>
                <div style="background:var(--input-bg);border-radius:8px;border:0.5px solid var(--border);overflow:hidden">
                    <table style="width:100%;border-collapse:collapse;font-size:12px">
                        <thead>
                            <tr style="border-bottom:0.5px solid var(--border)">
                                <th style="padding:8px 12px;text-align:left;font-weight:600;color:var(--text-secondary)">Name</th>
                                <th style="padding:8px 12px;text-align:left;font-weight:600;color:var(--text-secondary)">In</th>
                                <th style="padding:8px 12px;text-align:left;font-weight:600;color:var(--text-secondary)">Type</th>
                                <th style="padding:8px 12px;text-align:left;font-weight:600;color:var(--text-secondary)">Required</th>
                                <th style="padding:8px 12px;text-align:left;font-weight:600;color:var(--text-secondary)">Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${api.params.map(p => `
                                <tr style="border-bottom:0.5px solid var(--border)">
                                    <td style="padding:8px 12px;font-family:'SF Mono','Fira Code',monospace;color:var(--accent)">${p.name}</td>
                                    <td style="padding:8px 12px;color:var(--text-secondary)">${p.in}</td>
                                    <td style="padding:8px 12px;font-family:'SF Mono','Fira Code',monospace">${p.type}</td>
                                    <td style="padding:8px 12px">${p.required ? '<span style="color:var(--error);font-weight:600">Yes</span>' : '<span style="color:var(--text-secondary)">No</span>'}</td>
                                    <td style="padding:8px 12px;color:var(--text-secondary)">${p.desc}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }

    // Request body
    if (api.body) {
        html += `
            <div style="margin-bottom:14px">
                <div style="font-size:12px;font-weight:600;color:var(--text-secondary);margin-bottom:8px;text-transform:uppercase;letter-spacing:0.4px">Request Body</div>
                <pre style="background:var(--input-bg);border-radius:8px;border:0.5px solid var(--border);padding:12px 14px;font-family:'SF Mono','Fira Code','Menlo',monospace;font-size:12px;line-height:1.5;overflow-x:auto;margin:0;color:var(--text-primary)">${JSON.stringify(api.body, null, 2)}</pre>
            </div>
        `;
    }

    // Example
    html += `
        <div style="margin-bottom:14px">
            <div style="font-size:12px;font-weight:600;color:var(--text-secondary);margin-bottom:8px;text-transform:uppercase;letter-spacing:0.4px">Example</div>
            <div style="position:relative">
                <pre style="background:#1d1d1f;border-radius:8px;padding:14px;font-family:'SF Mono','Fira Code','Menlo',monospace;font-size:12px;line-height:1.6;overflow-x:auto;margin:0;color:#f5f5f7">${escapeHtml(api.example)}</pre>
                <button class="btn btn-sm" onclick="event.stopPropagation();apiCopyExample(this)" data-code="${escapeAttr(api.example)}"
                        style="position:absolute;top:8px;right:8px;background:rgba(255,255,255,0.1);color:#f5f5f7;border:0.5px solid rgba(255,255,255,0.2);font-size:11px;padding:3px 8px;border-radius:5px">
                    <i data-lucide="copy" style="width:12px;height:12px"></i> Copy
                </button>
            </div>
        </div>
    `;

    // Try it
    const tryId = 'try-' + key;
    html += `
        <div style="display:flex;gap:8px;align-items:center">
            <button class="btn btn-primary btn-sm" onclick="event.stopPropagation();apiTryIt('${key}')" id="btn-${tryId}">
                <i data-lucide="play" style="width:12px;height:12px"></i> Try it
            </button>
            <span style="font-size:11px;color:var(--text-secondary)">直接调用接口查看返回结果</span>
        </div>
        <div id="${tryId}" style="margin-top:10px;display:none"></div>
    `;

    html += '</div>';
    return html;
}

function apiToggleCard(key) {
    apiExpandedCards[key] = !apiExpandedCards[key];
    renderApiContent();
}

function apiCopyExample(btn) {
    const code = btn.getAttribute('data-code');
    navigator.clipboard.writeText(code).then(() => {
        btn.innerHTML = '<i data-lucide="check" style="width:12px;height:12px"></i> Copied';
        lucide.createIcons();
        setTimeout(() => {
            btn.innerHTML = '<i data-lucide="copy" style="width:12px;height:12px"></i> Copy';
            lucide.createIcons();
        }, 2000);
    });
}

async function apiTryIt(key) {
    const [catId, idxStr] = key.split('-');
    const cat = API_CATEGORIES.find(c => c.id === catId);
    const api = cat.apis[parseInt(idxStr)];
    const resultDiv = document.getElementById('try-' + key);
    const btn = document.getElementById('btn-try-' + key);

    if (!resultDiv) return;
    resultDiv.style.display = 'block';
    resultDiv.innerHTML = `
        <div style="background:var(--input-bg);border-radius:8px;border:0.5px solid var(--border);padding:12px;font-size:12px;color:var(--text-secondary)">
            <i data-lucide="loader-2" style="width:14px;height:14px;animation:spin 1s linear infinite;vertical-align:middle"></i> 请求中...
        </div>
    `;
    lucide.createIcons();

    // Build URL with path params replaced
    let url = api.path;
    if (api.params) {
        api.params.filter(p => p.in === 'path').forEach(p => {
            const val = prompt(`Enter ${p.name} (${p.desc}):`, p.name === 'userId' ? 'alice' : p.name === 'roomId' ? 'general' : p.name === 'orderId' ? 'A1B2C3D4' : p.name === 'conversationId' ? 'conv-alice-bob' : p.name === 'sessionId' ? 'abc123' : '');
            if (val) url = url.replace(`{${p.name}}`, val);
        });
        // Query params
        const queryParams = api.params.filter(p => p.in === 'query');
        if (queryParams.length > 0) {
            const parts = [];
            queryParams.forEach(p => {
                const val = prompt(`Enter ${p.name} (${p.desc}, optional - leave empty to skip):`, '');
                if (val) parts.push(`${p.name}=${encodeURIComponent(val)}`);
            });
            if (parts.length > 0) url += '?' + parts.join('&');
        }
    }

    const options = { method: api.method, headers: { 'Content-Type': 'application/json' } };
    if (api.body && (api.method === 'POST' || api.method === 'PUT' || api.method === 'PATCH')) {
        options.body = JSON.stringify(api.body);
    }

    try {
        const resp = await fetch(url, options);
        const status = resp.status;
        let body;
        try { body = await resp.json(); } catch { body = await resp.text(); }
        const statusColor = status >= 200 && status < 300 ? 'var(--success)' : 'var(--error)';
        resultDiv.innerHTML = `
            <div style="background:var(--input-bg);border-radius:8px;border:0.5px solid var(--border);overflow:hidden">
                <div style="padding:8px 12px;border-bottom:0.5px solid var(--border);display:flex;align-items:center;gap:8px">
                    <span style="background:${statusColor}15;color:${statusColor};font-size:11px;font-weight:700;padding:2px 8px;border-radius:4px">${status}</span>
                    <span style="font-size:11px;color:var(--text-secondary);font-family:'SF Mono','Fira Code',monospace">${api.method} ${url}</span>
                </div>
                <pre style="padding:12px;font-family:'SF Mono','Fira Code','Menlo',monospace;font-size:12px;line-height:1.5;overflow-x:auto;margin:0;color:var(--text-primary)">${typeof body === 'string' ? escapeHtml(body) : JSON.stringify(body, null, 2)}</pre>
            </div>
        `;
    } catch (err) {
        resultDiv.innerHTML = `
            <div style="background:rgba(255,59,48,0.06);border-radius:8px;border:0.5px solid rgba(255,59,48,0.2);padding:12px;font-size:12px;color:var(--error)">
                Error: ${escapeHtml(err.message)}
            </div>
        `;
    }
}

function escapeHtml(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function escapeAttr(str) {
    return String(str).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/'/g, '&#39;').replace(/\n/g, '&#10;');
}

// spin animation
const style = document.createElement('style');
style.textContent = '@keyframes spin{from{transform:rotate(0deg)}to{transform:rotate(360deg)}}';
document.head.appendChild(style);
