import { ChangeDetectorRef, Component } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { inject } from '@angular/core';
import { CitasService } from '../../core/services/citas.service';
import { Cita } from '../../shared/models/cita.model';

@Component({
  imports: [RouterLink, DatePipe],
  selector: 'app-mis-citas',
  styleUrl: './mis-citas.scss',
  templateUrl: './mis-citas.html',
})
export class MisCitas {
  private readonly citasService = inject(CitasService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  appointments: Cita[] = [];
  feedback = '';

  professionalName(appointment: Cita): string {
    const persona = appointment.medico_detalle?.persona;
    return persona
      ? `${persona.primer_nombre} ${persona.primer_apellido}`
      : `Profesional #${appointment.medico}`;
  }

  ngOnInit(): void {
    this.citasService.getCitas().subscribe({
      next: (items) => { this.appointments = items; this.changeDetector.markForCheck(); },
      error: () => { this.feedback = 'No fue posible cargar tus citas.'; this.changeDetector.markForCheck(); },
    });
  }
}
