import { useState, useMemo } from 'react';
import { X, ChevronLeft, ChevronRight, Calendar } from 'lucide-react';

interface MissingDatesCalendarProps {
    isOpen: boolean;
    onClose: () => void;
    symbol: string;
    stockName: string;
    missingDates: string[];  // YYYY-MM-DD 형식의 날짜 배열
    minDate: string;
    maxDate: string;
}

export function MissingDatesCalendar({
    isOpen,
    onClose,
    symbol,
    stockName,
    missingDates,
    minDate,
    maxDate
}: MissingDatesCalendarProps) {
    // 현재 보고 있는 년/월
    const [currentYear, setCurrentYear] = useState(() => {
        // 가장 최근 빠진 날짜가 있는 달로 시작
        if (missingDates.length > 0) {
            const sortedDates = [...missingDates].sort().reverse();
            return new Date(sortedDates[0]).getFullYear();
        }
        return new Date().getFullYear();
    });
    const [currentMonth, setCurrentMonth] = useState(() => {
        if (missingDates.length > 0) {
            const sortedDates = [...missingDates].sort().reverse();
            return new Date(sortedDates[0]).getMonth();
        }
        return new Date().getMonth();
    });

    // 빠진 날짜를 Set으로 변환 (빠른 조회용)
    const missingDateSet = useMemo(() => new Set(missingDates), [missingDates]);

    // 데이터 범위 파싱
    const dataMinDate = useMemo(() => minDate ? new Date(minDate) : null, [minDate]);
    const dataMaxDate = useMemo(() => maxDate ? new Date(maxDate) : null, [maxDate]);

    // 해당 월의 달력 데이터 생성
    const calendarData = useMemo(() => {
        const firstDay = new Date(currentYear, currentMonth, 1);
        const lastDay = new Date(currentYear, currentMonth + 1, 0);
        const daysInMonth = lastDay.getDate();
        const startingDayOfWeek = firstDay.getDay(); // 0 = Sunday

        const weeks: (Date | null)[][] = [];
        let currentWeek: (Date | null)[] = [];

        // 첫 주의 빈 칸
        for (let i = 0; i < startingDayOfWeek; i++) {
            currentWeek.push(null);
        }

        // 날짜 채우기
        for (let day = 1; day <= daysInMonth; day++) {
            const date = new Date(currentYear, currentMonth, day);
            currentWeek.push(date);

            if (currentWeek.length === 7) {
                weeks.push(currentWeek);
                currentWeek = [];
            }
        }

        // 마지막 주의 빈 칸
        if (currentWeek.length > 0) {
            while (currentWeek.length < 7) {
                currentWeek.push(null);
            }
            weeks.push(currentWeek);
        }

        return weeks;
    }, [currentYear, currentMonth]);

    // 이전/다음 달로 이동
    const goToPrevMonth = () => {
        if (currentMonth === 0) {
            setCurrentMonth(11);
            setCurrentYear(currentYear - 1);
        } else {
            setCurrentMonth(currentMonth - 1);
        }
    };

    const goToNextMonth = () => {
        if (currentMonth === 11) {
            setCurrentMonth(0);
            setCurrentYear(currentYear + 1);
        } else {
            setCurrentMonth(currentMonth + 1);
        }
    };

    // 날짜 상태 판별
    const getDateStatus = (date: Date) => {
        const dateStr = date.toISOString().split('T')[0];
        const dayOfWeek = date.getDay();
        const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;

        // 데이터 범위 밖
        if (dataMinDate && date < dataMinDate) return 'out-of-range';
        if (dataMaxDate && date > dataMaxDate) return 'out-of-range';

        // 주말
        if (isWeekend) return 'weekend';

        // 빠진 날짜
        if (missingDateSet.has(dateStr)) return 'missing';

        // 정상 데이터
        return 'normal';
    };

    // 현재 월의 빠진 날짜 수
    const currentMonthMissingCount = useMemo(() => {
        return missingDates.filter(dateStr => {
            const date = new Date(dateStr);
            return date.getFullYear() === currentYear && date.getMonth() === currentMonth;
        }).length;
    }, [missingDates, currentYear, currentMonth]);

    // 월별 빠진 날짜 요약
    const monthlySummary = useMemo(() => {
        const summary: { [key: string]: number } = {};
        missingDates.forEach(dateStr => {
            const date = new Date(dateStr);
            const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
            summary[key] = (summary[key] || 0) + 1;
        });
        return Object.entries(summary)
            .sort(([a], [b]) => b.localeCompare(a))
            .slice(0, 6);
    }, [missingDates]);

    if (!isOpen) return null;

    const weekDays = ['일', '월', '화', '수', '목', '금', '토'];
    const monthNames = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'];

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" onClick={onClose}>
            <div
                className="bg-slate-900 rounded-xl border border-slate-700 shadow-2xl max-w-lg w-full mx-4 max-h-[90vh] overflow-hidden"
                onClick={(e) => e.stopPropagation()}
            >
                {/* Header */}
                <div className="flex items-center justify-between p-4 border-b border-slate-700">
                    <div className="flex items-center gap-3">
                        <Calendar size={20} className="text-amber-400" />
                        <div>
                            <h3 className="font-bold text-white">{stockName} ({symbol})</h3>
                            <p className="text-xs text-slate-400">빠진 날짜: 총 {missingDates.length}일</p>
                        </div>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 hover:bg-slate-700 rounded-lg transition-colors"
                    >
                        <X size={20} className="text-slate-400" />
                    </button>
                </div>

                {/* Calendar Navigation */}
                <div className="flex items-center justify-between px-4 py-3 border-b border-slate-700/50">
                    <button
                        onClick={goToPrevMonth}
                        className="p-2 hover:bg-slate-700 rounded-lg transition-colors"
                    >
                        <ChevronLeft size={20} className="text-slate-400" />
                    </button>
                    <div className="text-center">
                        <span className="text-lg font-semibold text-white">
                            {currentYear}년 {monthNames[currentMonth]}
                        </span>
                        {currentMonthMissingCount > 0 && (
                            <span className="ml-2 px-2 py-0.5 bg-red-500/20 text-red-400 text-xs rounded-full">
                                {currentMonthMissingCount}일 누락
                            </span>
                        )}
                    </div>
                    <button
                        onClick={goToNextMonth}
                        className="p-2 hover:bg-slate-700 rounded-lg transition-colors"
                    >
                        <ChevronRight size={20} className="text-slate-400" />
                    </button>
                </div>

                {/* Calendar Grid */}
                <div className="p-4">
                    {/* Week Day Headers */}
                    <div className="grid grid-cols-7 gap-1 mb-2">
                        {weekDays.map((day, idx) => (
                            <div
                                key={day}
                                className={`text-center text-xs font-medium py-1 ${
                                    idx === 0 ? 'text-red-400' : idx === 6 ? 'text-blue-400' : 'text-slate-500'
                                }`}
                            >
                                {day}
                            </div>
                        ))}
                    </div>

                    {/* Calendar Days */}
                    <div className="space-y-1">
                        {calendarData.map((week, weekIdx) => (
                            <div key={weekIdx} className="grid grid-cols-7 gap-1">
                                {week.map((date, dayIdx) => {
                                    if (!date) {
                                        return <div key={dayIdx} className="aspect-square" />;
                                    }

                                    const status = getDateStatus(date);
                                    const isToday = date.toDateString() === new Date().toDateString();

                                    return (
                                        <div
                                            key={dayIdx}
                                            className={`aspect-square flex items-center justify-center text-sm rounded-lg transition-all relative ${
                                                status === 'missing'
                                                    ? 'bg-red-500/30 text-red-400 font-bold border-2 border-red-500/50'
                                                    : status === 'weekend'
                                                    ? 'bg-slate-800/50 text-slate-600'
                                                    : status === 'out-of-range'
                                                    ? 'text-slate-700'
                                                    : 'bg-emerald-500/10 text-emerald-400'
                                            } ${isToday ? 'ring-2 ring-blue-500' : ''}`}
                                            title={
                                                status === 'missing'
                                                    ? `${date.toLocaleDateString('ko-KR')} - 데이터 누락`
                                                    : status === 'weekend'
                                                    ? '주말'
                                                    : status === 'out-of-range'
                                                    ? '데이터 범위 외'
                                                    : '데이터 있음'
                                            }
                                        >
                                            {date.getDate()}
                                            {status === 'missing' && (
                                                <span className="absolute -top-0.5 -right-0.5 w-2 h-2 bg-red-500 rounded-full" />
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                        ))}
                    </div>
                </div>

                {/* Legend */}
                <div className="px-4 py-3 border-t border-slate-700/50 bg-slate-800/50">
                    <div className="flex flex-wrap items-center gap-4 text-xs">
                        <div className="flex items-center gap-1.5">
                            <div className="w-4 h-4 rounded bg-red-500/30 border-2 border-red-500/50" />
                            <span className="text-slate-400">누락</span>
                        </div>
                        <div className="flex items-center gap-1.5">
                            <div className="w-4 h-4 rounded bg-emerald-500/10" />
                            <span className="text-slate-400">정상</span>
                        </div>
                        <div className="flex items-center gap-1.5">
                            <div className="w-4 h-4 rounded bg-slate-800/50" />
                            <span className="text-slate-400">주말</span>
                        </div>
                    </div>
                </div>

                {/* Monthly Summary */}
                {monthlySummary.length > 0 && (
                    <div className="px-4 py-3 border-t border-slate-700/50">
                        <h4 className="text-xs font-medium text-slate-400 mb-2">월별 누락 현황</h4>
                        <div className="flex flex-wrap gap-2">
                            {monthlySummary.map(([monthKey, count]) => {
                                const [year, month] = monthKey.split('-');
                                return (
                                    <button
                                        key={monthKey}
                                        onClick={() => {
                                            setCurrentYear(parseInt(year));
                                            setCurrentMonth(parseInt(month) - 1);
                                        }}
                                        className={`px-2 py-1 text-xs rounded transition-all ${
                                            currentYear === parseInt(year) && currentMonth === parseInt(month) - 1
                                                ? 'bg-amber-500/30 text-amber-400 border border-amber-500/50'
                                                : 'bg-slate-700/50 text-slate-400 hover:bg-slate-700'
                                        }`}
                                    >
                                        {year}.{month} ({count}일)
                                    </button>
                                );
                            })}
                        </div>
                    </div>
                )}

                {/* Data Range Info */}
                <div className="px-4 py-3 border-t border-slate-700/50 text-xs text-slate-500">
                    데이터 범위: {minDate} ~ {maxDate}
                </div>
            </div>
        </div>
    );
}
