import { ChangeDetectorRef, Component, inject, PLATFORM_ID } from '@angular/core';
import { DatePipe, isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Auth } from '../../core/services/auth';
import { CitasService } from '../../core/services/citas.service';
import { Cita } from '../../shared/models/cita.model';

@Component({
  imports: [RouterLink, DatePipe],
  selector: 'app-dashboard',
  styleUrl: './dashboard.scss',
  templateUrl: './dashboard.html',
})
export class Dashboard {
  readonly auth = inject(Auth);
  private readonly citasService = inject(CitasService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly platformId = inject(PLATFORM_ID);
  nextAppointment: Cita | null = null;

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    this.citasService.getCitas().subscribe({
      next: (appointments) => {
        const now = Date.now();
        this.nextAppointment = appointments
          .filter((appointment) => appointment.estado !== 'CANCELADA')
          .filter((appointment) => new Date(appointment.fecha_hora).getTime() > now)
          .sort((left, right) => (
            new Date(left.fecha_hora).getTime()
            - new Date(right.fecha_hora).getTime()
          ))[0] ?? null;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.nextAppointment = null;
        this.changeDetector.markForCheck();
      },
    });
  }

  professionalName(appointment: Cita): string {
    const persona = appointment.medico_detalle?.persona;
    return persona
      ? `${persona.primer_nombre} ${persona.primer_apellido}`
      : `Profesional #${appointment.medico}`;
  }

  patientName(appointment: Cita): string {
    const persona = appointment.paciente_detalle?.persona;
    return persona
      ? `${persona.primer_nombre} ${persona.primer_apellido}`
      : `Paciente #${appointment.paciente}`;
  }

  isPatientOnly(): boolean {
    const roles = this.auth.roles();
    return roles.includes('PACIENTE')
      && !roles.some((role) => ['ADMIN', 'AGENDADOR', 'MEDICO'].includes(role));
  }
}
