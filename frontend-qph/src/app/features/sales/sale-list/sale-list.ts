import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { SaleService } from '../../../core/services/sale';
import { AuthService } from '../../../core/services/auth';
import { Sale } from '../../../shared/models/sale.model';

@Component({
  selector: 'app-sale-list',
  standalone: true,
  imports: [CommonModule, TableModule, ToastModule],
  providers: [MessageService],
  templateUrl: './sale-list.html',
  styleUrls: ['../../products/product-list/product-list.scss', './sale-list.scss']
})
export class SaleListComponent implements OnInit {

  private saleService = inject(SaleService);
  private authService = inject(AuthService);
  private messageService = inject(MessageService);

  sales: Sale[] = [];
  loading = false;
  isAdmin = this.authService.isAdmin();

  ngOnInit(): void {
    this.loading = true;
    this.saleService.getAll().subscribe({
      next: (data) => {
        this.sales = data;
        this.loading = false;
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudieron cargar las ventas'
        });
        this.loading = false;
      }
    });
  }
}
