import { useAuthStore } from "@/stores/auth-store";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";

export type User = {
    id: string;
    email: string;
    displayName: string;
  };
  
  export type AuthResponse = {
    accessToken: string;
    refreshToken: string;
    user: User;
  };
  

async function request<T>(
    path: string,
    options: RequestInit = {},
    auth = true,
    ): Promise<T> {
    const headers = new Headers(options.headers);
  
    if (!(options.body instanceof FormData)) {
      headers.set("Content-Type", "application/json");
    }
  
    if (auth) {
      const token = useAuthStore.getState().accessToken;
      if (token) {
        headers.set("Authorization", `Bearer ${token}`);
      }
    }
  
    const response = await fetch(`${API_URL}${path}`, { ...options, headers });
  
    if (!response.ok) {
      let message = "Request failed";
      try {
        const body = await response.json();
        if (body?.message) message = body.message;
      } catch {
      }
      throw new Error(message);
    }
    if (response.status === 204) {
      return undefined as T;
    }
    return response.json();
  }

export const api = {
  register: (body: { email: string; password: string; displayName: string }) =>
    request<AuthResponse>("/auth/register", { method: "POST", body: JSON.stringify(body) }, false),
  
    login: (body: { email: string; password: string }) =>
      request<AuthResponse>("/auth/login", { method: "POST", body: JSON.stringify(body) }, false),
  
    logout: () => {
      const refreshToken = useAuthStore.getState().refreshToken;
      return request<void>(
        "/auth/logout",
        { method: "POST", body: JSON.stringify({ refreshToken }) },
        false,
      );
    },
  
    me: () => request<User>("/auth/me"),
}