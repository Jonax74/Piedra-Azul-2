import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { UsuarioAutenticado, PerfilUsuario } from '../../shared/models/usuario.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getCurrentUser() {
    return this.http.get<UsuarioAutenticado>(`${this.apiUrl}/me/`);
  }

  getProfile() {
    return this.http.get<PerfilUsuario>(`${this.apiUrl}/me/profile/`);
  }

  async getProfileSync(): Promise<PerfilUsuario | null> {
    try {
      return await firstValueFrom(this.getProfile());
    } catch {
      return null;
    }
  }
}
