import { useQuery } from '@tanstack/react-query';
import { reportApi } from '../services/api';
import dayjs from 'dayjs';
import { useState } from 'react';

export default function ReportsPage() {
  const [date, setDate] = useState(dayjs().format('YYYY-MM-DD'));

  const { data: daily } = useQuery({
    queryKey: ['daily', date],
    queryFn: () => reportApi.dailyCollections(date).then(r => r.data),
  });

  const { data: outstanding } = useQuery({
    queryKey: ['outstanding'],
    queryFn: () => reportApi.outstanding().then(r => r.data),
  });

  const fmt = (n) => `₹${Number(n || 0).toLocaleString('en-IN')}`;

  return (
    <div className="p-6 space-y-6">
      <h1 className="text-xl font-bold text-gray-800">Reports</h1>

      {/* Daily Collections */}
      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <div className="flex items-center gap-4 mb-4">
          <h2 className="text-sm font-semibold text-gray-700">Daily Collections</h2>
          <input type="date" value={date} onChange={e => setDate(e.target.value)}
            className="border border-gray-200 rounded-lg px-3 py-1.5 text-sm outline-none focus:ring-2 focus:ring-blue-500 ml-auto" />
        </div>
        <div className="grid grid-cols-4 gap-4">
          {[
            { label: 'Total Collected', value: fmt(daily?.totalCollected), color: 'text-green-600' },
            { label: 'Transactions', value: daily?.transactionCount ?? '—', color: 'text-blue-600' },
            { label: 'Matched', value: daily?.matchedCount ?? '—', color: 'text-blue-600' },
            { label: 'Unmatched', value: daily?.unmatchedCount ?? '—', color: 'text-amber-600' },
          ].map(item => (
            <div key={item.label} className="text-center p-4 bg-gray-50 rounded-lg">
              <p className="text-xs text-gray-500 mb-1">{item.label}</p>
              <p className={`text-xl font-bold ${item.color}`}>{item.value}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Outstanding */}
      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <h2 className="text-sm font-semibold text-gray-700 mb-4">Outstanding Invoices</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50">
              <tr>{['Invoice', 'Customer', 'Due Date', 'Total', 'Outstanding', 'Status'].map(h =>
                <th key={h} className="px-3 py-2 text-left text-xs text-gray-500 uppercase">{h}</th>
              )}</tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {outstanding?.map(inv => (
                <tr key={inv.invoiceId} className="hover:bg-gray-50">
                  <td className="px-3 py-2 font-mono text-xs text-blue-600">{inv.invoiceNo}</td>
                  <td className="px-3 py-2 text-gray-700">{inv.customerName}</td>
                  <td className="px-3 py-2 text-gray-500">{inv.dueDate ? dayjs(inv.dueDate).format('DD MMM') : '—'}</td>
                  <td className="px-3 py-2">{fmt(inv.total)}</td>
                  <td className="px-3 py-2 font-medium text-red-600">{fmt(inv.outstanding)}</td>
                  <td className="px-3 py-2">
                    <span className={`px-2 py-0.5 rounded text-xs ${inv.status === 'PARTIAL' ? 'bg-amber-100 text-amber-700' : 'bg-blue-100 text-blue-700'}`}>
                      {inv.status}
                    </span>
                  </td>
                </tr>
              ))}
              {!outstanding?.length && <tr><td colSpan={6} className="px-3 py-8 text-center text-gray-400">No outstanding invoices</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
