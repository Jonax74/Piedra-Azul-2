import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DisponibilidadService } from '../../core/services/disponibilidad.service';
import { MedicosService } from '../../core/services/medicos.service';
import { PersonasService } from '../../core/services/personas.service';
import { Medico } from '../../shared/models/medico.model';
import { Persona } from '../../shared/models/persona.model';
import { Disponibilidad as DisponibilidadModel } from '../../shared/models/disponibilidad.model';

interface AvailabilityFieldErrors {
  professional?: string;
  day?: string;
  startTime?: string;
  endTime?: string;
  interval?: string;
}

@Component({
  imports: [FormsModule],
  selector: 'app-disponibilidad',
  styleUrl: './disponibilidad.scss',
  templateUrl: './disponibilidad.html',
})
export class Disponibilidad {
  private readonly disponibilidadService = inject(DisponibilidadService);
  private readonly medicosService = inject(MedicosService);
  private readonly personasService = inject(PersonasService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  readonly weekdays = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes'];
  readonly weekdayValues = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES'];
  professionals: Medico[] = [];
  schedules: DisponibilidadModel[] = [];
  people: Persona[] = [];
  relations: Array<{ medico: number; disponibilidad: number }> = [];
  relationsLoaded = false;
  currentPage = 1;
  readonly pageSize = 10;
  selectedProfessional = '';
  selectedDay = 'LUNES';
  startTime = '08:00';
  endTime = '17:00';
  interval = 30;
  isSaving = false;
  feedback = '';
  fieldErrors: AvailabilityFieldErrors = {};

  ngOnInit(): void {
    this.medicosService.getMedicos().subscribe({ next: (items) => { this.professionals = items.filter((item) => item.estado === 'ACTIVO'); this.changeDetector.markForCheck(); } });
    this.personasService.getPersonas().subscribe({ next: (items) => { this.people = items; this.changeDetector.markForCheck(); } });
    this.loadRelations();
    this.loadSchedules();
  }

  private loadRelations(): void {
    this.disponibilidadService.getMedicoDisponibilidades().subscribe({
      next: (items) => { this.relations = items; this.relationsLoaded = true; this.changeDetector.markForCheck(); },
      error: () => { this.feedback = 'No fue posible cargar las relaciones de los horarios.'; this.changeDetector.markForCheck(); },
    });
  }
  
  refreshSchedules(): void {
    this.loadRelations();
    this.loadSchedules();
  }

  loadSchedules(): void {
    this.disponibilidadService.getDisponibilidades().subscribe({
      next: (items) => { this.schedules = items; this.currentPage = 1; this.changeDetector.markForCheck(); },
      error: () => { this.feedback = 'No fue posible cargar los horarios.'; this.changeDetector.markForCheck(); },
    });
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

  saveSchedule(): void {
    this.fieldErrors = {};
    if (!this.selectedProfessional) {
      this.fieldErrors.professional = 'Selecciona un profesional.';
      this.feedback = '';
      return;
    }
    if (!this.selectedDay) {
      this.fieldErrors.day = 'Selecciona un día de atención.';
      this.feedback = '';
      return;
    }
    if (!this.startTime || !this.endTime) {
      if (!this.startTime) this.fieldErrors.startTime = 'Completa la hora inicial.';
      if (!this.endTime) this.fieldErrors.endTime = 'Completa la hora final.';
      this.feedback = '';
      return;
    }
    if (this.startTime >= this.endTime) {
      this.fieldErrors.endTime = 'Debe ser posterior a la hora inicial.';
      this.feedback = '';
      return;
    }
    if (!this.interval || this.interval < 1) {
      this.fieldErrors.interval = 'Selecciona un intervalo válido.';
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
      return;
    }
    this.isSaving = true;
    this.feedback = '';
    this.disponibilidadService.crearDisponibilidad({ dia_semana: this.selectedDay, hora_inicio: this.startTime, hora_fin: this.endTime, intervalo: this.interval }).subscribe({
      next: (schedule) => this.disponibilidadService.asociarConMedico({ medico: Number(this.selectedProfessional), disponibilidad: schedule.id! }).subscribe({
        next: () => {
          this.resetForm();
          this.feedback = 'Horario guardado correctamente.';
          this.isSaving = false;
          this.refreshSchedules();
          this.changeDetector.markForCheck();
        },
        error: () => { this.feedback = 'El horario se creó, pero no pudo asociarse al profesional.'; this.isSaving = false; this.changeDetector.markForCheck(); },
      }),
      error: () => { this.feedback = 'No fue posible guardar el horario.'; this.isSaving = false; this.changeDetector.markForCheck(); },
    });
  }

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

  private resetForm(): void {
    this.selectedProfessional = '';
    this.selectedDay = 'LUNES';
    this.startTime = '08:00';
    this.endTime = '17:00';
    this.interval = 30;
    this.fieldErrors = {};
    this.currentPage = 1;
  }
}
