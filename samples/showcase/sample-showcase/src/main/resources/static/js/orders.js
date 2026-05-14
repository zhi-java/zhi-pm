// ============================================================
// 订单追踪模块 — 多用户卡片式
// ============================================================
const ORDER_USERS = ['alice', 'bob', 'charlie'];
const ORDER_STATUSES = ['CREATED', 'PAID', 'SHIPPING', 'DELIVERED', 'COMPLETED'];
const ORDER_STATUS_COLORS = {
    CREATED: '#007AFF',
    PAID: '#FF9500',
    SHIPPING: '#C780FF',
    DELIVERED: '#4ECDC4',
    COMPLETED: '#34C759'
};
let orderData = {}; // { userId: [orders] }

function render_orders() {
    const panel = document.getElementById('panel-orders');
    panel.innerHTML = `
        <div class="card" style="margin-bottom:16px">
            <div class="card-title"><i data-lucide="shopping-cart" style="width:20px;height:20px"></i> 订单追踪</div>
            <p style="color:var(--text-secondary);font-size:14px;margin-bottom:12px">
                多用户独立窗口，点击「连接」建立 WebSocket，各自创建订单并实时追踪状态变更
            </p>
            <div id="order-stats" class="card-grid" style="grid-template-columns:repeat(auto-fit, minmax(140px, 1fr));margin-bottom:0"></div>
        </div>
        <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(360px, 1fr));gap:16px" id="order-user-cards"></div>
    `;
    lucide.createIcons();
    renderOrderUserCards();
    orderRefreshStats();
}

function renderOrderUserCards() {
    const container = document.getElementById('order-user-cards');
    if (!container) return;
    container.innerHTML = ORDER_USERS.map(userId => {
        const key = 'orders-' + userId;
        const client = Store.wsClients[key];
        const online = client && client.connected;
        const orders = orderData[userId] || [];
        const colors = {
            alice: '#FF6B6B',
            bob: '#4ECDC4',
            charlie: '#C780FF'
        };
        const color = colors[userId] || '#86868b';
        return `
            <div class="card" style="margin-bottom:0;border:${online ? '2px solid ' + color : '1px solid var(--border)'};transition:border-color 0.3s">
                <!-- 用户头部 -->
                <div style="display:flex;align-items:center;gap:12px;margin-bottom:16px">
                    <div class="user-avatar" style="background:${online ? color : 'var(--text-secondary)'};transition:background 0.3s">${userId[0].toUpperCase()}</div>
                    <div style="flex:1">
                        <div style="font-weight:600;font-size:15px">${userId.charAt(0).toUpperCase() + userId.slice(1)}</div>
                        <div style="font-size:12px;color:var(--text-secondary)">${online ? '已连接 WebSocket' : '未连接'}</div>
                    </div>
                    <span class="badge ${online ? 'badge-success' : 'badge-error'}" id="order-badge-${userId}">${online ? '在线' : '离线'}</span>
                    <button class="btn ${online ? 'btn-danger' : 'btn-primary'} btn-sm" onclick="orderToggleUser('${userId}')">
                        ${online ? '断开' : '连接'}
                    </button>
                </div>
                <!-- 创建订单 -->
                <div style="display:flex;gap:6px;margin-bottom:12px">
                    <input class="input" id="order-product-${userId}" placeholder="商品名称" value="MacBook Pro" style="flex:1;font-size:13px;padding:8px 10px" ${!online ? 'disabled' : ''}>
                    <input class="input" id="order-qty-${userId}" type="number" placeholder="数量" value="1" style="max-width:60px;font-size:13px;padding:8px 10px" ${!online ? 'disabled' : ''}>
                    <button class="btn btn-primary btn-sm" onclick="orderCreateFor('${userId}')" ${!online ? 'disabled' : ''}>
                        <i data-lucide="plus" style="width:12px;height:12px"></i> 创建
                    </button>
                </div>
                <div style="margin-bottom:12px">
                    <button class="btn btn-ghost btn-sm" onclick="orderGenerateFor('${userId}')" ${!online ? 'disabled' : ''} style="font-size:12px">
                        <i data-lucide="package" style="width:12px;height:12px"></i> 生成示例订单
                    </button>
                    <button class="btn btn-ghost btn-sm" onclick="orderRefreshFor('${userId}')" ${!online ? 'disabled' : ''} style="font-size:12px;margin-left:4px">
                        <i data-lucide="refresh-cw" style="width:12px;height:12px"></i> 刷新
                    </button>
                </div>
                <!-- 订单列表 -->
                <div id="order-list-${userId}" style="max-height:320px;overflow-y:auto">
                    ${renderOrderList(userId, orders)}
                </div>
            </div>
        `;
    }).join('');
    lucide.createIcons();
}

function renderOrderList(userId, orders) {
    if (!orders || orders.length === 0) {
        return '<div style="text-align:center;color:var(--text-secondary);padding:24px;font-size:13px">暂无订单</div>';
    }
    return orders.map(o => {
        const statusIndex = ORDER_STATUSES.indexOf(o.status);
        return `
            <div class="order-card" style="margin-bottom:8px">
                <div class="order-header">
                    <span class="order-id">${o.orderId}</span>
                    <span class="badge" style="background:${ORDER_STATUS_COLORS[o.status] || 'var(--accent)'}22;color:${ORDER_STATUS_COLORS[o.status] || 'var(--accent)'}">${o.status}</span>
                </div>
                <div class="order-detail">${o.product} x ${o.quantity}</div>
                <div class="order-timeline">
                    ${ORDER_STATUSES.map((s, i) => {
                        const active = i <= statusIndex;
                        const color = ORDER_STATUS_COLORS[s];
                        return `
                            <div class="order-timeline-step ${active ? 'active' : ''}">
                                <div class="order-timeline-dot" style="background:${active ? color : 'var(--border)'};${active ? 'box-shadow:0 0 0 3px ' + color + '22' : ''}"></div>
                                <div class="order-timeline-label" style="color:${active ? color : 'var(--text-secondary)'};${active ? 'font-weight:600' : ''}">${s}</div>
                            </div>
                        `;
                    }).join('')}
                </div>
                ${statusIndex < ORDER_STATUSES.length - 1 ? `
                    <div style="margin-top:10px;text-align:right">
                        <button class="btn btn-sm btn-ghost" style="font-size:12px" onclick="orderAdvanceFor('${userId}', '${o.orderId}', '${ORDER_STATUSES[statusIndex + 1]}')">
                            推进至 ${ORDER_STATUSES[statusIndex + 1]}
                            <i data-lucide="chevron-right" style="width:12px;height:12px"></i>
                        </button>
                    </div>
                ` : ''}
            </div>
        `;
    }).join('');
}

function orderToggleUser(userId) {
    const key = 'orders-' + userId;
    if (Store.wsClients[key] && Store.wsClients[key].connected) {
        Store.wsClients[key].disconnect();
        delete Store.wsClients[key];
        renderOrderUserCards();
        orderRefreshStats();
        return;
    }
    const client = new WsClient('orders', `${WS_BASE}/ws?access_token=${userId}-token`, userId);
    client.on('order.created', (msg) => {
        showToast(`${userId} 订单已创建: ${msg.payload?.orderId}`, 'success');
        orderRefreshFor(userId);
        orderRefreshStats();
    });
    client.on('order.status.changed', (msg) => {
        showToast(`${userId} 订单 ${msg.payload?.orderId}: ${msg.payload?.status}`, 'info');
        orderRefreshFor(userId);
    });
    client.onOpen(() => {
        renderOrderUserCards();
        orderRefreshFor(userId);
        orderRefreshStats();
    });
    Store.wsClients[key] = client;
    client.connect();
    renderOrderUserCards();
}

async function orderCreateFor(userId) {
    const product = document.getElementById('order-product-' + userId)?.value;
    const quantity = parseInt(document.getElementById('order-qty-' + userId)?.value) || 1;
    if (!product) { showToast('请输入商品名称', 'warning'); return; }
    const result = await api('/api/orders', { method: 'POST', body: JSON.stringify({ userId, product, quantity }) });
    if (result && result.orderId) {
        showToast(`${userId} 订单 ${result.orderId} 已创建`, 'success');
        orderRefreshFor(userId);
        orderRefreshStats();
    }
}

async function orderRefreshFor(userId) {
    const data = await api(`/api/orders/user/${userId}`);
    if (!data) return;
    orderData[userId] = data.orders || [];
    const list = document.getElementById('order-list-' + userId);
    if (list) {
        list.innerHTML = renderOrderList(userId, orderData[userId]);
        lucide.createIcons();
    }
}

async function orderAdvanceFor(userId, orderId, status) {
    await api(`/api/orders/${orderId}/status`, { method: 'POST', body: JSON.stringify({ status }) });
    orderRefreshFor(userId);
    orderRefreshStats();
}

async function orderGenerateFor(userId) {
    const samples = [
        { product: 'MacBook Pro 16"', quantity: 1 },
        { product: 'iPhone 16 Pro', quantity: 2 },
        { product: 'AirPods Pro', quantity: 3 }
    ];
    for (const s of samples) {
        await api('/api/orders', { method: 'POST', body: JSON.stringify({ userId, ...s }) });
    }
    const data = await api(`/api/orders/user/${userId}`);
    if (data?.orders?.length > 0) {
        const first = data.orders[0];
        await api(`/api/orders/${first.orderId}/status`, { method: 'POST', body: JSON.stringify({ status: 'PAID' }) });
    }
    if (data?.orders?.length > 1) {
        const second = data.orders[1];
        await api(`/api/orders/${second.orderId}/status`, { method: 'POST', body: JSON.stringify({ status: 'PAID' }) });
        await api(`/api/orders/${second.orderId}/status`, { method: 'POST', body: JSON.stringify({ status: 'SHIPPING' }) });
    }
    showToast(`${userId} 示例订单已生成`, 'success');
    orderRefreshFor(userId);
    orderRefreshStats();
}

async function orderRefreshStats() {
    let total = 0, active = 0, completed = 0;
    for (const userId of ORDER_USERS) {
        const data = await api(`/api/orders/user/${userId}`);
        if (data?.orders) {
            orderData[userId] = data.orders;
            total += data.orders.length;
            data.orders.forEach(o => {
                if (o.status === 'COMPLETED') completed++;
                else active++;
            });
        }
    }
    const statsEl = document.getElementById('order-stats');
    if (statsEl) {
        statsEl.innerHTML = `
            <div class="stat-card"><div class="stat-label">总订单</div><div class="stat-value">${total}</div></div>
            <div class="stat-card"><div class="stat-label">进行中</div><div class="stat-value" style="color:var(--accent)">${active}</div></div>
            <div class="stat-card"><div class="stat-label">已完成</div><div class="stat-value" style="color:var(--success)">${completed}</div></div>
            <div class="stat-card"><div class="stat-label">已连接</div><div class="stat-value">${ORDER_USERS.filter(u => Store.wsClients['orders-' + u]?.connected).length}/${ORDER_USERS.length}</div></div>
        `;
    }
    renderOrderUserCards();
}
