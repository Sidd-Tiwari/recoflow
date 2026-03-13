import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export const useAuthStore = create(
  persist(
    (set) => ({
      token: null,
      user: null,
      tenantId: null,

      setAuth: (authResponse) => set({
        token: authResponse.token,
        tenantId: authResponse.tenantId,
        user: {
          userId: authResponse.userId,
          name: authResponse.name,
          roles: authResponse.roles,
        },
      }),

      logout: () => set({ token: null, user: null, tenantId: null }),

      hasRole: (role) => {
        const state = useAuthStore.getState();
        return state.user?.roles?.includes(role) ?? false;
      },

      isAccountant: () => {
        const roles = useAuthStore.getState().user?.roles ?? [];
        return roles.includes('ROLE_ADMIN') || roles.includes('ROLE_ACCOUNTANT');
      },
    }),
    { name: 'recoflow-auth' }
  )
);
