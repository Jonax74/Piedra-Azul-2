import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  get<T>(path: string, params?: Record<string, string | number | boolean | null | undefined>) {
    return this.http.get<T>(`${this.apiUrl}${path}`, { params: this.toHttpParams(params) });
  }

  getBlob(path: string, params?: Record<string, string | number | boolean | null | undefined>) {
    return this.http.get(`${this.apiUrl}${path}`, {
      params: this.toHttpParams(params),
      responseType: 'blob',
    });
  }

  post<T>(path: string, body: unknown) {
    return this.http.post<T>(`${this.apiUrl}${path}`, body);
  }

  put<T>(path: string, body: unknown) {
    return this.http.put<T>(`${this.apiUrl}${path}`, body);
  }

  patch<T>(path: string, body: unknown) {
    return this.http.patch<T>(`${this.apiUrl}${path}`, body);
  }

  delete<T>(path: string) {
    return this.http.delete<T>(`${this.apiUrl}${path}`);
  }

  private toHttpParams(params?: Record<string, string | number | boolean | null | undefined>) {
    let httpParams = new HttpParams();

    Object.entries(params ?? {}).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    });

    return httpParams;
  }
}
