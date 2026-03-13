import { useQuery } from '@tanstack/react-query';
import { reportApi } from '../services/api';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';
import dayjs from 'dayjs';

function KpiCard({ title, value, sub, color = 'blue' }) {
  const colors = {
    blue: 'border-blue-500 bg-blue-50',
    green: 'border-green-500 bg-green-50',
    amber: 'border-amber-500 bg-amber-50',
    red: 'border-red-500 bg-red-50',
  };
  return (
    <div className={`rounded-xl border-l-4 p-5 shadow-sm ${colors[color]} bg-white`}>
      <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">{title}</p>
      <p className="text-2xl font-bold text-gray-800 mt-1">{value}</p>
      {sub && <p className="text-xs text-gray-500 mt-1">{sub}</p>}
    </div>
  );
}

export default function DashboardPage() {
  const today = dayjs().format('YYYY-MM-DD');

  const { data: daily } = useQuery({
    queryKey: ['daily-collections', today],
    queryFn: () => reportApi.dailyCollections(today).then(r => r.data),
  });

  const { data: outstanding } = useQuery({
    queryKey: ['outstanding'],
    queryFn: () => reportApi.outstanding().then(r => r.data),
  });

  const fmt = (n) => n != null ? `₹${Number(n).toLocaleString('en-IN')}` : '—';

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-xl font-bold text-gray-800">Dashboard</h1>
        <p className="text-sm text-gray-500">{dayjs().format('dddd, D MMMM YYYY')}</p>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <KpiCard
          title="Collected Today"
          value={fmt(daily?.totalCollected)}
          sub={`${daily?.transactionCount ?? 0} transactions`}
          color="green"
        />
        <KpiCard
          title="Matched Today"
          value={daily?.matchedCount ?? '—'}
          sub="reconciled transactions"
          color="blue"
        />
        <KpiCard
          title="Unmatched"
          value={daily?.unmatchedCount ?? '—'}
          sub="needs review"
          color="amber"
        />
        <KpiCard
          title="Outstanding"
          value={outstanding?.length ?? '—'}
          sub="unpaid / partial invoices"
          color="red"
        />
      </div>

      {/* Outstanding table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100">
        <div className="px-5 py-4 border-b border-gray-100">
          <h2 className="text-sm font-semibold text-gray-700">Outstanding Invoices</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50">
              <tr>
                {['Invoice', 'Customer', 'Total', 'Paid', 'Outstanding', 'Status'].map(h => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {outstanding?.slice(0, 10).map(inv => (
                <tr key={inv.invoiceId} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-mono text-xs text-blue-600">{inv.invoiceNo}</td>
                  <td className="px-4 py-3 text-gray-700">{inv.customerName}</td>
                  <td className="px-4 py-3 text-gray-700">{fmt(inv.total)}</td>
                  <td className="px-4 py-3 text-green-600">{fmt(inv.paidAmount)}</td>
                  <td className="px-4 py-3 font-medium text-red-600">{fmt(inv.outstanding)}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                      inv.status === 'PARTIAL' ? 'bg-amber-100 text-amber-700' : 'bg-blue-100 text-blue-700'
                    }`}>{inv.status}</span>
                  </td>
                </tr>
              ))}
              {!outstanding?.length && (
                <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">No outstanding invoices</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
