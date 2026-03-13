import { useQuery, useMutation } from '@tanstack/react-query';
import { useForm, useFieldArray } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { customerApi, invoiceApi } from '../services/api';
import toast from 'react-hot-toast';
import { Plus, Trash2 } from 'lucide-react';

export default function CreateInvoicePage() {
  const navigate = useNavigate();
  const { register, control, handleSubmit, watch } = useForm({
    defaultValues: { items: [{ description: '', quantity: 1, rate: 0, taxPct: 0, sortOrder: 0 }] }
  });
  const { fields, append, remove } = useFieldArray({ control, name: 'items' });

  const { data: customers } = useQuery({
    queryKey: ['customers'],
    queryFn: () => customerApi.list({ size: 100 }).then(r => r.data.content),
  });

  const mutation = useMutation({
    mutationFn: invoiceApi.create,
    onSuccess: (res) => {
      toast.success(`Invoice ${res.data.invoiceNo} created`);
      navigate('/invoices');
    },
    onError: (e) => toast.error(e.response?.data?.message || 'Failed'),
  });

  return (
    <div className="p-6 max-w-3xl">
      <h1 className="text-xl font-bold text-gray-800 mb-6">New Invoice</h1>

      <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="space-y-6">
        <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs text-gray-500 block mb-1">Customer</label>
              <select {...register('customerId', { required: true })}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500">
                <option value="">Select customer…</option>
                {customers?.map(c => <option key={c.customerId} value={c.customerId}>{c.name}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500 block mb-1">Invoice Date</label>
              <input {...register('invoiceDate', { required: true })} type="date"
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
            <div>
              <label className="text-xs text-gray-500 block mb-1">Due Date</label>
              <input {...register('dueDate')} type="date"
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
            <div>
              <label className="text-xs text-gray-500 block mb-1">Notes</label>
              <input {...register('notes')}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
          </div>
        </div>

        <div className="bg-white rounded-xl border border-gray-200 p-5">
          <div className="flex items-center justify-between mb-3">
            <p className="text-sm font-semibold text-gray-700">Line Items</p>
            <button type="button" onClick={() => append({ description: '', quantity: 1, rate: 0, taxPct: 0, sortOrder: fields.length })}
              className="flex items-center gap-1 text-xs text-blue-600 hover:text-blue-700">
              <Plus size={12} /> Add Item
            </button>
          </div>
          <div className="space-y-2">
            {fields.map((field, i) => (
              <div key={field.id} className="grid grid-cols-12 gap-2 items-center">
                <div className="col-span-5">
                  <input {...register(`items.${i}.description`, { required: true })} placeholder="Description"
                    className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm outline-none focus:ring-1 focus:ring-blue-400" />
                </div>
                <div className="col-span-2">
                  <input {...register(`items.${i}.quantity`, { valueAsNumber: true })} type="number" step="0.001" placeholder="Qty"
                    className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm outline-none focus:ring-1 focus:ring-blue-400" />
                </div>
                <div className="col-span-2">
                  <input {...register(`items.${i}.rate`, { valueAsNumber: true })} type="number" step="0.01" placeholder="Rate"
                    className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm outline-none focus:ring-1 focus:ring-blue-400" />
                </div>
                <div className="col-span-2">
                  <input {...register(`items.${i}.taxPct`, { valueAsNumber: true })} type="number" step="0.01" placeholder="Tax %"
                    className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm outline-none focus:ring-1 focus:ring-blue-400" />
                </div>
                <div className="col-span-1 flex justify-center">
                  {fields.length > 1 && (
                    <button type="button" onClick={() => remove(i)} className="text-red-400 hover:text-red-600">
                      <Trash2 size={14} />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="flex gap-3">
          <button type="submit" disabled={mutation.isPending}
            className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium disabled:opacity-50">
            {mutation.isPending ? 'Creating…' : 'Create Invoice'}
          </button>
          <button type="button" onClick={() => navigate('/invoices')}
            className="px-6 py-2 bg-gray-100 text-gray-600 rounded-lg text-sm font-medium hover:bg-gray-200">
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
