// ============================================================
// 配置
// ============================================================
const BASE = '';
const WS_BASE = `ws://${location.host}`;

// ============================================================
// 状态管理
// ============================================================
const Store = {
    currentUser: null,
    wsClients: {},
    logs: [],
    orders: [],
    theme: localStorage.getItem('theme') || 'light',
    listeners: [],

    set(key, value) {
        this[key] = value;
        this.listeners.forEach(fn => fn(key, value));
    },

    subscribe(fn) {
        this.listeners.push(fn);
    },

    addLog(type, data, module) {
        const entry = { time: new Date().toLocaleTimeString(), type, data, module };
        this.logs.unshift(entry);
        if (this.logs.length > 200) this.logs.pop();
        this.listeners.forEach(fn => fn('log', entry));
    }
};

// ============================================================
// 消息提示
// ============================================================
function showToast(message, type = 'info') {
    const container = document.getElementById('toasts');
    const toast = document.createElement('div');
    toast.className = 'toast';
    const colors = { info: 'var(--accent)', success: 'var(--success)', warning: 'var(--warning)', error: 'var(--error)' };
    toast.style.borderLeft = `3px solid ${colors[type] || colors.info}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => { toast.style.opacity = '0'; toast.style.transition = 'opacity 0.3s'; setTimeout(() => toast.remove(), 300); }, 3000);
}

// ============================================================
// WebSocket 客户端
// ============================================================
class WsClient {
    constructor(name, url, userId) {
        this.name = name;
        this.url = url;
        this.userId = userId;
        this.ws = null;
        this.handlers = new Map();
        this.reconnectDelay = 1000;
        this.maxReconnectDelay = 30000;
        this.connected = false;
        this.shouldConnect = true;
        this._hasConnected = false;
    }

    connect() {
        if (this.ws && this.ws.readyState <= 1) return;
        this.ws = new WebSocket(this.url);
        this.ws.onopen = () => {
            this.connected = true;
            this.reconnectDelay = 1000;
            updateWsStatus();
            Store.addLog('CONNECT', `${this.name}: ${this.userId}`, this.name);
            if (!this._hasConnected) {
                showToast(`${this.name} 已连接: ${this.userId}`, 'success');
                this._hasConnected = true;
            }
            if (this._onOpenFn) this._onOpenFn();
        };
        this.ws.onmessage = (e) => {
            try {
                const msg = JSON.parse(e.data);
                Store.addLog('RECV', `${msg.type}: ${JSON.stringify(msg.payload || {}).substring(0, 80)}`, this.name);
                const handler = this.handlers.get(msg.type);
                if (handler) handler(msg);
                const wildcard = this.handlers.get('*');
                if (wildcard) wildcard(msg);
            } catch (err) {
                Store.addLog('ERROR', `Parse error: ${e.data}`, this.name);
            }
        };
        this.ws.onclose = () => {
            this.connected = false;
            updateWsStatus();
            Store.addLog('DISCONNECT', `${this.name}: ${this.userId}`, this.name);
            if (this.shouldConnect) {
                setTimeout(() => this.connect(), this.reconnectDelay);
                this.reconnectDelay = Math.min(this.reconnectDelay * 2, this.maxReconnectDelay);
            }
        };
        this.ws.onerror = () => {};
    }

    send(type, payload, options = {}) {
        if (!this.ws || this.ws.readyState !== 1) {
            showToast('未连接', 'error');
            return;
        }
        const msg = { type, payload, from: this.userId, timestamp: new Date().toISOString() };
        if (options.roomId) msg.roomId = options.roomId;
        if (options.to) msg.to = options.to;
        this.ws.send(JSON.stringify(msg));
        Store.addLog('SEND', `${type}: ${JSON.stringify(payload || {}).substring(0, 80)}`, this.name);
    }

    onOpen(fn) {
        this._onOpenFn = fn;
    }

    on(type, handler) {
        this.handlers.set(type, handler);
    }

    disconnect() {
        this.shouldConnect = false;
        if (this.ws) this.ws.close();
        this.connected = false;
        updateWsStatus();
    }
}

function updateWsStatus() {
    const clients = Object.values(Store.wsClients);
    const connected = clients.filter(c => c.connected).length;
    const dot = document.getElementById('ws-dot');
    const label = document.getElementById('ws-label');
    if (!dot || !label) return;
    if (connected === 0) {
        dot.className = 'ws-dot';
        label.textContent = '未连接';
    } else if (connected < clients.length) {
        dot.className = 'ws-dot reconnecting';
        label.textContent = `${connected}/${clients.length} 个连接`;
    } else {
        dot.className = 'ws-dot connected';
        label.textContent = `${connected} 个连接`;
    }
}

// ============================================================
// 主题切换
// ============================================================
function toggleTheme() {
    Store.theme = Store.theme === 'light' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', Store.theme);
    localStorage.setItem('theme', Store.theme);
    const icon = document.getElementById('theme-icon');
    if (icon) {
        icon.setAttribute('data-lucide', Store.theme === 'dark' ? 'sun' : 'moon');
        lucide.createIcons();
    }
}

// ============================================================
// API 工具
// ============================================================
async function api(path, options = {}) {
    try {
        const resp = await fetch(BASE + path, {
            headers: { 'Content-Type': 'application/json' },
            ...options
        });
        return await resp.json();
    } catch (e) {
        showToast(`请求错误: ${e.message}`, 'error');
        return null;
    }
}

// ============================================================
// 初始化
// ============================================================
document.addEventListener('DOMContentLoaded', () => {
    if (Store.theme === 'dark') {
        document.documentElement.setAttribute('data-theme', 'dark');
        const icon = document.getElementById('theme-icon');
        if (icon) icon.setAttribute('data-lucide', 'sun');
    }
    lucide.createIcons();
});
