import { useState, useEffect } from 'react';

interface TradeLog {
    id: number;
    timestamp: string;
    symbol: string;
    type: 'BUY' | 'SELL';
    price: number;
    quantity: number;
    reason: string;
    signalCode: string;
    strategyName: string;
}

export function TradeLogPanel() {
    const [trades, setTrades] = useState<TradeLog[]>([]);

    useEffect(() => {
        fetchTrades();
        const interval = setInterval(fetchTrades, 5000); // Poll every 5 seconds
        return () => clearInterval(interval);
    }, []);

    const fetchTrades = async () => {
        try {
            const res = await fetch('/api/trades');
            if (res.ok) {
                const data = await res.json();
                setTrades(data);
            }
        } catch (error) {
            console.error('Failed to fetch trades', error);
        }
    };

    return (
        <div className="bg-white p-4 rounded shadow">
            <h2 className="text-lg font-bold mb-4 flex justify-between items-center">
                Trade History
                <span className="text-xs font-normal text-gray-500">Auto-refresh (5s)</span>
            </h2>
            <div className="overflow-x-auto">
                <table className="min-w-full text-sm text-left text-gray-500">
                    <thead className="text-xs text-gray-700 uppercase bg-gray-50">
                        <tr>
                            <th className="px-4 py-2">Time</th>
                            <th className="px-4 py-2">Symbol</th>
                            <th className="px-4 py-2">Type</th>
                            <th className="px-4 py-2">Price</th>
                            <th className="px-4 py-2">Qty</th>
                            <th className="px-4 py-2">Strategy</th>
                            <th className="px-4 py-2">Reason</th>
                        </tr>
                    </thead>
                    <tbody>
                        {trades.length === 0 ? (
                            <tr>
                                <td colSpan={7} className="px-4 py-4 text-center">
                                    No trades recorded yet.
                                </td>
                            </tr>
                        ) : (
                            trades.map((trade) => (
                                <tr key={trade.id} className="bg-white border-b hover:bg-gray-50">
                                    <td className="px-4 py-2 whitespace-nowrap">
                                        {new Date(trade.timestamp).toLocaleString()}
                                    </td>
                                    <td className="px-4 py-2 font-medium text-gray-900">{trade.symbol}</td>
                                    <td className={`px-4 py-2 font-bold ${trade.type === 'BUY' ? 'text-red-600' : 'text-blue-600'}`}>
                                        {trade.type}
                                    </td>
                                    <td className="px-4 py-2">{trade.price.toLocaleString()}</td>
                                    <td className="px-4 py-2">{trade.quantity}</td>
                                    <td className="px-4 py-2">{trade.strategyName}</td>
                                    <td className="px-4 py-2 text-xs truncate max-w-xs" title={trade.reason}>
                                        {trade.reason}
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
