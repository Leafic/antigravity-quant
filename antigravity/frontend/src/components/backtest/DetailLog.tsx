import React, { useState } from 'react';
import { Terminal, ChevronDown, ChevronRight } from 'lucide-react';

interface DetailLogProps {
    trades: any[];
}

export const DetailLog: React.FC<DetailLogProps> = ({ trades }) => {
    const [isOpen, setIsOpen] = useState(false);

    if (!trades || trades.length === 0) return null;

    return (
        <div className="bg-slate-900 rounded-lg border border-slate-700 overflow-hidden mt-2">
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="w-full flex items-center gap-2 p-3 bg-slate-800 hover:bg-slate-750 transition-colors text-xs text-slate-400 hover:text-slate-200"
            >
                <Terminal size={14} />
                <span className="font-semibold">상세 로그 (개발자용)</span>
                {isOpen ? <ChevronDown size={14} className="ml-auto" /> : <ChevronRight size={14} className="ml-auto" />}
            </button>

            {isOpen && (
                <div className="p-3 bg-black/50 text-[10px] font-mono text-slate-400 h-[200px] overflow-y-auto custom-scrollbar">
                    {trades.map((t, idx) => (
                        <div key={idx} className="mb-2 border-b border-slate-800 pb-2">
                            <span className="text-blue-400">[{t.time}]</span> {t.type} @ {t.price}
                            <div className="pl-2 mt-1 text-slate-500 whitespace-pre-wrap">
                                {JSON.stringify(t, null, 2)}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};
