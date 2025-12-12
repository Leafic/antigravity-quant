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
    getCandles: async (symbol: string): Promise<any[]> => {
        const res = await fetch(`${API_BASE_URL}/candles?symbol=${symbol}`);
        return res.json();
    }
};
