import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Sale, SaleRequest } from '../../shared/models/sale.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class SaleService {

  private readonly apiUrl = `${environment.apiUrl}/sales`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Sale[]> {
    return this.http.get<Sale[]>(this.apiUrl);
  }

  create(sale: SaleRequest, idempotencyKey: string): Observable<Sale> {
    const headers = new HttpHeaders({ 'Idempotency-Key': idempotencyKey });
    return this.http.post<Sale>(this.apiUrl, sale, { headers });
  }
}
