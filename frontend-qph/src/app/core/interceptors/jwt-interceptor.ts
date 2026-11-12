import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth';
import { environment } from '../../../environments/environment';

function withToken(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
}

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(environment.apiUrl) || req.url.startsWith(`${environment.apiUrl}/auth/`)) {
    return next(req);
  }

  const authService = inject(AuthService);
  const router = inject(Router);

  return next(withToken(req, authService.getToken())).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401) {
        return throwError(() => error);
      }
      return authService.refresh().pipe(
        catchError(() => {
          authService.clearSession();
          router.navigate(['/login']);
          return throwError(() => error);
        }),
        switchMap(response => next(withToken(req, response.token)))
      );
    })
  );
};
