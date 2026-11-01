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
  currentPage = 1;

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.appointments.length / this.pageSize));
  }

  get paginatedAppointments(): Cita[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.appointments.slice(start, start + this.pageSize);
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
        this.currentPage = 1;
        this.changeDetector.markForCheck();
      },
      error: () => { this.feedback = 'No fue posible cargar tus citas.'; this.changeDetector.markForCheck(); },
    });
  }

  previousPage(): void {
    if (this.currentPage > 1) this.currentPage -= 1;
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) this.currentPage += 1;
  }

  updateAppointment(updated: Cita): void {
    this.appointments = this.sortAppointments(
      this.appointments.map((appointment) =>
        appointment.id === updated.id ? updated : appointment,
      ),
    );
    const updatedIndex = this.appointments.findIndex(
      (appointment) => appointment.id === updated.id,
    );
    if (updatedIndex >= 0) {
      this.currentPage = Math.floor(updatedIndex / this.pageSize) + 1;
    }
    this.changeDetector.markForCheck();
  }

  private sortAppointments(appointments: Cita[]): Cita[] {
    return [...appointments].sort((a, b) =>
      new Date(a.fecha_hora).getTime() - new Date(b.fecha_hora).getTime(),
    );
  }
}
