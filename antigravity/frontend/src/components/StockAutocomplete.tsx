import { useState, useEffect, useRef } from 'react';
import { Search } from 'lucide-react';
import { api } from '../services/api';

interface Stock {
    code: string;
    name: string;
    market: string;
}

interface StockAutocompleteProps {
    value: string;
    onChange: (value: string) => void;
    onSelect?: (stock: Stock) => void;
    placeholder?: string;
    className?: string;
}

export function StockAutocomplete({
    value,
    onChange,
    onSelect,
    placeholder = "종목명 또는 종목코드 검색...",
    className = ""
}: StockAutocompleteProps) {
    const [suggestions, setSuggestions] = useState<Stock[]>([]);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [selectedIndex, setSelectedIndex] = useState(-1);
    const wrapperRef = useRef<HTMLDivElement>(null);

    // 외부 클릭 감지
    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
                setShowSuggestions(false);
            }
        }
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    // 검색어 변경시 자동완성
    useEffect(() => {
        const fetchSuggestions = async () => {
            if (value.trim().length < 1) {
                setSuggestions([]);
                setShowSuggestions(false);
                return;
            }

            try {
                const response = await api.getStockMasters({
                    query: value,
                    size: 10,
                    page: 0
                });
                setSuggestions(response.content || []);
                setShowSuggestions(true);
                setSelectedIndex(-1);
            } catch (e) {
                console.error('Failed to fetch suggestions:', e);
                setSuggestions([]);
            }
        };

        const timeoutId = setTimeout(fetchSuggestions, 200); // 200ms debounce
        return () => clearTimeout(timeoutId);
    }, [value]);

    const handleSelect = (stock: Stock) => {
        onChange(stock.code);
        setShowSuggestions(false);
        if (onSelect) {
            onSelect(stock);
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (!showSuggestions || suggestions.length === 0) return;

        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                setSelectedIndex(prev =>
                    prev < suggestions.length - 1 ? prev + 1 : prev
                );
                break;
            case 'ArrowUp':
                e.preventDefault();
                setSelectedIndex(prev => prev > 0 ? prev - 1 : -1);
                break;
            case 'Enter':
                e.preventDefault();
                if (selectedIndex >= 0 && selectedIndex < suggestions.length) {
                    handleSelect(suggestions[selectedIndex]);
                }
                break;
            case 'Escape':
                setShowSuggestions(false);
                break;
        }
    };

    const getMarketColor = (market: string) => {
        switch (market) {
            case 'KOSPI': return 'text-blue-400';
            case 'KOSDAQ': return 'text-cyan-400';
            case 'KONEX': return 'text-purple-400';
            default: return 'text-slate-400';
        }
    };

    return (
        <div ref={wrapperRef} className={`relative ${className}`}>
            <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400" size={20} />
                <input
                    type="text"
                    value={value}
                    onChange={(e) => onChange(e.target.value)}
                    onKeyDown={handleKeyDown}
                    onFocus={() => value.trim().length > 0 && suggestions.length > 0 && setShowSuggestions(true)}
                    placeholder={placeholder}
                    className="w-full pl-10 pr-4 py-2 bg-slate-900 border border-slate-600 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
            </div>

            {showSuggestions && suggestions.length > 0 && (
                <div className="absolute z-50 w-full mt-1 bg-slate-800 border border-slate-600 rounded-lg shadow-xl max-h-64 overflow-y-auto">
                    {suggestions.map((stock, index) => (
                        <div
                            key={stock.code}
                            onClick={() => handleSelect(stock)}
                            className={`px-4 py-3 cursor-pointer border-b border-slate-700 last:border-b-0 transition-colors ${
                                index === selectedIndex
                                    ? 'bg-slate-700'
                                    : 'hover:bg-slate-700'
                            }`}
                        >
                            <div className="flex items-center justify-between">
                                <div className="flex-1">
                                    <div className="flex items-center gap-2">
                                        <span className="font-medium text-white">{stock.name}</span>
                                        <span className={`text-xs px-2 py-0.5 rounded ${getMarketColor(stock.market)}`}>
                                            {stock.market}
                                        </span>
                                    </div>
                                    <div className="text-sm text-slate-400 font-mono mt-0.5">{stock.code}</div>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {showSuggestions && value.trim().length > 0 && suggestions.length === 0 && (
                <div className="absolute z-50 w-full mt-1 bg-slate-800 border border-slate-600 rounded-lg shadow-xl p-4 text-center text-slate-400">
                    검색 결과가 없습니다
                </div>
            )}
        </div>
    );
}
