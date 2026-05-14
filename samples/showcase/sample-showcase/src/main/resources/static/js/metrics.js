// ============================================================
// 实时指标模块
// ============================================================
let metricsInterval = null;
let metricsHistory = { connections: [], users: [], rooms: [], successRate: [] };
const METRICS_MAX_POINTS = 60;
let metricsPaused = false;
let metricsPrevValues = {};

function render_metrics() {
    if (metricsInterval) clearInterval(metricsInterval);
    const panel = document.getElementById('panel-metrics');
    panel.innerHTML = `
        <!-- 顶部状态栏 -->
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:20px">
            <div style="display:flex;align-items:center;gap:12px">
                <div style="width:40px;height:40px;border-radius:12px;background:rgba(0,122,255,0.12);display:flex;align-items:center;justify-content:center">
                    <i data-lucide="bar-chart-3" style="width:20px;height:20px;color:var(--accent)"></i>
                </div>
                <div>
                    <div style="font-weight:700;font-size:18px">实时指标</div>
                    <div class="ops-pulse" id="metrics-live-indicator" style="color:${metricsPaused ? 'var(--warning)' : 'var(--success)'}">
                        <span class="ops-pulse-dot" style="background:${metricsPaused ? 'var(--warning)' : 'var(--success)'}"></span>
                        ${metricsPaused ? '已暂停' : '实时采集中'}
                    </div>
                </div>
            </div>
            <div style="display:flex;gap:8px;align-items:center">
                <span style="font-size:12px;color:var(--text-secondary)" id="metrics-point-count">0 数据点</span>
                <span style="font-size:12px;color:var(--text-secondary)" id="metrics-last-update">-</span>
                <button class="btn btn-sm ${metricsPaused ? 'btn-primary' : 'btn-ghost'}" onclick="metricsTogglePause()">
                    <i data-lucide="${metricsPaused ? 'play' : 'pause'}" style="width:13px;height:13px"></i>
                    ${metricsPaused ? '恢复' : '暂停'}
                </button>
                <button class="btn btn-sm btn-ghost" onclick="metricsReset()">
                    <i data-lucide="rotate-ccw" style="width:13px;height:13px"></i> 重置
                </button>
            </div>
        </div>

        <!-- 统计卡片 -->
        <div id="metrics-stats" style="display:grid;grid-template-columns:repeat(4, 1fr);gap:14px;margin-bottom:20px"></div>

        <!-- 图表区: 上排 -->
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:16px">
            <div class="ops-chart">
                <div class="ops-chart-header">
                    <div>
                        <div class="ops-chart-title">
                            <i data-lucide="link" style="width:14px;height:14px;color:var(--accent)"></i> 活跃连接数
                        </div>
                        <div class="ops-chart-current" id="metrics-conn-current" style="color:var(--accent)">-</div>
                    </div>
                </div>
                <canvas id="chart-connections" style="width:100%;height:180px"></canvas>
            </div>
            <div class="ops-chart">
                <div class="ops-chart-header">
                    <div>
                        <div class="ops-chart-title">
                            <i data-lucide="check-circle" style="width:14px;height:14px;color:var(--success)"></i> 推送成功率
                        </div>
                        <div class="ops-chart-current" id="metrics-success-current" style="color:var(--success)">-</div>
                    </div>
                </div>
                <canvas id="chart-success" style="width:100%;height:180px"></canvas>
            </div>
        </div>
        <!-- 图表区: 下排 -->
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
            <div class="ops-chart">
                <div class="ops-chart-header">
                    <div>
                        <div class="ops-chart-title">
                            <i data-lucide="users" style="width:14px;height:14px;color:#C780FF"></i> 在线用户数
                        </div>
                        <div class="ops-chart-current" id="metrics-users-current" style="color:#C780FF">-</div>
                    </div>
                </div>
                <canvas id="chart-users" style="width:100%;height:180px"></canvas>
            </div>
            <div class="ops-chart">
                <div class="ops-chart-header">
                    <div>
                        <div class="ops-chart-title">
                            <i data-lucide="home" style="width:14px;height:14px;color:var(--warning)"></i> 活跃房间数
                        </div>
                        <div class="ops-chart-current" id="metrics-rooms-current" style="color:var(--warning)">-</div>
                    </div>
                </div>
                <canvas id="chart-rooms" style="width:100%;height:180px"></canvas>
            </div>
        </div>
    `;
    lucide.createIcons();
    metricsHistory = { connections: [], users: [], rooms: [], successRate: [] };
    metricsPrevValues = {};
    metricsRefresh();
    metricsInterval = setInterval(() => { if (!metricsPaused) metricsRefresh(); }, 2000);
}

function metricsTogglePause() {
    metricsPaused = !metricsPaused;
    render_metrics();
}

function metricsReset() {
    metricsHistory = { connections: [], users: [], rooms: [], successRate: [] };
    metricsPrevValues = {};
    metricsRefresh();
}

function metricsUpdateCurrent(id, value, color) {
    const el = document.getElementById(id);
    if (!el) return;
    const oldText = el.textContent;
    const newText = value;
    el.textContent = newText;
    if (oldText !== newText && oldText !== '-') {
        el.classList.remove('changed');
        void el.offsetWidth;
        el.classList.add('changed');
    }
}

async function metricsRefresh() {
    const stats = await api('/admin/api/stats');
    if (!stats) return;
    metricsHistory.connections.push(stats.activeConnections || 0);
    metricsHistory.users.push(stats.onlineUsers || 0);
    metricsHistory.rooms.push(stats.activeRooms || 0);
    metricsHistory.successRate.push((stats.pushSuccessRate || 1) * 100);
    if (metricsHistory.connections.length > METRICS_MAX_POINTS) {
        metricsHistory.connections.shift();
        metricsHistory.users.shift();
        metricsHistory.rooms.shift();
        metricsHistory.successRate.shift();
    }
    // 更新时间
    const lastEl = document.getElementById('metrics-last-update');
    if (lastEl) lastEl.textContent = new Date().toLocaleTimeString();
    const pointEl = document.getElementById('metrics-point-count');
    if (pointEl) pointEl.textContent = `${metricsHistory.connections.length} 数据点`;
    // 统计卡片
    const container = document.getElementById('metrics-stats');
    if (container) {
        const len = metricsHistory.connections.length;
        const connCurr = metricsHistory.connections[len - 1] || 0;
        const connPrev = len >= 2 ? metricsHistory.connections[len - 2] : connCurr;
        const connDelta = connCurr - connPrev;
        const usersCurr = stats.onlineUsers || 0;
        const roomsCurr = stats.activeRooms || 0;
        const successCurr = metricsHistory.successRate[len - 1] || 100;
        container.innerHTML = `
            <div class="ops-stat">
                <div class="ops-stat-icon" style="background:var(--accent)18"><i data-lucide="link" style="width:18px;height:18px;color:var(--accent)"></i></div>
                <div class="ops-stat-label">活跃连接</div>
                <div class="ops-stat-value ops-number-animate" style="color:var(--accent)">${connCurr}</div>
                <span class="ops-stat-trend ${connDelta > 0 ? 'up' : connDelta < 0 ? 'down' : 'flat'}">${connDelta > 0 ? '&#9650; +' + connDelta : connDelta < 0 ? '&#9660; ' + connDelta : '- 持平'}</span>
            </div>
            <div class="ops-stat">
                <div class="ops-stat-icon" style="background:#C780FF18"><i data-lucide="users" style="width:18px;height:18px;color:#C780FF"></i></div>
                <div class="ops-stat-label">在线用户</div>
                <div class="ops-stat-value ops-number-animate" style="color:#C780FF">${usersCurr}</div>
                <span class="ops-stat-trend flat">- 持平</span>
            </div>
            <div class="ops-stat">
                <div class="ops-stat-icon" style="background:var(--warning)18"><i data-lucide="home" style="width:18px;height:18px;color:var(--warning)"></i></div>
                <div class="ops-stat-label">活跃房间</div>
                <div class="ops-stat-value ops-number-animate" style="color:var(--warning)">${roomsCurr}</div>
                <span class="ops-stat-trend flat">- 持平</span>
            </div>
            <div class="ops-stat">
                <div class="ops-stat-icon" style="background:var(--success)18"><i data-lucide="check-circle" style="width:18px;height:18px;color:var(--success)"></i></div>
                <div class="ops-stat-label">推送成功率</div>
                <div class="ops-stat-value ops-number-animate" style="color:${successCurr >= 99 ? 'var(--success)' : successCurr >= 90 ? 'var(--warning)' : 'var(--error)'}">${successCurr.toFixed(1)}%</div>
                <span class="ops-stat-trend ${successCurr >= 99 ? 'flat' : 'down'}">${successCurr >= 99 ? '&#10003; 正常' : '&#9888; 偏低'}</span>
            </div>
        `;
        lucide.createIcons();
    }
    // 更新图表当前值
    const len = metricsHistory.connections.length;
    metricsUpdateCurrent('metrics-conn-current', `${metricsHistory.connections[len - 1] || 0}`, 'var(--accent)');
    metricsUpdateCurrent('metrics-success-current', `${(metricsHistory.successRate[len - 1] || 100).toFixed(1)}%`, 'var(--success)');
    metricsUpdateCurrent('metrics-users-current', `${metricsHistory.users[len - 1] || 0}`, '#C780FF');
    metricsUpdateCurrent('metrics-rooms-current', `${metricsHistory.rooms[len - 1] || 0}`, 'var(--warning)');
    // 绘制图表
    drawChart('chart-connections', metricsHistory.connections, '#007AFF');
    drawChart('chart-success', metricsHistory.successRate, '#34C759', 0, 100);
    drawChart('chart-users', metricsHistory.users, '#C780FF');
    drawChart('chart-rooms', metricsHistory.rooms, '#FF9500');
}

function drawChart(canvasId, data, color, minVal, maxVal) {
    const canvas = document.getElementById(canvasId);
    if (!canvas || data.length < 2) return;
    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    ctx.scale(dpr, dpr);
    const w = rect.width, h = rect.height;
    const padding = { top: 8, right: 12, bottom: 22, left: 40 };
    const plotW = w - padding.left - padding.right;
    const plotH = h - padding.top - padding.bottom;
    ctx.clearRect(0, 0, w, h);
    const min = minVal !== undefined ? minVal : Math.min(...data) * 0.9;
    const max = maxVal !== undefined ? maxVal : Math.max(...data) * 1.1 || 1;
    // 网格线
    ctx.strokeStyle = 'rgba(0,0,0,0.04)';
    ctx.lineWidth = 1;
    for (let i = 0; i <= 4; i++) {
        const y = padding.top + (plotH / 4) * i;
        ctx.beginPath();
        ctx.moveTo(padding.left, y);
        ctx.lineTo(w - padding.right, y);
        ctx.stroke();
        ctx.fillStyle = '#86868b';
        ctx.font = '10px -apple-system, sans-serif';
        ctx.textAlign = 'right';
        ctx.fillText((max - (max - min) * (i / 4)).toFixed(0), padding.left - 8, y + 3);
    }
    // x 轴标签
    ctx.fillStyle = '#86868b';
    ctx.font = '10px -apple-system, sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('-' + (data.length * 2) + 's', padding.left, h - 4);
    ctx.fillText('now', padding.left + plotW, h - 4);
    // 渐变填充
    const gradient = ctx.createLinearGradient(0, padding.top, 0, padding.top + plotH);
    gradient.addColorStop(0, color + '28');
    gradient.addColorStop(1, color + '03');
    // 折线
    ctx.beginPath();
    ctx.strokeStyle = color;
    ctx.lineWidth = 2;
    ctx.lineJoin = 'round';
    ctx.lineCap = 'round';
    data.forEach((val, i) => {
        const x = padding.left + (plotW / (data.length - 1)) * i;
        const y = padding.top + plotH - ((val - min) / (max - min)) * plotH;
        if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
    });
    ctx.stroke();
    // 填充
    ctx.lineTo(padding.left + plotW, padding.top + plotH);
    ctx.lineTo(padding.left, padding.top + plotH);
    ctx.closePath();
    ctx.fillStyle = gradient;
    ctx.fill();
    // 最新值圆点 + 光晕
    const lastVal = data[data.length - 1];
    const dotX = padding.left + plotW;
    const dotY = padding.top + plotH - ((lastVal - min) / (max - min)) * plotH;
    ctx.beginPath();
    ctx.arc(dotX, dotY, 6, 0, Math.PI * 2);
    ctx.fillStyle = color + '20';
    ctx.fill();
    ctx.beginPath();
    ctx.arc(dotX, dotY, 3.5, 0, Math.PI * 2);
    ctx.fillStyle = color;
    ctx.fill();
    ctx.strokeStyle = '#fff';
    ctx.lineWidth = 1.5;
    ctx.stroke();
}
