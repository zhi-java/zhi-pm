// ============================================================
// 聊天室模块 — 多用户卡片式
// ============================================================
const CHAT_USERS = ['alice', 'bob', 'charlie'];
const CHAT_USER_COLORS = {
    alice: { bg: '#FF6B6B', light: 'rgba(255,107,107,0.15)' },
    bob: { bg: '#4ECDC4', light: 'rgba(78,205,196,0.15)' },
    charlie: { bg: '#C780FF', light: 'rgba(199,128,255,0.15)' }
};
let chatConversations = [
    { id: 'conv-alice-bob', name: 'Alice ↔ Bob', type: 'single', members: ['alice', 'bob'] },
    { id: 'conv-alice-charlie', name: 'Alice ↔ Charlie', type: 'single', members: ['alice', 'charlie'] },
    { id: 'conv-bob-charlie', name: 'Bob ↔ Charlie', type: 'single', members: ['bob', 'charlie'] },
    { id: 'group-team', name: '团队群聊', type: 'group', members: ['alice', 'bob', 'charlie'] }
];
let chatMessages = {};
let chatActiveConv = {};

function render_chat() {
    const panel = document.getElementById('panel-chat');
    panel.innerHTML = `
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
            <div style="font-size:13px;color:var(--text-secondary)">点击用户卡片连接，选择会话后即可互发消息</div>
            <button class="btn btn-sm btn-ghost" onclick="chatReconnectAll()"><i data-lucide="refresh-cw" style="width:14px;height:14px"></i> 全部重连</button>
        </div>
        <div class="chat-cards" id="chat-cards"></div>
    `;
    lucide.createIcons();
    renderChatCards();
}

function renderChatCards() {
    const container = document.getElementById('chat-cards');
    if (!container) return;
    container.innerHTML = CHAT_USERS.map(userId => {
        const key = 'chat-' + userId;
        const client = Store.wsClients[key];
        const online = client && client.connected;
        const colors = CHAT_USER_COLORS[userId];
        const activeConvId = chatActiveConv[userId] || '';
        return `
            <div class="chat-card ${online ? 'online' : ''}" id="chat-card-${userId}">
                <div class="chat-card-header">
                    <div class="user-avatar" style="background:${online ? colors.bg : 'var(--text-secondary)'}">${userId[0].toUpperCase()}</div>
                    <div class="user-info">
                        <div class="user-name">${userId.charAt(0).toUpperCase() + userId.slice(1)}</div>
                        <div class="user-status" style="color:${online ? colors.bg : 'var(--text-secondary)'}">${online ? '已连接' : '离线'}</div>
                    </div>
                    <button class="btn btn-sm ${online ? 'btn-danger' : 'btn-primary'}" onclick="chatToggleUser('${userId}')">${online ? '断开' : '连接'}</button>
                </div>
                <div class="chat-card-conv-list" id="chat-convs-${userId}">
                    ${online ? renderChatConvList(userId, activeConvId) : '<div style="padding:12px;text-align:center;font-size:12px;color:var(--text-secondary)">请先连接</div>'}
                </div>
                <div class="chat-card-messages" id="chat-msgs-${userId}">
                    ${online && activeConvId ? renderChatCardMessages(userId, activeConvId) : '<div style="text-align:center;color:var(--text-secondary);padding:20px;font-size:13px">选择会话开始聊天</div>'}
                </div>
                <div class="chat-card-input">
                    <input class="input" id="chat-input-${userId}" placeholder="输入消息..." ${!online ? 'disabled' : ''} onkeydown="if(event.key==='Enter')chatSendAs('${userId}')">
                    <button class="btn btn-sm btn-primary" onclick="chatSendAs('${userId}')" ${!online ? 'disabled' : ''}>
                        <i data-lucide="send" style="width:13px;height:13px"></i>
                    </button>
                </div>
            </div>
        `;
    }).join('');
    lucide.createIcons();
}

function renderChatConvList(userId, activeConvId) {
    const userConvs = chatConversations.filter(c => c.members.includes(userId));
    if (userConvs.length === 0) return '<div style="padding:12px;text-align:center;font-size:12px;color:var(--text-secondary)">无可用会话</div>';
    return userConvs.map(c => {
        const msgs = chatMessages[c.id] || [];
        const last = msgs.length > 0 ? msgs[msgs.length - 1].content.substring(0, 20) : '暂无消息';
        const isActive = activeConvId === c.id;
        return `
            <div class="chat-card-conv-item ${isActive ? 'active' : ''}" onclick="chatSelectConv('${userId}', '${c.id}')">
                <span>${c.name}</span>
                <span style="font-size:11px;color:var(--text-secondary);max-width:80px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${last}</span>
            </div>
        `;
    }).join('');
}

function renderChatCardMessages(userId, convId) {
    const msgs = chatMessages[convId] || [];
    if (msgs.length === 0) return '<div style="text-align:center;color:var(--text-secondary);padding:20px;font-size:13px">暂无消息</div>';
    return msgs.map(m => `
        <div class="message-bubble ${m.sender === userId ? 'sent' : 'received'}">
            ${m.sender !== userId ? `<div style="font-size:11px;font-weight:600;margin-bottom:2px;color:${(CHAT_USER_COLORS[m.sender] || {}).bg || 'var(--accent)'}">${m.sender}</div>` : ''}
            <div>${m.content}</div>
            <div class="message-meta">${m.time}</div>
        </div>
    `).join('');
}

function chatToggleUser(userId) {
    const key = 'chat-' + userId;
    if (Store.wsClients[key] && Store.wsClients[key].connected) {
        Store.wsClients[key].disconnect();
        delete Store.wsClients[key];
        chatActiveConv[userId] = '';
        renderChatCards();
        return;
    }
    const client = new WsClient('chat', `${WS_BASE}/ws?access_token=${userId}-token`, userId);
    client.on('chat.message', (msg) => {
        const p = msg.payload || {};
        const convId = p.conversationId;
        if (!chatMessages[convId]) chatMessages[convId] = [];
        chatMessages[convId].push({ id: p.messageId, sender: p.senderId, content: p.content, time: new Date().toLocaleTimeString() });
        renderChatCards();
        client.send('chat.ack', { messageId: p.messageId });
    });
    Store.wsClients[key] = client;
    client.onOpen(() => {
        chatConversations.filter(c => c.type === 'group' && c.members.includes(userId)).forEach(c => {
            client.send('room.join', { roomId: c.id });
        });
        renderChatCards();
    });
    client.connect();
    setTimeout(() => renderChatCards(), 600);
}

function chatSelectConv(userId, convId) {
    chatActiveConv[userId] = convId;
    const key = 'chat-' + userId;
    const client = Store.wsClients[key];
    const conv = chatConversations.find(c => c.id === convId);
    if (conv && conv.type === 'group' && client && client.connected) {
        client.send('room.join', { roomId: convId });
    }
    if (client && client.connected) {
        client.send('chat.read', { conversationId: convId });
    }
    renderChatCards();
    loadChatCardHistory(userId, convId);
}

async function loadChatCardHistory(userId, convId) {
    const data = await api(`/api/chat/conversations/${convId}/history?limit=20`);
    if (data && data.messages) {
        chatMessages[convId] = data.messages.map(m => ({
            id: m.messageId, sender: m.senderId, content: m.content,
            time: new Date(m.createdAt).toLocaleTimeString()
        }));
        renderChatCards();
    }
}

function chatSendAs(userId) {
    const input = document.getElementById('chat-input-' + userId);
    const content = input.value.trim();
    if (!content) return;
    const convId = chatActiveConv[userId];
    if (!convId) {
        showToast('请先选择会话', 'warning');
        return;
    }
    const key = 'chat-' + userId;
    const client = Store.wsClients[key];
    if (!client || !client.connected) {
        showToast(userId + ' 未连接', 'warning');
        return;
    }
    const conv = chatConversations.find(c => c.id === convId);
    client.send('chat.send', {
        conversationId: convId,
        conversationType: conv ? conv.type : 'single',
        content,
        contentType: 'text'
    });
    if (!chatMessages[convId]) chatMessages[convId] = [];
    chatMessages[convId].push({ id: 'local-' + Date.now(), sender: userId, content, time: new Date().toLocaleTimeString() });
    renderChatCards();
    input.value = '';
}

function chatReconnectAll() {
    CHAT_USERS.forEach(userId => {
        const key = 'chat-' + userId;
        if (!Store.wsClients[key] || !Store.wsClients[key].connected) {
            chatToggleUser(userId);
        }
    });
}
