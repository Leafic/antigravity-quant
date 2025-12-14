import React, { useState } from 'react';
import { ChevronDown, ChevronUp, Settings } from 'lucide-react';
import { StockSearch } from '../StockSearch';
import { StrategySelect } from '../StrategySelect';

interface BacktestSettingsProps {
    symbol: string;
    setSymbol: (s: string) => void;
    strategyId: string;
    setStrategyId: (s: string) => void;
    params: string;
    setParams: (p: string) => void;
    start: string;
    setStart: (s: string) => void;
    end: string;
    setEnd: (s: string) => void;
    handleStrategySelect: (id: string, defaultParams: string) => void;
}

export const BacktestSettings: React.FC<BacktestSettingsProps> = ({
    symbol, setSymbol,
    strategyId, params, setParams,
    start, setStart,
    end, setEnd,
    handleStrategySelect
}) => {
    const [isOpen, setIsOpen] = useState(true);
    const [showParams, setShowParams] = useState(false);
    const [mode, setMode] = useState<'DAILY' | 'INTRADAY'>('DAILY');

    return (
        <div className="bg-slate-800 rounded-lg border border-slate-700 overflow-hidden mb-4">
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="w-full px-4 py-3 bg-slate-800 flex items-center justify-between hover:bg-slate-750 transition-colors"
            >
                <div className="flex items-center gap-2 font-semibold text-sm text-slate-300">
                    <Settings size={14} />
                    테스트 설정
                </div>
                {isOpen ? <ChevronUp size={14} className="text-slate-500" /> : <ChevronDown size={14} className="text-slate-500" />}
            </button>

            {isOpen && (
                <div className="p-4 space-y-4 border-t border-slate-700 animate-in slide-in-from-top-2 duration-200">
                    <div className="space-y-3">
                        <div className="space-y-1">
                            <label className="text-xs text-slate-500">종목</label>
                            <StockSearch selectedCode={symbol} onSelect={setSymbol} />
                        </div>
                        <div className="space-y-1">
                            <label className="text-xs text-slate-500">모드</label>
                            <div className="flex bg-slate-900 rounded p-1 border border-slate-700">
                                <button
                                    onClick={() => setMode('DAILY')}
                                    className={`flex-1 text-xs py-1 rounded ${mode === 'DAILY' ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-slate-200'}`}
                                >
                                    일봉 스윙 (Daily)
                                </button>
                                <button
                                    onClick={() => setMode('INTRADAY')}
                                    className={`flex-1 text-xs py-1 rounded ${mode === 'INTRADAY' ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-slate-200'}`}
                                >
                                    단타 (Intraday)
                                </button>
                            </div>
                        </div>
                        <div className="space-y-1">
                            <label className="text-xs text-slate-500">전략</label>
                            <StrategySelect selectedId={strategyId} onSelect={handleStrategySelect} mode={mode} />
                        </div>
                    </div>

                    <div>
                        <button
                            onClick={() => setShowParams(!showParams)}
                            className="text-xs text-blue-400 hover:text-blue-300 underline mb-2"
                        >
                            {showParams ? '전략 파라미터 숨기기' : '전략 파라미터 설정'}
                        </button>
                        {showParams && (
                            <textarea
                                value={params}
                                onChange={(e) => setParams(e.target.value)}
                                className="w-full bg-slate-900 border border-slate-600 rounded p-3 text-xs font-mono h-24 focus:outline-none focus:border-blue-500 transition-colors custom-scrollbar"
                            />
                        )}
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="block text-xs text-slate-500 mb-1">시작일</label>
                            <input
                                type="datetime-local"
                                value={start}
                                onChange={(e) => setStart(e.target.value)}
                                className="w-full bg-slate-900 border border-slate-600 rounded p-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                            />
                        </div>
                        <div>
                            <label className="block text-xs text-slate-500 mb-1">종료일</label>
                            <input
                                type="datetime-local"
                                value={end}
                                onChange={(e) => setEnd(e.target.value)}
                                className="w-full bg-slate-900 border border-slate-600 rounded p-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                            />
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};
