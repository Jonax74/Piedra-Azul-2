import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DisponibilidadService } from '../../core/services/disponibilidad.service';
import { MedicosService } from '../../core/services/medicos.service';
import { Medico } from '../../shared/models/medico.model';
import { Disponibilidad as DisponibilidadModel } from '../../shared/models/disponibilidad.model';

@Component({
  imports: [FormsModule],
  selector: 'app-disponibilidad',
  styleUrl: './disponibilidad.scss',
  templateUrl: './disponibilidad.html',
})
export class Disponibilidad {
  private readonly disponibilidadService = inject(DisponibilidadService);
  private readonly medicosService = inject(MedicosService);
  readonly weekdays = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes'];
  readonly weekdayValues = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES'];
  professionals: Medico[] = [];
  schedules: DisponibilidadModel[] = [];
  selectedProfessional = '';
  selectedDay = 'LUNES';
  startTime = '08:00';
  endTime = '17:00';
  interval = 30;
  readonly validIntervals = [30, 45, 60];
  isSaving = false;
  feedback = '';

  ngOnInit(): void {
    this.medicosService.getMedicos().subscribe({ next: (items) => this.professionals = items.filter((item) => item.estado === 'ACTIVO') });
    this.loadSchedules();
  }

  loadSchedules(): void {
    this.disponibilidadService.getDisponibilidades().subscribe({
<<<<<<< Updated upstream
      next: (items) => this.schedules = items,
      error: () => this.feedback = 'No fue posible cargar los horarios.',
    });
  }

=======
      next: (items) => { this.schedules = [...items].sort((left, right) => this.compareSchedules(left, right)); this.currentPage = 1; this.changeDetector.markForCheck(); },
      error: () => { this.feedback = 'No fue posible cargar los horarios.'; this.changeDetector.markForCheck(); },
    });
  }

  private compareSchedules(left: DisponibilidadModel, right: DisponibilidadModel): number {
    const leftDay = this.weekdayValues.indexOf(left.dia_semana);
    const rightDay = this.weekdayValues.indexOf(right.dia_semana);
    return (leftDay - rightDay)
      || left.hora_inicio.localeCompare(right.hora_inicio)
      || left.hora_fin.localeCompare(right.hora_fin)
      || (left.id ?? 0) - (right.id ?? 0);
  }

  get paginatedSchedules(): DisponibilidadModel[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredSchedules.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredSchedules.length / this.pageSize));
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, index) => index + 1);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) this.currentPage = page;
  }

  get filteredSchedules(): DisponibilidadModel[] {
    if (!this.selectedProfessional) return this.schedules;
    const scheduleIds = new Set(
      this.relations
        .filter((relation) => relation.medico === Number(this.selectedProfessional))
        .map((relation) => relation.disponibilidad),
    );
    return this.schedules.filter((schedule) => schedule.id !== undefined && scheduleIds.has(schedule.id));
  }

  professionalChanged(): void {
    this.fieldErrors.professional = '';
    this.currentPage = 1;
  }

  professionalNames(schedule: DisponibilidadModel): string {
    const names = this.relations
      .filter((relation) => relation.disponibilidad === schedule.id)
      .map((relation) => this.professionals.find((professional) => professional.persona === relation.medico))
      .filter((professional): professional is Medico => Boolean(professional))
      .map((professional) => this.professionalName(professional));
    return names.length ? names.join(', ') : 'Profesional no identificado';
  }

  professionalName(professional: Medico): string {
    const person = this.people.find((item) => item.id === professional.persona);
    return person ? `${person.primer_nombre} ${person.primer_apellido}` : `Profesional #${professional.persona}`;
  }

>>>>>>> Stashed changes
  saveSchedule(): void {
    if (!this.selectedProfessional) {
<<<<<<< Updated upstream
      this.feedback = 'Selecciona un profesional.';
=======
      this.fieldErrors.professional = 'Selecciona un profesional.';
    }
    if (!this.professionals.some((professional) => professional.persona === Number(this.selectedProfessional))) {
      this.fieldErrors.professional = 'Selecciona un profesional válido.';
    }
    if (!this.weekdayValues.includes(this.selectedDay)) {
      this.fieldErrors.day = 'Selecciona un día de atención.';
    }
    if (!this.startTime || !this.endTime) {
      if (!this.startTime) this.fieldErrors.startTime = 'Completa la hora inicial.';
      if (!this.endTime) this.fieldErrors.endTime = 'Completa la hora final.';
    }
    if (this.startTime && !this.isValidTime(this.startTime)) {
      this.fieldErrors.startTime = 'Usa una hora válida.';
    }
    if (this.endTime && !this.isValidTime(this.endTime)) {
      this.fieldErrors.endTime = 'Usa una hora válida.';
    }
    if (!this.validIntervals.includes(Number(this.interval))) {
      this.fieldErrors.interval = 'Selecciona un intervalo válido.';
    }
    if (Object.keys(this.fieldErrors).length) {
      this.feedback = '';
      return;
    }
    const start = this.minutes(this.startTime);
    const end = this.minutes(this.endTime);
    if (start >= end) {
      this.fieldErrors.endTime = 'Debe ser posterior a la hora inicial.';
      this.feedback = '';
      return;
    }
    if (!this.relationsLoaded) {
      this.feedback = 'Espera a que se carguen las disponibilidades existentes.';
      return;
    }
    if (this.overlapsExistingSchedule()) {
      this.fieldErrors.startTime = 'El horario se cruza con otra disponibilidad de este profesional.';
      this.fieldErrors.endTime = 'Elige una franja que no se cruce con otra del mismo día.';
      this.feedback = '';
>>>>>>> Stashed changes
      return;
    }
    this.isSaving = true;
    this.feedback = '';
    this.disponibilidadService.crearDisponibilidad({ dia_semana: this.selectedDay, hora_inicio: this.startTime, hora_fin: this.endTime, intervalo: this.interval }).subscribe({
      next: (schedule) => this.disponibilidadService.asociarConMedico({ medico: Number(this.selectedProfessional), disponibilidad: schedule.id! }).subscribe({
        next: () => { this.feedback = 'Horario guardado correctamente.'; this.isSaving = false; this.loadSchedules(); },
        error: () => { this.feedback = 'El horario se creó, pero no pudo asociarse al profesional.'; this.isSaving = false; },
      }),
      error: () => { this.feedback = 'No fue posible guardar el horario.'; this.isSaving = false; },
    });
  }
<<<<<<< Updated upstream
=======

  private overlapsExistingSchedule(): boolean {
    const selectedProfessional = Number(this.selectedProfessional);
    const relatedScheduleIds = new Set(
      this.relations
        .filter((relation) => relation.medico === selectedProfessional)
        .map((relation) => relation.disponibilidad),
    );
    const start = this.minutes(this.startTime);
    const end = this.minutes(this.endTime);
    return this.schedules
      .filter((schedule) => schedule.id !== undefined && relatedScheduleIds.has(schedule.id) && schedule.dia_semana === this.selectedDay)
      .some((schedule) => start < this.minutes(schedule.hora_fin) && end > this.minutes(schedule.hora_inicio));
  }

  private minutes(time: string): number {
    const [hours, minutes] = time.split(':').map(Number);
    return (hours * 60) + minutes;
  }

  private isValidTime(time: string): boolean {
    return /^([01]\d|2[0-3]):[0-5]\d$/.test(time);
  }

  private resetForm(): void {
    this.selectedProfessional = '';
    this.selectedDay = 'LUNES';
    this.startTime = '08:00';
    this.endTime = '17:00';
    this.interval = 30;
    this.fieldErrors = {};
    this.currentPage = 1;
  }
>>>>>>> Stashed changes
}
