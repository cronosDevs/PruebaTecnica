import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, finalize, shareReplay, tap } from 'rxjs';
import { AuthRequest, AuthResponse, SessionUser } from '../../shared/models/auth.model';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly TOKEN_KEY = 'jwt_token';
  private readonly USER_KEY = 'session_user';
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  private refreshRequest$: Observable<AuthResponse> | null = null;

  constructor(private http: HttpClient, private router: Router) {}

  login(credentials: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials, { withCredentials: true }).pipe(
      tap(response => this.saveSession(response))
    );
  }

  refresh(): Observable<AuthResponse> {
    if (!this.refreshRequest$) {
      this.refreshRequest$ = this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, {}, { withCredentials: true }).pipe(
        tap(response => this.saveSession(response)),
        finalize(() => this.refreshRequest$ = null),
        shareReplay(1)
      );
    }
    return this.refreshRequest$;
  }

  logout(): void {
    this.http.post(`${this.apiUrl}/logout`, {}, { withCredentials: true }).pipe(
      finalize(() => {
        this.clearSession();
        this.router.navigate(['/login']);
      })
    ).subscribe({ error: () => {} });
  }

  clearSession(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getUser(): SessionUser | null {
    const data = localStorage.getItem(this.USER_KEY);
    return data ? JSON.parse(data) : null;
  }

  isAdmin(): boolean {
    return this.getUser()?.rol === 'ADMIN';
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) return false;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }

  private saveSession(response: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, response.token);
    const user: SessionUser = {
      username: response.username,
      nombreCompleto: response.nombreCompleto,
      rol: response.rol
    };
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }
}
