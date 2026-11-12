export interface InventoryMovement {
  id: number;
  tipo: string;
  cantidad: number;
  stockResultante: number;
  fecha: string;
  usuario: string;
  ventaCodigo: string | null;
}
