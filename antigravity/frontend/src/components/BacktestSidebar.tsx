import { useState, useEffect } from 'react';
import { X, Play, Settings, Calendar, Sliders } from 'lucide-react';
import { api } from '../services/api';

interface Props {
    isOpen: boolean;
    onClose: () => void;
    onRunBacktest: (params: BacktestParams) => void;
    initialSymbol?: string;
}

export interface BacktestParams {
    symbol: string;
    strategyId: string;
    startDate: string;
    endDate: string;
    params?: string;
}

interface Strategy {
    id: string;
    name: string;
    description: string;
    defaultParams: string;
}

export function BacktestSidebar({ isOpen, onClose, onRunBacktest, initialSymbol }: Props) {
    const [symbol, setSymbol] = useState('005930');
    const [strategyId, setStrategyId] = useState('S1');
    const [startDate, setStartDate] = useState('2023-01-01');
    const [endDate, setEndDate] = useState('2023-12-31');
    const [strategies, setStrategies] = useState<Strategy[]>([]);
    const [strategyParams, setStrategyParams] = useState('');
    const [showParamsEditor, setShowParamsEditor] = useState(false);
    const [dataRange, setDataRange] = useState<any>(null);
    const [checkingData, setCheckingData] = useState(false);

    // 사이드바가 열릴 때 현재 차트의 종목으로 설정
    useEffect(() => {
        if (isOpen) {
            if (initialSymbol) {
                setSymbol(initialSymbol);
            }
            fetchStrategies();
        }
    }, [isOpen, initialSymbol]);

    useEffect(() => {
        if (symbol) {
            checkDataAvailability(symbol);
        }
    }, [symbol]);

    const fetchStrategies = async () => {
        try {
            const data = await api.getStrategies();
            setStrategies(data);
            if (data.length > 0) {
                setStrategyParams(data[0].defaultParams || '');
            }
        } catch (e) {
            console.error('Failed to fetch strategies:', e);
        }
    };

    const checkDataAvailability = async (sym: string) => {
        if (!sym) return;

        setCheckingData(true);
        try {
            const range = await api.getDataRange(sym);
            setDataRange(range);

            // 데이터가 있으면 자동으로 날짜 범위 설정
            if (range.hasData) {
                setStartDate(range.minDate);
                setEndDate(range.maxDate);
            }
        } catch (e) {
            console.error('Failed to check data range:', e);
            setDataRange(null);
        } finally {
            setCheckingData(false);
        }
    };

    const handleStrategyChange = (id: string) => {
        setStrategyId(id);
        const strategy = strategies.find(s => s.id === id);
        if (strategy) {
            setStrategyParams(strategy.defaultParams || '');
        }
    };

    const handleRun = () => {
        onRunBacktest({
            symbol,
            strategyId,
            startDate,
            endDate,
            params: strategyParams
        });
    };

    if (!isOpen) return null;

    return (
        <div className="fixed bottom-6 right-6 w-96 max-h-[80vh] bg-slate-900/95 backdrop-blur-sm rounded-xl border border-slate-700 shadow-2xl z-40 flex flex-col">
                <div className="flex-1 overflow-y-auto p-6">
                    {/* Header */}
                    <div className="flex items-center justify-between mb-6">
                        <div className="flex items-center gap-2">
                            <Settings size={20} className="text-blue-400" />
                            <h2 className="text-xl font-bold text-white">백테스트 설정</h2>
                        </div>
                        <button
                            onClick={onClose}
                            className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
                        >
                            <X size={20} className="text-slate-400" />
                        </button>
                    </div>

                    {/* Form */}
                    <div className="space-y-4">
                        {/* Symbol (Read-only) */}
                        <div>
                            <label className="block text-sm font-medium text-slate-300 mb-2">
                                종목 코드 (현재 차트)
                            </label>
                            <div className="w-full px-3 py-2 bg-slate-700/50 border border-slate-600 rounded-lg text-slate-300 font-mono">
                                {symbol}
                            </div>
                            {checkingData && (
                                <div className="mt-2 text-xs text-slate-400">
                                    데이터 확인 중...
                                </div>
                            )}
                            {!checkingData && dataRange && (
                                <div className="mt-2">
                                    {dataRange.hasData ? (
                                        <div className="text-xs text-emerald-400">
                                            ✓ 데이터 보유: {dataRange.minDate} ~ {dataRange.maxDate} ({dataRange.totalDays}일)
                                        </div>
                                    ) : (
                                        <div className="text-xs text-red-400">
                                            ✗ 데이터 없음. 먼저 데이터 수집을 실행하세요.
                                        </div>
                                    )}
                                </div>
                            )}
                            <div className="mt-2 text-xs text-slate-500">
                                💡 차트에서 다른 종목을 선택하면 자동으로 변경됩니다
                            </div>
                        </div>

                        {/* Strategy */}
                        <div>
                            <label className="block text-sm font-medium text-slate-300 mb-2">
                                전략 선택
                            </label>
                            <select
                                value={strategyId}
                                onChange={(e) => handleStrategyChange(e.target.value)}
                                className="w-full px-3 py-2 bg-slate-800 border border-slate-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                            >
                                {strategies.length === 0 ? (
                                    <option value="">로딩중...</option>
                                ) : (
                                    strategies.map((strategy) => (
                                        <option key={strategy.id} value={strategy.id}>
                                            {strategy.name}
                                        </option>
                                    ))
                                )}
                            </select>
                            {strategies.find(s => s.id === strategyId) && (
                                <div className="mt-2 text-xs text-slate-400">
                                    {strategies.find(s => s.id === strategyId)?.description}
                                </div>
                            )}
                        </div>

                        {/* Strategy Parameters */}
                        <div className="border border-slate-700 rounded-lg p-4">
                            <div
                                className="flex items-center justify-between cursor-pointer mb-3"
                                onClick={() => setShowParamsEditor(!showParamsEditor)}
                            >
                                <div className="flex items-center gap-2">
                                    <Sliders size={16} className="text-slate-400" />
                                    <span className="text-sm font-medium text-slate-300">전략 파라미터</span>
                                </div>
                                <button className="text-xs text-blue-400 hover:text-blue-300">
                                    {showParamsEditor ? '접기' : '편집'}
                                </button>
                            </div>

                            {showParamsEditor && (
                                <div>
                                    <textarea
                                        value={strategyParams}
                                        onChange={(e) => setStrategyParams(e.target.value)}
                                        className="w-full px-3 py-2 bg-slate-800 border border-slate-600 rounded text-white text-xs font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        rows={4}
                                        placeholder='{"param1": "value1"}'
                                    />
                                    <div className="mt-2 text-xs text-slate-500">
                                        JSON 형식으로 전략 파라미터를 설정하세요
                                    </div>
                                </div>
                            )}
                        </div>

                        {/* Date Range */}
                        <div className="border border-slate-700 rounded-lg p-4">
                            <div className="flex items-center gap-2 mb-3">
                                <Calendar size={16} className="text-slate-400" />
                                <span className="text-sm font-medium text-slate-300">기간 설정</span>
                            </div>

                            <div className="space-y-3">
                                <div>
                                    <label className="block text-xs text-slate-400 mb-1">시작일</label>
                                    <input
                                        type="date"
                                        value={startDate}
                                        onChange={(e) => setStartDate(e.target.value)}
                                        className="w-full px-3 py-2 bg-slate-800 border border-slate-600 rounded text-white text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>

                                <div>
                                    <label className="block text-xs text-slate-400 mb-1">종료일</label>
                                    <input
                                        type="date"
                                        value={endDate}
                                        onChange={(e) => setEndDate(e.target.value)}
                                        className="w-full px-3 py-2 bg-slate-800 border border-slate-600 rounded text-white text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>
                            </div>

                            <div className="mt-3 text-xs text-slate-500">
                                💡 차트에서 직접 기간을 선택할 수도 있습니다
                            </div>
                        </div>

                        {/* Run Button */}
                        <button
                            onClick={handleRun}
                            className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-blue-500 hover:bg-blue-600 text-white font-semibold rounded-lg transition-colors"
                        >
                            <Play size={18} />
                            백테스트 시작
                        </button>
                    </div>

                    {/* Info */}
                    <div className="mt-6 p-4 bg-slate-800/50 border border-slate-700 rounded-lg">
                        <h3 className="text-sm font-medium text-slate-300 mb-2">안내</h3>
                        <ul className="text-xs text-slate-400 space-y-1">
                            <li>• 백테스트는 과거 데이터를 기반으로 실행됩니다</li>
                            <li>• 결과는 실제 수익을 보장하지 않습니다</li>
                            <li>• 전략별 파라미터는 자동으로 최적화됩니다</li>
                        </ul>
                    </div>
                </div>
            </div>
    );
}
