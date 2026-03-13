// AuditPage.jsx
import { useQuery } from '@tanstack/react-query';
import api from '../services/api';
import dayjs from 'dayjs';

export default function AuditPage() {
  const { data } = useQuery({
    queryKey: ['audit-logs'],
    queryFn: () => api.get('/api/audit-logs', { params: { size: 50 } }).then(r => r.data),
  });

  const logs = data?.content || [];

  return (
    <div className="p-6">
      <h1 className="text-xl font-bold text-gray-800 mb-6">Audit Logs</h1>
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="px-4 py-3 bg-gray-50 grid grid-cols-5 text-xs font-medium text-gray-500 uppercase">
          <span>Action</span><span>Entity</span><span>Entity ID</span><span>Actor</span><span>Time</span>
        </div>
        {logs.map(l => (
          <div key={l.auditId} className="px-4 py-3 grid grid-cols-5 items-center border-t border-gray-50 hover:bg-gray-50">
            <span className="text-xs font-mono font-medium text-blue-700">{l.action}</span>
            <span className="text-xs text-gray-600">{l.entityType}</span>
            <span className="text-xs font-mono text-gray-400 truncate">{l.entityId?.slice(0, 12)}…</span>
            <span className="text-xs text-gray-600">{l.actorUser?.name || '—'}</span>
            <span className="text-xs text-gray-400">{dayjs(l.ts).format('DD MMM HH:mm:ss')}</span>
          </div>
        ))}
        {logs.length === 0 && <div className="px-4 py-12 text-center text-gray-400">No audit logs</div>}
      </div>
    </div>
  );
}
