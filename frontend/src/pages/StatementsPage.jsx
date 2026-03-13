import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useRef } from 'react';
import { statementApi } from '../services/api';
import toast from 'react-hot-toast';
import { Upload } from 'lucide-react';
import dayjs from 'dayjs';

const STATUS_COLORS = {
  UPLOADED: 'bg-gray-100 text-gray-600',
  PARSING:  'bg-blue-100 text-blue-600',
  PARSED:   'bg-green-100 text-green-700',
  FAILED:   'bg-red-100 text-red-700',
};

export default function StatementsPage() {
  const fileRef = useRef();
  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['statements'],
    queryFn: () => statementApi.list({ size: 20 }).then(r => r.data),
    refetchInterval: 5000, // poll for parsing status
  });

  const uploadMutation = useMutation({
    mutationFn: statementApi.upload,
    onSuccess: () => { toast.success('File uploaded, parsing…'); qc.invalidateQueries(['statements']); },
    onError: () => toast.error('Upload failed'),
  });

  const handleFile = (e) => {
    const file = e.target.files[0];
    if (file) uploadMutation.mutate(file);
  };

  const files = data?.content || [];

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-800">Bank Statements</h1>
          <p className="text-sm text-gray-500">Upload CSV statements for auto-reconciliation</p>
        </div>
        <button
          onClick={() => fileRef.current?.click()}
          disabled={uploadMutation.isPending}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium transition-colors disabled:opacity-50">
          <Upload size={14} />
          {uploadMutation.isPending ? 'Uploading…' : 'Upload CSV'}
        </button>
        <input ref={fileRef} type="file" accept=".csv" hidden onChange={handleFile} />
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="px-4 py-3 bg-gray-50 grid grid-cols-5 text-xs font-medium text-gray-500 uppercase">
          <span>Filename</span><span>Status</span>
          <span>Total</span><span>Valid</span><span>Uploaded</span>
        </div>
        {files.map(f => (
          <div key={f.fileId} className="px-4 py-3 grid grid-cols-5 items-center border-t border-gray-50 hover:bg-gray-50">
            <span className="text-sm text-gray-700 truncate">{f.filename}</span>
            <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium w-fit ${STATUS_COLORS[f.status] || ''}`}>{f.status}</span>
            <span className="text-sm text-gray-600">{f.totalRows ?? '—'}</span>
            <span className="text-sm text-green-600">{f.validRows ?? '—'}</span>
            <span className="text-xs text-gray-400">{dayjs(f.createdAt).format('DD MMM HH:mm')}</span>
          </div>
        ))}
        {!isLoading && files.length === 0 && (
          <div className="px-4 py-12 text-center text-gray-400 text-sm">No statements uploaded yet</div>
        )}
      </div>

      <div className="mt-4 p-4 bg-blue-50 rounded-lg border border-blue-100">
        <p className="text-xs text-blue-700 font-medium mb-1">Expected CSV Format</p>
        <p className="text-xs text-blue-600 font-mono">date, time, amount, utr, remark, payer_vpa, type</p>
        <p className="text-xs text-blue-500 mt-1">e.g.: 20/01/2025, 14:32:00, 5000.00, UTR123456789, Payment INV-2025-0042, user@upi, CREDIT</p>
      </div>
    </div>
  );
}
