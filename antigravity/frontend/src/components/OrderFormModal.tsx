import { useState, useEffect } from 'react';
import { X } from 'lucide-react';
import { StockAutocomplete } from './StockAutocomplete';

interface OrderFormModalProps {
    isOpen: boolean;
    onClose: () => void;
    initialSymbol?: string;
    initialName?: string;
}

export function OrderFormModal({ isOpen, onClose, initialSymbol = '', initialName = '' }: OrderFormModalProps) {
    const [symbol, setSymbol] = useState(initialSymbol);
    const [name, setName] = useState(initialName);
    const [type, setType] = useState<'BUY' | 'SELL'>('BUY');
    const [quantity, setQuantity] = useState(1);
    const [price, setPrice] = useState(0); // 0 = Market
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (isOpen) {
            setSymbol(initialSymbol);
            setName(initialName);
            setQuantity(1);
            setPrice(0);
            setType('BUY');
        }
    }, [isOpen, initialSymbol, initialName]);

    const handleSubmit = async () => {
        if (!symbol) {
            alert('종목을 선택해주세요.');
            return;
        }

        if(!confirm(`${name}(${symbol}) ${quantity}주를 ${type === 'BUY' ? '매수' : '매도'} 하시겠습니까?\n(가격: ${price === 0 ? '시장가' : price})`)) return;

        setLoading(true);
        try {
            const response = await fetch('/api/orders', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    symbol,
                    type,
                    quantity,
                    price,
                    reason: 'Manual User Order'
                })
            });

            if (response.ok) {
                alert('주문이 전송되었습니다.');
                onClose();
            } else {
                const err = await response.json();
                alert('주문 실패: ' + err.message);
            }
        } catch (e) {
            console.error(e);
            alert('주문 전송 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
            <div className="bg-slate-800 rounded-xl border border-slate-700 shadow-2xl w-full max-w-md overflow-hidden">
                <div className="flex items-center justify-between p-4 border-b border-slate-700 bg-slate-800/80">
                    <h3 className="font-bold text-white text-lg">주문하기</h3>
                    <button onClick={onClose} className="text-slate-400 hover:text-white transition-colors">
                        <X size={20} />
                    </button>
                </div>

                <div className="p-6 space-y-4">
                    {/* Stock Selection */}
                    <div className="space-y-1">
                        <label className="text-sm text-slate-400 block">종목</label>
                        <StockAutocomplete
                            value={symbol} // Currently Autocomplete takes value as query, need to adjust logic or use name?
                            // Logic gap: The StockAutocomplete expects 'value' to be the input text. 
                            // If I pass 'symbol', it searches by symbol. If I pass 'name', it searches by name.
                            // Better to let user search if they want, but default to passed symbol.
                            // Let's rely on the user searching if they want to change.
                            onChange={(val) => setSymbol(val)} 
                            onSelect={(s) => {
                                setSymbol(s.code);
                                setName(s.name);
                            }}
                            placeholder={name ? `${name} (${symbol})` : "종목 검색..."}
                        />
                         {name && <div className="text-xs text-slate-500 text-right mt-1">{name} ({symbol})</div>}
                    </div>

                    {/* Type Selection */}
                    <div className="grid grid-cols-2 gap-2 bg-slate-900 p-1 rounded-lg">
                        <button
                            onClick={() => setType('BUY')}
                            className={`py-2 rounded-md font-bold transition-all ${
                                type === 'BUY' 
                                    ? 'bg-red-500 text-white shadow-lg' 
                                    : 'text-slate-500 hover:text-red-400'
                            }`}
                        >
                            매수
                        </button>
                        <button
                            onClick={() => setType('SELL')}
                            className={`py-2 rounded-md font-bold transition-all ${
                                type === 'SELL' 
                                    ? 'bg-blue-500 text-white shadow-lg' 
                                    : 'text-slate-500 hover:text-blue-400'
                            }`}
                        >
                            매도
                        </button>
                    </div>

                    {/* Quantity */}
                    <div className="space-y-1">
                        <label className="text-sm text-slate-400 block">수량</label>
                        <div className="flex items-center gap-2">
                             <input
                                type="number"
                                min="1"
                                value={quantity}
                                onChange={(e) => setQuantity(Number(e.target.value))}
                                className="flex-1 bg-slate-900 border border-slate-600 rounded px-3 py-2 text-white text-right focus:outline-none focus:border-blue-500"
                            />
                            <span className="text-slate-400">주</span>
                        </div>
                    </div>

                     {/* Price */}
                    <div className="space-y-1">
                        <label className="text-sm text-slate-400 block">가격 (0 = 시장가)</label>
                        <div className="flex items-center gap-2">
                             <input
                                type="number"
                                min="0"
                                step="100"
                                value={price}
                                onChange={(e) => setPrice(Number(e.target.value))}
                                className="flex-1 bg-slate-900 border border-slate-600 rounded px-3 py-2 text-white text-right focus:outline-none focus:border-blue-500"
                            />
                            <span className="text-slate-400">원</span>
                        </div>
                    </div>

                    <div className="pt-4">
                        <button
                            onClick={handleSubmit}
                            disabled={loading}
                            className={`w-full py-3 rounded-lg font-bold text-lg shadow-lg transition-all ${
                                type === 'BUY'
                                    ? 'bg-gradient-to-r from-red-600 to-red-500 hover:from-red-500 hover:to-red-400 text-white'
                                    : 'bg-gradient-to-r from-blue-600 to-blue-500 hover:from-blue-500 hover:to-blue-400 text-white'
                            } ${loading ? 'opacity-50 cursor-not-allowed' : ''}`}
                        >
                            {loading ? '처리중...' : (
                                `${type === 'BUY' ? '매수' : '매도'} 주문 전송`
                            )}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
