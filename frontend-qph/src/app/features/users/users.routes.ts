import { Routes } from '@angular/router';

export const USER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./user-list/user-list').then(m => m.UserListComponent)
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./user-form/user-form').then(m => m.UserFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () =>
      import('./user-form/user-form').then(m => m.UserFormComponent)
  }
];
