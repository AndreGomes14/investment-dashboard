export interface AuthResponse {
  expiresIn: number;
  token: string;
  user: {
    id: number;
    username: string;
    email: string;
  };
  expiresAt: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}
