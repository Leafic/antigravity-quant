import { useEffect, useState } from 'react';
import { Power, RefreshCw } from 'lucide-react';
import { api } from './services/api';
import { BacktestPanel } from './components/BacktestPanel';
import { StockChart } from './components/StockChart';

function App() {
    const [systemActive, setSystemActive] = useState<boolean | null>(null);
    const [candles, setCandles] = useState<any[]>([]);
    const [balance, setBalance] = useState<any>({ totalEvaluation: '0', deposit: '0' });
    const [holdings, setHoldings] = useState<any[]>([]);
    const [targets, setTargets] = useState<any[]>([]);
    const [selectedSymbol, setSelectedSymbol] = useState('006620'); // DongKoo Bio Default
    const [timeframe, setTimeframe] = useState('daily');

    useEffect(() => {
        fetchData();
        const interval = setInterval(fetchData, 5000);
        return () => clearInterval(interval);
    }, []);

    useEffect(() => {
        fetchChartData(selectedSymbol, timeframe);
    }, [selectedSymbol, timeframe]);

    const fetchData = async () => {
        try {
            const status = await api.getKillSwitchStatus();
            setSystemActive(status);

            const bal = await api.getBalance();
            setBalance(bal);

            const hld = await api.getHoldings();
            setHoldings(hld);

            const tgts = await api.getTargets();
            setTargets(tgts);
        } catch (e) {
            console.error("Failed to fetch data", e);
        }
    };

    const fetchChartData = async (symbol: string, tf: string) => {
        try {
            const data = await api.getCandles(symbol, tf);
            const chartData = data.map((c: any) => ({
                time: c.time.split('T')[0],
                open: c.open,
                high: c.high,
                low: c.low,
                close: c.close
            }));
            setCandles(chartData);
        } catch (e) {
            console.error("Failed to fetch chart", e);
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

    const formatCurrency = (val: string | number) => {
        const num = Number(val);
        if (isNaN(num)) return '0 원';
        return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(num);
    };

    return (
        <div className="min-h-screen bg-slate-900 text-white p-6 font-sans">
            <header className="mb-8 flex items-center justify-between border-b border-slate-700 pb-4">
                <div>
                    <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-400 to-indigo-500 bg-clip-text text-transparent">
                        주식매매 시스템
                    </h1>
                    <p className="text-slate-400 text-sm">하이브리드 주식 트레이딩 시스템</p>
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
                            {systemActive === null ? '로딩중...' : (systemActive ? '시스템 정상 작동' : '킬 스위치 발동됨')}
                        </span>
                    </button>
                </div>
            </header>

            <main className="grid grid-cols-1 lg:grid-cols-2 gap-6">

                <div className="space-y-6">
                    {/* Account Summary */}
                    <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
                        <h2 className="text-xl font-semibold mb-4 text-slate-300">내 계좌 현황</h2>
                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <div className="text-sm text-slate-500">총 평가금액</div>
                                <div className="text-2xl font-bold text-emerald-400">{formatCurrency(balance.totalEvaluation)}</div>
                            </div>
                            <div>
                                <div className="text-sm text-slate-500">주문 가능 예수금</div>
                                <div className="text-xl font-medium text-slate-200">{formatCurrency(balance.deposit)}</div>
                            </div>
                        </div>
                    </div>

                    {/* Portfolio Holdings */}
                    <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
                        <h2 className="text-xl font-semibold mb-4 text-slate-300">보유 종목 (Portfolio)</h2>
                        {holdings.length > 0 ? (
                            <div className="space-y-3">
                                {holdings.map((h: any) => (
                                    <div key={h.pdno} className="flex justify-between items-center p-3 bg-slate-700/50 rounded-lg">
                                        <div>
                                            <div className="font-medium">{h.prdt_name} ({h.pdno})</div>
                                            <div className="text-xs text-slate-400">{h.hldg_qty}주 보유</div>
                                        </div>
                                        <div className="text-right">
                                            <div className="font-mono">{formatCurrency(h.prpr)}</div>
                                            <div className={`text-xs ${Number(h.evlu_pfls_rt) >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>
                                                {h.evlu_pfls_rt}% ({formatCurrency(h.evlu_pfls_amt)})
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <div className="text-center text-slate-500 py-4">보유 중인 종목이 없습니다.</div>
                        )}
                    </div>

                    {/* Backtest Panel */}
                    <BacktestPanel />
                </div>

                <div className="space-y-6">
                    {/* Chart Card */}
                    <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
                        <div className="flex justify-between items-center mb-4">
                            <h2 className="text-xl font-semibold text-slate-300">시장 시세 ({selectedSymbol})</h2>
                            <span className="text-xs text-slate-500">실시간 (15분 지연)</span>
                        </div>
                        {candles.length > 0 ? (
                            <StockChart data={candles} onTimeframeChange={setTimeframe} />
                        ) : (
                            <div className="h-[300px] flex items-center justify-center text-slate-500 bg-slate-900/50 rounded-lg">
                                데이터 로딩중... (또는 데이터 없음)
                            </div>
                        )}
                    </div>

                    {/* Active Targets Card */}
                    <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
                        <div className="flex justify-between items-center mb-4">
                            <h2 className="text-xl font-semibold text-slate-300">오늘의 타겟 (Active Targets)</h2>
                            <button onClick={fetchData} className="p-1 hover:bg-slate-700 rounded"><RefreshCw size={16} /></button>
                        </div>
                        <div className="space-y-3">
                            {targets.length > 0 ? (
                                targets.map((t: any) => (
                                    <div
                                        key={t.symbol}
                                        onClick={() => setSelectedSymbol(t.symbol)}
                                        className={`flex justify-between items-center p-3 bg-slate-700/50 rounded-lg cursor-pointer hover:bg-slate-700 border-l-4 transition-all ${selectedSymbol === t.symbol ? 'border-blue-500 bg-slate-700' : 'border-transparent'}`}
                                    >
                                        <span className="font-medium">{t.name} ({t.symbol})</span>
                                        <span className="text-slate-400 text-xs">{t.sector}</span>
                                    </div>
                                ))
                            ) : (
                                <div className="text-center text-slate-500">활성 타겟이 없습니다.</div>
                            )}
                        </div>
                    </div>
                </div>

            </main>
        </div>
    );
}

export default App;
