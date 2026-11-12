import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { adminGuard } from './core/guards/admin-guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login').then(m => m.LoginComponent)
  },
  {
    path: '',
    loadComponent: () =>
      import('./layout/main-layout/main-layout').then(m => m.MainLayoutComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'products', pathMatch: 'full' },
      {
        path: 'products',
        loadChildren: () =>
          import('./features/products/products.routes').then(m => m.PRODUCT_ROUTES)
      },
      {
        path: 'sales',
        loadComponent: () =>
          import('./features/sales/sale-list/sale-list').then(m => m.SaleListComponent)
      },
      {
        path: 'users',
        canActivate: [adminGuard],
        loadChildren: () =>
          import('./features/users/users.routes').then(m => m.USER_ROUTES)
      }
    ]
  },
  { path: '**', redirectTo: 'products' }
];
