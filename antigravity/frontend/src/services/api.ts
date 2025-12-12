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
    runBacktest: async (symbol: string, start: string, end: string): Promise<any> => {
        const res = await fetch(`${API_BASE_URL}/backtest?symbol=${symbol}&start=${start}&end=${end}`, {
            method: 'POST',
        });
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
