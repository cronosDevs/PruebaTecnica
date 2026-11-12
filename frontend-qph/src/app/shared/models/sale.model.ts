export interface SaleItem {
  productoId: number;
  productoNombre: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface Sale {
  id: number;
  codigo: string;
  total: number;
  vendedor: string;
  vendedorNombre: string;
  fechaCreacion: string;
  items: SaleItem[];
}

export interface SaleRequest {
  items: { productoId: number; cantidad: number }[];
}
