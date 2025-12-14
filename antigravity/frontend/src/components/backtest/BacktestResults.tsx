import React from 'react';
import { TrendingUp, Activity, DollarSign, AlertTriangle } from 'lucide-react';

interface BacktestResultsProps {
    result: any;
}

const MetricCard: React.FC<{ label: string; value: string; subValue?: string; tone?: 'positive' | 'negative' | 'neutral'; icon?: React.ElementType }> = ({ label, value, subValue, tone = 'neutral', icon: Icon }) => {
    const colorClass = tone === 'positive' ? 'text-emerald-400' : tone === 'negative' ? 'text-red-400' : 'text-slate-200';

    return (
        <div className="bg-slate-700/30 p-3 rounded-lg border border-slate-700/50 flex flex-col items-center justify-center text-center hover:bg-slate-700/50 transition-colors">
            <div className="text-[10px] text-slate-500 mb-1 flex items-center gap-1 uppercase tracking-wider">
                {Icon && <Icon size={10} />}
                {label}
            </div>
            <div className={`text-base font-bold ${colorClass}`}>
                {value}
            </div>
            {subValue && <div className="text-[10px] text-slate-500 mt-0.5">{subValue}</div>}
        </div>
    );
};

export const BacktestResults: React.FC<BacktestResultsProps> = ({ result }) => {
    if (!result) {
        return (
            <div className="mb-4 p-8 text-center border border-slate-700 border-dashed rounded-lg bg-slate-800/20">
                <p className="text-sm text-slate-500">아직 실행된 백테스트가 없습니다.</p>
            </div>
        );
    }

    const netProfit = result.finalBalance - result.initialBalance;
    const winRate = result.winRate ? result.winRate.toFixed(1) : '-';

    return (
        <div className="mb-4 animate-in fade-in zoom-in duration-300">
            <div className="flex items-center gap-2 mb-3">
                <TrendingUp size={14} className="text-blue-400" />
                <h3 className="text-sm font-semibold text-slate-300">백테스트 결과</h3>
            </div>
            <div className="grid grid-cols-2 gap-2">
                <MetricCard
                    label="총 수익률"
                    value={`${result.totalReturnPercent}%`}
                    tone={result.totalReturnPercent >= 0 ? 'positive' : 'negative'}
                    icon={TrendingUp}
                />
                <MetricCard
                    label="최종 잔고"
                    value={new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW', maximumFractionDigits: 0 }).format(result.finalBalance)}
                    subValue={`Net: ${new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW', maximumFractionDigits: 0 }).format(netProfit)}`}
                    tone="neutral"
                    icon={DollarSign}
                />
                <MetricCard
                    label="거래 횟수"
                    value={`${result.totalTrades}회`}
                    subValue={`승률: ${winRate}%`}
                    tone="neutral"
                    icon={Activity}
                />
                <MetricCard
                    label="MDD"
                    value={`${result.mdd ? result.mdd.toFixed(2) : '0.00'}%`}
                    tone="negative"
                    icon={AlertTriangle}
                />
            </div>
        </div>
    );
};
