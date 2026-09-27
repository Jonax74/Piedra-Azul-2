import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CitasService } from '../../core/services/citas.service';
import { MedicosService } from '../../core/services/medicos.service';
import { PersonasService, Paciente } from '../../core/services/personas.service';
import { Medico } from '../../shared/models/medico.model';
import { Especialidad } from '../../shared/models/especialidad.model';
import { Persona } from '../../shared/models/persona.model';

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
  specialties: Especialidad[] = [];
  professionals: Medico[] = [];
  people: Persona[] = [];
  patients: Paciente[] = [];
  slots: Array<{ fecha_hora: string; hora: string }> = [];
  selectedSpecialty = '';
  selectedProfessional = '';
  selectedDate = new Date().toISOString().slice(0, 10);
  selectedPatient = '';
  selectedSlot = '';
  isLoading = false;
  isSaving = false;
  feedback = '';
  showConfirmation = false;
  confirmationMessage = '';

  ngOnInit(): void {
    this.feedback = 'Cargando pacientes, profesionales y especialidades...';
<<<<<<< Updated upstream
    this.medicosService.getMedicos().subscribe({ next: (items) => { this.professionals = items.filter((item) => item.estado === 'ACTIVO'); this.personasService.getPersonas().subscribe({ next: (people) => this.people = people }); }, error: () => this.feedback = 'No fue posible cargar los profesionales.' });
    this.medicosService.getEspecialidades().subscribe({ next: (items) => this.specialties = items, error: () => this.feedback = 'No fue posible cargar las especialidades.' });
=======
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
    const dateError = this.dateValidationMessage();
    this.fieldErrors.date = dateError;
    this.selectedSlot = '';
    this.slots = [];
  }

  specialtyChanged(): void {
    if (this.selectedProfessional && !this.filteredProfessionals.some((professional) => professional.persona === Number(this.selectedProfessional))) {
      this.selectedProfessional = '';
      this.selectedSlot = '';
      this.slots = [];
    }
    this.fieldErrors.professional = '';
    this.fieldErrors.slot = '';
  }

  dateValidationMessage(): string {
    if (!this.selectedDate) return 'Selecciona una fecha.';
    if (this.selectedDate < this.minBookingDate) return 'No puedes seleccionar fechas pasadas.';
    if (this.selectedDate > this.maxBookingDate) return `Solo puedes agendar dentro de las próximas ${this.bookingWeeks} semanas.`;
    if (this.selectedProfessional && this.availabilityRulesLoaded && !this.isDateSelectable(this.selectedDate)) {
      return 'El profesional no atiende en la fecha seleccionada.';
    }
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
    this.loadSlots();
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
>>>>>>> Stashed changes
    this.personasService.getPacientes().subscribe({ next: (items) => {
      this.patients = items;
      this.selectedPatient = items[0]?.persona.id?.toString() ?? '';
      this.feedback = items.length ? 'Selecciona una fecha y consulta los horarios disponibles.' : 'No hay pacientes registrados para agendar.';
    }, error: () => this.feedback = 'No fue posible cargar los pacientes.' });
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
<<<<<<< Updated upstream
    if (!this.selectedProfessional || !this.selectedDate) {
      this.feedback = 'Selecciona un profesional y una fecha.';
=======
    this.fieldErrors.professional = this.isSelectedProfessionalValid()
      ? ''
      : 'Selecciona un profesional válido.';
    const dateError = this.dateValidationMessage();
    this.fieldErrors.date = dateError;
    if (!this.selectedDate) {
      this.fieldErrors.date = 'Selecciona una fecha.';
    }
    if (this.fieldErrors.professional || this.fieldErrors.date) {
      this.feedback = '';
>>>>>>> Stashed changes
      return;
    }
    this.isLoading = true;
    this.feedback = '';
    this.selectedSlot = '';
    this.citasService.getFranjasDisponibles(Number(this.selectedProfessional), this.selectedDate).subscribe({
      next: (response) => { this.slots = response.franjas; this.isLoading = false; this.feedback = response.franjas.length ? `Hay ${response.franjas.length} horarios disponibles para el ${this.selectedDate}.` : `No hay horarios disponibles para el ${this.selectedDate}.`; },
      error: (error: { name?: string; error?: { detail?: string } }) => { this.feedback = error.name === 'TimeoutError' ? 'La consulta de horarios tardó demasiado. Verifica que Django y PostgreSQL estén activos.' : error.error?.detail ?? 'No fue posible consultar los horarios. Verifica el profesional y la fecha.'; this.isLoading = false; },
    });
  }

  book(): void {
<<<<<<< Updated upstream
    if (!this.selectedPatient || !this.selectedProfessional || !this.selectedSlot) {
      this.feedback = 'Completa paciente, profesional y horario.';
=======
    this.fieldErrors = {};
    if (!this.selectedPatient || !this.patients.some((patient) => patient.persona.id?.toString() === this.selectedPatient)) {
      this.fieldErrors.patient = 'Selecciona un paciente.';
    }
    if (!this.isSelectedProfessionalValid()) {
      this.fieldErrors.professional = 'Selecciona un profesional válido.';
    }
    const dateError = this.dateValidationMessage();
    if (dateError) this.fieldErrors.date = dateError;
    if (!this.selectedDate) {
      this.fieldErrors.date = 'Selecciona una fecha.';
    }
    if (!this.selectedSlot || !this.slots.some((slot) => slot.fecha_hora === this.selectedSlot)) {
      this.fieldErrors.slot = this.slots.length
        ? 'Selecciona un horario disponible antes de confirmar la cita.'
        : 'Consulta la disponibilidad y selecciona un horario antes de confirmar.';
    }
    if (Object.keys(this.fieldErrors).length) {
      this.feedback = '';
>>>>>>> Stashed changes
      return;
    }
    this.isSaving = true;
    this.citasService.crearCita({ paciente: Number(this.selectedPatient), medico: Number(this.selectedProfessional), fecha_hora: this.selectedSlot, estado: 'PROGRAMADA' }).subscribe({
      next: () => { this.confirmationMessage = `Tu cita con ${this.professionalName(this.professionals.find((item) => item.persona.toString() === this.selectedProfessional)!) } quedó agendada para el ${this.selectedDate} a las ${this.slots.find((slot) => slot.fecha_hora === this.selectedSlot)?.hora ?? 'la hora seleccionada'}.`; this.showConfirmation = true; this.feedback = ''; this.isSaving = false; this.selectedSlot = ''; },
      error: (error: { error?: { detail?: string; non_field_errors?: string[] } }) => { this.feedback = error.error?.detail ?? error.error?.non_field_errors?.[0] ?? 'No fue posible agendar la cita. El horario puede haberse ocupado o la fecha no ser válida.'; this.isSaving = false; },
    });
  }

  closeConfirmation(): void {
    this.showConfirmation = false;
  }

  private isSelectedProfessionalValid(): boolean {
    return this.filteredProfessionals.some((professional) => professional.persona === Number(this.selectedProfessional));
  }
}
