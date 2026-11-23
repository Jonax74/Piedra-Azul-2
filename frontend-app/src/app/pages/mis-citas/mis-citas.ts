import { ChangeDetectorRef, Component, PLATFORM_ID } from '@angular/core';
import { DatePipe, isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { inject } from '@angular/core';
import { CitasService } from '../../core/services/citas.service';
import { Cita } from '../../shared/models/cita.model';
import { AppointmentActions } from '../../shared/components/appointment-actions/appointment-actions';

@Component({
  imports: [RouterLink, DatePipe, AppointmentActions],
  selector: 'app-mis-citas',
  styleUrl: './mis-citas.scss',
  templateUrl: './mis-citas.html',
})
export class MisCitas {
  private readonly citasService = inject(CitasService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly platformId = inject(PLATFORM_ID);
  appointments: Cita[] = [];
  feedback = '';
  readonly pageSize = 10;
  currentUpcomingPage = 1;
  currentPastPage = 1;

  get upcomingAppointments(): Cita[] {
    const now = Date.now();
    return this.appointments.filter(
      (appointment) => new Date(appointment.fecha_hora).getTime() >= now,
    );
  }

  get pastAppointments(): Cita[] {
    const now = Date.now();
    return this.appointments
      .filter((appointment) => new Date(appointment.fecha_hora).getTime() < now)
      .reverse();
  }

  get upcomingTotalPages(): number {
    return Math.max(1, Math.ceil(this.upcomingAppointments.length / this.pageSize));
  }

  get pastTotalPages(): number {
    return Math.max(1, Math.ceil(this.pastAppointments.length / this.pageSize));
  }

  get paginatedUpcomingAppointments(): Cita[] {
    const start = (this.currentUpcomingPage - 1) * this.pageSize;
    return this.upcomingAppointments.slice(start, start + this.pageSize);
  }

  get paginatedPastAppointments(): Cita[] {
    const start = (this.currentPastPage - 1) * this.pageSize;
    return this.pastAppointments.slice(start, start + this.pageSize);
  }

  professionalName(appointment: Cita): string {
    const persona = appointment.medico_detalle?.persona;
    return persona
      ? `${persona.primer_nombre} ${persona.primer_apellido}`
      : `Profesional #${appointment.medico}`;
  }

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    this.citasService.getCitas().subscribe({
      next: (items) => {
        this.appointments = this.sortAppointments(items);
        this.currentUpcomingPage = 1;
        this.currentPastPage = 1;
        this.changeDetector.markForCheck();
      },
      error: () => { this.feedback = 'No fue posible cargar tus citas.'; this.changeDetector.markForCheck(); },
    });
  }

  previousUpcomingPage(): void {
    if (this.currentUpcomingPage > 1) this.currentUpcomingPage -= 1;
  }

  nextUpcomingPage(): void {
    if (this.currentUpcomingPage < this.upcomingTotalPages) this.currentUpcomingPage += 1;
  }

  previousPastPage(): void {
    if (this.currentPastPage > 1) this.currentPastPage -= 1;
  }

  nextPastPage(): void {
    if (this.currentPastPage < this.pastTotalPages) this.currentPastPage += 1;
  }

  updateAppointment(updated: Cita): void {
    this.appointments = this.sortAppointments(
      this.appointments.map((appointment) =>
        appointment.id === updated.id ? updated : appointment,
      ),
    );
    const upcomingIndex = this.upcomingAppointments.findIndex(
      (appointment) => appointment.id === updated.id,
    );
    if (upcomingIndex >= 0) {
      this.currentUpcomingPage = Math.floor(upcomingIndex / this.pageSize) + 1;
    } else {
      this.currentUpcomingPage = Math.min(this.currentUpcomingPage, this.upcomingTotalPages);
    }
    this.currentPastPage = Math.min(this.currentPastPage, this.pastTotalPages);
    this.changeDetector.markForCheck();
  }

  private sortAppointments(appointments: Cita[]): Cita[] {
    return [...appointments].sort((a, b) =>
      new Date(a.fecha_hora).getTime() - new Date(b.fecha_hora).getTime(),
    );
  }
}
