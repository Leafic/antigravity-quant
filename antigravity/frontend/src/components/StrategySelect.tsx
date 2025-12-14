import React, { useEffect, useState } from 'react';
import { api } from '../services/api';

interface Strategy {
    id: string;
    name: string;
    description: string;
    defaultParams: string;
}

interface Props {
    selectedId: string;
    onSelect: (id: string, params: string) => void;
    mode: 'DAILY' | 'INTRADAY';
}

export const StrategySelect: React.FC<Props> = ({ selectedId, onSelect, mode }) => {
    const [strategies, setStrategies] = useState<Strategy[]>([]);

    useEffect(() => {
        api.getStrategies().then(setStrategies).catch(console.error);
    }, []);

    const handleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        const id = e.target.value;
        const strat = strategies.find(s => s.id === id);
        onSelect(id, strat ? strat.defaultParams : '{}');
    };

    return (
        <div>
            <label className="block text-xs text-slate-400 mb-1">전략 선택 (Strategy)</label>
            <select
                value={selectedId}
                onChange={handleChange}
                className="w-full bg-slate-900 border border-slate-700 rounded p-2 text-sm text-slate-200"
            >
                {strategies.filter(s => {
                    const isIntraday = s.id === 'S5';
                    return mode === 'INTRADAY' ? isIntraday : !isIntraday;
                }).map(s => (
                    <option key={s.id} value={s.id}>
                        {s.name}
                    </option>
                ))}
            </select>
            {selectedId && (
                <div className="text-xs text-slate-500 mt-1 truncate">
                    {strategies.find(s => s.id === selectedId)?.description}
                </div>
            )}
        </div>
    );
};
