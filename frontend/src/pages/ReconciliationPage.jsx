import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { reconApi } from '../services/api';
import toast from 'react-hot-toast';
import { CheckCircle, XCircle, ChevronDown, ChevronUp } from 'lucide-react';
import dayjs from 'dayjs';

const CONFIDENCE_LABELS = {
  HIGH:   { label: 'HIGH',   bg: 'bg-green-100',  text: 'text-green-700',  threshold: 0.8 },
  MEDIUM: { label: 'MEDIUM', bg: 'bg-amber-100',  text: 'text-amber-700',  threshold: 0.5 },
  LOW:    { label: 'LOW',    bg: 'bg-red-100',    text: 'text-red-700',    threshold: 0 },
};

function confidenceLevel(score) {
  const n = Number(score);
  if (n >= 0.8) return CONFIDENCE_LABELS.HIGH;
  if (n >= 0.5) return CONFIDENCE_LABELS.MEDIUM;
  return CONFIDENCE_LABELS.LOW;
}

function ReconCard({ recon, onConfirm, onReject, isConfirming, isRejecting }) {
  const [expanded, setExpanded] = useState(false);
  const conf = confidenceLevel(recon.confidence);
  const txn = recon.transaction;
  const inv = recon.invoice;
  const fmt = (n) => `₹${Number(n).toLocaleString('en-IN')}`;

  return (
    <div className="bg-white border border-gray-200 rounded-xl shadow-sm overflow-hidden">
      {/* Header row */}
      <div className="flex items-center gap-4 px-5 py-4 cursor-pointer hover:bg-gray-50"
           onClick={() => setExpanded(!expanded)}>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <span className={`px-2 py-0.5 rounded text-xs font-bold ${conf.bg} ${conf.text}`}>
              {conf.label} {(Number(recon.confidence) * 100).toFixed(0)}%
            </span>
            <span className="text-sm font-medium text-gray-800 truncate">
              {fmt(txn?.amount)} — {txn?.remark?.slice(0, 50) || 'No remark'}
            </span>
          </div>
          <div className="text-xs text-gray-500 mt-1">
            {dayjs(txn?.txnTime).format('DD MMM YYYY HH:mm')} · UTR: {txn?.utr}
          </div>
        </div>
        <div className="text-right min-w-[120px]">
          <div className="text-xs font-medium text-gray-700">{inv?.customer?.name}</div>
          <div className="text-xs text-blue-600 font-mono">{inv?.invoiceNo}</div>
        </div>
        <div className="flex gap-2 ml-2">
          <button onClick={(e) => { e.stopPropagation(); onConfirm(recon.reconId); }}
            disabled={isConfirming}
            className="flex items-center gap-1 px-3 py-1.5 bg-green-600 hover:bg-green-700 text-white rounded-lg text-xs font-medium transition-colors disabled:opacity-50">
            <CheckCircle size={12} /> Confirm
          </button>
          <button onClick={(e) => { e.stopPropagation(); onReject(recon.reconId); }}
            disabled={isRejecting}
            className="flex items-center gap-1 px-3 py-1.5 bg-gray-100 hover:bg-red-50 hover:text-red-600 text-gray-600 rounded-lg text-xs font-medium transition-colors">
            <XCircle size={12} /> Reject
          </button>
        </div>
        {expanded ? <ChevronUp size={16} className="text-gray-400" /> : <ChevronDown size={16} className="text-gray-400" />}
      </div>

      {/* Expanded detail */}
      {expanded && (
        <div className="border-t border-gray-100 px-5 py-4 grid grid-cols-2 gap-6 bg-gray-50">
          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase mb-2">Transaction</p>
            <dl className="space-y-1 text-sm">
              <div className="flex gap-2"><dt className="text-gray-500 w-24">Amount:</dt><dd className="font-medium">{fmt(txn?.amount)}</dd></div>
              <div className="flex gap-2"><dt className="text-gray-500 w-24">Payer VPA:</dt><dd className="font-mono text-xs">{txn?.payerVpa || '—'}</dd></div>
              <div className="flex gap-2"><dt className="text-gray-500 w-24">Remark:</dt><dd className="text-gray-700">{txn?.remark || '—'}</dd></div>
            </dl>
          </div>
          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase mb-2">Invoice</p>
            <dl className="space-y-1 text-sm">
              <div className="flex gap-2"><dt className="text-gray-500 w-24">Invoice No:</dt><dd className="font-mono">{inv?.invoiceNo}</dd></div>
              <div className="flex gap-2"><dt className="text-gray-500 w-24">Total:</dt><dd className="font-medium">{fmt(inv?.total)}</dd></div>
              <div className="flex gap-2"><dt className="text-gray-500 w-24">Status:</dt><dd>{inv?.status}</dd></div>
            </dl>
          </div>
          <div className="col-span-2">
            <p className="text-xs font-semibold text-gray-500 uppercase mb-2">Match Reasons</p>
            <div className="flex flex-wrap gap-2">
              {(recon.reason || []).map(r => (
                <span key={r} className="px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs font-mono">{r}</span>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default function ReconciliationPage() {
  const [statusFilter, setStatusFilter] = useState('SUGGESTED');
  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['reconciliations', statusFilter],
    queryFn: () => reconApi.list({ status: statusFilter, size: 50 }).then(r => r.data),
  });

  const confirmMutation = useMutation({
    mutationFn: reconApi.confirm,
    onSuccess: () => { toast.success('Reconciliation confirmed!'); qc.invalidateQueries(['reconciliations']); },
    onError: (e) => toast.error(e.response?.data?.message || 'Failed'),
  });

  const rejectMutation = useMutation({
    mutationFn: (id) => reconApi.reject(id),
    onSuccess: () => { toast.success('Rejected'); qc.invalidateQueries(['reconciliations']); },
    onError: (e) => toast.error(e.response?.data?.message || 'Failed'),
  });

  const items = data?.content || [];

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-800">Reconciliation</h1>
          <p className="text-sm text-gray-500">Review auto-matched transactions</p>
        </div>
        <div className="flex gap-2">
          {['SUGGESTED', 'CONFIRMED', 'REJECTED'].map(s => (
            <button key={s} onClick={() => setStatusFilter(s)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                statusFilter === s ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}>
              {s}
            </button>
          ))}
        </div>
      </div>

      {isLoading ? (
        <div className="text-center py-12 text-gray-400">Loading…</div>
      ) : items.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <p className="text-4xl mb-3">✓</p>
          <p className="text-sm">No {statusFilter.toLowerCase()} reconciliations</p>
        </div>
      ) : (
        <div className="space-y-3">
          {items.map(r => (
            <ReconCard
              key={r.reconId}
              recon={r}
              onConfirm={(id) => confirmMutation.mutate(id)}
              onReject={(id) => rejectMutation.mutate(id)}
              isConfirming={confirmMutation.isPending}
              isRejecting={rejectMutation.isPending}
            />
          ))}
        </div>
      )}
    </div>
  );
}
