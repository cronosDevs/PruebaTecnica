import { Routes } from '@angular/router';
import { adminGuard } from '../../core/guards/admin-guard';

export const PRODUCT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./product-list/product-list').then(m => m.ProductListComponent)
  },
  {
    path: 'new',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./product-form/product-form').then(m => m.ProductFormComponent)
  },
  {
    path: 'edit/:id',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./product-form/product-form').then(m => m.ProductFormComponent)
  }
];
