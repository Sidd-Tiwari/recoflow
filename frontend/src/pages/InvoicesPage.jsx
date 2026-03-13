import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { invoiceApi } from '../services/api';
import { Plus } from 'lucide-react';
import dayjs from 'dayjs';

const STATUS_STYLES = {
  DRAFT:     'bg-gray-100 text-gray-600',
  SENT:      'bg-blue-100 text-blue-700',
  PARTIAL:   'bg-amber-100 text-amber-700',
  PAID:      'bg-green-100 text-green-700',
  CANCELLED: 'bg-red-100 text-red-600',
};

export default function InvoicesPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['invoices'],
    queryFn: () => invoiceApi.list({ size: 50 }).then(r => r.data),
  });

  const fmt = (n) => `₹${Number(n).toLocaleString('en-IN')}`;
  const invoices = data?.content || [];

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-800">Invoices</h1>
          <p className="text-sm text-gray-500">{data?.totalElements ?? 0} total invoices</p>
        </div>
        <Link to="/invoices/new"
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">
          <Plus size={14} /> New Invoice
        </Link>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="px-4 py-3 bg-gray-50 grid grid-cols-6 text-xs font-medium text-gray-500 uppercase">
          {['Invoice No','Customer','Date','Total','Paid','Status'].map(h => <span key={h}>{h}</span>)}
        </div>
        {invoices.map(inv => (
          <Link key={inv.invoiceId} to={`/invoices/${inv.invoiceId}`}
            className="px-4 py-3 grid grid-cols-6 items-center border-t border-gray-50 hover:bg-blue-50 transition-colors">
            <span className="text-sm font-mono text-blue-600">{inv.invoiceNo}</span>
            <span className="text-sm text-gray-700">{inv.customer?.name}</span>
            <span className="text-xs text-gray-500">{dayjs(inv.invoiceDate).format('DD MMM YYYY')}</span>
            <span className="text-sm font-medium text-gray-800">{fmt(inv.total)}</span>
            <span className="text-sm text-green-600">{fmt(inv.paidAmount)}</span>
            <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium w-fit ${STATUS_STYLES[inv.status]}`}>
              {inv.status}
            </span>
          </Link>
        ))}
        {!isLoading && invoices.length === 0 && (
          <div className="px-4 py-12 text-center text-gray-400">No invoices yet. <Link to="/invoices/new" className="text-blue-500 hover:underline">Create one</Link></div>
        )}
      </div>
    </div>
  );
}
