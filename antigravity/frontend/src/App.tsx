import { useState } from 'react';
import { LayoutDashboard, Calendar, Database, Menu, X } from 'lucide-react';
import { TradingDashboard } from './pages/TradingDashboard';
import { SchedulerDashboard } from './pages/SchedulerDashboard';
import { StockMasterPage } from './pages/StockMasterPage';

function App() {
    const [currentPage, setCurrentPage] = useState<'trading' | 'scheduler' | 'stocks'>('trading');
    const [menuOpen, setMenuOpen] = useState(false);

    const navItems = [
        { id: 'trading', label: '트레이딩', icon: LayoutDashboard },
        { id: 'scheduler', label: '스케줄러', icon: Calendar },
        { id: 'stocks', label: '종목 관리', icon: Database },
    ];

    return (
        <div className="min-h-screen bg-slate-900 text-white font-sans flex">
            {/* 사이드바 */}
            <aside className={`fixed lg:static inset-y-0 left-0 z-50 w-64 bg-slate-800 border-r border-slate-700 transform transition-transform lg:transform-none ${menuOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}`}>
                <div className="p-6 border-b border-slate-700 flex justify-between items-center">
                    <h1 className="text-xl font-bold bg-gradient-to-r from-blue-400 to-indigo-500 bg-clip-text text-transparent">
                        AntiGravity
                    </h1>
                    <button
                        onClick={() => setMenuOpen(false)}
                        className="lg:hidden text-slate-400 hover:text-white"
                    >
                        <X size={24} />
                    </button>
                </div>

                <nav className="p-4 space-y-2">
                    {navItems.map((item) => {
                        const Icon = item.icon;
                        const isActive = currentPage === item.id;
                        return (
                            <button
                                key={item.id}
                                onClick={() => {
                                    setCurrentPage(item.id as any);
                                    setMenuOpen(false);
                                }}
                                className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all ${isActive
                                    ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30'
                                    : 'text-slate-400 hover:bg-slate-700 hover:text-white'
                                    }`}
                            >
                                <Icon size={20} />
                                <span className="font-medium">{item.label}</span>
                            </button>
                        );
                    })}
                </nav>
            </aside>

            {/* 메인 콘텐츠 */}
            <div className="flex-1">
                {/* 모바일 헤더 */}
                <header className="lg:hidden bg-slate-800 border-b border-slate-700 p-4 flex items-center justify-between">
                    <button
                        onClick={() => setMenuOpen(true)}
                        className="text-slate-400 hover:text-white"
                    >
                        <Menu size={24} />
                    </button>
                    <h1 className="text-lg font-bold">AntiGravity</h1>
                    <div className="w-6"></div> {/* Spacer */}
                </header>

                {/* 페이지 콘텐츠 */}
                <div className="p-6">
                    {currentPage === 'trading' && <TradingDashboard />}
                    {currentPage === 'scheduler' && <SchedulerDashboard />}
                    {currentPage === 'stocks' && <StockMasterPage />}
                </div>
            </div>

            {/* 모바일 오버레이 */}
            {menuOpen && (
                <div
                    className="fixed inset-0 bg-black/50 z-40 lg:hidden"
                    onClick={() => setMenuOpen(false)}
                ></div>
            )}
        </div>
    );
}

export default App;
