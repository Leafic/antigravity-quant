import React, { useState } from 'react';
import { api } from '../services/api';
import { Play, Loader2 } from 'lucide-react';
import { StockChart } from './StockChart';

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
                백테스트 시뮬레이터 (유니버스 시나리오 매매)
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
                <div className="mt-6 mb-6 h-[320px] bg-slate-900/50 rounded-lg border border-slate-700 p-4">
                    <h3 className="text-sm font-semibold text-slate-400 mb-2">시각화 (Visualization)</h3>
                    {/* @ts-ignore */}
                    <StockChart
                        data={result.candles}
                        markers={result.trades?.map((t: any) => ({
                            time: t.time,
                            type: t.type,
                            text: t.reason
                        }))}
                    />
                </div>
            )}

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

            {result && result.trades && result.trades.length > 0 && (
                <div className="mt-4">
                    <h3 className="text-sm font-semibold text-slate-400 mb-2">매매 기록 (Timeline)</h3>
                    <div className="bg-slate-900 rounded-lg overflow-hidden border border-slate-700">
                        <table className="w-full text-xs text-left text-slate-300">
                            <thead className="text-xs uppercase bg-slate-800 text-slate-400">
                                <tr>
                                    <th className="px-3 py-2">시간</th>
                                    <th className="px-3 py-2">유형</th>
                                    <th className="px-3 py-2 text-right">가격</th>
                                    <th className="px-3 py-2 text-right">수량</th>
                                    <th className="px-3 py-2 text-center">수익률</th>
                                    <th className="px-3 py-2">이유</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-700">
                                {result.trades.map((trade: any, idx: number) => (
                                    <tr key={idx} className="hover:bg-slate-800/50">
                                        <td className="px-3 py-2 font-mono">
                                            {new Date(trade.time).toLocaleString('ko-KR', {
                                                month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit'
                                            })}
                                        </td>
                                        <td className={`px-3 py-2 font-bold ${trade.type === 'BUY' ? 'text-red-400' : 'text-blue-400'}`}>
                                            {trade.type}
                                        </td>
                                        <td className="px-3 py-2 text-right">
                                            {new Intl.NumberFormat('ko-KR').format(trade.price)}
                                        </td>
                                        <td className="px-3 py-2 text-right">
                                            {trade.quantity}
                                        </td>
                                        <td className={`px-3 py-2 text-center font-bold ${trade.pnlPercent > 0 ? 'text-red-400' : trade.pnlPercent < 0 ? 'text-blue-400' : 'text-slate-500'}`}>
                                            {trade.pnlPercent ? trade.pnlPercent.toFixed(2) + '%' : '-'}
                                        </td>
                                        <td className="px-3 py-2 text-slate-400 truncate max-w-[150px]" title={trade.reason}>
                                            {trade.reason}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
    );
};
