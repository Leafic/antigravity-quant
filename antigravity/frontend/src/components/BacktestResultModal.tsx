import { X, TrendingUp, TrendingDown, DollarSign, Activity } from 'lucide-react';

interface Trade {
    time: string;
    type: string;
    price: number;
    quantity: number;
    reason: string;
    pnlPercent: number;
}

interface BacktestResult {
    symbol: string;
    finalBalance: number;
    totalReturnPercent: number;
    totalTrades: number;
    trades: Trade[];
    rejectionStats?: { [key: string]: number };
}

interface Props {
    isOpen: boolean;
    onClose: () => void;
    result: BacktestResult | null;
}

export function BacktestResultModal({ isOpen, onClose, result }: Props) {
    if (!isOpen || !result) return null;

    const initialBalance = 10000000;
    const profit = result.finalBalance - initialBalance;
    const isProfit = profit >= 0;

    const sellTrades = result.trades.filter(t => t.type === 'SELL');
    const winTrades = sellTrades.filter(t => t.pnlPercent > 0);
    const winRate = sellTrades.length > 0 ? (winTrades.length / sellTrades.length * 100) : 0;

    return (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
            <div className="bg-slate-900 rounded-xl max-w-6xl w-full max-h-[90vh] overflow-hidden border border-slate-700 shadow-2xl">
                {/* Header */}
                <div className="flex items-center justify-between p-6 border-b border-slate-700">
                    <div>
                        <h2 className="text-2xl font-bold text-white mb-1">백테스트 결과</h2>
                        <div className="text-sm text-slate-400">
                            {result.symbol} • 총 {result.totalTrades}건의 거래
                        </div>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
                    >
                        <X size={24} className="text-slate-400" />
                    </button>
                </div>

                {/* Summary Stats */}
                <div className="p-6 border-b border-slate-700 bg-slate-800/30">
                    <div className="grid grid-cols-4 gap-4">
                        <div className="bg-slate-800 rounded-lg p-4 border border-slate-700">
                            <div className="flex items-center gap-2 mb-2">
                                <DollarSign size={18} className="text-blue-400" />
                                <span className="text-xs text-slate-400">최종 잔고</span>
                            </div>
                            <div className="text-xl font-bold text-white">
                                {result.finalBalance.toLocaleString()}원
                            </div>
                        </div>

                        <div className="bg-slate-800 rounded-lg p-4 border border-slate-700">
                            <div className="flex items-center gap-2 mb-2">
                                <Activity size={18} className="text-purple-400" />
                                <span className="text-xs text-slate-400">총 수익률</span>
                            </div>
                            <div className={`text-xl font-bold ${isProfit ? 'text-red-400' : 'text-blue-400'}`}>
                                {isProfit ? '+' : ''}{result.totalReturnPercent.toFixed(2)}%
                            </div>
                        </div>

                        <div className="bg-slate-800 rounded-lg p-4 border border-slate-700">
                            <div className="flex items-center gap-2 mb-2">
                                {isProfit ? (
                                    <TrendingUp size={18} className="text-emerald-400" />
                                ) : (
                                    <TrendingDown size={18} className="text-red-400" />
                                )}
                                <span className="text-xs text-slate-400">손익</span>
                            </div>
                            <div className={`text-xl font-bold ${isProfit ? 'text-red-400' : 'text-blue-400'}`}>
                                {isProfit ? '+' : ''}{profit.toLocaleString()}원
                            </div>
                        </div>

                        <div className="bg-slate-800 rounded-lg p-4 border border-slate-700">
                            <div className="flex items-center gap-2 mb-2">
                                <Activity size={18} className="text-yellow-400" />
                                <span className="text-xs text-slate-400">승률</span>
                            </div>
                            <div className="text-xl font-bold text-white">
                                {winRate.toFixed(1)}%
                            </div>
                            <div className="text-xs text-slate-500 mt-1">
                                {winTrades.length}승 / {sellTrades.length}건
                            </div>
                        </div>
                    </div>
                </div>

                {/* Trade List */}
                <div className="p-6 overflow-y-auto max-h-[500px]">
                    <h3 className="text-lg font-bold text-white mb-4">거래 내역</h3>
                    <div className="space-y-2">
                        {result.trades.map((trade, idx) => {
                            const isBuy = trade.type === 'BUY';
                            return (
                                <div
                                    key={idx}
                                    className="bg-slate-800/50 rounded-lg p-4 border border-slate-700 hover:bg-slate-800 transition-colors"
                                >
                                    <div className="flex items-center justify-between">
                                        <div className="flex items-center gap-4">
                                            <div className={`px-3 py-1 rounded-full text-sm font-bold ${
                                                isBuy
                                                    ? 'bg-red-500/20 text-red-400 border border-red-500/50'
                                                    : 'bg-blue-500/20 text-blue-400 border border-blue-500/50'
                                            }`}>
                                                {trade.type}
                                            </div>
                                            <div>
                                                <div className="text-white font-medium">
                                                    {new Date(trade.time).toLocaleDateString('ko-KR', {
                                                        year: 'numeric',
                                                        month: '2-digit',
                                                        day: '2-digit',
                                                        hour: '2-digit',
                                                        minute: '2-digit'
                                                    })}
                                                </div>
                                                <div className="text-xs text-slate-400">
                                                    {trade.reason}
                                                </div>
                                            </div>
                                        </div>

                                        <div className="text-right">
                                            <div className="text-white font-mono">
                                                {trade.price.toLocaleString()}원 × {trade.quantity.toLocaleString()}주
                                            </div>
                                            {!isBuy && trade.pnlPercent !== 0 && (
                                                <div className={`text-sm font-bold ${
                                                    trade.pnlPercent > 0 ? 'text-red-400' : 'text-blue-400'
                                                }`}>
                                                    {trade.pnlPercent > 0 ? '+' : ''}{trade.pnlPercent.toFixed(2)}%
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </div>

                {/* Footer */}
                <div className="p-6 border-t border-slate-700 bg-slate-800/30">
                    <button
                        onClick={onClose}
                        className="w-full px-4 py-3 bg-blue-500 hover:bg-blue-600 text-white font-semibold rounded-lg transition-colors"
                    >
                        닫기
                    </button>
                </div>
            </div>
        </div>
    );
}
