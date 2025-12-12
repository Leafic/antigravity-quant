import { useState, useEffect } from 'react';
import { Power } from 'lucide-react';
import { api } from './services/api';
import { BacktestPanel } from './components/BacktestPanel';
import { StockChart } from './components/StockChart';

function App() {
    const [systemActive, setSystemActive] = useState<boolean | null>(null);
    const [candles, setCandles] = useState<any[]>([]);

    useEffect(() => {
        // Initial fetch
        fetchSystemStatus();
        fetchChartData();

        // Polling every 5 seconds
        const interval = setInterval(() => {
            fetchSystemStatus();
        }, 5000);

        return () => clearInterval(interval);
    }, []);

    const fetchSystemStatus = async () => {
        try {
            const status = await api.getKillSwitchStatus();
            setSystemActive(status);
        } catch (e) {
            console.error("Failed to fetch system status", e);
        }
    };

    const fetchChartData = async () => {
        try {
            // Mock data loading or real data if available
            // For now, we load a default symbol
            const data = await api.getCandles('005930');
            // Transform for lightweight-charts
            const chartData = data.map((c: any) => ({
                time: c.time.split('T')[0], // Simplified for daily/mock
                open: c.open,
                high: c.high,
                low: c.low,
                close: c.close
            }));
            setCandles(chartData);
        } catch (e) {
            console.error("Failed to fetch candle data", e);
        }
    };

    const toggleKillSwitch = async () => {
        if (systemActive === null) return;
        try {
            await api.toggleKillSwitch(!systemActive);
            setSystemActive(!systemActive);
        } catch (e) {
            console.error("Failed to toggle kill switch", e);
        }
    };

    return (
        <div className="min-h-screen bg-slate-900 text-white p-6">
            <header className="mb-8 flex items-center justify-between border-b border-slate-700 pb-4">
                <div>
                    <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-400 to-indigo-500 bg-clip-text text-transparent">
                        AntiGravity
                    </h1>
                    <p className="text-slate-400 text-sm">Resilient Hybrid Trading System</p>
                </div>
                <div className="flex items-center gap-4">
                    <button
                        onClick={toggleKillSwitch}
                        className={`flex items-center gap-2 px-4 py-2 border rounded-lg transition-all ${systemActive === false
                            ? 'bg-red-500/10 border-red-500 text-red-500 hover:bg-red-500/20'
                            : 'bg-emerald-500/10 border-emerald-500 text-emerald-500 hover:bg-emerald-500/20'
                            }`}
                    >
                        <Power size={20} />
                        <span className="font-semibold">
                            {systemActive === null ? 'LOADING...' : (systemActive ? 'SYSTEM ACTIVE' : 'KILL SWITCH ENGAGED')}
                        </span>
                    </button>
                </div>
            </header>

            <main className="grid grid-cols-1 lg:grid-cols-2 gap-6">

                <div className="space-y-6">
                    {/* Summary Card */}
                    <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
                        <h2 className="text-xl font-semibold mb-4 text-slate-300">Daily Performance</h2>
                        <div className="flex items-end gap-2">
                            <div className="text-4xl font-bold text-emerald-400">+ ₩1,250,000</div>
                            <span className="text-emerald-500 text-sm mb-1">(+1.2%)</span>
                        </div>
                        <div className="text-sm text-slate-500 mt-2">Today's P/L</div>
                    </div>

                    {/* Backtest Panel */}
                    <BacktestPanel />
                </div>

                <div className="space-y-6">
                    {/* Chart Card */}
                    <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
                        <div className="flex justify-between items-center mb-4">
                            <h2 className="text-xl font-semibold text-slate-300">Market Overview (005930)</h2>
                            <span className="text-xs text-slate-500">Real-time (Delayed 15m)</span>
                        </div>
                        {candles.length > 0 ? (
                            <StockChart data={candles} />
                        ) : (
                            <div className="h-[300px] flex items-center justify-center text-slate-500 bg-slate-900/50 rounded-lg">
                                No Data / Loading...
                            </div>
                        )}
                    </div>

                    {/* Active Targets Card */}
                    <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
                        <h2 className="text-xl font-semibold mb-4 text-slate-300">Active Targets</h2>
                        <div className="space-y-3">
                            <div className="flex justify-between items-center p-3 bg-slate-700/50 rounded-lg border-l-4 border-emerald-500">
                                <span className="font-medium">Samsung Elec (005930)</span>
                                <span className="text-emerald-400 font-mono">72,500</span>
                            </div>
                            <div className="flex justify-between items-center p-3 bg-slate-700/50 rounded-lg border-l-4 border-red-500">
                                <span className="font-medium">SK Hynix (000660)</span>
                                <span className="text-red-400 font-mono">132,000</span>
                            </div>
                        </div>
                    </div>
                </div>

            </main>
        </div>
    );
}

export default App;
