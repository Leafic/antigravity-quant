import { X, Wallet, TrendingUp, Package } from 'lucide-react';

interface Holding {
    pdno: string;
    prdt_name: string;
    hldg_qty: string;
    prpr: string;
    evlu_pfls_rt: string;
    evlu_pfls_amt: string;
}

interface Balance {
    totalEvaluation: string;
    deposit: string;
    totalProfitLoss?: string;
    profitLossRate?: string;
}

interface Props {
    isOpen: boolean;
    onClose: () => void;
    balance: Balance;
    holdings: Holding[];
}

export function AccountModal({ isOpen, onClose, balance, holdings }: Props) {
    if (!isOpen) return null;

    const formatCurrency = (val: string | number) => {
        const num = Number(val);
        if (isNaN(num)) return '0 원';
        return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(num);
    };

    return (
        <>
            {/* Backdrop */}
            <div
                className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center"
                onClick={onClose}
            >
                {/* Modal */}
                <div
                    className="bg-slate-900 rounded-xl border border-slate-700 shadow-2xl max-w-4xl w-full mx-4 max-h-[90vh] overflow-hidden"
                    onClick={(e) => e.stopPropagation()}
                >
                    {/* Header */}
                    <div className="flex items-center justify-between p-6 border-b border-slate-700">
                        <div className="flex items-center gap-3">
                            <Wallet size={24} className="text-blue-400" />
                            <h2 className="text-2xl font-bold text-white">내 계좌</h2>
                        </div>
                        <button
                            onClick={onClose}
                            className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
                        >
                            <X size={20} className="text-slate-400" />
                        </button>
                    </div>

                    {/* Content */}
                    <div className="p-6 overflow-y-auto max-h-[calc(90vh-80px)]">
                        {/* Balance Summary */}
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                            <div className="bg-gradient-to-br from-blue-500/10 to-blue-600/10 border border-blue-500/30 rounded-lg p-4">
                                <div className="flex items-center gap-2 mb-2">
                                    <TrendingUp size={18} className="text-blue-400" />
                                    <span className="text-sm text-slate-400">총 평가금액</span>
                                </div>
                                <div className="text-2xl font-bold text-white">
                                    {formatCurrency(balance.totalEvaluation)}
                                </div>
                            </div>

                            <div className="bg-gradient-to-br from-emerald-500/10 to-emerald-600/10 border border-emerald-500/30 rounded-lg p-4">
                                <div className="flex items-center gap-2 mb-2">
                                    <Wallet size={18} className="text-emerald-400" />
                                    <span className="text-sm text-slate-400">주문 가능 금액</span>
                                </div>
                                <div className="text-2xl font-bold text-white">
                                    {formatCurrency(balance.deposit)}
                                </div>
                            </div>

                            <div className={`bg-gradient-to-br ${
                                Number(balance.profitLossRate || 0) >= 0
                                    ? 'from-red-500/10 to-red-600/10 border-red-500/30'
                                    : 'from-blue-500/10 to-blue-600/10 border-blue-500/30'
                            } border rounded-lg p-4`}>
                                <div className="flex items-center gap-2 mb-2">
                                    <TrendingUp size={18} className={
                                        Number(balance.profitLossRate || 0) >= 0 ? 'text-red-400' : 'text-blue-400'
                                    } />
                                    <span className="text-sm text-slate-400">총 손익</span>
                                </div>
                                <div className={`text-2xl font-bold ${
                                    Number(balance.profitLossRate || 0) >= 0 ? 'text-red-400' : 'text-blue-400'
                                }`}>
                                    {Number(balance.profitLossRate || 0) >= 0 ? '+' : ''}
                                    {balance.profitLossRate || '0.00'}%
                                </div>
                                <div className="text-sm text-slate-400 mt-1">
                                    {formatCurrency(balance.totalProfitLoss || 0)}
                                </div>
                            </div>
                        </div>

                        {/* Holdings */}
                        <div className="bg-slate-800/50 rounded-lg border border-slate-700 p-4">
                            <div className="flex items-center gap-2 mb-4">
                                <Package size={20} className="text-purple-400" />
                                <h3 className="text-lg font-semibold text-white">보유 종목</h3>
                                <span className="text-sm text-slate-500">({holdings.length})</span>
                            </div>

                            {holdings.length > 0 ? (
                                <div className="space-y-3">
                                    {holdings.map((holding) => (
                                        <div
                                            key={holding.pdno}
                                            className="bg-slate-700/50 rounded-lg p-4 hover:bg-slate-700 transition-colors"
                                        >
                                            <div className="flex items-start justify-between mb-2">
                                                <div>
                                                    <div className="font-semibold text-white text-lg">
                                                        {holding.prdt_name}
                                                    </div>
                                                    <div className="text-sm text-slate-400 font-mono">
                                                        {holding.pdno}
                                                    </div>
                                                </div>
                                                <div className="text-right">
                                                    <div className="text-lg font-bold text-white font-mono">
                                                        {formatCurrency(holding.prpr)}
                                                    </div>
                                                    <div className="text-sm text-slate-400">
                                                        {holding.hldg_qty}주 보유
                                                    </div>
                                                </div>
                                            </div>

                                            <div className="flex items-center justify-between pt-2 border-t border-slate-600">
                                                <span className="text-sm text-slate-400">평가손익</span>
                                                <div className="text-right">
                                                    <span className={`text-sm font-bold ${
                                                        Number(holding.evlu_pfls_rt) >= 0 ? 'text-red-400' : 'text-blue-400'
                                                    }`}>
                                                        {Number(holding.evlu_pfls_rt) >= 0 ? '+' : ''}
                                                        {holding.evlu_pfls_rt}%
                                                    </span>
                                                    <span className={`ml-2 text-sm ${
                                                        Number(holding.evlu_pfls_rt) >= 0 ? 'text-red-400' : 'text-blue-400'
                                                    }`}>
                                                        ({formatCurrency(holding.evlu_pfls_amt)})
                                                    </span>
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div className="text-center text-slate-500 py-8">
                                    보유 중인 종목이 없습니다
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </>
    );
}
