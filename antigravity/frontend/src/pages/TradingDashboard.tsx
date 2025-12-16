import { useEffect, useState } from 'react';
import { Power, Settings as SettingsIcon, Wallet, Menu, X } from 'lucide-react';
import { api } from '../services/api';
import { StockChart } from '../components/StockChart';
import { StockAutocomplete } from '../components/StockAutocomplete';
import { TodayTargetPanel } from '../components/TodayTargetPanel';
import { BacktestSidebar, BacktestParams } from '../components/BacktestSidebar';
import { AccountModal } from '../components/AccountModal';
import { OrderFormModal } from '../components/OrderFormModal';

export function TradingDashboard() {
    const [systemActive, setSystemActive] = useState<boolean | null>(null);
    const [candles, setCandles] = useState<any[]>([]);
    const [balance, setBalance] = useState<any>({ totalEvaluation: '0', deposit: '0' });
    const [holdings, setHoldings] = useState<any[]>([]);
    const [targets, setTargets] = useState<any[]>([]);
    const [selectedSymbol, setSelectedSymbol] = useState('005930');
    const [selectedStockName, setSelectedStockName] = useState('삼성전자');
    const [searchInput, setSearchInput] = useState('005930'); // 검색창 입력용 별도 state
    const [timeframe, setTimeframe] = useState('daily');

    // UI States
    const [showBacktestSidebar, setShowBacktestSidebar] = useState(false);
    const [showAccountModal, setShowAccountModal] = useState(false);
    const [showOrderModal, setShowOrderModal] = useState(false);
    const [showBacktestResult, setShowBacktestResult] = useState(false);
    const [backtestResult, setBacktestResult] = useState<any>(null);
    const [isRunningBacktest, setIsRunningBacktest] = useState(false);

    // 실시간 갱신이 필요한 데이터만 주기적으로 호출
    useEffect(() => {
        fetchRealtimeData();
        const interval = setInterval(fetchRealtimeData, 5000);
        return () => clearInterval(interval);
    }, []);

    // 초기 로딩 시에만 필요한 데이터
    useEffect(() => {
        fetchStaticData();
    }, []);

    useEffect(() => {
        let isCancelled = false;

        const fetchChart = async () => {
            try {
                const data = await api.getCandles(selectedSymbol, timeframe);

                // 요청 도중 다른 종목으로 변경되었으면 무시
                if (isCancelled) return;

                const chartData = data.map((c: any) => ({
                    time: c.time.split('T')[0],
                    open: c.open,
                    high: c.high,
                    low: c.low,
                    close: c.close
                }));
                setCandles(chartData);
            } catch (e) {
                if (isCancelled) return;
                console.error("Failed to fetch chart", e);
                setCandles([]);
            }
        };

        fetchChart();

        return () => {
            isCancelled = true;
        };
    }, [selectedSymbol, timeframe]);

    // 실시간 갱신이 필요한 데이터 (킬스위치, 오늘의 타겟)
    const fetchRealtimeData = async () => {
        try {
            const status = await api.getKillSwitchStatus();
            setSystemActive(status);

            const tgts = await api.getTargets();
            // Transform targets to include mock data for now
            const transformedTargets = tgts.map((t: any) => ({
                ...t,
                reason: t.reason || 'AI 분석 결과 골든크로스 패턴 감지',
                changePercent: t.changePercent || Math.random() * 10 - 5,
                price: t.price || Math.floor(Math.random() * 100000),
                signal: t.signal || '매수'
            }));
            setTargets(transformedTargets);
        } catch (e) {
            console.error("Failed to fetch realtime data", e);
        }
    };

    // 한번만 로딩하면 되는 데이터 (잔고, 보유종목 - 계좌 모달 열 때만 갱신)
    const fetchStaticData = async () => {
        try {
            const bal = await api.getBalance();
            setBalance(bal);

            const hld = await api.getHoldings();
            setHoldings(hld);
        } catch (e) {
            console.error("Failed to fetch static data", e);
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

    const handleStockSelect = (stock: any) => {
        // 종목 변경 시 차트 데이터를 먼저 비움 (이전 데이터 잔상 방지)
        setCandles([]);
        setBacktestResult(null);
        setShowBacktestResult(false);
        setSelectedSymbol(stock.code);
        setSelectedStockName(stock.name);
        setSearchInput(stock.code); // 검색창에도 선택된 종목코드 반영
    };

    const handleTargetSelect = (symbol: string) => {
        // 종목 변경 시 차트 데이터를 먼저 비움 (이전 데이터 잔상 방지)
        setCandles([]);
        setBacktestResult(null);
        setShowBacktestResult(false);
        const target = targets.find(t => t.symbol === symbol);
        setSelectedSymbol(symbol);
        setSearchInput(symbol); // 검색창에도 반영
        if (target) {
            setSelectedStockName(target.name);
        }
    };

    const handleRunBacktest = async (params: BacktestParams) => {
        try {
            setIsRunningBacktest(true);

            console.log('Running backtest with params:', params);

            const result = await api.runBacktest(
                params.symbol,
                params.startDate,
                params.endDate,
                params.strategyId,
                params.params
            );

            setBacktestResult(result);
            setShowBacktestResult(true);
        } catch (e) {
            console.error('Backtest failed:', e);
            alert('백테스트 실행 실패: ' + (e as Error).message);
        } finally {
            setIsRunningBacktest(false);
        }
    };

    return (
        <div className="min-h-screen bg-slate-950 flex flex-col">
            {/* Top Navigation Bar (TradingView style) */}
            <header className="bg-slate-900 border-b border-slate-700 px-4 py-2 flex items-center justify-between sticky top-0 z-30">
                {/* Left: Logo + Stock Search */}
                <div className="flex items-center gap-4 flex-1">
                    <div className="flex items-center gap-2">
                        <Menu size={20} className="text-slate-400" />
                        <h1 className="text-lg font-bold bg-gradient-to-r from-blue-400 to-indigo-500 bg-clip-text text-transparent">
                            AntiGravity
                        </h1>
                    </div>

                    {/* Stock Search */}
                    <div className="w-80">
                        <StockAutocomplete
                            value={searchInput}
                            onChange={setSearchInput}
                            onSelect={handleStockSelect}
                            placeholder="종목 검색..."
                        />
                    </div>

                    <div className="text-sm text-slate-400">
                        {selectedStockName} ({selectedSymbol})
                    </div>
                </div>

                {/* Right: Controls */}
                <div className="flex items-center gap-3">
                    {/* Manual Order Button */}
                    <button
                        onClick={() => setShowOrderModal(true)}
                        className="px-4 py-1.5 bg-blue-600 hover:bg-blue-500 text-white font-bold rounded-lg shadow-lg shadow-blue-500/20 transition-all hover:scale-105 active:scale-95"
                    >
                        주문하기
                    </button>

                    {/* Account Button */}
                    <button
                        onClick={() => {
                            fetchStaticData(); // 계좌 정보 갱신
                            setShowAccountModal(true);
                        }}
                        className="flex items-center gap-2 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 border border-slate-600 rounded-lg transition-colors"
                    >
                        <Wallet size={16} className="text-slate-400" />
                        <span className="text-sm text-slate-300">내 계좌</span>
                    </button>

                    {/* Kill Switch */}
                    <button
                        onClick={toggleKillSwitch}
                        className={`flex items-center gap-2 px-3 py-1.5 border rounded-lg transition-all ${
                            systemActive === false
                                ? 'bg-red-500/10 border-red-500 text-red-400'
                                : 'bg-emerald-500/10 border-emerald-500 text-emerald-400'
                        }`}
                    >
                        <Power size={16} />
                        <span className="text-sm font-medium">
                            {systemActive === null ? '로딩중' : (systemActive ? 'ON' : 'OFF')}
                        </span>
                    </button>
                </div>
            </header>

            {/* Main Content Area */}
            <main className="flex-1 relative overflow-hidden">
                <div className="absolute inset-0 p-4">
                    {/* Chart - Full Size */}
                    {candles.length > 0 ? (
                        <StockChart
                            data={candles}
                            onTimeframeChange={setTimeframe}
                            markers={showBacktestResult && backtestResult ? backtestResult.trades.map((t: any) => ({
                                time: t.time,
                                type: t.type,
                                text: t.type
                            })) : undefined}
                        />
                    ) : (
                        <div className="h-full flex items-center justify-center text-slate-500 bg-slate-800/50 rounded-lg border border-slate-700">
                            <div className="text-center">
                                <div className="text-lg mb-2">데이터 로딩중...</div>
                                <div className="text-sm text-slate-600">
                                    {selectedSymbol} 차트를 불러오는 중입니다
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Floating Today's Target Panel */}
                    {!showBacktestSidebar && !showBacktestResult && (
                        <div className="absolute bottom-6 right-6 w-80 z-20">
                            <TodayTargetPanel
                                targets={targets}
                                selectedSymbol={selectedSymbol}
                                onSelectSymbol={handleTargetSelect}
                            />
                        </div>
                    )}

                    {/* Floating Backtest Button */}
                    {!showBacktestSidebar && !showBacktestResult && (
                        <div className="absolute bottom-6 right-6 z-20">
                            <button
                                onClick={() => setShowBacktestSidebar(true)}
                                disabled={isRunningBacktest}
                                className={`flex items-center gap-2 px-4 py-2 text-white font-semibold rounded-lg shadow-lg transition-all ${
                                    isRunningBacktest
                                        ? 'bg-gray-500 cursor-not-allowed'
                                        : 'bg-blue-500 hover:bg-blue-600'
                                }`}
                            >
                                <SettingsIcon size={18} className={isRunningBacktest ? 'animate-spin' : ''} />
                                {isRunningBacktest ? '백테스트 실행중...' : '백테스트'}
                            </button>
                        </div>
                    )}

                    {/* Backtest Result Panel - Floating from bottom-right */}
                    {showBacktestResult && backtestResult && (
                        <div className="absolute bottom-6 right-6 w-[600px] max-h-[70vh] bg-slate-900/95 backdrop-blur-sm rounded-xl border border-slate-700 shadow-2xl z-30 flex flex-col">
                            {/* Header */}
                            <div className="flex items-center justify-between p-4 border-b border-slate-700">
                                <h3 className="text-lg font-bold text-white">백테스트 결과</h3>
                                <button
                                    onClick={() => setShowBacktestResult(false)}
                                    className="p-1.5 hover:bg-slate-700 rounded transition-colors"
                                >
                                    <X size={18} className="text-slate-400" />
                                </button>
                            </div>

                            {/* Content - Scrollable */}
                            <div className="flex-1 overflow-y-auto p-4">
                                {/* Summary Stats */}
                                <div className="grid grid-cols-2 gap-3 mb-4">
                                    <div className="bg-slate-800/80 rounded-lg p-3 border border-slate-700">
                                        <div className="text-xs text-slate-400 mb-1">최종 잔고</div>
                                        <div className="text-lg font-bold text-white">
                                            {backtestResult.finalBalance.toLocaleString()}원
                                        </div>
                                    </div>
                                    <div className="bg-slate-800/80 rounded-lg p-3 border border-slate-700">
                                        <div className="text-xs text-slate-400 mb-1">수익률</div>
                                        <div className={`text-lg font-bold ${
                                            backtestResult.totalReturnPercent >= 0 ? 'text-red-400' : 'text-blue-400'
                                        }`}>
                                            {backtestResult.totalReturnPercent >= 0 ? '+' : ''}
                                            {backtestResult.totalReturnPercent.toFixed(2)}%
                                        </div>
                                    </div>
                                    <div className="bg-slate-800/80 rounded-lg p-3 border border-slate-700">
                                        <div className="text-xs text-slate-400 mb-1">거래 횟수</div>
                                        <div className="text-lg font-bold text-white">
                                            {backtestResult.totalTrades}건
                                        </div>
                                    </div>
                                    <div className="bg-slate-800/80 rounded-lg p-3 border border-slate-700">
                                        <div className="text-xs text-slate-400 mb-1">승률</div>
                                        <div className="text-lg font-bold text-white">
                                            {(() => {
                                                const sellTrades = backtestResult.trades.filter((t: any) => t.type === 'SELL');
                                                const winTrades = sellTrades.filter((t: any) => t.pnlPercent > 0);
                                                return sellTrades.length > 0
                                                    ? ((winTrades.length / sellTrades.length) * 100).toFixed(1)
                                                    : '0.0';
                                            })()}%
                                        </div>
                                    </div>
                                </div>

                                {/* Trades List */}
                                <div>
                                    <h4 className="text-sm font-semibold text-slate-300 mb-2">거래 내역</h4>
                                    <div className="space-y-2">
                                        {backtestResult.trades.map((trade: any, idx: number) => (
                                            <div key={idx} className="bg-slate-800/50 rounded-lg p-3 border border-slate-700/50">
                                                <div className="flex items-center justify-between mb-2">
                                                    <div className="flex items-center gap-2">
                                                        <span className={`px-2 py-0.5 rounded text-xs font-bold ${
                                                            trade.type === 'BUY'
                                                                ? 'bg-red-500/20 text-red-400'
                                                                : 'bg-blue-500/20 text-blue-400'
                                                        }`}>
                                                            {trade.type}
                                                        </span>
                                                        <span className="text-sm text-slate-300">
                                                            {new Date(trade.time).toLocaleDateString('ko-KR')}
                                                        </span>
                                                    </div>
                                                    {trade.type === 'SELL' && trade.pnlPercent !== 0 && (
                                                        <span className={`text-sm font-bold ${
                                                            trade.pnlPercent > 0 ? 'text-red-400' : 'text-blue-400'
                                                        }`}>
                                                            {trade.pnlPercent > 0 ? '+' : ''}{trade.pnlPercent.toFixed(2)}%
                                                        </span>
                                                    )}
                                                </div>
                                                <div className="text-xs text-slate-400 font-mono mb-1">
                                                    {trade.price.toLocaleString()}원 × {trade.quantity.toLocaleString()}주
                                                </div>
                                                {trade.reason && (
                                                    <div className="text-xs text-slate-500 italic">
                                                        💡 {trade.reason}
                                                    </div>
                                                )}
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </main>

            {/* Backtest Sidebar */}
            <BacktestSidebar
                isOpen={showBacktestSidebar}
                onClose={() => setShowBacktestSidebar(false)}
                onRunBacktest={handleRunBacktest}
                initialSymbol={selectedSymbol}
            />

            {/* Account Modal */}
            <AccountModal
                isOpen={showAccountModal}
                onClose={() => setShowAccountModal(false)}
                balance={balance}
                holdings={holdings}
            />

            {/* Manual Order Modal */}
            <OrderFormModal
                isOpen={showOrderModal}
                onClose={() => setShowOrderModal(false)}
                initialSymbol={selectedSymbol}
                initialName={selectedStockName}
            />
        </div>
    );
}
