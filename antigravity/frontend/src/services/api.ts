// Basic fetch wrapper
const API_BASE_URL = '/api'; // Vite proxy will handle localhost:8080

export const api = {
    // System
    getKillSwitchStatus: async (): Promise<boolean> => {
        const res = await fetch(`${API_BASE_URL}/system/kill-switch`);
        return res.json();
    },
    toggleKillSwitch: async (active: boolean): Promise<string> => {
        const res = await fetch(`${API_BASE_URL}/system/kill-switch?active=${active}`, {
            method: 'POST',
        });
        return res.text();
    },

    // Backtest
    async getStrategies() {
        const res = await fetch('/api/strategies');
        return res.json();
    },

    async searchStocks(query: string) {
        const res = await fetch(`/api/stocks/search?query=${encodeURIComponent(query)}`);
        return res.json();
    },

    async runBacktest(symbol: string, start: string, end: string, strategyId?: string, params?: string) {
        const paramsObj = new URLSearchParams({ symbol, start, end });
        if (strategyId) paramsObj.append('strategyId', strategyId);
        if (params) paramsObj.append('params', params);

        const res = await fetch(`/api/backtest?${paramsObj.toString()}`, {
            method: 'POST'
        });
        if (!res.ok) throw new Error('Backtest failed');
        return res.json();
    },

    // Candles
    getCandles: async (symbol: string, type: string = 'daily'): Promise<any[]> => {
        const res = await fetch(`${API_BASE_URL}/candles?symbol=${symbol}&type=${type}`);
        return res.json();
    },

    // Account
    getBalance: async (): Promise<any> => {
        const res = await fetch(`${API_BASE_URL}/account/balance`);
        return res.json();
    },
    getHoldings: async (): Promise<any[]> => {
        const res = await fetch(`${API_BASE_URL}/account/holdings`);
        return res.json();
    },

    // Targets
    getTargets: async (): Promise<any[]> => {
        const res = await fetch(`${API_BASE_URL}/targets`);
        return res.json();
    }
};
