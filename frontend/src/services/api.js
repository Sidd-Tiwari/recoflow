import axios from 'axios';
import { useAuthStore } from '../store/authStore';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor — attach JWT
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Response interceptor — handle 401
api.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;

// ─── Auth ─────────────────────────────────────────────────────────────────────
export const authApi = {
  register: (data) => api.post('/api/auth/register', data),
  login: (data) => api.post('/api/auth/login', data),
};

// ─── Customers ────────────────────────────────────────────────────────────────
export const customerApi = {
  list: (params) => api.get('/api/customers', { params }),
  search: (q) => api.get('/api/customers/search', { params: { q } }),
  getById: (id) => api.get(`/api/customers/${id}`),
  create: (data) => api.post('/api/customers', data),
  update: (id, data) => api.put(`/api/customers/${id}`, data),
  delete: (id) => api.delete(`/api/customers/${id}`),
};

// ─── Invoices ─────────────────────────────────────────────────────────────────
export const invoiceApi = {
  list: (params) => api.get('/api/invoices', { params }),
  getById: (id) => api.get(`/api/invoices/${id}`),
  create: (data) => api.post('/api/invoices', data),
  updateStatus: (id, status) => api.put(`/api/invoices/${id}/status`, null, { params: { status } }),
};

// ─── Statements ───────────────────────────────────────────────────────────────
export const statementApi = {
  upload: (file) => {
    const form = new FormData();
    form.append('file', file);
    return api.post('/api/statements/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  list: (params) => api.get('/api/statements', { params }),
  status: (fileId) => api.get(`/api/statements/${fileId}/status`),
  transactions: (fileId, params) => api.get(`/api/statements/${fileId}/transactions`, { params }),
};

// ─── Reconciliations ──────────────────────────────────────────────────────────
export const reconApi = {
  list: (params) => api.get('/api/reconciliations', { params }),
  confirm: (id) => api.post(`/api/reconciliations/${id}/confirm`),
  reject: (id, notes) => api.post(`/api/reconciliations/${id}/reject`, null, { params: { notes } }),
  manual: (data) => api.post('/api/reconciliations/manual', data),
};

// ─── Reports ──────────────────────────────────────────────────────────────────
export const reportApi = {
  dailyCollections: (date) => api.get('/api/reports/daily-collections', { params: { date } }),
  outstanding: () => api.get('/api/reports/outstanding'),
};
