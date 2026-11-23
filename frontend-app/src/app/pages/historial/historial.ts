import { ChangeDetectorRef, Component, PLATFORM_ID, inject } from '@angular/core';
import { DatePipe, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CitasService } from '../../core/services/citas.service';
import { Cita } from '../../shared/models/cita.model';

@Component({
  imports: [DatePipe, FormsModule],
  selector: 'app-historial',
  styleUrl: './historial.scss',
  templateUrl: './historial.html',
})
export class Historial {
  private readonly citasService = inject(CitasService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly platformId = inject(PLATFORM_ID);
  appointments: Cita[] = [];
  feedback = '';
  readonly pageSize = 10;
  currentPage = 1;
  selectedDate = '';
  selectedProfessional = '';

  get historyAppointments(): Cita[] {
    const now = Date.now();
    return this.appointments
      .filter((appointment) =>
        appointment.estado === 'CANCELADA'
        || new Date(appointment.fecha_hora).getTime() < now,
      )
      .reverse();
  }

  get filteredAppointments(): Cita[] {
    return this.historyAppointments.filter((appointment) =>
      (!this.selectedDate || this.appointmentDate(appointment) === this.selectedDate)
      && (!this.selectedProfessional || appointment.medico === Number(this.selectedProfessional)),
    );
  }

  get professionals(): Array<{ id: number; name: string }> {
    const namesById = new Map<number, string>();
    for (const appointment of this.historyAppointments) {
      namesById.set(appointment.medico, this.professionalName(appointment));
    }
    return [...namesById]
      .map(([id, name]) => ({ id, name }))
      .sort((left, right) => left.name.localeCompare(right.name, 'es'));
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredAppointments.length / this.pageSize));
  }

  get paginatedAppointments(): Cita[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredAppointments.slice(start, start + this.pageSize);
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
        this.appointments = [...items].sort((a, b) =>
          new Date(a.fecha_hora).getTime() - new Date(b.fecha_hora).getTime(),
        );
        this.currentPage = 1;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.feedback = 'No fue posible cargar el historial de tus citas.';
        this.changeDetector.markForCheck();
      },
    });
  }

  onFilterChange(): void {
    this.currentPage = 1;
  }

  clearFilters(): void {
    this.selectedDate = '';
    this.selectedProfessional = '';
    this.onFilterChange();
  }

  previousPage(): void {
    if (this.currentPage > 1) this.currentPage -= 1;
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) this.currentPage += 1;
  }

  private appointmentDate(appointment: Cita): string {
    const date = new Date(appointment.fecha_hora);
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }
}
