import React, { useState } from 'react';
import { api } from '../services/api';
import { Play, Loader2 } from 'lucide-react';

export const BacktestPanel: React.FC = () => {
    const [symbol, setSymbol] = useState('005930');
    const [start, setStart] = useState('2023-01-01T00:00:00');
    const [end, setEnd] = useState('2023-12-31T23:59:59');
    const [result, setResult] = useState<any>(null);
    const [loading, setLoading] = useState(false);

    const handleRun = async () => {
        setLoading(true);
        try {
            // Append seconds if missing (common with datetime-local)
            const fmtStart = start.length === 16 ? start + ':00' : start;
            const fmtEnd = end.length === 16 ? end + ':59' : end;

            const data = await api.runBacktest(symbol, fmtStart, fmtEnd);
            setResult(data);
        } catch (error) {
            console.error(error);
            alert('백테스트 실패. 콘솔을 확인하세요.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
            <h2 className="text-xl font-semibold mb-4 text-slate-300 flex items-center gap-2">
                <Play size={20} className="text-blue-400" />
                백테스트 시뮬레이터 (Golden Cross)
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
                <div>
                    <label className="block text-xs text-slate-400 mb-1">종목코드 (Symbol)</label>
                    <input
                        type="text"
                        value={symbol}
                        onChange={(e) => setSymbol(e.target.value)}
                        className="w-full bg-slate-900 border border-slate-700 rounded p-2 text-sm"
                    />
                </div>
                <div>
                    <label className="block text-xs text-slate-400 mb-1">시작일 (Start)</label>
                    <input
                        type="datetime-local"
                        value={start}
                        onChange={(e) => setStart(e.target.value)}
                        className="w-full bg-slate-900 border border-slate-700 rounded p-2 text-sm"
                    />
                </div>
                <div>
                    <label className="block text-xs text-slate-400 mb-1">종료일 (End)</label>
                    <input
                        type="datetime-local"
                        value={end}
                        onChange={(e) => setEnd(e.target.value)}
                        className="w-full bg-slate-900 border border-slate-700 rounded p-2 text-sm"
                    />
                </div>
            </div>

            <button
                onClick={handleRun}
                disabled={loading}
                className="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 rounded-lg transition-colors flex items-center justify-center gap-2"
            >
                {loading ? <Loader2 className="animate-spin" size={18} /> : '백테스트 실행'}
            </button>

            {result && (
                <div className="mt-6 p-4 bg-slate-900/50 rounded-lg border border-slate-700">
                    <div className="grid grid-cols-3 gap-4 text-center">
                        <div>
                            <div className="text-xs text-slate-500">총 수익률</div>
                            <div className={`text-xl font-bold ${result.totalReturnPercent >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>
                                {result.totalReturnPercent}%
                            </div>
                        </div>
                        <div>
                            <div className="text-xs text-slate-500">최종 잔고</div>
                            <div className="text-lg font-semibold text-slate-200">
                                {new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(result.finalBalance)}
                            </div>
                        </div>
                        <div>
                            <div className="text-xs text-slate-500">매매 횟수</div>
                            <div className="text-lg font-semibold text-slate-200">
                                {result.totalTrades}
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};
