import { useEffect, useState } from 'react';
import { Calendar, Clock, Play, CheckCircle, XCircle, RefreshCw, Activity, Plus, Trash2, Power } from 'lucide-react';
import { api } from '../services/api';
import { StockAutocomplete } from '../components/StockAutocomplete';

export function SchedulerDashboard() {
    const [status, setStatus] = useState<any>(null);
    const [history, setHistory] = useState<any[]>([]);
    const [scheduledStocks, setScheduledStocks] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [triggering, setTriggering] = useState(false);
    const [showAddStock, setShowAddStock] = useState(false);
    const [newStock, setNewStock] = useState({ symbol: '', name: '', note: '' });
    const [collectionDays, setCollectionDays] = useState(100);

    useEffect(() => {
        fetchData();
        const interval = setInterval(fetchData, 10000); // 10초마다 갱신
        return () => clearInterval(interval);
    }, []);

    const fetchData = async () => {
        try {
            const [statusData, historyData, stocksData] = await Promise.all([
                api.getSchedulerStatus(),
                api.getSchedulerHistory(20),
                api.getScheduledStocks()
            ]);
            setStatus(statusData);
            setHistory(historyData);
            setScheduledStocks(stocksData);
        } catch (e) {
            console.error('Failed to fetch scheduler data:', e);
        } finally {
            setLoading(false);
        }
    };

    const handleTriggerCollection = async () => {
        if (!confirm(`최근 ${collectionDays}일간 데이터 수집을 수동으로 실행하시겠습니까?`)) return;

        setTriggering(true);
        try {
            await api.triggerDataCollection(collectionDays);
            alert('데이터 수집이 시작되었습니다. 히스토리에서 확인하세요.');
            fetchData();
        } catch (e) {
            alert('데이터 수집 실행 실패: ' + e);
        } finally {
            setTriggering(false);
        }
    };

    const handleSyncStockMaster = async () => {
        if (!confirm('종목 마스터 데이터를 동기화하시겠습니까?\n(KOSPI + KOSDAQ 전체 종목 정보를 다운로드합니다)')) return;

        setTriggering(true);
        try {
            const result = await api.syncStockMaster();
            if (result.success) {
                alert(`종목 마스터 동기화 완료!\n총 ${result.totalCount}개 종목 (KOSPI: ${result.kospiCount}, KOSDAQ: ${result.kosdaqCount})`);
                fetchData();
            } else {
                alert('동기화 실패: ' + result.message);
            }
        } catch (e) {
            alert('종목 마스터 동기화 실패: ' + e);
        } finally {
            setTriggering(false);
        }
    };

    const handleAddStock = async () => {
        if (!newStock.symbol || !newStock.name) {
            alert('종목코드와 종목명을 입력하세요');
            return;
        }

        try {
            const result = await api.addScheduledStock(newStock.symbol, newStock.name, newStock.note);
            if (result.success) {
                alert('종목이 추가되었습니다');
                setNewStock({ symbol: '', name: '', note: '' });
                setShowAddStock(false);
                fetchData();
            } else {
                alert(result.message || '종목 추가 실패');
            }
        } catch (e) {
            alert('종목 추가 실패: ' + e);
        }
    };

    const handleToggleStock = async (id: number) => {
        try {
            const result = await api.toggleScheduledStock(id);
            if (result.success) {
                fetchData();
            }
        } catch (e) {
            alert('상태 변경 실패: ' + e);
        }
    };

    const handleDeleteStock = async (id: number, name: string) => {
        if (!confirm(`${name} 종목을 삭제하시겠습니까?`)) return;

        try {
            const result = await api.deleteScheduledStock(id);
            if (result.success) {
                alert('종목이 삭제되었습니다');
                fetchData();
            }
        } catch (e) {
            alert('종목 삭제 실패: ' + e);
        }
    };

    const formatDateTime = (dateStr: string) => {
        if (!dateStr) return '-';
        const date = new Date(dateStr);
        return date.toLocaleString('ko-KR');
    };

    const formatDuration = (seconds: number | null) => {
        if (!seconds) return '-';
        if (seconds < 60) return `${seconds}초`;
        const minutes = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${minutes}분 ${secs}초`;
    };

    const getStatusBadge = (statusStr: string) => {
        switch (statusStr) {
            case 'SUCCESS':
                return (
                    <span className="inline-flex items-center gap-1 px-2 py-1 text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 rounded-full">
                        <CheckCircle size={12} />
                        성공
                    </span>
                );
            case 'FAILED':
                return (
                    <span className="inline-flex items-center gap-1 px-2 py-1 text-xs font-medium bg-red-500/10 text-red-400 border border-red-500/30 rounded-full">
                        <XCircle size={12} />
                        실패
                    </span>
                );
            case 'RUNNING':
                return (
                    <span className="inline-flex items-center gap-1 px-2 py-1 text-xs font-medium bg-blue-500/10 text-blue-400 border border-blue-500/30 rounded-full animate-pulse">
                        <Activity size={12} />
                        실행중
                    </span>
                );
            default:
                return <span className="text-xs text-slate-500">{statusStr}</span>;
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-slate-900 text-white flex items-center justify-center">
                <div className="text-center">
                    <RefreshCw className="animate-spin mx-auto mb-4" size={48} />
                    <p className="text-slate-400">로딩 중...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-slate-900 text-white p-6">
            <header className="mb-8 flex items-center justify-between border-b border-slate-700 pb-4">
                <div>
                    <h1 className="text-3xl font-bold bg-gradient-to-r from-purple-400 to-pink-500 bg-clip-text text-transparent">
                        스케줄러 관리 대시보드
                    </h1>
                    <p className="text-slate-400 text-sm">데이터 수집 스케줄러 모니터링</p>
                </div>
                <div className="flex items-center gap-4">
                    <button
                        onClick={fetchData}
                        className="flex items-center gap-2 px-4 py-2 bg-slate-800 border border-slate-600 text-slate-300 rounded-lg hover:bg-slate-700 transition-all"
                    >
                        <RefreshCw size={16} />
                        새로고침
                    </button>
                    <button
                        onClick={handleSyncStockMaster}
                        disabled={triggering}
                        className="flex items-center gap-2 px-4 py-2 bg-purple-500/10 border border-purple-500 text-purple-400 rounded-lg hover:bg-purple-500/20 transition-all disabled:opacity-50"
                    >
                        <RefreshCw size={16} />
                        {triggering ? '동기화 중...' : '종목 마스터 동기화'}
                    </button>
                    <div className="flex items-center gap-2">
                        <input
                            type="number"
                            value={collectionDays}
                            onChange={(e) => setCollectionDays(Number(e.target.value))}
                            min="1"
                            max="3650"
                            className="w-20 px-3 py-2 bg-slate-900 border border-slate-600 rounded-lg text-white text-center"
                        />
                        <span className="text-sm text-slate-400">일</span>
                        <button
                            onClick={handleTriggerCollection}
                            disabled={triggering}
                            className="flex items-center gap-2 px-4 py-2 bg-blue-500/10 border border-blue-500 text-blue-400 rounded-lg hover:bg-blue-500/20 transition-all disabled:opacity-50"
                        >
                            <Play size={16} />
                            {triggering ? '실행 중...' : '데이터 수집 실행'}
                        </button>
                    </div>
                </div>
            </header>

            <main className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
                {/* 스케줄러 상태 */}
                <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
                    <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
                        <Calendar size={20} className="text-purple-400" />
                        스케줄러 상태
                    </h2>
                    <div className="space-y-3">
                        <div>
                            <div className="text-sm text-slate-500">활성화 여부</div>
                            <div className={`text-lg font-bold ${status?.enabled ? 'text-emerald-400' : 'text-red-400'}`}>
                                {status?.enabled ? '✓ 활성화됨' : '✗ 비활성화됨'}
                            </div>
                        </div>
                        <div>
                            <div className="text-sm text-slate-500">Cron 표현식</div>
                            <div className="text-sm font-mono text-slate-300">{status?.cronExpression}</div>
                        </div>
                        <div>
                            <div className="text-sm text-slate-500">다음 실행 예정</div>
                            <div className="text-sm text-blue-400">{status?.nextScheduledTime ? formatDateTime(status.nextScheduledTime) : '-'}</div>
                        </div>
                    </div>
                </div>

                {/* 마지막 실행 정보 */}
                <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
                    <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
                        <Clock size={20} className="text-blue-400" />
                        마지막 실행
                    </h2>
                    {status?.lastRun ? (
                        <div className="space-y-3">
                            <div>
                                <div className="text-sm text-slate-500">상태</div>
                                {getStatusBadge(status.lastRun.status)}
                            </div>
                            <div>
                                <div className="text-sm text-slate-500">시작 시간</div>
                                <div className="text-sm text-slate-300">{formatDateTime(status.lastRun.startTime)}</div>
                            </div>
                            <div>
                                <div className="text-sm text-slate-500">소요 시간</div>
                                <div className="text-sm text-slate-300">{formatDuration(status.lastRun.duration)}</div>
                            </div>
                        </div>
                    ) : (
                        <p className="text-slate-500 text-sm">실행 기록 없음</p>
                    )}
                </div>

                {/* 통계 */}
                <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
                    <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
                        <Activity size={20} className="text-emerald-400" />
                        실행 통계
                    </h2>
                    {status?.statistics ? (
                        <div className="space-y-3">
                            <div>
                                <div className="text-sm text-slate-500">총 실행 횟수</div>
                                <div className="text-2xl font-bold text-slate-200">{status.statistics.totalRuns}</div>
                            </div>
                            <div className="flex justify-between">
                                <div>
                                    <div className="text-xs text-slate-500">성공</div>
                                    <div className="text-lg font-semibold text-emerald-400">{status.statistics.successRuns}</div>
                                </div>
                                <div>
                                    <div className="text-xs text-slate-500">실패</div>
                                    <div className="text-lg font-semibold text-red-400">{status.statistics.failedRuns}</div>
                                </div>
                            </div>
                            <div>
                                <div className="text-sm text-slate-500">성공률</div>
                                <div className="text-xl font-bold text-blue-400">{status.statistics.successRate}</div>
                            </div>
                        </div>
                    ) : (
                        <p className="text-slate-500 text-sm">통계 없음</p>
                    )}
                </div>
            </main>

            {/* 스케줄링 종목 관리 */}
            <div className="bg-slate-800 rounded-xl p-6 border border-slate-700 mb-8">
                <div className="flex justify-between items-center mb-4">
                    <h2 className="text-xl font-semibold">스케줄링 종목 관리</h2>
                    <button
                        onClick={() => setShowAddStock(!showAddStock)}
                        className="flex items-center gap-2 px-4 py-2 bg-blue-500/10 border border-blue-500 text-blue-400 rounded-lg hover:bg-blue-500/20 transition-all"
                    >
                        <Plus size={16} />
                        종목 추가
                    </button>
                </div>

                {/* 종목 추가 폼 */}
                {showAddStock && (
                    <div className="mb-4 p-4 bg-slate-700/50 rounded-lg border border-slate-600">
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                            <div>
                                <label className="block text-xs text-slate-400 mb-1">종목 검색</label>
                                <StockAutocomplete
                                    value={newStock.symbol}
                                    onChange={(value) => setNewStock({ ...newStock, symbol: value })}
                                    onSelect={(stock) => setNewStock({
                                        ...newStock,
                                        symbol: stock.code,
                                        name: stock.name
                                    })}
                                    placeholder="종목명 또는 코드 검색..."
                                />
                            </div>
                            <input
                                type="text"
                                placeholder="메모 (선택사항)"
                                value={newStock.note}
                                onChange={(e) => setNewStock({ ...newStock, note: e.target.value })}
                                className="px-3 py-2 bg-slate-800 border border-slate-600 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
                            />
                        </div>
                        <div className="flex gap-2">
                            <button
                                onClick={handleAddStock}
                                className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-all"
                            >
                                추가
                            </button>
                            <button
                                onClick={() => setShowAddStock(false)}
                                className="px-4 py-2 bg-slate-600 text-white rounded-lg hover:bg-slate-700 transition-all"
                            >
                                취소
                            </button>
                        </div>
                    </div>
                )}

                {/* 종목 목록 */}
                <div className="space-y-2">
                    {scheduledStocks.length > 0 ? (
                        scheduledStocks.map((stock: any) => (
                            <div
                                key={stock.id}
                                className="flex items-center justify-between p-4 bg-slate-700/30 rounded-lg border border-slate-600 hover:bg-slate-700/50 transition-all"
                            >
                                <div className="flex items-center gap-4">
                                    <button
                                        onClick={() => handleToggleStock(stock.id)}
                                        className={`p-2 rounded-lg transition-all ${stock.enabled
                                            ? 'bg-emerald-500/20 text-emerald-400 hover:bg-emerald-500/30'
                                            : 'bg-slate-600/50 text-slate-500 hover:bg-slate-600'
                                            }`}
                                    >
                                        <Power size={16} />
                                    </button>
                                    <div>
                                        <div className="font-medium text-slate-200">
                                            {stock.name} <span className="text-slate-500">({stock.symbol})</span>
                                        </div>
                                        {stock.note && <div className="text-xs text-slate-400">{stock.note}</div>}
                                    </div>
                                </div>
                                <div className="flex items-center gap-2">
                                    <span className={`text-xs px-2 py-1 rounded-full ${stock.enabled
                                        ? 'bg-emerald-500/20 text-emerald-400'
                                        : 'bg-slate-600/50 text-slate-500'
                                        }`}>
                                        {stock.enabled ? '활성화' : '비활성화'}
                                    </span>
                                    <button
                                        onClick={() => handleDeleteStock(stock.id, stock.name)}
                                        className="p-2 text-red-400 hover:bg-red-500/20 rounded-lg transition-all"
                                    >
                                        <Trash2 size={16} />
                                    </button>
                                </div>
                            </div>
                        ))
                    ) : (
                        <p className="text-center text-slate-500 py-8">
                            등록된 종목이 없습니다. 종목을 추가하여 스케줄링을 시작하세요.
                        </p>
                    )}
                </div>
            </div>

            {/* 실행 히스토리 */}
            <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
                <h2 className="text-xl font-semibold mb-4">실행 히스토리</h2>
                <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                        <thead>
                            <tr className="border-b border-slate-700">
                                <th className="text-left py-3 px-4 text-slate-400 font-medium">ID</th>
                                <th className="text-left py-3 px-4 text-slate-400 font-medium">Job</th>
                                <th className="text-left py-3 px-4 text-slate-400 font-medium">상태</th>
                                <th className="text-left py-3 px-4 text-slate-400 font-medium">시작 시간</th>
                                <th className="text-left py-3 px-4 text-slate-400 font-medium">소요 시간</th>
                                <th className="text-left py-3 px-4 text-slate-400 font-medium">메시지</th>
                            </tr>
                        </thead>
                        <tbody>
                            {history.length > 0 ? (
                                history.map((h: any) => (
                                    <tr key={h.id} className="border-b border-slate-700/50 hover:bg-slate-700/30">
                                        <td className="py-3 px-4 text-slate-400">#{h.id}</td>
                                        <td className="py-3 px-4 font-mono text-xs text-slate-300">{h.jobName}</td>
                                        <td className="py-3 px-4">{getStatusBadge(h.status)}</td>
                                        <td className="py-3 px-4 text-slate-300">{formatDateTime(h.startTime)}</td>
                                        <td className="py-3 px-4 text-slate-300">
                                            {h.endTime ? formatDuration(Math.floor((new Date(h.endTime).getTime() - new Date(h.startTime).getTime()) / 1000)) : '-'}
                                        </td>
                                        <td className="py-3 px-4 text-slate-400 max-w-xs truncate">{h.message || '-'}</td>
                                    </tr>
                                ))
                            ) : (
                                <tr>
                                    <td colSpan={6} className="py-8 text-center text-slate-500">
                                        실행 히스토리가 없습니다
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}
