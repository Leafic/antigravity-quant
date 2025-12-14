import React, { useEffect, useRef, useState } from 'react';
import { createChart, ColorType, IChartApi, ISeriesApi, Time } from 'lightweight-charts';

interface StockChartProps {
    data: { time: string; open: number; high: number; low: number; close: number }[];
    colors?: {
        backgroundColor?: string;
        lineColor?: string;
        textColor?: string;
        areaTopColor?: string;
        areaBottomColor?: string;
    };
    onTimeframeChange?: (tf: string) => void;
    markers?: { time: string; type: 'BUY' | 'SELL'; text?: string }[];
    highlightTimestamp?: string | null;
    onChartClick?: (time: string) => void;
}

function calculateSMA(data: { time: any; close: number }[], period: number) {
    const smaData = [];
    for (let i = period - 1; i < data.length; i++) {
        const val = data.slice(i - period + 1, i + 1).reduce((a, b) => a + b.close, 0) / period;
        smaData.push({ time: data[i].time, value: val });
    }
    return smaData;
}

export const StockChart: React.FC<StockChartProps> = (props) => {
    const {
        data,
        colors: {
            backgroundColor = '#1e293b',
            textColor = '#cbd5e1',
        } = {},
        onTimeframeChange,
        highlightTimestamp,
        onChartClick
    } = props;

    const chartContainerRef = useRef<HTMLDivElement>(null);
    const chartRef = useRef<IChartApi | null>(null);
    const candleSeriesRef = useRef<ISeriesApi<"Candlestick"> | null>(null);
    const sma5Ref = useRef<ISeriesApi<"Line"> | null>(null);
    const sma20Ref = useRef<ISeriesApi<"Line"> | null>(null);
    const sma60Ref = useRef<ISeriesApi<"Line"> | null>(null);

    const [timeframe, setTimeframe] = useState('daily');

    // Init Chart
    useEffect(() => {
        if (!chartContainerRef.current) return;

        const handleResize = () => {
            chartRef.current?.applyOptions({ width: chartContainerRef.current!.clientWidth });
        };

        const chart = createChart(chartContainerRef.current, {
            layout: {
                background: { type: ColorType.Solid, color: backgroundColor },
                textColor,
            },
            width: chartContainerRef.current.clientWidth,
            height: 300,
            grid: {
                vertLines: { color: '#334155' },
                horzLines: { color: '#334155' },
            },
            timeScale: {
                timeVisible: true,
                secondsVisible: false,
            },
        });

        chartRef.current = chart;

        candleSeriesRef.current = chart.addCandlestickSeries({
            upColor: '#26a69a',
            downColor: '#ef5350',
            borderVisible: false,
            wickUpColor: '#26a69a',
            wickDownColor: '#ef5350',
        });

        sma20Ref.current = chart.addLineSeries({ color: '#4ade80', lineWidth: 1, title: 'SMA 20' }); // Green
        sma60Ref.current = chart.addLineSeries({ color: '#c084fc', lineWidth: 1, title: 'SMA 60' }); // Purple

        chart.subscribeClick((param) => {
            if (param.time && onChartClick) {
                const timeStr = param.time.toString();
                onChartClick(timeStr);
            }
        });

        window.addEventListener('resize', handleResize);

        return () => {
            window.removeEventListener('resize', handleResize);
            chart.remove();
        };
    }, []); // Run once

    // ... (UseEffect for data update omitted - no changes needed there from previous step)

    // Handle Highlight
    useEffect(() => {
        if (highlightTimestamp && chartRef.current) {
            try {
                const targetTime = highlightTimestamp.split('T')[0];
                const date = new Date(targetTime);
                const fromDate = new Date(date);
                fromDate.setDate(date.getDate() - 30);
                const toDate = new Date(date);
                toDate.setDate(date.getDate() + 10);

                const fromStr = fromDate.toISOString().split('T')[0] as Time;
                const toStr = toDate.toISOString().split('T')[0] as Time;

                chartRef.current.timeScale().setVisibleRange({ from: fromStr, to: toStr });
            } catch (e) {
                // Ignore
            }
        }
    }, [highlightTimestamp]);

    // Update Data - 종목 변경 시 이전 데이터를 완전히 지우고 새로 그림
    useEffect(() => {
        if (!candleSeriesRef.current) return;

        // 먼저 마커를 초기화 (이전 종목의 마커 제거)
        // @ts-ignore
        candleSeriesRef.current.setMarkers([]);

        // 데이터가 없으면 차트 비우기
        if (!data || data.length === 0) {
            candleSeriesRef.current.setData([]);
            if (sma5Ref.current) sma5Ref.current.setData([]);
            if (sma20Ref.current) sma20Ref.current.setData([]);
            if (sma60Ref.current) sma60Ref.current.setData([]);
            return;
        }

        // Cast time to Time type for lightweight-charts
        const formattedData = data.map(d => ({ ...d, time: d.time as Time }));
        candleSeriesRef.current.setData(formattedData);

        // Update SMA
        if (sma5Ref.current) sma5Ref.current.setData(calculateSMA(formattedData, 5).map(d => ({ ...d, time: d.time as Time })));
        if (sma20Ref.current) sma20Ref.current.setData(calculateSMA(formattedData, 20).map(d => ({ ...d, time: d.time as Time })));
        if (sma60Ref.current) sma60Ref.current.setData(calculateSMA(formattedData, 60).map(d => ({ ...d, time: d.time as Time })));

        // 차트 시간 범위를 데이터에 맞게 리셋
        if (chartRef.current) {
            chartRef.current.timeScale().fitContent();
        }

        // Set Markers (새 데이터에 대한 마커)
        if (props.markers && props.markers.length > 0 && candleSeriesRef.current) {
            const markers = props.markers.map(m => ({
                time: m.time.split('T')[0] as Time,
                position: m.type === 'BUY' ? 'belowBar' : 'aboveBar',
                color: m.type === 'BUY' ? '#ef4444' : '#3b82f6',
                shape: m.type === 'BUY' ? 'arrowUp' : 'arrowDown',
                size: 1,
            }));
            // @ts-ignore
            candleSeriesRef.current.setMarkers(markers);
        }

    }, [data, props.markers]);

    const handleTfChange = (tf: string) => {
        setTimeframe(tf);
        if (onTimeframeChange) onTimeframeChange(tf);
    }

    return (
        <div className="relative w-full">
            <div className="absolute top-2 right-2 z-10 flex gap-2">
                <button
                    onClick={() => handleTfChange('daily')}
                    className={`px-2 py-1 text-xs rounded ${timeframe === 'daily' ? 'bg-blue-600 text-white' : 'bg-slate-700 text-slate-300'}`}
                >
                    일봉
                </button>
                <button
                    onClick={() => handleTfChange('minute')}
                    className={`px-2 py-1 text-xs rounded ${timeframe === 'minute' ? 'bg-blue-600 text-white' : 'bg-slate-700 text-slate-300'}`}
                >
                    30분
                </button>
            </div>
            <div ref={chartContainerRef} className="w-full h-[300px]" />
        </div>
    );
};
