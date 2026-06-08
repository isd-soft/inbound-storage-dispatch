import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('jwt_token') || null);
  const role = ref(localStorage.getItem('user_role') || null);
  const user = ref(null);

  const login = async (email, password) => {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        if (email === 'supervisor@gmail.com') {
          token.value = 'mock-jwt-token-123';
          role.value = 'ROLE_SUPERVISOR';
          user.value = { email: 'supervisor@gmail.com' };

          localStorage.setItem('jwt_token', token.value);
          localStorage.setItem('user_role', role.value);
          resolve({ role: 'ROLE_SUPERVISOR' });

        } else if (email === 'smoothOperator@gmail.com') {
          token.value = 'mock-jwt-token-456';
          role.value = 'ROLE_OPERATOR';
          user.value = { email: 'smoothOperator@gmail.com' };

          localStorage.setItem('jwt_token', token.value);
          localStorage.setItem('user_role', role.value);
          resolve({ role: 'ROLE_OPERATOR' });

        } else if (email === 'dev@gmail.com') {
          token.value = 'mock-jwt-token-789';
          role.value = 'ROLE_DEV';
          user.value = { email: 'dev@gmail.com' };

          localStorage.setItem('jwt_token', token.value);
          localStorage.setItem('user_role', role.value);
          resolve({ role: 'ROLE_DEV' });

        } else {
          reject(new Error('Wrong Data'));
        }
      }, 500);
    });
  };

  const logout = () => {
    token.value = null;
    role.value = null;
    user.value = null;
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_role');
  };

  return { token, role, user, login, logout };
});
