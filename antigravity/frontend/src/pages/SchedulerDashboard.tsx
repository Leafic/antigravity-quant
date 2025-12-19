import { useEffect, useState } from 'react';
import { Calendar, Clock, Play, CheckCircle, XCircle, RefreshCw, Activity, Plus, Trash2, Power, Database, CheckSquare, Square, AlertTriangle, CalendarDays } from 'lucide-react';
import { api } from '../services/api';
import { StockAutocomplete } from '../components/StockAutocomplete';
import { MissingDatesCalendar } from '../components/MissingDatesCalendar';
import { Skeleton } from '../components/ui/Skeleton';

export function SchedulerDashboard() {
    const [status, setStatus] = useState<any>(null);
    const [history, setHistory] = useState<any[]>([]);
    const [scheduledStocks, setScheduledStocks] = useState<any[]>([]);
    const [stockDataStatus, setStockDataStatus] = useState<{[key: string]: any}>({});
    const [loading, setLoading] = useState(true);
    const [triggering, setTriggering] = useState(false);
    const [showAddStock, setShowAddStock] = useState(false);
    const [newStock, setNewStock] = useState({ symbol: '', name: '', note: '' });
    const [collectionDays, setCollectionDays] = useState(100);
    const [useRange, setUseRange] = useState(false);
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [selectedSymbols, setSelectedSymbols] = useState<Set<string>>(new Set());
    const [calendarModal, setCalendarModal] = useState<{
        isOpen: boolean;
        symbol: string;
        stockName: string;
        missingDates: string[];
        minDate: string;
        maxDate: string;
    }>({
        isOpen: false,
        symbol: '',
        stockName: '',
        missingDates: [],
        minDate: '',
        maxDate: ''
    });

    useEffect(() => {
        // 기본 날짜 설정 (오늘부터 100일 전)
        const today = new Date();
        const hundredDaysAgo = new Date(today);
        hundredDaysAgo.setDate(today.getDate() - 100);
        setEndDate(today.toISOString().split('T')[0]);
        setStartDate(hundredDaysAgo.toISOString().split('T')[0]);

        fetchData();
        const interval = setInterval(fetchData, 10000);
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

            // 각 종목의 데이터 현황 조회
            const dataStatuses: {[key: string]: any} = {};
            for (const stock of stocksData) {
                try {
                    const dataStatus = await api.getStockDataStatus(stock.symbol);
                    dataStatuses[stock.symbol] = dataStatus;
                } catch (e) {
                    console.error(`Failed to fetch data status for ${stock.symbol}:`, e);
                }
            }
            setStockDataStatus(dataStatuses);
        } catch (e) {
            console.error('Failed to fetch scheduler data:', e);
        } finally {
            setLoading(false);
        }
    };

    const handleTriggerCollection = async () => {
        const symbolsToCollect = Array.from(selectedSymbols);
        const hasSelection = symbolsToCollect.length > 0;
        const targetDesc = hasSelection
            ? `선택된 ${symbolsToCollect.length}개 종목`
            : '모든 활성화 종목';

        if (useRange) {
            if (!startDate || !endDate) {
                alert('시작일과 종료일을 선택하세요.');
                return;
            }
            if (!confirm(`${targetDesc}의 ${startDate} ~ ${endDate} 기간 데이터 수집을 실행하시겠습니까?\n(이미 있는 데이터는 건너뜁니다)`)) return;

            setTriggering(true);
            try {
                const result = hasSelection
                    ? await api.triggerSelectedDataCollectionInRange(symbolsToCollect, startDate, endDate)
                    : await api.triggerDataCollectionInRange(startDate, endDate);
                if (result.success) {
                    alert(`데이터 수집 완료!\n신규 데이터: ${result.newDataCount}건`);
                } else {
                    alert(`일부 실패: ${result.message}`);
                }
                fetchData();
            } catch (e) {
                alert('데이터 수집 실행 실패: ' + e);
            } finally {
                setTriggering(false);
            }
        } else {
            if (!confirm(`${targetDesc}의 최근 ${collectionDays}일간 데이터 수집을 실행하시겠습니까?\n(이미 있는 데이터는 건너뜁니다)`)) return;

            setTriggering(true);
            try {
                const result = hasSelection
                    ? await api.triggerSelectedDataCollection(symbolsToCollect, collectionDays)
                    : await api.triggerDataCollection(collectionDays);
                if (result.success) {
                    alert(`데이터 수집 완료!\n신규 데이터: ${result.newDataCount || 0}건`);
                } else {
                    alert(`일부 실패: ${result.message || '알 수 없는 오류'}`);
                }
                fetchData();
            } catch (e) {
                alert('데이터 수집 실행 실패: ' + e);
            } finally {
                setTriggering(false);
            }
        }
    };

    const handleToggleSelect = (symbol: string) => {
        setSelectedSymbols(prev => {
            const newSet = new Set(prev);
            if (newSet.has(symbol)) {
                newSet.delete(symbol);
            } else {
                newSet.add(symbol);
            }
            return newSet;
        });
    };

    const handleSelectAll = () => {
        const enabledSymbols = scheduledStocks.filter(s => s.enabled).map(s => s.symbol);
        if (selectedSymbols.size === enabledSymbols.length) {
            setSelectedSymbols(new Set());
        } else {
            setSelectedSymbols(new Set(enabledSymbols));
        }
    };

    const handleCollectGaps = async () => {
        const symbolsToCollect = Array.from(selectedSymbols);

        if (symbolsToCollect.length === 0) {
            alert('갭 수집할 종목을 선택하세요.');
            return;
        }

        // 갭이 있는 종목만 필터링
        const symbolsWithGaps = symbolsToCollect.filter(symbol => {
            const status = stockDataStatus[symbol];
            return status?.hasGaps && status?.gapCount > 0;
        });

        if (symbolsWithGaps.length === 0) {
            alert('선택된 종목 중 빠진 날짜(갭)가 있는 종목이 없습니다.');
            return;
        }

        const totalGaps = symbolsWithGaps.reduce((sum, symbol) => {
            return sum + (stockDataStatus[symbol]?.gapCount || 0);
        }, 0);

        if (!confirm(`선택된 ${symbolsWithGaps.length}개 종목의 빠진 날짜(총 ${totalGaps}일)를 수집하시겠습니까?`)) return;

        setTriggering(true);
        try {
            const result = await api.collectGapsForSymbols(symbolsWithGaps);
            if (result.success) {
                alert(`갭 수집 완료!\n신규 데이터: ${result.newDataCount}건`);
            } else {
                alert(`일부 실패: ${result.message}`);
            }
            fetchData();
        } catch (e) {
            alert('갭 수집 실행 실패: ' + e);
        } finally {
            setTriggering(false);
        }
    };

    const handleOpenCalendar = (symbol: string, stockName: string, e: React.MouseEvent) => {
        e.stopPropagation();
        const status = stockDataStatus[symbol];
        if (!status?.missingDates || status.missingDates.length === 0) return;

        setCalendarModal({
            isOpen: true,
            symbol,
            stockName,
            missingDates: status.missingDates,
            minDate: status.minDate,
            maxDate: status.maxDate
        });
    };

    const handleCollectSingleGap = async (symbol: string, e: React.MouseEvent) => {
        e.stopPropagation();

        const status = stockDataStatus[symbol];
        if (!status?.hasGaps) return;

        if (!confirm(`${symbol}의 빠진 ${status.gapCount}일 데이터를 수집하시겠습니까?`)) return;

        setTriggering(true);
        try {
            const result = await api.collectGaps(symbol);
            if (result.success) {
                alert(`갭 수집 완료!\n신규 데이터: ${result.newDataCount}건`);
            } else {
                alert(`실패: ${result.message}`);
            }
            fetchData();
        } catch (e) {
            alert('갭 수집 실행 실패: ' + e);
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

    const handleDeleteStock = async (id: number, name: string, symbol: string) => {
        const stock = scheduledStocks.find(s => s.id === id);
        const dataStatus = stockDataStatus[symbol];
        const hasData = dataStatus?.hasData && dataStatus?.totalDays > 0;

        // 데이터가 있으면 삭제 옵션 제공
        let deleteData = false;
        if (hasData) {
            const choice = window.confirm(
                `${name} (${symbol}) 종목을 삭제하시겠습니까?\n\n` +
                `📊 수집된 데이터: ${dataStatus.totalDays}일 (${dataStatus.minDate} ~ ${dataStatus.maxDate})\n\n` +
                `[확인] 종목만 삭제 (데이터 유지)\n` +
                `[취소] 후 아래 질문에서 데이터 삭제 선택 가능`
            );

            if (choice) {
                // 종목만 삭제
                deleteData = false;
            } else {
                // 데이터도 함께 삭제할지 다시 확인
                const deleteDataChoice = window.confirm(
                    `${name}의 수집된 데이터(${dataStatus.totalDays}일)도 함께 삭제하시겠습니까?\n\n` +
                    `⚠️ 이 작업은 되돌릴 수 없습니다.\n\n` +
                    `[확인] 종목 + 데이터 모두 삭제\n` +
                    `[취소] 삭제 취소`
                );

                if (!deleteDataChoice) {
                    return; // 삭제 취소
                }
                deleteData = true;
            }
        } else {
            // 데이터가 없으면 단순 삭제 확인
            if (!confirm(`${name} 종목을 삭제하시겠습니까?`)) return;
        }

        try {
            const result = await api.deleteScheduledStock(id, deleteData);
            if (result.success) {
                alert(result.message || '종목이 삭제되었습니다');
                // 선택 목록에서도 제거
                if (stock) {
                    setSelectedSymbols(prev => {
                        const newSet = new Set(prev);
                        newSet.delete(stock.symbol);
                        return newSet;
                    });
                }
                fetchData();
            } else {
                alert('삭제 실패: ' + (result.message || '알 수 없는 오류'));
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
            <div className="min-h-screen bg-slate-900 text-white p-6">
                {/* Skeleton Header */}
                <div className="mb-8 flex items-center justify-between border-b border-slate-700 pb-4">
                    <div>
                        <Skeleton className="h-9 w-64 mb-2" />
                        <Skeleton className="h-4 w-48" />
                    </div>
                    <div className="flex items-center gap-4">
                        <Skeleton className="h-10 w-28" />
                        <Skeleton className="h-10 w-40" />
                    </div>
                </div>

                {/* Skeleton Data Collection Panel */}
                <div className="bg-slate-800 rounded-xl p-6 border border-slate-700 mb-6">
                    <Skeleton className="h-7 w-32 mb-4" />
                    <div className="flex flex-wrap items-center gap-4">
                        <Skeleton className="h-5 w-24" />
                        <Skeleton className="h-10 w-20" />
                        <Skeleton className="h-10 w-32" />
                        <Skeleton className="h-10 w-24" />
                    </div>
                    <Skeleton className="h-4 w-96 mt-3" />
                </div>

                {/* Skeleton Grid (Stats) */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
                    {[1, 2, 3].map((i) => (
                        <div key={i} className="bg-slate-800 rounded-xl p-6 border border-slate-700">
                            <Skeleton className="h-7 w-32 mb-4" />
                            <div className="space-y-3">
                                <Skeleton className="h-4 w-20" />
                                <Skeleton className="h-6 w-40" />
                                <Skeleton className="h-4 w-24 mt-2" />
                                <Skeleton className="h-6 w-32" />
                            </div>
                        </div>
                    ))}
                </div>

                {/* Skeleton Stock List */}
                <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
                    <div className="flex justify-between items-center mb-4">
                        <Skeleton className="h-8 w-48" />
                        <Skeleton className="h-10 w-32" />
                    </div>
                    <div className="space-y-2">
                        {[1, 2, 3, 4, 5].map((i) => (
                            <Skeleton key={i} className="h-20 w-full" />
                        ))}
                    </div>
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
                </div>
            </header>

            {/* 데이터 수집 패널 */}
            <div className="bg-slate-800 rounded-xl p-6 border border-slate-700 mb-6">
                <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
                    <Database size={20} className="text-blue-400" />
                    데이터 수집
                </h2>

                <div className="flex flex-wrap items-center gap-4">
                    {/* 수집 모드 선택 */}
                    <div className="flex items-center gap-2">
                        <label className="flex items-center gap-2 cursor-pointer">
                            <input
                                type="radio"
                                checked={!useRange}
                                onChange={() => setUseRange(false)}
                                className="w-4 h-4 text-blue-500"
                            />
                            <span className="text-sm text-slate-300">최근 N일</span>
                        </label>
                        <label className="flex items-center gap-2 cursor-pointer">
                            <input
                                type="radio"
                                checked={useRange}
                                onChange={() => setUseRange(true)}
                                className="w-4 h-4 text-blue-500"
                            />
                            <span className="text-sm text-slate-300">기간 지정</span>
                        </label>
                    </div>

                    {/* 최근 N일 모드 */}
                    {!useRange && (
                        <div className="flex items-center gap-2">
                            <span className="text-sm text-slate-400">최근</span>
                            <input
                                type="number"
                                value={collectionDays}
                                onChange={(e) => setCollectionDays(Number(e.target.value))}
                                min="1"
                                max="3650"
                                className="w-20 px-3 py-2 bg-slate-900 border border-slate-600 rounded-lg text-white text-center"
                            />
                            <span className="text-sm text-slate-400">일</span>
                        </div>
                    )}

                    {/* 기간 지정 모드 */}
                    {useRange && (
                        <div className="flex items-center gap-2">
                            <input
                                type="date"
                                value={startDate}
                                onChange={(e) => setStartDate(e.target.value)}
                                className="px-3 py-2 bg-slate-900 border border-slate-600 rounded-lg text-white"
                            />
                            <span className="text-slate-400">~</span>
                            <input
                                type="date"
                                value={endDate}
                                onChange={(e) => setEndDate(e.target.value)}
                                className="px-3 py-2 bg-slate-900 border border-slate-600 rounded-lg text-white"
                            />
                        </div>
                    )}

                    <button
                        onClick={handleTriggerCollection}
                        disabled={triggering}
                        className="flex items-center gap-2 px-4 py-2 bg-blue-500 text-white font-medium rounded-lg hover:bg-blue-600 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        <Play size={16} />
                        {triggering ? '수집 중...' : (selectedSymbols.size > 0 ? `선택 종목(${selectedSymbols.size}) 수집` : '전체 수집')}
                    </button>

                    <button
                        onClick={handleCollectGaps}
                        disabled={triggering || selectedSymbols.size === 0}
                        className="flex items-center gap-2 px-4 py-2 bg-amber-500 text-white font-medium rounded-lg hover:bg-amber-600 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                        title="선택된 종목의 빠진 날짜만 수집합니다"
                    >
                        <AlertTriangle size={16} />
                        {triggering ? '수집 중...' : '갭 수집'}
                    </button>
                </div>

                <p className="mt-3 text-xs text-slate-500">
                    * 이미 저장된 날짜의 데이터는 자동으로 건너뜁니다. 아래 종목 목록에서 수집할 종목을 선택하세요.
                    <br />
                    * <span className="text-amber-400">갭 수집</span>: 선택된 종목의 빠진 날짜(주말 제외)만 수집합니다.
                </p>
            </div>

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
                    <div className="flex items-center gap-4">
                        <h2 className="text-xl font-semibold">스케줄링 종목 관리</h2>
                        {scheduledStocks.filter(s => s.enabled).length > 0 && (
                            <button
                                onClick={handleSelectAll}
                                className="flex items-center gap-2 px-3 py-1.5 text-sm bg-slate-700 border border-slate-600 text-slate-300 rounded-lg hover:bg-slate-600 transition-all"
                            >
                                {selectedSymbols.size === scheduledStocks.filter(s => s.enabled).length ? (
                                    <>
                                        <CheckSquare size={14} />
                                        선택 해제
                                    </>
                                ) : (
                                    <>
                                        <Square size={14} />
                                        전체 선택
                                    </>
                                )}
                            </button>
                        )}
                        {selectedSymbols.size > 0 && (
                            <span className="text-sm text-blue-400">
                                {selectedSymbols.size}개 선택됨
                            </span>
                        )}
                    </div>
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
                        scheduledStocks.map((stock: any) => {
                            const dataStatus = stockDataStatus[stock.symbol];
                            const isSelected = selectedSymbols.has(stock.symbol);
                            return (
                                <div
                                    key={stock.id}
                                    className={`flex items-center justify-between p-4 rounded-lg border transition-all cursor-pointer ${
                                        isSelected
                                            ? 'bg-blue-500/10 border-blue-500/50 hover:bg-blue-500/20'
                                            : 'bg-slate-700/30 border-slate-600 hover:bg-slate-700/50'
                                    }`}
                                    onClick={() => stock.enabled && handleToggleSelect(stock.symbol)}
                                >
                                    <div className="flex items-center gap-4">
                                        {/* 체크박스 */}
                                        {stock.enabled && (
                                            <div
                                                className={`w-5 h-5 rounded border-2 flex items-center justify-center transition-all ${
                                                    isSelected
                                                        ? 'bg-blue-500 border-blue-500'
                                                        : 'border-slate-500 hover:border-slate-400'
                                                }`}
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    handleToggleSelect(stock.symbol);
                                                }}
                                            >
                                                {isSelected && (
                                                    <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                                                    </svg>
                                                )}
                                            </div>
                                        )}
                                        <button
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                handleToggleStock(stock.id);
                                            }}
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
                                            {/* 데이터 현황 표시 */}
                                            {dataStatus && (
                                                <div className="text-xs mt-1 flex items-center gap-2 flex-wrap">
                                                    {dataStatus.hasData ? (
                                                        <>
                                                            <span className="text-emerald-400">
                                                                <Database size={12} className="inline mr-1" />
                                                                {dataStatus.minDate} ~ {dataStatus.maxDate} ({dataStatus.totalDays}일)
                                                            </span>
                                                            {/* 신뢰도 배지 */}
                                                            {dataStatus.reliabilityLevel && (
                                                                <span
                                                                    className={`px-1.5 py-0.5 rounded text-xs font-medium ${
                                                                        dataStatus.reliabilityLevel === 'HIGH'
                                                                            ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                                                                            : dataStatus.reliabilityLevel === 'MEDIUM'
                                                                            ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30'
                                                                            : dataStatus.reliabilityLevel === 'LOW'
                                                                            ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                                                                            : 'bg-red-500/20 text-red-400 border border-red-500/30'
                                                                    }`}
                                                                    title={`완결성: ${dataStatus.completenessRate?.toFixed(1)}% (${dataStatus.totalDays}/${dataStatus.expectedTradingDays}일)`}
                                                                >
                                                                    {dataStatus.reliabilityLevel === 'HIGH' ? '신뢰' :
                                                                     dataStatus.reliabilityLevel === 'MEDIUM' ? '양호' :
                                                                     dataStatus.reliabilityLevel === 'LOW' ? '주의' : '불완전'}
                                                                    {' '}{dataStatus.completenessRate?.toFixed(0)}%
                                                                </span>
                                                            )}
                                                            {dataStatus.hasGaps && dataStatus.gapCount > 0 && (
                                                                <span className="text-amber-400 flex items-center gap-1">
                                                                    <AlertTriangle size={12} />
                                                                    빠진 {dataStatus.gapCount}일
                                                                    <button
                                                                        onClick={(e) => handleOpenCalendar(stock.symbol, stock.name, e)}
                                                                        className="ml-1 p-1 bg-amber-500/20 border border-amber-500/50 text-amber-400 rounded hover:bg-amber-500/30 transition-all"
                                                                        title="빠진 날짜 달력 보기"
                                                                    >
                                                                        <CalendarDays size={12} />
                                                                    </button>
                                                                    <button
                                                                        onClick={(e) => handleCollectSingleGap(stock.symbol, e)}
                                                                        disabled={triggering}
                                                                        className="px-2 py-0.5 bg-amber-500/20 border border-amber-500/50 text-amber-400 rounded hover:bg-amber-500/30 transition-all disabled:opacity-50"
                                                                    >
                                                                        수집
                                                                    </button>
                                                                </span>
                                                            )}
                                                        </>
                                                    ) : (
                                                        <span className="text-amber-400">
                                                            <Database size={12} className="inline mr-1" />
                                                            데이터 없음
                                                        </span>
                                                    )}
                                                </div>
                                            )}
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
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                handleDeleteStock(stock.id, stock.name, stock.symbol);
                                            }}
                                            className="p-2 text-red-400 hover:bg-red-500/20 rounded-lg transition-all"
                                        >
                                            <Trash2 size={16} />
                                        </button>
                                    </div>
                                </div>
                            );
                        })
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

            {/* Missing Dates Calendar Modal */}
            <MissingDatesCalendar
                isOpen={calendarModal.isOpen}
                onClose={() => setCalendarModal(prev => ({ ...prev, isOpen: false }))}
                symbol={calendarModal.symbol}
                stockName={calendarModal.stockName}
                missingDates={calendarModal.missingDates}
                minDate={calendarModal.minDate}
                maxDate={calendarModal.maxDate}
            />
        </div>
    );
}
