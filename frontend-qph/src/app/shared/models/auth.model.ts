export interface AuthRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  nombreCompleto: string;
  rol: string;
}

export interface SessionUser {
  username: string;
  nombreCompleto: string;
  rol: string;
}
