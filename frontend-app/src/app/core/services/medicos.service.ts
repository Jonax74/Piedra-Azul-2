import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { Medico } from '../../shared/models/medico.model';
import { Persona } from '../../shared/models/persona.model';
import { Especialidad } from '../../shared/models/especialidad.model';
import { shareReplay } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class MedicosService {
  private readonly api = inject(ApiService);
  private readonly especialidades$ = this.api.get<Especialidad[]>('/especialidades/').pipe(shareReplay({ bufferSize: 1, refCount: false }));

  getMedicos(params?: { q?: string; dni?: string }) {
    return this.api.get<Medico[]>('/medicos/', params);
  }

  getEspecialidades() {
    return this.especialidades$;
  }

  crearMedico(payload: Partial<Medico> & {
    persona_data?: Partial<Persona>;
    username?: string;
    password?: string;
    password_confirmation?: string;
    especialidad_ids?: number[];
  }) {
    return this.api.post<Medico>('/medicos/', payload);
  }

  actualizarMedico(id: number, payload: Partial<Medico>) {
    return this.api.put<Medico>(`/medicos/${id}/`, payload);
  }
}
