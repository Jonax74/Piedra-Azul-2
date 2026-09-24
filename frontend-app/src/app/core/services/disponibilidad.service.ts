import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { Disponibilidad } from '../../shared/models/disponibilidad.model';

@Injectable({ providedIn: 'root' })
export class DisponibilidadService {
  private readonly api = inject(ApiService);

  getDisponibilidades() {
    return this.api.get<Disponibilidad[]>('/disponibilidades/');
  }

  getMedicoDisponibilidades() {
    return this.api.get<Array<{ medico: number; disponibilidad: number }>>('/medico-disponibilidades/');
  }

  crearDisponibilidad(payload: Partial<Disponibilidad>) {
    return this.api.post<Disponibilidad>('/disponibilidades/', payload);
  }

  eliminarDisponibilidad(id: number) {
    return this.api.delete<unknown>(`/disponibilidades/${id}/`);
  }

  asociarConMedico(payload: { medico: number; disponibilidad: number }) {
    return this.api.post<{ medico: number; disponibilidad: number }>('/medico-disponibilidades/', payload);
  }
}
