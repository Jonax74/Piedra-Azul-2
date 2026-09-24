import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { Medico } from '../../shared/models/medico.model';
import { Especialidad } from '../../shared/models/especialidad.model';

@Injectable({ providedIn: 'root' })
export class MedicosService {
  private readonly api = inject(ApiService);

  getMedicos() {
    return this.api.get<Medico[]>('/medicos/');
  }

  getEspecialidades() {
    return this.api.get<Especialidad[]>('/especialidades/');
  }

  crearMedico(payload: Partial<Medico>) {
    return this.api.post<Medico>('/medicos/', payload);
  }

  actualizarMedico(id: number, payload: Partial<Medico>) {
    return this.api.put<Medico>(`/medicos/${id}/`, payload);
  }
}
