import { ChangeDetectorRef, Component, inject, PLATFORM_ID } from '@angular/core';
import { DatePipe, isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CitasService } from '../../core/services/citas.service';
import { Cita } from '../../shared/models/cita.model';
import { AppointmentActions } from '../../shared/components/appointment-actions/appointment-actions';
import { SearchableSelect, SearchableSelectOption } from '../../shared/components/searchable-select/searchable-select';
import { DatePicker } from '../../shared/components/date-picker/date-picker';

@Component({
  imports: [RouterLink, DatePipe, FormsModule, AppointmentActions, SearchableSelect, DatePicker],
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
  selectedDate = '';
  selectedProfessional = '';

  get upcomingAppointments(): Cita[] {
    const now = Date.now();
    return this.appointments.filter(
      (appointment) =>
        appointment.estado !== 'CANCELADA'
        && new Date(appointment.fecha_hora).getTime() >= now,
    );
  }

  get filteredAppointments(): Cita[] {
    return this.upcomingAppointments.filter((appointment) =>
      (!this.selectedDate || this.appointmentDate(appointment) === this.selectedDate)
      && (!this.selectedProfessional || appointment.medico === Number(this.selectedProfessional)),
    );
  }

  get professionals(): Array<{ id: number; name: string }> {
    const namesById = new Map<number, string>();
    for (const appointment of this.upcomingAppointments) {
      namesById.set(appointment.medico, this.professionalName(appointment));
    }
    return [...namesById]
      .map(([id, name]) => ({ id, name }))
      .sort((left, right) => left.name.localeCompare(right.name, 'es'));
  }

  get professionalOptions(): SearchableSelectOption[] {
    return [
      { value: '', label: 'Todos los médicos' },
      ...this.professionals.map(({ id, name }) => ({
        value: id.toString(),
        label: name,
        searchText: name,
      })),
    ];
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
        this.appointments = this.sortAppointments(items);
        this.currentPage = 1;
        this.changeDetector.markForCheck();
      },
      error: () => { this.feedback = 'No fue posible cargar tus citas.'; this.changeDetector.markForCheck(); },
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

  updateAppointment(updated: Cita): void {
    this.appointments = this.sortAppointments(
      this.appointments.map((appointment) =>
        appointment.id === updated.id ? updated : appointment,
      ),
    );
    const updatedIndex = this.filteredAppointments.findIndex(
      (appointment) => appointment.id === updated.id,
    );
    if (updatedIndex >= 0) {
      this.currentPage = Math.floor(updatedIndex / this.pageSize) + 1;
    } else {
      this.currentPage = Math.min(this.currentPage, this.totalPages);
    }
    this.changeDetector.markForCheck();
  }

  private appointmentDate(appointment: Cita): string {
    const date = new Date(appointment.fecha_hora);
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }

  private sortAppointments(appointments: Cita[]): Cita[] {
    return [...appointments].sort((a, b) =>
      new Date(a.fecha_hora).getTime() - new Date(b.fecha_hora).getTime(),
    );
  }
}
