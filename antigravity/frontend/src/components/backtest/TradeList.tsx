import React, { useRef, useEffect } from 'react';
import { List } from 'lucide-react';

interface TradeListProps {
    trades: any[];
    onHoverTrade: (timestamp: string | null) => void;
    onClickTrade: (timestamp: string) => void;
    highlightTimestamp?: string | null;
}

export const TradeList: React.FC<TradeListProps> = ({ trades, onHoverTrade, onClickTrade, highlightTimestamp }) => {
    const listRef = useRef<HTMLDivElement>(null);

    // Scroll to highlighted item
    useEffect(() => {
        if (highlightTimestamp && listRef.current) {
            const el = document.getElementById(`trade-${highlightTimestamp}`);
            if (el) {
                el.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        }
    }, [highlightTimestamp]);

    if (!trades || trades.length === 0) return null;

    return (
        <div className="bg-slate-800 rounded-lg border border-slate-700 overflow-hidden flex flex-col h-[300px]">
            <div className="p-3 bg-slate-800 border-b border-slate-700 flex items-center gap-2 shrink-0">
                <List size={14} className="text-slate-400" />
                <h3 className="text-sm font-semibold text-slate-300">거래 내역 ({trades.length})</h3>
            </div>

            <div ref={listRef} className="overflow-y-auto flex-1 custom-scrollbar">
                <table className="w-full text-xs text-left text-slate-300 table-fixed">
                    <thead className="sticky top-0 bg-slate-800 text-slate-500 shadow-sm z-10 text-[10px] uppercase tracking-wider">
                        <tr>
                            <th className="px-3 py-2 w-16">유형</th>
                            <th className="px-3 py-2 w-24">시간</th>
                            <th className="px-3 py-2 w-20 text-right">가격</th>
                            <th className="px-3 py-2 w-16 text-right">수익률</th>
                            <th className="px-3 py-2">이유</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-700/50">
                        {trades.map((trade: any, idx: number) => {
                            const isHighlighted = highlightTimestamp === trade.time;
                            return (
                                <tr
                                    key={idx}
                                    id={`trade-${trade.time}`}
                                    className={`group cursor-pointer transition-colors ${isHighlighted
                                            ? 'bg-blue-500/20 hover:bg-blue-500/30 ring-1 ring-inset ring-blue-500/50'
                                            : 'hover:bg-slate-700/50'
                                        }`}
                                    onMouseEnter={() => onHoverTrade(trade.time)}
                                    onMouseLeave={() => onHoverTrade(null)}
                                    onClick={() => onClickTrade(trade.time)}
                                >
                                    <td className="px-3 py-2 align-top">
                                        <span className={`px-1.5 py-0.5 rounded-sm font-bold text-[10px] ${trade.type === 'BUY' ? 'bg-red-500/10 text-red-400' : 'bg-blue-500/10 text-blue-400'}`}>
                                            {trade.type === 'BUY' ? '매수' : '매도'}
                                        </span>
                                    </td>
                                    <td className="px-3 py-2 font-mono text-[10px] text-slate-400 group-hover:text-slate-200 align-top">
                                        {new Date(trade.time).toLocaleDateString()}
                                        <br />
                                        {new Date(trade.time).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                    </td>
                                    <td className="px-3 py-2 text-right font-mono align-top text-slate-300">
                                        {new Intl.NumberFormat('ko-KR').format(trade.price)}
                                    </td>
                                    <td className={`px-3 py-2 text-right font-bold align-top ${trade.pnlPercent > 0 ? 'text-red-400' : trade.pnlPercent < 0 ? 'text-blue-400' : 'text-slate-500'}`}>
                                        {trade.pnlPercent ? trade.pnlPercent.toFixed(1) + '%' : '-'}
                                    </td>
                                    <td className="px-3 py-2 text-slate-500 text-[10px] leading-snug break-words group-hover:text-slate-300 align-top">
                                        {trade.reason}
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        </div>
    );
};
