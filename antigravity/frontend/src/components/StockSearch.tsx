import React, { useState, useEffect, useRef } from 'react';
import { api } from '../services/api';
import { Search } from 'lucide-react';

interface Stock {
    code: string;
    name: string;
    market: string;
    sector: string;
}

interface Props {
    selectedCode: string;
    onSelect: (code: string) => void;
}

export const StockSearch: React.FC<Props> = ({ selectedCode, onSelect }) => {
    const [query, setQuery] = useState(selectedCode);
    const [suggestions, setSuggestions] = useState<Stock[]>([]);
    const [isOpen, setIsOpen] = useState(false);
    const wrapperRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        setQuery(selectedCode);
    }, [selectedCode]);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleSearch = async (val: string) => {
        setQuery(val);
        if (val.length >= 2) {
            try {
                const results = await api.searchStocks(val);
                setSuggestions(results);
                setIsOpen(true);
            } catch (e) {
                console.error(e);
            }
        } else {
            setSuggestions([]);
            setIsOpen(false);
        }
    };

    return (
        <div ref={wrapperRef} className="relative">
            <label className="block text-xs text-slate-400 mb-1">종목 검색 (Search)</label>
            <div className="relative">
                <input
                    type="text"
                    value={query}
                    onChange={(e) => handleSearch(e.target.value)}
                    placeholder="종목명 또는 코드"
                    className="w-full bg-slate-900 border border-slate-700 rounded p-2 pl-8 text-sm text-slate-200"
                />
                <Search size={14} className="absolute left-2.5 top-2.5 text-slate-500" />
            </div>

            {isOpen && suggestions.length > 0 && (
                <div className="absolute z-10 w-full mt-1 bg-slate-800 border border-slate-700 rounded-lg shadow-lg max-h-60 overflow-y-auto">
                    {suggestions.map((stock) => (
                        <div
                            key={stock.code}
                            onClick={() => {
                                onSelect(stock.code);
                                setQuery(`${stock.name} (${stock.code})`);
                                setIsOpen(false);
                            }}
                            className="p-2 hover:bg-slate-700 cursor-pointer text-sm"
                        >
                            <div className="font-medium text-slate-200">{stock.name}</div>
                            <div className="text-xs text-slate-500">{stock.code} | {stock.market} | {stock.sector}</div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};
