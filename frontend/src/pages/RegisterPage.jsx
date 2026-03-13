import { useForm } from 'react-hook-form';
import { useNavigate, Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { authApi } from '../services/api';
import { useAuthStore } from '../store/authStore';

export default function RegisterPage() {
  const { register, handleSubmit } = useForm();
  const setAuth = useAuthStore((s) => s.setAuth);
  const navigate = useNavigate();

  const mutation = useMutation({
    mutationFn: authApi.register,
    onSuccess: ({ data }) => {
      setAuth(data);
      toast.success(`Organization created! Tenant ID: ${data.tenantId}`);
      navigate('/dashboard');
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Registration failed'),
  });

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-900">
      <div className="bg-gray-800 p-8 rounded-xl shadow-xl w-full max-w-sm">
        <h1 className="text-white text-2xl font-bold mb-1">Create Account</h1>
        <p className="text-gray-400 text-sm mb-6">Set up your organization on RecoFlow</p>

        <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="space-y-4">
          <div>
            <label className="text-xs text-gray-400 block mb-1">Organization Name</label>
            <input {...register('organizationName', { required: true })}
              className="w-full bg-gray-700 text-white rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <div>
            <label className="text-xs text-gray-400 block mb-1">Your Name</label>
            <input {...register('name', { required: true })}
              className="w-full bg-gray-700 text-white rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <div>
            <label className="text-xs text-gray-400 block mb-1">Email</label>
            <input {...register('email', { required: true })} type="email"
              className="w-full bg-gray-700 text-white rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <div>
            <label className="text-xs text-gray-400 block mb-1">Password (min 8 chars)</label>
            <input {...register('password', { required: true, minLength: 8 })} type="password"
              className="w-full bg-gray-700 text-white rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <button type="submit" disabled={mutation.isPending}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50">
            {mutation.isPending ? 'Creating…' : 'Create Organization'}
          </button>
        </form>

        <p className="text-gray-500 text-xs text-center mt-4">
          Already have an account? <Link to="/login" className="text-blue-400 hover:underline">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
