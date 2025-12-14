import { TrendingUp, TrendingDown, Target } from 'lucide-react';

interface TargetStock {
    symbol: string;
    name: string;
    reason: string;
    changePercent: number;
    price: number;
    signal: string;
}

interface Props {
    targets: TargetStock[];
    selectedSymbol: string;
    onSelectSymbol: (symbol: string) => void;
}

export function TodayTargetPanel({ targets, selectedSymbol, onSelectSymbol }: Props) {
    if (targets.length === 0) {
        return (
            <div className="bg-slate-800/80 backdrop-blur-sm rounded-lg p-4 border border-slate-700/50 shadow-xl">
                <div className="flex items-center gap-2 mb-3">
                    <Target size={18} className="text-yellow-400" />
                    <h3 className="text-sm font-semibold text-slate-200">오늘의 종목</h3>
                </div>
                <div className="text-xs text-slate-500 text-center py-4">
                    추천 종목이 없습니다
                </div>
            </div>
        );
    }

    return (
        <div className="bg-slate-800/80 backdrop-blur-sm rounded-lg p-4 border border-slate-700/50 shadow-xl max-h-[400px] overflow-y-auto">
            <div className="flex items-center gap-2 mb-3">
                <Target size={18} className="text-yellow-400" />
                <h3 className="text-sm font-semibold text-slate-200">오늘의 종목</h3>
                <span className="text-xs text-slate-500">({targets.length})</span>
            </div>

            <div className="space-y-2">
                {targets.map((target) => (
                    <div
                        key={target.symbol}
                        onClick={() => onSelectSymbol(target.symbol)}
                        className={`p-3 rounded-lg cursor-pointer transition-all border ${
                            selectedSymbol === target.symbol
                                ? 'bg-blue-500/20 border-blue-500/50'
                                : 'bg-slate-700/50 border-transparent hover:bg-slate-700 hover:border-slate-600'
                        }`}
                    >
                        <div className="flex items-start justify-between mb-2">
                            <div className="flex-1">
                                <div className="font-medium text-sm text-white">{target.name}</div>
                                <div className="text-xs text-slate-400 font-mono">{target.symbol}</div>
                            </div>
                            <div className="text-right">
                                <div className={`flex items-center gap-1 text-sm font-bold ${
                                    target.changePercent >= 0 ? 'text-red-400' : 'text-blue-400'
                                }`}>
                                    {target.changePercent >= 0 ? (
                                        <TrendingUp size={14} />
                                    ) : (
                                        <TrendingDown size={14} />
                                    )}
                                    {target.changePercent >= 0 ? '+' : ''}{target.changePercent.toFixed(2)}%
                                </div>
                                <div className="text-xs text-slate-400 font-mono">
                                    {target.price.toLocaleString()}원
                                </div>
                            </div>
                        </div>

                        <div className="text-xs text-slate-300 mb-1">
                            <span className="text-slate-500">시그널:</span> {target.signal}
                        </div>

                        <div className="text-xs text-slate-400 line-clamp-2">
                            {target.reason}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
