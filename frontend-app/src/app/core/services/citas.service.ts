import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { inject } from '@angular/core';
import { Cita } from '../../shared/models/cita.model';
import { timeout } from 'rxjs';

interface AgendaResponse {
  cantidad: number;
  resultados: Cita[];
}

interface AgendaDatesResponse {
  mes: string;
  fechas: string[];
}

interface FranjasResponse {
  medico: number;
  fecha: string;
  cantidad: number;
  franjas: Array<{
    fecha: string;
    hora: string;
    fecha_hora: string;
  }>;
}

@Injectable({ providedIn: 'root' })
export class CitasService {
  private readonly api = inject(ApiService);

  getAgenda(medicoId?: number, fecha?: string) {
    return this.api.get<AgendaResponse>('/citas/agenda/', { medico: medicoId, fecha }).pipe(timeout({ first: 12000 }));
  }

  getFechasAgenda(mes: string, medicoId?: number) {
    return this.api.get<AgendaDatesResponse>('/citas/agenda/fechas/', {
      mes,
      medico: medicoId,
    }).pipe(timeout({ first: 12000 }));
  }

  exportarAgenda(medicoId: number, fecha: string) {
    return this.api.getBlob('/citas/agenda/exportar/', {
      medico: medicoId,
      fecha,
    }).pipe(timeout({ first: 12000 }));
  }

  exportarAgendaMedico(fecha: string) {
    return this.api.getBlob('/citas/medico/agenda/exportar/', { fecha })
      .pipe(timeout({ first: 12000 }));
  }

  getAgendaMedico(fecha?: string) {
    return this.api.get<AgendaResponse>('/citas/medico/agenda/', { fecha }).pipe(timeout({ first: 12000 }));
  }

  getFranjasDisponibles(medicoId: number, fecha: string) {
    return this.api.get<FranjasResponse>('/citas/disponibles/', { medico: medicoId, fecha }).pipe(timeout({ first: 12000 }));
  }

  getCitas() {
    return this.api.get<Cita[]>('/citas/');
  }

  crearCita(payload: Partial<Cita>) {
    return this.api.post<Cita>('/citas/', payload);
  }

  actualizarCita(id: number, payload: Partial<Cita>) {
    return this.api.put<Cita>(`/citas/${id}/`, payload);
  }
}
