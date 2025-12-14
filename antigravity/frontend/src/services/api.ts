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
        // Convert YYYY-MM-DD to ISO DateTime format (YYYY-MM-DDTHH:mm:ss)
        const startDateTime = `${start}T00:00:00`;
        const endDateTime = `${end}T23:59:59`;

        const paramsObj = new URLSearchParams({
            symbol,
            start: startDateTime,
            end: endDateTime
        });
        if (strategyId) paramsObj.append('strategyId', strategyId);
        if (params) paramsObj.append('params', params);

        const res = await fetch(`/api/backtest?${paramsObj.toString()}`, {
            method: 'POST'
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(`Backtest failed: ${errorText}`);
        }
        return res.json();
    },

    // Candles
    getCandles: async (symbol: string, type: string = 'daily'): Promise<any[]> => {
        const res = await fetch(`${API_BASE_URL}/candles?symbol=${symbol}&type=${type}`);
        return res.json();
    },
    getDataRange: async (symbol: string): Promise<any> => {
        const res = await fetch(`${API_BASE_URL}/candles/data-range?symbol=${symbol}`);
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
    },

    // Scheduler Management
    getSchedulerStatus: async (): Promise<any> => {
        const res = await fetch(`${API_BASE_URL}/scheduler/status`);
        return res.json();
    },
    getSchedulerHistory: async (limit: number = 50): Promise<any[]> => {
        const res = await fetch(`${API_BASE_URL}/scheduler/history?limit=${limit}`);
        return res.json();
    },
    getSchedulerConfig: async (): Promise<any> => {
        const res = await fetch(`${API_BASE_URL}/scheduler/config`);
        return res.json();
    },
    triggerDataCollection: async (days: number = 100, all: boolean = false): Promise<any> => {
        const res = await fetch(`${API_BASE_URL}/data-pipeline/collect?days=${days}&all=${all}`, {
            method: 'POST'
        });
        return res.json();
    },

    // Scheduled Stocks Management
    getScheduledStocks: async (): Promise<any[]> => {
        const res = await fetch(`${API_BASE_URL}/scheduled-stocks`);
        return res.json();
    },
    getActiveScheduledStocks: async (): Promise<any[]> => {
        const res = await fetch(`${API_BASE_URL}/scheduled-stocks/active`);
        return res.json();
    },
    addScheduledStock: async (symbol: string, name: string, note?: string): Promise<any> => {
        const res = await fetch(`${API_BASE_URL}/scheduled-stocks`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ symbol, name, enabled: true, note })
        });
        return res.json();
    },
    toggleScheduledStock: async (id: number): Promise<any> => {
        const res = await fetch(`${API_BASE_URL}/scheduled-stocks/${id}/toggle`, {
            method: 'POST'
        });
        return res.json();
    },
    deleteScheduledStock: async (id: number): Promise<any> => {
        const res = await fetch(`${API_BASE_URL}/scheduled-stocks/${id}`, {
            method: 'DELETE'
        });
        return res.json();
    },

    // Stock Master Sync
    syncStockMaster: async (): Promise<any> => {
        const res = await fetch(`${API_BASE_URL}/scheduler/sync-stock-master`, {
            method: 'POST'
        });
        return res.json();
    },

    // Stock Master Management
    getStockMasters: async (params: {
        page?: number;
        size?: number;
        query?: string;
        market?: string;
        favoriteOnly?: boolean;
        sortBy?: string;
        sortOrder?: string;
    } = {}): Promise<any> => {
        const queryParams = new URLSearchParams();
        if (params.page !== undefined) queryParams.append('page', params.page.toString());
        if (params.size !== undefined) queryParams.append('size', params.size.toString());
        if (params.query) queryParams.append('query', params.query);
        if (params.market) queryParams.append('market', params.market);
        if (params.favoriteOnly) queryParams.append('favoriteOnly', 'true');
        if (params.sortBy) queryParams.append('sortBy', params.sortBy);
        if (params.sortOrder) queryParams.append('sortOrder', params.sortOrder);

        const res = await fetch(`${API_BASE_URL}/stock-master?${queryParams.toString()}`);
        return res.json();
    },
    getStockMaster: async (code: string): Promise<any> => {
        const res = await fetch(`${API_BASE_URL}/stock-master/${code}`);
        return res.json();
    },
    toggleStockFavorite: async (code: string): Promise<any> => {
        const res = await fetch(`${API_BASE_URL}/stock-master/${code}/favorite`, {
            method: 'POST'
        });
        return res.json();
    },
    getFavoriteStocks: async (): Promise<any[]> => {
        const res = await fetch(`${API_BASE_URL}/stock-master/favorites`);
        return res.json();
    },
    getStockMasterStats: async (): Promise<any> => {
        const res = await fetch(`${API_BASE_URL}/stock-master/stats`);
        return res.json();
    },
    deleteStockMaster: async (code: string): Promise<any> => {
        const res = await fetch(`${API_BASE_URL}/stock-master/${code}`, {
            method: 'DELETE'
        });
        return res.json();
    }
};
