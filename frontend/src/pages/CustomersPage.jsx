import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { customerApi } from '../services/api';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import { Plus, X } from 'lucide-react';

function CustomerModal({ onClose, onSave }) {
  const { register, handleSubmit } = useForm();
  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-semibold text-gray-800">New Customer</h2>
          <button onClick={onClose}><X size={16} /></button>
        </div>
        <form onSubmit={handleSubmit(onSave)} className="space-y-3">
          {[
            { name: 'name', label: 'Name', required: true },
            { name: 'phone', label: 'Phone' },
            { name: 'email', label: 'Email', type: 'email' },
            { name: 'gstin', label: 'GSTIN' },
            { name: 'vpaHint', label: 'UPI VPA Hint' },
          ].map(f => (
            <div key={f.name}>
              <label className="text-xs text-gray-500 block mb-1">{f.label}</label>
              <input {...register(f.name, { required: f.required })} type={f.type || 'text'}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
          ))}
          <button type="submit" className="w-full bg-blue-600 text-white py-2 rounded-lg text-sm font-medium">Save Customer</button>
        </form>
      </div>
    </div>
  );
}

export default function CustomersPage() {
  const [showModal, setShowModal] = useState(false);
  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['customers'],
    queryFn: () => customerApi.list({ size: 50 }).then(r => r.data),
  });

  const createMutation = useMutation({
    mutationFn: customerApi.create,
    onSuccess: () => { toast.success('Customer created'); qc.invalidateQueries(['customers']); setShowModal(false); },
    onError: () => toast.error('Failed to create customer'),
  });

  const customers = data?.content || [];

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-800">Customers</h1>
          <p className="text-sm text-gray-500">{data?.totalElements ?? 0} customers</p>
        </div>
        <button onClick={() => setShowModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">
          <Plus size={14} /> Add Customer
        </button>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="px-4 py-3 bg-gray-50 grid grid-cols-4 text-xs font-medium text-gray-500 uppercase">
          <span>Name</span><span>Phone</span><span>Email</span><span>VPA Hint</span>
        </div>
        {customers.map(c => (
          <div key={c.customerId} className="px-4 py-3 grid grid-cols-4 border-t border-gray-50 hover:bg-gray-50">
            <span className="text-sm font-medium text-gray-800">{c.name}</span>
            <span className="text-sm text-gray-500">{c.phone || '—'}</span>
            <span className="text-sm text-gray-500">{c.email || '—'}</span>
            <span className="text-xs font-mono text-blue-600">{c.vpaHint || '—'}</span>
          </div>
        ))}
        {!isLoading && customers.length === 0 && (
          <div className="px-4 py-12 text-center text-gray-400">No customers yet</div>
        )}
      </div>

      {showModal && (
        <CustomerModal
          onClose={() => setShowModal(false)}
          onSave={(d) => createMutation.mutate(d)}
        />
      )}
    </div>
  );
}
