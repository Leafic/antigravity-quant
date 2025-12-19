import { useEffect, useState } from 'react';
import { PieChart, Clock, TrendingUp, AlertCircle, DollarSign, Wallet } from 'lucide-react';

interface Holding {
    pdno: string;          // Symbol
    prdt_name: string;     // Name
    hldg_qty: string;      // Qty
    evlu_pfls_rt: string;  // P/L Rate
    evlu_pfls_amt: string; // P/L Amount
    prpr: string;          // Current Price
    pchs_avg_pric: string; // Avg Price
}

interface TradeLog {
    symbol: string;
    type: string;
    price: number;
    quantity: number;
    timestamp: string;
    reason: string;
}

interface TargetStock {
    symbol: string;
    name: string;
    sector: string;
    reason: string;
}

import { StockAutocomplete } from '../components/StockAutocomplete';
import { Skeleton } from '../components/ui/Skeleton';

interface Schedule {
    id: number;
    symbol: string;
    name: string;
    type: 'BUY' | 'SELL';
    scheduleTime: string;
    quantity: number;
    isActive: boolean;
    lastExecutedAt?: string;
}

export function PortfolioPage() {
    const [holdings, setHoldings] = useState<Holding[]>([]);
    const [history, setHistory] = useState<TradeLog[]>([]);
    const [targets, setTargets] = useState<TargetStock[]>([]);
    const [schedules, setSchedules] = useState<Schedule[]>([]);
    const [balance, setBalance] = useState({ totalEvaluation: "0", deposit: "0" });
    const [loading, setLoading] = useState(true);
    
    // Form State
    const [isAdding, setIsAdding] = useState(false);
    const [editingId, setEditingId] = useState<number | null>(null);
    const [scheduleForm, setScheduleForm] = useState({
        symbol: '',
        name: '',
        type: 'BUY' as 'BUY'|'SELL',
        scheduleTime: '09:00',
        quantity: 1,
        isActive: true
    });

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        try {
            const [holdingsRes, historyRes, targetsRes, balanceRes, scheduleRes] = await Promise.all([
               fetch('/api/account/holdings'),
               fetch('/api/trades'),
               fetch('/api/targets'),
               fetch('/api/account/balance'),
               fetch('/api/autotrade')
            ]);

            if(holdingsRes.ok) setHoldings(await holdingsRes.json());
            if(historyRes.ok) setHistory(await historyRes.json());
            if(targetsRes.ok) setTargets(await targetsRes.json());
            if(balanceRes.ok) setBalance(await balanceRes.json());
            if(scheduleRes.ok) setSchedules(await scheduleRes.json());

        } catch (error) {
            console.error("Failed to fetch portfolio data", error);
        } finally {
            setLoading(false);
        }
    };
    
    const handleSaveSchedule = async () => {
        if (!scheduleForm.symbol) {
            alert('종목을 선택해주세요.');
            return;
        }

        try {
            const url = editingId ? `/api/autotrade/${editingId}` : '/api/autotrade';
            const method = editingId ? 'PUT' : 'POST';

            await fetch(url, {
                method: method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(editingId ? { ...scheduleForm, id: editingId } : scheduleForm)
            });

            setIsAdding(false);
            setEditingId(null);
            setScheduleForm({
                symbol: '',
                name: '',
                type: 'BUY',
                scheduleTime: '09:00',
                quantity: 1,
                isActive: true
            });
            fetchData(); // Refresh
        } catch (e) {
            console.error(e);
            alert('저장 중 오류가 발생했습니다.');
        }
    };

    const handleEditClick = (s: Schedule) => {
        setEditingId(s.id);
        setScheduleForm({
            symbol: s.symbol,
            name: s.name,
            type: s.type,
            scheduleTime: s.scheduleTime,
            quantity: s.quantity,
            isActive: s.isActive
        });
        setIsAdding(true);
    };

    const handleCancelEdit = () => {
        setIsAdding(false);
        setEditingId(null);
        setScheduleForm({
            symbol: '',
            name: '',
            type: 'BUY',
            scheduleTime: '09:00',
            quantity: 1,
            isActive: true
        });
    };

    const handleDeleteSchedule = async (id: number) => {
        if(!confirm('정말 삭제하시겠습니까?')) return;
        try {
            await fetch(`/api/autotrade/${id}`, { method: 'DELETE' });
            fetchData();
        } catch (e) {
            console.error(e);
        }
    };

    const toggleScheduleActive = async (s: Schedule) => {
        try {
            await fetch(`/api/autotrade/${s.id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ ...s, isActive: !s.isActive })
            });
            fetchData();
        } catch (e) {
            console.error(e);
        }
    };

    const formatCurrency = (val: string | number) => {
        return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(Number(val));
    };

    if (loading) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-8 w-64 mb-6" />
                
                {/* Skeleton Summary Cards */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    {[1, 2, 3].map((i) => (
                        <div key={i} className="bg-slate-800 p-6 rounded-xl border border-slate-700">
                            <Skeleton className="h-4 w-24 mb-2" />
                            <Skeleton className="h-8 w-32" />
                        </div>
                    ))}
                </div>

                {/* Skeleton Auto-Trade Settings */}
                <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
                    <div className="flex justify-between items-center mb-4">
                        <Skeleton className="h-6 w-40" />
                        <Skeleton className="h-8 w-20" />
                    </div>
                    <Skeleton className="h-10 w-full mb-4" />
                    <div className="space-y-2">
                        <Skeleton className="h-16 w-full" />
                        <Skeleton className="h-16 w-full" />
                    </div>
                </div>

                {/* Skeleton Recommendations */}
                 <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
                    <Skeleton className="h-6 w-48 mb-4" />
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                         {[1, 2, 3].map((i) => (
                            <Skeleton key={i} className="h-20 w-full" />
                        ))}
                    </div>
                 </div>

                {/* Skeleton Holdings Table */}
                <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden">
                    <div className="p-4 border-b border-slate-700 flex items-center gap-2">
                         <Skeleton className="h-6 w-40" />
                    </div>
                    <div className="p-4 space-y-3">
                        {[1, 2, 3].map((i) => (
                            <Skeleton key={i} className="h-12 w-full" />
                        ))}
                    </div>
                </div>

                {/* Skeleton History Table */}
                 <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden">
                    <div className="p-4 border-b border-slate-700 flex items-center gap-2">
                         <Skeleton className="h-6 w-32" />
                    </div>
                    <div className="p-4 space-y-3">
                        {[1, 2, 3].map((i) => (
                            <Skeleton key={i} className="h-10 w-full" />
                        ))}
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <h2 className="text-2xl font-bold bg-gradient-to-r from-green-400 to-emerald-500 bg-clip-text text-transparent">내 투자 포트폴리오</h2>

            {/* Summary Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="bg-slate-800 p-6 rounded-xl border border-slate-700 shadow-lg">
                    <div className="flex items-center gap-3 mb-2 text-slate-400">
                        <Wallet size={20} />
                        <span className="text-sm font-medium uppercase tracking-wider">총 평가 자산</span>
                    </div>
                    <div className="text-3xl font-bold text-white">{formatCurrency(balance.totalEvaluation)}</div>
                </div>
                <div className="bg-slate-800 p-6 rounded-xl border border-slate-700 shadow-lg">
                    <div className="flex items-center gap-3 mb-2 text-slate-400">
                        <DollarSign size={20} />
                        <span className="text-sm font-medium uppercase tracking-wider">예수금</span>
                    </div>
                    <div className="text-2xl font-bold text-slate-200">{formatCurrency(balance.deposit)}</div>
                </div>
                 <div className="bg-slate-800 p-6 rounded-xl border border-slate-700 shadow-lg">
                    <div className="flex items-center gap-3 mb-2 text-slate-400">
                        <TrendingUp size={20} />
                        <span className="text-sm font-medium uppercase tracking-wider">보유 종목 수</span>
                    </div>
                    <div className="text-2xl font-bold text-slate-200">{holdings.length}</div>
                </div>
            </div>

            {/* Auto-Trade Settings */}
            <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden shadow-lg">
                <div className="p-4 border-b border-slate-700 flex justify-between items-center bg-slate-800/50">
                    <div className="flex items-center gap-2">
                        <Clock className="text-blue-400" size={20} />
                        <h3 className="font-bold text-lg text-white">자동 매매 예약 설정</h3>
                    </div>
                    <button 
                        onClick={isAdding ? handleCancelEdit : () => setIsAdding(true)}
                        className={`px-3 py-1 text-sm rounded transition-colors ${
                            isAdding ? 'bg-slate-600 hover:bg-slate-500 text-slate-200' : 'bg-blue-600 hover:bg-blue-500 text-white'
                        }`}
                    >
                        {isAdding ? '취소' : '+ 새 예약'}
                    </button>
                </div>
                
                {isAdding && (
                    <div className="p-4 bg-slate-700/30 border-b border-slate-700 flex flex-wrap gap-4 items-end">
                        <div className="flex flex-col gap-1 w-60">
                            <label className="text-xs text-slate-400">종목검색</label>
                            <StockAutocomplete
                                value={scheduleForm.symbol} // This still uses symbol as value, need to handle query properly
                                // For edit, user might want to see Name, but Autocomplete expects query. 
                                // Let's use StockAutocomplete properly: default to symbol code.
                                // Ideal: Autocomplete that accepts initial value.
                                onChange={(val) => setScheduleForm(prev => ({...prev, symbol: val}))}
                                onSelect={(s) => setScheduleForm(prev => ({...prev, symbol: s.code, name: s.name}))}
                                placeholder={scheduleForm.name || "종목명/번호 검색"}
                            />
                        </div>
                        {scheduleForm.name && (
                            <div className="flex flex-col gap-1">
                                <label className="text-xs text-slate-400">종목명</label>
                                <div className="px-3 py-2 text-slate-300 font-medium">
                                    {scheduleForm.name}
                                </div>
                            </div>
                        )}
                        <div className="flex flex-col gap-1">
                            <label className="text-xs text-slate-400">시간 (HH:mm)</label>
                            <input 
                                type="time" 
                                value={scheduleForm.scheduleTime}
                                onChange={e => setScheduleForm({...scheduleForm, scheduleTime: e.target.value})}
                                className="bg-slate-900 border border-slate-600 rounded px-2 py-1 text-white"
                            />
                        </div>
                         <div className="flex flex-col gap-1">
                            <label className="text-xs text-slate-400">구분</label>
                            <select 
                                value={scheduleForm.type}
                                onChange={e => setScheduleForm({...scheduleForm, type: e.target.value as 'BUY'|'SELL'})}
                                className="bg-slate-900 border border-slate-600 rounded px-2 py-1 text-white"
                            >
                                <option value="BUY">매수</option>
                                <option value="SELL">매도</option>
                            </select>
                        </div>
                        <div className="flex flex-col gap-1">
                            <label className="text-xs text-slate-400">수량</label>
                            <input 
                                type="number" 
                                value={scheduleForm.quantity}
                                onChange={e => setScheduleForm({...scheduleForm, quantity: Number(e.target.value)})}
                                className="bg-slate-900 border border-slate-600 rounded px-2 py-1 text-white w-20"
                            />
                        </div>
                        <button 
                            onClick={handleSaveSchedule}
                            className={`px-4 py-1.5 text-white rounded text-sm font-medium ${
                                editingId ? 'bg-orange-600 hover:bg-orange-500' : 'bg-green-600 hover:bg-green-500'
                            }`}
                        >
                            {editingId ? '수정 저장' : '추가'}
                        </button>
                    </div>
                )}

                <div className="overflow-x-auto">
                    <table className="w-full text-left">
                        <thead className="bg-slate-700/50 text-slate-400 text-sm uppercase tracking-wider">
                            <tr>
                                <th className="p-4">시간</th>
                                <th className="p-4">종목</th>
                                <th className="p-4">주문</th>
                                <th className="p-4">수량</th>
                                <th className="p-4">상태</th>
                                <th className="p-4">마지막 실행</th>
                                <th className="p-4 text-right">관리</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-700">
                            {schedules.map((s) => (
                                <tr key={s.id} className="hover:bg-slate-700/30 transition-colors">
                                    <td className="p-4 font-mono text-lg text-white font-bold">{s.scheduleTime}</td>
                                    <td className="p-4">
                                        <div className="font-medium text-slate-200">{s.name}</div>
                                        <div className="text-xs text-slate-500">{s.symbol}</div>
                                    </td>
                                    <td className="p-4">
                                        <span className={`px-2 py-1 rounded text-xs font-bold ${
                                            s.type === 'BUY' ? 'bg-red-500/10 text-red-400 border border-red-500/20' : 'bg-blue-500/10 text-blue-400 border border-blue-500/20'
                                        }`}>
                                            {s.type === 'BUY' ? '매수' : '매도'}
                                        </span>
                                    </td>
                                    <td className="p-4 text-slate-300">{s.quantity}주</td>
                                    <td className="p-4">
                                         <button 
                                            onClick={() => toggleScheduleActive(s)}
                                            className={`w-10 h-5 rounded-full relative transition-colors ${s.isActive ? 'bg-green-500' : 'bg-slate-600'}`}
                                         >
                                            <div className={`absolute top-1 w-3 h-3 rounded-full bg-white transition-all ${s.isActive ? 'left-6' : 'left-1'}`}></div>
                                         </button>
                                    </td>
                                    <td className="p-4 text-sm text-slate-500">
                                        {s.lastExecutedAt ? new Date(s.lastExecutedAt).toLocaleString() : '-'}
                                    </td>
                                    <td className="p-4 text-right space-x-2">
                                        <button 
                                            onClick={() => handleEditClick(s)}
                                            className="text-slate-400 hover:text-white transition-colors"
                                        >
                                            수정
                                        </button>
                                        <button 
                                            onClick={() => handleDeleteSchedule(s.id)}
                                            className="text-slate-500 hover:text-red-400 transition-colors"
                                        >
                                            삭제
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            {schedules.length === 0 && (
                                <tr>
                                    <td colSpan={7} className="p-8 text-center text-slate-500">
                                        등록된 자동 매매 예약이 없습니다.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Recommendations using Targets */}
             <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden shadow-lg">
                <div className="p-4 border-b border-slate-700 flex items-center gap-2 bg-slate-800/50">
                    <AlertCircle className="text-yellow-400" size={20} />
                    <h3 className="font-bold text-lg text-white">AI 추천 종목 (오늘의 타겟)</h3>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 p-4">
                    {targets.map((t, i) => (
                        <div key={i} className="bg-slate-700/30 p-4 rounded-lg flex justify-between items-center group hover:bg-slate-700 transition-all border border-slate-600/30 hover:border-slate-500">
                            <div>
                                <div className="font-bold text-lg text-white">{t.name}</div>
                                <div className="text-sm text-slate-400">{t.symbol} | {t.sector}</div>
                            </div>
                            <span className="px-3 py-1 bg-green-500/10 text-green-400 border border-green-500/20 rounded-full text-xs">
                                추천됨
                            </span>
                        </div>
                    ))}
                    {targets.length === 0 && <div className="text-slate-500 col-span-full text-center py-4">현재 추천 종목이 없습니다.</div>}
                </div>
            </div>

            {/* Holdings Table */}
            <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden shadow-lg">
                <div className="p-4 border-b border-slate-700 flex items-center gap-2 bg-slate-800/50">
                    <PieChart className="text-blue-400" size={20} />
                    <h3 className="font-bold text-lg text-white">보유 주식 현황</h3>
                </div>
                <div className="overflow-x-auto">
                    <table className="w-full text-left">
                        <thead className="bg-slate-700/50 text-slate-400 text-sm uppercase tracking-wider">
                            <tr>
                                <th className="p-4">종목명</th>
                                <th className="p-4">수량</th>
                                <th className="p-4">평균단가</th>
                                <th className="p-4">현재가</th>
                                <th className="p-4">평가손익</th>
                                <th className="p-4">수익률</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-700">
                            {holdings.map((h, i) => (
                                <tr key={i} className="hover:bg-slate-700/30 transition-colors">
                                    <td className="p-4 font-medium text-white">
                                        <div>{h.prdt_name}</div>
                                        <div className="text-xs text-slate-500">{h.pdno}</div>
                                    </td>
                                    <td className="p-4 text-slate-300">{h.hldg_qty}주</td>
                                    <td className="p-4 text-slate-300">{formatCurrency(h.pchs_avg_pric)}</td>
                                    <td className="p-4 text-slate-300">{formatCurrency(h.prpr)}</td>
                                    <td className={`p-4 font-medium ${Number(h.evlu_pfls_amt) > 0 ? 'text-red-400' : Number(h.evlu_pfls_amt) < 0 ? 'text-blue-400' : 'text-slate-400'}`}>
                                        {formatCurrency(h.evlu_pfls_amt)}
                                    </td>
                                     <td className={`p-4 font-medium ${Number(h.evlu_pfls_rt) > 0 ? 'text-red-400' : Number(h.evlu_pfls_rt) < 0 ? 'text-blue-400' : 'text-slate-400'}`}>
                                        {h.evlu_pfls_rt}%
                                    </td>
                                </tr>
                            ))}
                            {holdings.length === 0 && (
                                <tr>
                                    <td colSpan={6} className="p-8 text-center text-slate-500">보유 중인 주식이 없습니다.</td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* History Table */}
            <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden shadow-lg">
                <div className="p-4 border-b border-slate-700 flex items-center gap-2 bg-slate-800/50">
                    <Clock className="text-purple-400" size={20} />
                    <h3 className="font-bold text-lg text-white">매매 이력</h3>
                </div>
                <div className="overflow-x-auto max-h-96 overflow-y-auto custom-scrollbar">
                    <table className="w-full text-left">
                        <thead className="bg-slate-700/50 text-slate-400 text-sm uppercase tracking-wider">
                            <tr>
                                <th className="p-4">시간</th>
                                <th className="p-4">종목</th>
                                <th className="p-4">구분</th>
                                <th className="p-4">가격</th>
                                <th className="p-4">수량</th>
                                <th className="p-4">사유</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-700">
                            {history.map((tx, i) => (
                                <tr key={i} className="hover:bg-slate-700/30 transition-colors">
                                    <td className="p-4 text-slate-400 text-sm whitespace-nowrap">
                                        {new Date(tx.timestamp).toLocaleString('ko-KR')}
                                    </td>
                                    <td className="p-4 font-medium text-white">{tx.symbol}</td>
                                    <td className="p-4">
                                        <span className={`px-2 py-1 rounded text-xs font-bold ${
                                            tx.type === 'BUY' ? 'bg-red-500/10 text-red-400 border border-red-500/20' : 'bg-blue-500/10 text-blue-400 border border-blue-500/20'
                                        }`}>
                                            {tx.type === 'BUY' ? '매수' : '매도'}
                                        </span>
                                    </td>
                                    <td className="p-4 text-slate-300">{formatCurrency(tx.price)}</td>
                                    <td className="p-4 text-slate-300">{tx.quantity}</td>
                                    <td className="p-4 text-slate-400 text-sm max-w-xs truncate" title={tx.reason}>{tx.reason}</td>
                                </tr>
                            ))}
                             {history.length === 0 && (
                                <tr>
                                    <td colSpan={6} className="p-8 text-center text-slate-500">매매 이력이 없습니다.</td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}
