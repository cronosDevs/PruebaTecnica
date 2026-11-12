import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }
  if (!authService.getUser()) {
    return router.createUrlTree(['/login']);
  }
  return authService.refresh().pipe(
    map(() => true),
    catchError(() => {
      authService.clearSession();
      return of(router.createUrlTree(['/login']));
    })
  );
};
