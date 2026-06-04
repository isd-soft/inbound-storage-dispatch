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
          role.value = 'SUPERVISOR';
          user.value = { email: 'supervisor@gmail.com' };

          localStorage.setItem('jwt_token', token.value);
          localStorage.setItem('user_role', role.value);
          resolve({ role: 'SUPERVISOR' });

        } else if (email === 'smoothOperator@gmail.com') {
          token.value = 'mock-jwt-token-456';
          role.value = 'OPERATOR';
          user.value = { email: 'smoothOperator@gmail.com' };

          localStorage.setItem('jwt_token', token.value);
          localStorage.setItem('user_role', role.value);
          resolve({ role: 'OPERATOR' });

        } else {
          reject(new Error('Неверные учетные данные'));
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
