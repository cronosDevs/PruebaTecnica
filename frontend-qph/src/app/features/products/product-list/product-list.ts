import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ProductService } from '../../../core/services/product';
import { AuthService } from '../../../core/services/auth';
import { SaleService } from '../../../core/services/sale';
import { Product } from '../../../shared/models/product.model';
import { InventoryMovement } from '../../../shared/models/inventory-movement.model';

// PrimeNG
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';
import { DialogModule } from 'primeng/dialog';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmationService, MessageService } from 'primeng/api';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    CardModule,
    TagModule,
    ConfirmDialogModule,
    ToastModule,
    ToolbarModule,
    DialogModule,
    TooltipModule
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './product-list.html',
  styleUrls: ['./product-list.scss']
})
export class ProductListComponent implements OnInit {

  private productService = inject(ProductService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private confirmationService = inject(ConfirmationService);
  private messageService = inject(MessageService);
  private saleService = inject(SaleService);

  products: Product[] = [];
  loading = false;
  isAdmin = this.authService.isAdmin();

  cart: { product: Product; cantidad: number }[] = [];
  addVisible = false;
  addProduct: Product | null = null;
  addQuantity = 1;
  sellVisible = false;
  selling = false;
  private idempotencyKey = '';

  kardexVisible = false;
  kardexProduct: Product | null = null;
  movements: InventoryMovement[] = [];
  loadingKardex = false;

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading = true;
    this.productService.getAll().subscribe({
      next: (data) => {
        this.products = data;
        this.cart = this.cart
          .map(item => ({ ...item, product: data.find(p => p.id === item.product.id)! }))
          .filter(item => item.product && item.product.stock > 0);
        this.loading = false;
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudieron cargar los productos'
        });
        this.loading = false;
      }
    });
  }

  goToCreate(): void {
    this.router.navigate(['/products/new']);
  }

  goToEdit(id: number): void {
    this.router.navigate(['/products/edit', id]);
  }

  confirmDelete(product: Product): void {
    this.confirmationService.confirm({
      message: `¿Estás seguro de eliminar "${product.nombre}"?`,
      header: 'Confirmar eliminación',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Sí, eliminar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.deleteProduct(product.id!)
    });
  }

  private deleteProduct(id: number): void {
    this.productService.delete(id).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Éxito',
          detail: 'Producto eliminado correctamente'
        });
        this.loadProducts();
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudo eliminar el producto'
        });
      }
    });
  }

  openKardex(product: Product): void {
    this.kardexProduct = product;
    this.movements = [];
    this.kardexVisible = true;
    this.loadingKardex = true;
    this.productService.getMovements(product.id!).subscribe({
      next: (data) => {
        this.movements = data;
        this.loadingKardex = false;
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudo cargar el kardex del producto'
        });
        this.loadingKardex = false;
      }
    });
  }

  getStockSeverity(stock: number): 'success' | 'warn' | 'danger' {
    if (stock > 10) return 'success';
    if (stock > 0) return 'warn';
    return 'danger';
  }

  openAdd(product: Product): void {
    this.addProduct = product;
    this.addQuantity = 1;
    this.addVisible = true;
  }

  availableToAdd(product: Product): number {
    return product.stock - this.quantityInCart(product);
  }

  isValidAdd(): boolean {
    return !!this.addProduct
      && Number.isInteger(this.addQuantity)
      && this.addQuantity >= 1
      && this.addQuantity <= this.availableToAdd(this.addProduct);
  }

  confirmAdd(): void {
    if (!this.addProduct || !this.isValidAdd()) return;
    if (this.cart.length === 0) {
      this.idempotencyKey = crypto.randomUUID();
    }
    const item = this.cart.find(i => i.product.id === this.addProduct!.id);
    if (item) {
      item.cantidad += this.addQuantity;
    } else {
      this.cart.push({ product: this.addProduct, cantidad: this.addQuantity });
    }
    this.addVisible = false;
  }

  quantityInCart(product: Product): number {
    return this.cart.find(i => i.product.id === product.id)?.cantidad ?? 0;
  }

  removeFromCart(productId: number): void {
    this.cart = this.cart.filter(i => i.product.id !== productId);
    if (this.cart.length === 0) {
      this.sellVisible = false;
    }
  }

  cartTotal(): number {
    return this.cart.reduce((total, i) => total + i.product.precio * i.cantidad, 0);
  }

  isValidCart(): boolean {
    return this.cart.length > 0 && this.cart.every(i =>
      Number.isInteger(i.cantidad) && i.cantidad >= 1 && i.cantidad <= i.product.stock);
  }

  confirmSell(): void {
    if (!this.isValidCart()) return;
    this.selling = true;

    const request = {
      items: this.cart.map(i => ({ productoId: i.product.id!, cantidad: i.cantidad }))
    };
    this.saleService.create(request, this.idempotencyKey).subscribe({
      next: (sale) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Venta registrada',
          detail: `Venta ${sale.codigo} por ${sale.total.toFixed(2)} USD`
        });
        this.cart = [];
        this.selling = false;
        this.sellVisible = false;
        this.loadProducts();
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'No se pudo registrar la venta',
          detail: err.error?.detail ?? 'Error al registrar la venta'
        });
        this.selling = false;
        if (err.status === 409 || err.status === 404) {
          this.loadProducts();
        }
      }
    });
  }
}