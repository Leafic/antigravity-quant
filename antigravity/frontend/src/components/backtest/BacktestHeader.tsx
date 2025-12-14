import React from 'react';
import { Play, RotateCcw } from 'lucide-react';

interface BacktestHeaderProps {
    symbolName: string;
    strategyName: string;
    period: string;
    onReset?: () => void;
}

export const BacktestHeader: React.FC<BacktestHeaderProps> = ({ symbolName, strategyName, period, onReset }) => {
    return (
        <div className="flex items-center justify-between mb-4 p-4 bg-slate-800 rounded-lg border border-slate-700 shadow-sm sticky top-0 z-10">
            <div className="flex flex-col">
                <div className="flex items-center gap-2 mb-1">
                    <Play size={16} className="text-blue-400 fill-blue-400/20" />
                    <h2 className="text-sm font-bold text-slate-100">{symbolName}</h2>
                </div>
                <div className="text-xs text-slate-400 flex gap-2">
                    <span className="px-1.5 py-0.5 bg-slate-700 rounded text-slate-300">{strategyName}</span>
                    <span className="self-center">{period}</span>
                </div>
            </div>
            {onReset && (
                <button
                    onClick={onReset}
                    className="p-2 text-slate-400 hover:text-white hover:bg-slate-700 rounded-full transition-colors"
                    title="최근 설정으로 다시 실행"
                >
                    <RotateCcw size={16} />
                </button>
            )}
        </div>
    );
};
