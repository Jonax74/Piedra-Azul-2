import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { Medico } from '../../shared/models/medico.model';
import { Especialidad } from '../../shared/models/especialidad.model';
import { shareReplay } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class MedicosService {
  private readonly api = inject(ApiService);
  private readonly medicos$ = this.api.get<Medico[]>('/medicos/').pipe(shareReplay({ bufferSize: 1, refCount: false }));
  private readonly especialidades$ = this.api.get<Especialidad[]>('/especialidades/').pipe(shareReplay({ bufferSize: 1, refCount: false }));

  getMedicos() {
    return this.medicos$;
  }

  getEspecialidades() {
    return this.especialidades$;
  }

  crearMedico(payload: Partial<Medico>) {
    return this.api.post<Medico>('/medicos/', payload);
  }

  actualizarMedico(id: number, payload: Partial<Medico>) {
    return this.api.put<Medico>(`/medicos/${id}/`, payload);
  }
}
