import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Role, User, UserRequest } from '../../shared/models/user.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class UserService {

  private readonly apiUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<User[]> {
    return this.http.get<User[]>(this.apiUrl);
  }

  getById(id: number): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${id}`);
  }

  create(user: UserRequest): Observable<User> {
    return this.http.post<User>(this.apiUrl, user);
  }

  update(id: number, user: UserRequest): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/${id}`, user);
  }

  changeStatus(id: number, activo: boolean): Observable<User> {
    return this.http.patch<User>(`${this.apiUrl}/${id}/estado`, { activo });
  }

  getRoles(): Observable<Role[]> {
    return this.http.get<Role[]>(`${environment.apiUrl}/roles`);
  }
}
