import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductService } from '../../../core/services/product';

// PrimeNG
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    InputTextModule,
    InputNumberModule,
    ButtonModule,
    ToastModule
  ],
  providers: [MessageService],
  templateUrl: './product-form.html',
  styleUrls: ['./product-form.scss']
})
export class ProductFormComponent implements OnInit {

  private fb = inject(FormBuilder);
  private productService = inject(ProductService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private messageService = inject(MessageService);

  form = this.fb.group({
    nombre: ['', [Validators.required, Validators.minLength(2)]],
    precio: [null as number | null, [Validators.required, Validators.min(0.01)]],
    stock: [null as number | null, [Validators.required, Validators.min(0)]]
  });

  isEdit = false;
  productId?: number;
  loading = false;
  submitting = false;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.productId = Number(id);
      this.loadProduct(this.productId);
    }
  }

  private loadProduct(id: number): void {
    this.loading = true;
    this.productService.getById(id).subscribe({
      next: (product) => {
        this.form.patchValue(product);
        this.loading = false;
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudo cargar el producto'
        });
        this.loading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    const data = this.form.value as { nombre: string; precio: number; stock: number };
    const request = this.isEdit
      ? this.productService.update(this.productId!, data)
      : this.productService.create(data);

    request.subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Éxito',
          detail: this.isEdit ? 'Producto actualizado' : 'Producto creado'
        });
        setTimeout(() => this.router.navigate(['/products']), 1500);
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: err.error?.detail ?? 'Error al guardar el producto'
        });
        this.submitting = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/products']);
  }
}