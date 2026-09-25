import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CitasService } from '../../core/services/citas.service';
import { MedicosService } from '../../core/services/medicos.service';
import { PersonasService, Paciente } from '../../core/services/personas.service';
import { AuthService } from '../../core/services/auth.service';
import { ConfiguracionService } from '../../core/services/configuracion.service';
import { DisponibilidadService } from '../../core/services/disponibilidad.service';
import { Medico } from '../../shared/models/medico.model';
import { Especialidad } from '../../shared/models/especialidad.model';
import { Persona } from '../../shared/models/persona.model';

interface BookingFieldErrors {
  patient?: string;
  professional?: string;
  date?: string;
  slot?: string;
}

interface CalendarDay {
  iso: string;
  number: number;
}

@Component({
  imports: [FormsModule],
  selector: 'app-agendar-cita',
  styleUrl: './agendar-cita.scss',
  templateUrl: './agendar-cita.html',
})
export class AgendarCita {
  private readonly medicosService = inject(MedicosService);
  private readonly personasService = inject(PersonasService);
  private readonly citasService = inject(CitasService);
  private readonly authService = inject(AuthService);
  private readonly configuracionService = inject(ConfiguracionService);
  private readonly disponibilidadService = inject(DisponibilidadService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  specialties: Especialidad[] = [];
  professionals: Medico[] = [];
  people: Persona[] = [];
  patients: Paciente[] = [];
  slots: Array<{ fecha_hora: string; hora: string }> = [];
  selectedSpecialty = '';
  selectedProfessional = '';
  selectedDate = '';
  selectedPatient = '';
  selectedSlot = '';
  isLoading = false;
  isSaving = false;
  feedback = '';
  showConfirmation = false;
  confirmationMessage = '';
  patientOnly = false;
  currentPersonaId: number | null = null;
  fieldErrors: BookingFieldErrors = {};
  bookingWeeks = 4;
  minBookingDate = this.todayIso();
  maxBookingDate = this.addDays(this.minBookingDate, this.bookingWeeks * 7);
  calendarMonth = new Date(`${this.minBookingDate}T12:00:00`);
  availabilityRulesLoaded = false;
  private readonly availabilityByProfessional = new Map<number, Set<string>>();

  get currentStep(): number {
    if (this.showConfirmation) return 1;
    if (this.selectedSlot || this.slots.length) return 2;
    return 1;
  }

  ngOnInit(): void {
    this.feedback = 'Cargando pacientes, profesionales y especialidades...';
    this.medicosService.getMedicos().subscribe({ next: (items) => { this.professionals = items.filter((item) => item.estado === 'ACTIVO'); this.personasService.getPersonas().subscribe({ next: (people) => { this.people = people; this.changeDetector.markForCheck(); }, error: () => { this.feedback = 'No fue posible cargar los nombres de los profesionales.'; this.changeDetector.markForCheck(); } }); this.changeDetector.markForCheck(); }, error: () => { this.feedback = 'No fue posible cargar los profesionales.'; this.changeDetector.markForCheck(); } });
    this.medicosService.getEspecialidades().subscribe({ next: (items) => { this.specialties = items; this.changeDetector.markForCheck(); }, error: () => { this.feedback = 'No fue posible cargar las especialidades.'; this.changeDetector.markForCheck(); } });
    this.loadBookingRules();
    this.loadAvailabilityRules();
    this.authService.getProfile().subscribe({
      next: (profile) => {
        const elevatedRoles = ['ADMIN', 'AGENDADOR', 'MEDICO'];
        this.patientOnly = profile.roles.includes('PACIENTE')
          && !profile.roles.some((role) => elevatedRoles.includes(role));
        this.currentPersonaId = profile.persona_id;
        this.loadPatients();
        this.changeDetector.markForCheck();
      },
      error: () => { this.feedback = 'No fue posible cargar tu perfil.'; this.changeDetector.markForCheck(); },
    });
  }

  private loadBookingRules(): void {
    this.configuracionService.getConfiguracion().subscribe({
      next: (configuracion) => {
        this.bookingWeeks = configuracion.semanas_agendamiento;
        this.maxBookingDate = this.addDays(this.minBookingDate, this.bookingWeeks * 7);
        this.changeDetector.markForCheck();
      },
      error: () => { this.feedback = 'Se usará la ventana de agendamiento predeterminada.'; this.changeDetector.markForCheck(); },
    });
  }

  private loadAvailabilityRules(): void {
    this.disponibilidadService.getDisponibilidades().subscribe({
      next: (disponibilidades) => {
        const daysById = new Map(disponibilidades.map((item) => [item.id, item.dia_semana]));
        this.disponibilidadService.getMedicoDisponibilidades().subscribe({
          next: (relations) => {
            relations.forEach((relation) => {
              const day = daysById.get(relation.disponibilidad);
              if (!day) return;
              const days = this.availabilityByProfessional.get(relation.medico) ?? new Set<string>();
              days.add(day);
              this.availabilityByProfessional.set(relation.medico, days);
            });
            this.availabilityRulesLoaded = true;
            this.changeDetector.markForCheck();
          },
          error: () => { this.feedback = 'No fue posible cargar los días disponibles.'; this.changeDetector.markForCheck(); },
        });
      },
      error: () => { this.feedback = 'No fue posible cargar las disponibilidades.'; this.changeDetector.markForCheck(); },
    });
  }

  dateChanged(): void {
    this.fieldErrors.date = this.dateValidationMessage();
    this.selectedSlot = '';
    this.slots = [];
  }

  dateValidationMessage(): string {
    if (!this.selectedDate) return 'Selecciona una fecha.';
    if (this.selectedDate < this.minBookingDate) return 'No puedes seleccionar fechas pasadas.';
    if (this.selectedDate > this.maxBookingDate) return `Solo puedes agendar dentro de las próximas ${this.bookingWeeks} semanas.`;
    return '';
  }

  calendarCells(): Array<CalendarDay | null> {
    const year = this.calendarMonth.getFullYear();
    const month = this.calendarMonth.getMonth();
    const firstWeekday = (new Date(year, month, 1).getDay() + 6) % 7;
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const cells: Array<CalendarDay | null> = Array(firstWeekday).fill(null);
    for (let day = 1; day <= daysInMonth; day += 1) {
      cells.push({ iso: this.toIso(new Date(year, month, day)), number: day });
    }
    return cells;
  }

  calendarMonthLabel(): string {
    return this.calendarMonth.toLocaleDateString('es-CO', { month: 'long', year: 'numeric' });
  }

  isDateSelectable(date: string): boolean {
    if (!this.selectedProfessional || !this.availabilityRulesLoaded) return false;
    if (date < this.minBookingDate || date > this.maxBookingDate) return false;
    const days = this.availabilityByProfessional.get(Number(this.selectedProfessional));
    if (!days?.size) return false;
    return days.has(this.weekdayFor(date));
  }

  selectDate(date: string): void {
    if (!this.isDateSelectable(date)) return;
    this.selectedDate = date;
    this.dateChanged();
  }

  previousMonth(): void {
    if (this.canPreviousMonth()) {
      this.calendarMonth = new Date(this.calendarMonth.getFullYear(), this.calendarMonth.getMonth() - 1, 1);
    }
  }

  nextMonth(): void {
    if (this.canNextMonth()) {
      this.calendarMonth = new Date(this.calendarMonth.getFullYear(), this.calendarMonth.getMonth() + 1, 1);
    }
  }

  canPreviousMonth(): boolean {
    return this.calendarMonth.getFullYear() > this.dateFromIso(this.minBookingDate).getFullYear()
      || this.calendarMonth.getMonth() > this.dateFromIso(this.minBookingDate).getMonth();
  }

  canNextMonth(): boolean {
    const maxMonth = this.dateFromIso(this.maxBookingDate);
    return this.calendarMonth.getFullYear() < maxMonth.getFullYear()
      || (this.calendarMonth.getFullYear() === maxMonth.getFullYear() && this.calendarMonth.getMonth() < maxMonth.getMonth());
  }

  private weekdayFor(date: string): string {
    return ['DOMINGO', 'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO'][this.dateFromIso(date).getDay()];
  }

  private todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private addDays(date: string, days: number): string {
    const result = new Date(`${date}T12:00:00`);
    result.setDate(result.getDate() + days);
    return result.toISOString().slice(0, 10);
  }

  private dateFromIso(date: string): Date {
    return new Date(`${date}T12:00:00`);
  }

  private toIso(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  private loadPatients(): void {
    this.personasService.getPacientes().subscribe({ next: (items) => {
      this.patients = items;
      const ownPatient = this.currentPersonaId
        ? items.find((item) => item.persona.id === this.currentPersonaId)
        : undefined;
      this.selectedPatient = this.patientOnly
        ? ownPatient?.persona.id?.toString() ?? ''
        : '';
      this.feedback = items.length ? 'Selecciona una fecha y consulta los horarios disponibles.' : 'No hay pacientes registrados para agendar.';
      this.changeDetector.markForCheck();
    }, error: () => { this.feedback = 'No fue posible cargar los pacientes.'; this.changeDetector.markForCheck(); } });
  }

  patientName(): string {
    const patient = this.patients.find(
      (item) => item.persona.id?.toString() === this.selectedPatient,
    );
    return patient
      ? `${patient.persona.primer_nombre} ${patient.persona.primer_apellido}`
      : 'Paciente no vinculado';
  }

  get filteredProfessionals(): Medico[] {
    if (!this.selectedSpecialty) return this.professionals;
    return this.professionals.filter((professional) => professional.especialidades?.some((specialty) => specialty.id?.toString() === this.selectedSpecialty));
  }

  professionalName(professional?: Medico): string {
    if (!professional) return 'el profesional seleccionado';
    const person = this.people.find((item) => item.id === professional.persona);
    return person ? `${person.primer_nombre} ${person.primer_apellido}` : 'Profesional';
  }

  loadSlots(): void {
    this.fieldErrors.date = this.dateValidationMessage();
    if (!this.selectedProfessional || !this.selectedDate) {
      if (!this.selectedProfessional) {
        this.fieldErrors.professional = 'Selecciona un profesional.';
      }
      if (!this.selectedDate) {
        this.fieldErrors.date = 'Selecciona una fecha.';
      }
      this.feedback = '';
      return;
    }
    if (this.fieldErrors.date) {
      this.feedback = '';
      return;
    }
    this.isLoading = true;
    this.feedback = '';
    this.selectedSlot = '';
    this.citasService.getFranjasDisponibles(Number(this.selectedProfessional), this.selectedDate).subscribe({
      next: (response) => { this.slots = response.franjas; this.isLoading = false; this.feedback = response.franjas.length ? `Hay ${response.franjas.length} horarios disponibles para el ${this.selectedDate}.` : `No hay horarios disponibles para el ${this.selectedDate}.`; this.changeDetector.markForCheck(); },
      error: (error: { name?: string; error?: { detail?: string } }) => { this.feedback = error.name === 'TimeoutError' ? 'La consulta de horarios tardó demasiado. Verifica que Django y PostgreSQL estén activos.' : error.error?.detail ?? 'No fue posible consultar los horarios. Verifica el profesional y la fecha.'; this.isLoading = false; this.changeDetector.markForCheck(); },
    });
  }

  book(): void {
    this.fieldErrors.date = this.dateValidationMessage();
    if (this.fieldErrors.date) {
      this.feedback = '';
      return;
    }
    if (!this.selectedPatient) {
      this.fieldErrors.patient = 'Selecciona un paciente.';
      this.feedback = '';
      return;
    }
    if (!this.selectedProfessional) {
      this.fieldErrors.professional = 'Selecciona un profesional.';
      this.feedback = '';
      return;
    }
    if (!this.selectedDate) {
      this.fieldErrors.date = 'Selecciona una fecha.';
      this.feedback = '';
      return;
    }
    if (!this.selectedSlot) {
      this.fieldErrors.slot = this.slots.length
        ? 'Selecciona un horario disponible antes de confirmar la cita.'
        : 'Consulta la disponibilidad y selecciona un horario antes de confirmar.';
      this.feedback = '';
      return;
    }
    this.isSaving = true;
    this.citasService.crearCita({ paciente: Number(this.selectedPatient), medico: Number(this.selectedProfessional), fecha_hora: this.selectedSlot, estado: 'PROGRAMADA' }).subscribe({
      next: () => {
        this.confirmationMessage = `Tu cita con ${this.professionalName(this.professionals.find((item) => item.persona.toString() === this.selectedProfessional)!) } quedó agendada para el ${this.selectedDate} a las ${this.slots.find((slot) => slot.fecha_hora === this.selectedSlot)?.hora ?? 'la hora seleccionada'}.`;
        this.resetForm();
        this.fieldErrors = {};
        this.showConfirmation = true;
        this.feedback = '';
        this.isSaving = false;
        this.changeDetector.markForCheck();
      },
      error: (error: { error?: { detail?: string; non_field_errors?: string[] } }) => { this.feedback = error.error?.detail ?? error.error?.non_field_errors?.[0] ?? 'No fue posible agendar la cita. El horario puede haberse ocupado o la fecha no ser válida.'; this.isSaving = false; this.changeDetector.markForCheck(); },
    });
  }

  closeConfirmation(): void {
    this.showConfirmation = false;
    this.changeDetector.markForCheck();
  }

  private resetForm(): void {
    this.selectedSpecialty = '';
    this.selectedProfessional = '';
    this.selectedPatient = this.patientOnly
      ? this.currentPersonaId?.toString() ?? ''
      : '';
    this.selectedDate = '';
    this.selectedSlot = '';
    this.slots = [];
  }
}
