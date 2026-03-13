import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import { invoiceApi } from '../services/api';
import toast from 'react-hot-toast';
import dayjs from 'dayjs';

const STATUS_TRANSITIONS = {
  DRAFT:    ['SENT', 'CANCELLED'],
  SENT:     ['CANCELLED'],
  PARTIAL:  ['CANCELLED'],
  PAID:     [],
  CANCELLED:[],
};

export default function InvoiceDetailPage() {
  const { id } = useParams();
  const qc = useQueryClient();
  const navigate = useNavigate();

  const { data: invoice, isLoading } = useQuery({
    queryKey: ['invoice', id],
    queryFn: () => invoiceApi.getById(id).then(r => r.data),
  });

  const statusMutation = useMutation({
    mutationFn: (status) => invoiceApi.updateStatus(id, status),
    onSuccess: () => { toast.success('Status updated'); qc.invalidateQueries(['invoice', id]); },
    onError: (e) => toast.error(e.response?.data?.message || 'Failed'),
  });

  const fmt = (n) => `₹${Number(n || 0).toLocaleString('en-IN')}`;

  if (isLoading) return <div className="p-6 text-gray-400">Loading…</div>;

  const allowedTransitions = STATUS_TRANSITIONS[invoice?.status] || [];

  return (
    <div className="p-6 max-w-3xl">
      <button onClick={() => navigate('/invoices')} className="text-sm text-blue-600 hover:underline mb-4">← Back to Invoices</button>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <div className="flex items-start justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-800">{invoice?.invoiceNo}</h1>
            <p className="text-gray-500">{invoice?.customer?.name}</p>
            <p className="text-xs text-gray-400 mt-1">{dayjs(invoice?.invoiceDate).format('DD MMMM YYYY')}</p>
          </div>
          <div className="text-right">
            <span className="px-3 py-1 bg-blue-100 text-blue-700 rounded-full text-sm font-medium">{invoice?.status}</span>
            {allowedTransitions.length > 0 && (
              <div className="flex gap-2 mt-3">
                {allowedTransitions.map(s => (
                  <button key={s} onClick={() => statusMutation.mutate(s)}
                    className="px-3 py-1 bg-gray-100 hover:bg-gray-200 text-gray-600 rounded text-xs font-medium">
                    → {s}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Items table */}
        <table className="w-full text-sm mb-6">
          <thead className="bg-gray-50">
            <tr>{['Description', 'Qty', 'Rate', 'Tax %', 'Amount'].map(h =>
              <th key={h} className="px-3 py-2 text-left text-xs text-gray-500">{h}</th>)}</tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {invoice?.items?.map(item => (
              <tr key={item.itemId}>
                <td className="px-3 py-2 text-gray-700">{item.description}</td>
                <td className="px-3 py-2 text-gray-600">{item.quantity}</td>
                <td className="px-3 py-2 text-gray-600">{fmt(item.rate)}</td>
                <td className="px-3 py-2 text-gray-600">{item.taxPct}%</td>
                <td className="px-3 py-2 font-medium">{fmt(item.amount)}</td>
              </tr>
            ))}
          </tbody>
        </table>

        <div className="flex flex-col items-end gap-1 text-sm border-t pt-4">
          <div className="flex gap-8"><span className="text-gray-500">Subtotal</span><span>{fmt(invoice?.subtotal)}</span></div>
          <div className="flex gap-8"><span className="text-gray-500">Tax</span><span>{fmt(invoice?.taxAmount)}</span></div>
          <div className="flex gap-8 font-bold text-base"><span>Total</span><span>{fmt(invoice?.total)}</span></div>
          <div className="flex gap-8 text-green-600"><span>Paid</span><span>{fmt(invoice?.paidAmount)}</span></div>
          <div className="flex gap-8 text-red-600 font-medium"><span>Outstanding</span>
            <span>{fmt((invoice?.total || 0) - (invoice?.paidAmount || 0))}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
