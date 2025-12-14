import React, { useState } from 'react';
import { api } from '../services/api';
import { Loader2 } from 'lucide-react';
import { StockChart } from './StockChart';
import { BacktestHeader } from './backtest/BacktestHeader';
import { BacktestSettings } from './backtest/BacktestSettings';
import { BacktestResults } from './backtest/BacktestResults';
import { TradeList } from './backtest/TradeList';
import { DetailLog } from './backtest/DetailLog';

export const BacktestPanel: React.FC = () => {
    // State
    const [symbol, setSymbol] = useState('005930');
    const [start, setStart] = useState('2025-01-01T00:00:00');
    const [end, setEnd] = useState('2025-12-31T23:59:59');
    const [strategyId, setStrategyId] = useState('S4_Ensemble'); // Default S4
    const [params, setParams] = useState('{}');
    // Result
    const [result, setResult] = useState<any>(null);
    const [loading, setLoading] = useState(false);
    // UI Interaction
    const [highlightTimestamp, setHighlightTimestamp] = useState<string | null>(null);

    const handleRun = async () => {
        setLoading(true);
        try {
            const fmtStart = start.length === 16 ? start + ':00' : start;
            const fmtEnd = end.length === 16 ? end + ':59' : end;

            const data = await api.runBacktest(symbol, fmtStart, fmtEnd, strategyId, params);
            setResult(data);
            // reset highlight
            setHighlightTimestamp(null);
        } catch (error) {
            console.error(error);
            alert('백테스트 실패. 콘솔을 확인하세요.');
        } finally {
            setLoading(false);
        }
    };

    const handleReset = () => {
        setResult(null);
        setHighlightTimestamp(null);
    };

    const handleStrategySelect = (id: string, defaultParams: string) => {
        setStrategyId(id);
        setParams(defaultParams);
    };

    const onHoverTrade = (_time: string | null) => {
        // Implementation for hover highlight if needed later
    };

    const onClickTrade = (time: string) => {
        setHighlightTimestamp(time);
    };

    const onChartClick = (time: string) => {
        setHighlightTimestamp(time);
    };

    // Calculate period days for header
    let periodStr = "";
    if (start && end) {
        periodStr = `${start.substring(0, 10)} ~ ${end.substring(0, 10)}`;
    }

    return (
        <div className="bg-slate-800 rounded-xl p-0 border border-slate-700 flex flex-col h-full overflow-hidden relative">
            {/* Header (Always Visible) */}
            <div className="p-4 border-b border-slate-700 bg-slate-800 z-20">
                <BacktestHeader
                    symbolName={symbol} // ideally fetch name but code is ok for now
                    strategyName={strategyId}
                    period={periodStr}
                    onReset={result ? handleReset : undefined}
                />
            </div>

            {/* Scrollable Content Area */}
            <div className="flex-1 overflow-y-auto p-4 custom-scrollbar pb-20">

                {/* Section A: Settings */}
                <BacktestSettings
                    symbol={symbol} setSymbol={setSymbol}
                    strategyId={strategyId} setStrategyId={setStrategyId}
                    params={params} setParams={setParams}
                    start={start} setStart={setStart}
                    end={end} setEnd={setEnd}
                    handleStrategySelect={handleStrategySelect}
                />

                {/* Spacer / Divider */}
                <div className="h-4"></div>

                {/* Section B/C/D: Results (Only if result exists) */}
                {result && (
                    <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
                        {/* Section B: Results KPI */}
                        <BacktestResults result={result} />

                        {/* Chart Area */}
                        <div className="h-[350px] bg-slate-900/50 rounded-lg border border-slate-700 p-2 relative">
                            {/* @ts-ignore */}
                            <StockChart
                                data={result.candles}
                                markers={result.trades?.map((t: any) => ({
                                    time: t.time,
                                    type: t.type,
                                    text: t.reason
                                }))}
                                highlightTimestamp={highlightTimestamp}
                                onChartClick={onChartClick}
                            />
                        </div>

                        {/* Section C: Trade List */}
                        <TradeList
                            trades={result.trades}
                            onHoverTrade={onHoverTrade}
                            onClickTrade={onClickTrade}
                            highlightTimestamp={highlightTimestamp}
                        />

                        {/* Section D: Detail Log */}
                        <DetailLog trades={result.trades} />
                    </div>
                )}
            </div>

            {/* Fixed Footer: Run Button */}
            <div className="absolute bottom-0 left-0 right-0 p-4 bg-slate-800/95 backdrop-blur border-t border-slate-700 z-30">
                <button
                    onClick={handleRun}
                    disabled={loading}
                    className="flex-1 bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 rounded-lg transition-colors flex items-center justify-center gap-2"
                >
                    {loading ? <Loader2 className="animate-spin" size={18} /> : '백테스트 실행'}
                </button>
            </div>

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
                                    <th className="px-3 py-2">이유 (Reason)</th>
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
                                        <td className="px-3 py-2 text-slate-400" title={trade.reason}>
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
