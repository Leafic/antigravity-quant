import { useEffect, useState } from 'react';
import { Search, Star, Database, ChevronLeft, ChevronRight, X } from 'lucide-react';
import { api } from '../services/api';

export function StockMasterPage() {
    const [stocks, setStocks] = useState<any[]>([]);
    const [stats, setStats] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalItems, setTotalItems] = useState(0);
    const [searchInput, setSearchInput] = useState(''); // 입력용
    const [searchQuery, setSearchQuery] = useState(''); // 실제 검색용
    const [marketFilter, setMarketFilter] = useState<string>('');
    const [favoriteOnly, setFavoriteOnly] = useState(false);
    const [sortBy, setSortBy] = useState('name');
    const [sortOrder, setSortOrder] = useState('asc');
    const [pageSize, setPageSize] = useState(50);

    useEffect(() => {
        fetchData();
    }, [page, marketFilter, favoriteOnly, sortBy, sortOrder, searchQuery, pageSize]);

    useEffect(() => {
        fetchStats();
    }, []);

    const fetchData = async () => {
        setLoading(true);
        try {
            const response = await api.getStockMasters({
                page,
                size: pageSize,
                query: searchQuery || undefined,
                market: marketFilter || undefined,
                favoriteOnly,
                sortBy,
                sortOrder
            });

            setStocks(response.content);
            setTotalPages(response.totalPages);
            setTotalItems(response.totalItems);
        } catch (e) {
            console.error('Failed to fetch stocks:', e);
        } finally {
            setLoading(false);
        }
    };

    const fetchStats = async () => {
        try {
            const statsData = await api.getStockMasterStats();
            setStats(statsData);
        } catch (e) {
            console.error('Failed to fetch stats:', e);
        }
    };

    const handleToggleFavorite = async (code: string) => {
        try {
            const result = await api.toggleStockFavorite(code);
            if (result.success) {
                fetchData();
                fetchStats();
            }
        } catch (e) {
            alert('즐겨찾기 변경 실패: ' + e);
        }
    };

    const handleSearch = (e: React.FormEvent) => {
        e.preventDefault();
        setSearchQuery(searchInput); // 입력값을 실제 검색어로 설정
        setPage(0);
    };

    const handleClearFilters = () => {
        setSearchInput('');
        setSearchQuery('');
        setMarketFilter('');
        setFavoriteOnly(false);
        setPage(0);
    };

    const formatDate = (dateStr: string) => {
        if (!dateStr || dateStr.length !== 8) return '-';
        return `${dateStr.substring(0, 4)}-${dateStr.substring(4, 6)}-${dateStr.substring(6, 8)}`;
    };

    if (loading && page === 0) {
        return (
            <div className="min-h-screen bg-slate-900 text-white flex items-center justify-center">
                <div className="text-center">
                    <Database className="animate-pulse mx-auto mb-4" size={48} />
                    <p className="text-slate-400">로딩 중...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-slate-900 text-white p-6">
            {/* Header */}
            <header className="mb-8 border-b border-slate-700 pb-4">
                <div className="flex items-center justify-between mb-4">
                    <div>
                        <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-400 to-cyan-500 bg-clip-text text-transparent">
                            종목 마스터 관리
                        </h1>
                        <p className="text-slate-400 text-sm">전체 종목 데이터 조회 및 즐겨찾기 관리</p>
                    </div>
                </div>

                {/* Stats */}
                {stats && (
                    <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mt-6">
                        <div className="bg-slate-800 rounded-lg p-4 border border-slate-700">
                            <div className="text-sm text-slate-400">전체 종목</div>
                            <div className="text-2xl font-bold text-white">{stats.totalCount.toLocaleString()}</div>
                        </div>
                        <div className="bg-slate-800 rounded-lg p-4 border border-blue-500/30">
                            <div className="text-sm text-slate-400">KOSPI</div>
                            <div className="text-2xl font-bold text-blue-400">{stats.kospiCount.toLocaleString()}</div>
                        </div>
                        <div className="bg-slate-800 rounded-lg p-4 border border-cyan-500/30">
                            <div className="text-sm text-slate-400">KOSDAQ</div>
                            <div className="text-2xl font-bold text-cyan-400">{stats.kosdaqCount.toLocaleString()}</div>
                        </div>
                        <div className="bg-slate-800 rounded-lg p-4 border border-purple-500/30">
                            <div className="text-sm text-slate-400">KONEX</div>
                            <div className="text-2xl font-bold text-purple-400">{stats.konexCount.toLocaleString()}</div>
                        </div>
                        <div className="bg-slate-800 rounded-lg p-4 border border-yellow-500/30">
                            <div className="text-sm text-slate-400">즐겨찾기</div>
                            <div className="text-2xl font-bold text-yellow-400">{stats.favoriteCount.toLocaleString()}</div>
                        </div>
                    </div>
                )}
            </header>

            {/* Filters */}
            <div className="bg-slate-800 rounded-xl p-6 mb-6 border border-slate-700">
                <form onSubmit={handleSearch} className="flex flex-col md:flex-row gap-4">
                    <div className="flex-1 relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400" size={20} />
                        <input
                            type="text"
                            value={searchInput}
                            onChange={(e) => setSearchInput(e.target.value)}
                            placeholder="종목명 또는 종목코드 검색..."
                            className="w-full pl-10 pr-4 py-2 bg-slate-900 border border-slate-600 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>

                    <select
                        value={marketFilter}
                        onChange={(e) => { setMarketFilter(e.target.value); setPage(0); }}
                        className="px-4 py-2 bg-slate-900 border border-slate-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                        <option value="">전체 시장</option>
                        <option value="KOSPI">KOSPI</option>
                        <option value="KOSDAQ">KOSDAQ</option>
                        <option value="KONEX">KONEX</option>
                    </select>

                    <button
                        type="button"
                        onClick={() => { setFavoriteOnly(!favoriteOnly); setPage(0); }}
                        className={`flex items-center gap-2 px-4 py-2 rounded-lg border transition-all ${
                            favoriteOnly
                                ? 'bg-yellow-500/20 border-yellow-500 text-yellow-400'
                                : 'bg-slate-900 border-slate-600 text-slate-400 hover:border-yellow-500'
                        }`}
                    >
                        <Star size={16} fill={favoriteOnly ? 'currentColor' : 'none'} />
                        즐겨찾기만
                    </button>

                    <button
                        type="button"
                        onClick={handleClearFilters}
                        className="flex items-center gap-2 px-4 py-2 bg-slate-900 border border-slate-600 text-slate-400 rounded-lg hover:border-red-500 hover:text-red-400 transition-all"
                    >
                        <X size={16} />
                        초기화
                    </button>

                    <button
                        type="submit"
                        className="px-6 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg transition-all"
                    >
                        검색
                    </button>
                </form>
            </div>

            {/* Results Info */}
            <div className="flex items-center justify-between mb-4">
                <div className="text-sm text-slate-400">
                    총 <span className="text-white font-semibold">{totalItems.toLocaleString()}</span>개 종목
                    {searchQuery && ` (검색어: "${searchQuery}")`}
                </div>
                <div className="flex items-center gap-4">
                    <div className="flex items-center gap-2">
                        <label className="text-sm text-slate-400">표시 개수:</label>
                        <select
                            value={pageSize}
                            onChange={(e) => { setPageSize(Number(e.target.value)); setPage(0); }}
                            className="px-3 py-1 bg-slate-800 border border-slate-600 rounded text-sm text-white"
                        >
                            <option value="20">20개</option>
                            <option value="50">50개</option>
                            <option value="100">100개</option>
                            <option value="200">200개</option>
                        </select>
                    </div>
                    <div className="flex items-center gap-2">
                        <label className="text-sm text-slate-400">정렬:</label>
                        <select
                            value={sortBy}
                            onChange={(e) => setSortBy(e.target.value)}
                            className="px-3 py-1 bg-slate-800 border border-slate-600 rounded text-sm text-white"
                        >
                            <option value="name">종목명</option>
                            <option value="code">종목코드</option>
                            <option value="market">시장</option>
                            <option value="listingDate">상장일</option>
                        </select>
                        <button
                            onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
                            className="px-3 py-1 bg-slate-800 border border-slate-600 rounded text-sm text-white hover:bg-slate-700"
                        >
                            {sortOrder === 'asc' ? '↑ 오름차순' : '↓ 내림차순'}
                        </button>
                    </div>
                </div>
            </div>

            {/* Table */}
            <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead className="bg-slate-900 border-b border-slate-700">
                            <tr>
                                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-400 uppercase w-12">
                                    <Star size={16} />
                                </th>
                                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-400 uppercase">종목코드</th>
                                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-400 uppercase">종목명</th>
                                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-400 uppercase">시장</th>
                                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-400 uppercase">업종</th>
                                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-400 uppercase">상장일</th>
                                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-400 uppercase">상태</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-700">
                            {stocks.map((stock) => (
                                <tr key={stock.code} className="hover:bg-slate-700/50 transition-colors">
                                    <td className="px-4 py-3">
                                        <button
                                            onClick={() => handleToggleFavorite(stock.code)}
                                            className="text-slate-400 hover:text-yellow-400 transition-colors"
                                        >
                                            <Star
                                                size={18}
                                                fill={stock.isFavorite ? 'currentColor' : 'none'}
                                                className={stock.isFavorite ? 'text-yellow-400' : ''}
                                            />
                                        </button>
                                    </td>
                                    <td className="px-4 py-3 font-mono text-sm text-blue-400">{stock.code}</td>
                                    <td className="px-4 py-3 font-medium text-white">{stock.name}</td>
                                    <td className="px-4 py-3">
                                        <span className={`inline-block px-2 py-1 text-xs rounded-full ${
                                            stock.market === 'KOSPI'
                                                ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30'
                                                : stock.market === 'KOSDAQ'
                                                ? 'bg-cyan-500/20 text-cyan-400 border border-cyan-500/30'
                                                : 'bg-purple-500/20 text-purple-400 border border-purple-500/30'
                                        }`}>
                                            {stock.market}
                                        </span>
                                    </td>
                                    <td className="px-4 py-3 text-sm text-slate-300">{stock.sector || '-'}</td>
                                    <td className="px-4 py-3 text-sm text-slate-400">{formatDate(stock.listingDate)}</td>
                                    <td className="px-4 py-3">
                                        <div className="flex gap-1">
                                            {stock.isManaged && (
                                                <span className="inline-block px-2 py-1 text-xs rounded bg-orange-500/20 text-orange-400 border border-orange-500/30">
                                                    관리
                                                </span>
                                            )}
                                            {stock.isSuspended && (
                                                <span className="inline-block px-2 py-1 text-xs rounded bg-red-500/20 text-red-400 border border-red-500/30">
                                                    정지
                                                </span>
                                            )}
                                            {!stock.isManaged && !stock.isSuspended && (
                                                <span className="text-xs text-slate-500">-</span>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

                {/* Pagination */}
                <div className="bg-slate-900 px-4 py-3 border-t border-slate-700 flex items-center justify-between">
                    <div className="text-sm text-slate-400">
                        페이지 {page + 1} / {totalPages} ({(page * pageSize) + 1}-{Math.min((page + 1) * pageSize, totalItems)} of {totalItems})
                    </div>
                    <div className="flex gap-2">
                        <button
                            onClick={() => setPage(Math.max(0, page - 1))}
                            disabled={page === 0}
                            className="px-3 py-1 bg-slate-800 border border-slate-600 rounded text-white disabled:opacity-30 disabled:cursor-not-allowed hover:bg-slate-700 transition-all flex items-center gap-1"
                        >
                            <ChevronLeft size={16} />
                            이전
                        </button>
                        <button
                            onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                            disabled={page >= totalPages - 1}
                            className="px-3 py-1 bg-slate-800 border border-slate-600 rounded text-white disabled:opacity-30 disabled:cursor-not-allowed hover:bg-slate-700 transition-all flex items-center gap-1"
                        >
                            다음
                            <ChevronRight size={16} />
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
