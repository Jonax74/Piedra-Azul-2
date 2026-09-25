import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { ConfiguracionSistema } from '../../shared/models/configuracion.model';

@Injectable({ providedIn: 'root' })
export class ConfiguracionService {
  private readonly api = inject(ApiService);

  getConfiguracion() {
    return this.api.get<ConfiguracionSistema>('/configuracion/');
  }

  actualizarConfiguracion(payload: Partial<ConfiguracionSistema>) {
    return this.api.put<ConfiguracionSistema>('/configuracion/', payload);
  }
}
