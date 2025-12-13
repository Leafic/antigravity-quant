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

        // Initialize SMA Series
        sma5Ref.current = chart.addLineSeries({ color: '#fbbf24', lineWidth: 1, title: 'SMA 5' }); // Amber
        sma20Ref.current = chart.addLineSeries({ color: '#4ade80', lineWidth: 1, title: 'SMA 20' }); // Green
        sma60Ref.current = chart.addLineSeries({ color: '#c084fc', lineWidth: 1, title: 'SMA 60' }); // Purple

        window.addEventListener('resize', handleResize);

        return () => {
            window.removeEventListener('resize', handleResize);
            chart.remove();
        };
    }, []); // Run once

    // Update Data
    useEffect(() => {
        if (!data || data.length === 0) return;
        if (!candleSeriesRef.current) return;

        // Cast time to Time type for lightweight-charts
        const formattedData = data.map(d => ({ ...d, time: d.time as Time }));
        candleSeriesRef.current.setData(formattedData);

        // Update SMA
        if (sma5Ref.current) sma5Ref.current.setData(calculateSMA(formattedData, 5).map(d => ({ ...d, time: d.time as Time })));
        if (sma20Ref.current) sma20Ref.current.setData(calculateSMA(formattedData, 20).map(d => ({ ...d, time: d.time as Time })));
        if (sma60Ref.current) sma60Ref.current.setData(calculateSMA(formattedData, 60).map(d => ({ ...d, time: d.time as Time })));

        if (sma60Ref.current) sma60Ref.current.setData(calculateSMA(formattedData, 60).map(d => ({ ...d, time: d.time as Time })));

        // Set Markers
        if (props.markers && candleSeriesRef.current) {
            const markers = props.markers.map(m => ({
                time: m.time.split('T')[0] as Time, // Assuming daily 'yyyy-MM-dd' or similar
                position: m.type === 'BUY' ? 'belowBar' : 'aboveBar',
                color: m.type === 'BUY' ? '#ef4444' : '#3b82f6',
                shape: m.type === 'BUY' ? 'arrowUp' : 'arrowDown',
                text: m.text || m.type,
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
