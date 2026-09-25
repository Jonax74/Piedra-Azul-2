import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { Persona } from '../../shared/models/persona.model';
import { shareReplay } from 'rxjs';

export interface Paciente {
  persona: Persona;
}

@Injectable({ providedIn: 'root' })
export class PersonasService {
  private readonly api = inject(ApiService);
  private readonly pacientes$ = this.api.get<Paciente[]>('/pacientes/').pipe(shareReplay({ bufferSize: 1, refCount: false }));
  private readonly personas$ = this.api.get<Persona[]>('/personas/').pipe(shareReplay({ bufferSize: 1, refCount: false }));

  getPacientes() {
    return this.pacientes$;
  }

  getPersonas() {
    return this.personas$;
  }
}