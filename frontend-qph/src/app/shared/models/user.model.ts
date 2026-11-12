export interface User {
  id: number;
  username: string;
  nombreCompleto: string;
  email: string;
  rol: string;
  activo: boolean;
  fechaCreacion: string;
}

export interface UserRequest {
  username?: string;
  password?: string | null;
  nombreCompleto: string;
  email: string;
  rol: string;
}

export interface Role {
  id: number;
  nombre: string;
  descripcion: string;
}
