import { DatePipe, isPlatformBrowser } from '@angular/common';
import { ChangeDetectorRef, Component, inject, PLATFORM_ID, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Auth } from '../../core/services/auth';
import { CitasService } from '../../core/services/citas.service';
import { MedicosService } from '../../core/services/medicos.service';
import { Cita } from '../../shared/models/cita.model';
import { Medico } from '../../shared/models/medico.model';
import { Persona } from '../../shared/models/persona.model';
import { PersonasService } from '../../core/services/personas.service';

@Component({
  imports: [FormsModule, DatePipe],
  selector: 'app-agenda',
  styleUrl: './agenda.scss',
  templateUrl: './agenda.html',
})
export class Agenda {
  private readonly medicosService = inject(MedicosService);
  private readonly personasService = inject(PersonasService);
  private readonly citasService = inject(CitasService);
  private readonly auth = inject(Auth);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly platformId = inject(PLATFORM_ID);
  professionals: Medico[] = [];
  people: Persona[] = [];
  readonly appointments = signal<Cita[]>([]);
  private doctorAppointments: Cita[] = [];
  selectedProfessional = '';
  selectedDate = '';
  readonly isLoading = signal(false);
  readonly isLoadingDates = signal(false);
  readonly isExporting = signal(false);
  isCalendarOpen = false;
  readonly feedback = signal('');
  readonly calendarError = signal('');
  calendarMonth = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
  readonly availableAppointmentDates = signal(new Set<string>());
  readonly isDoctorOnly = this.auth.roles().includes('MEDICO')
    && !this.auth.roles().some((role) => ['ADMIN', 'AGENDADOR'].includes(role));
  readonly isSchedulerOnly = this.auth.roles().includes('AGENDADOR')
    && !this.auth.roles().includes('ADMIN');
  readonly pageSize = 10;
  currentPage = 1;

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.appointments().length / this.pageSize));
  }

  get paginatedAppointments(): Cita[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.appointments().slice(start, start + this.pageSize);
  }

  calendarMonthLabel(): string {
    return new Intl.DateTimeFormat('es-CO', {
      month: 'long',
      year: 'numeric',
    }).format(this.calendarMonth);
  }

  calendarCells(): Array<{ iso: string; number: number; hasAppointments: boolean } | null> {
    const year = this.calendarMonth.getFullYear();
    const month = this.calendarMonth.getMonth();
    const firstWeekday = (new Date(year, month, 1).getDay() + 6) % 7;
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const cells: Array<{ iso: string; number: number; hasAppointments: boolean } | null> = [];

    for (let index = 0; index < firstWeekday; index += 1) cells.push(null);
    for (let day = 1; day <= daysInMonth; day += 1) {
      const iso = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      cells.push({ iso, number: day, hasAppointments: this.availableAppointmentDates().has(iso) });
    }
    while (cells.length % 7 !== 0) cells.push(null);

    return cells;
  }

  changeCalendarMonth(offset: number): void {
    this.calendarMonth = new Date(
      this.calendarMonth.getFullYear(),
      this.calendarMonth.getMonth() + offset,
      1,
    );
    this.selectedDate = '';
    this.isCalendarOpen = true;
    if (this.isSchedulerOnly) this.loadSchedulerAppointmentDates();
    this.search();
  }

  selectAppointmentDate(date: string): void {
    if (!this.availableAppointmentDates().has(date) || this.isLoadingDates()) return;
    this.selectedDate = date;
    this.isCalendarOpen = false;
    this.search();
  }

  onProfessionalChange(): void {
    if (!this.isSchedulerOnly) return;
    this.selectedDate = '';
    this.loadSchedulerAppointmentDates();
    this.search();
  }

  toggleCalendar(): void {
    this.isCalendarOpen = !this.isCalendarOpen;
  }

  clearDoctorDateFilter(): void {
    this.selectedDate = '';
    this.search();
  }

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    if (this.isDoctorOnly) {
      this.loadAppointmentDates();
    } else {
      this.feedback.set('Cargando profesionales...');
      this.medicosService.getMedicos().subscribe({
        next: (medicos) => {
          this.professionals = medicos.filter((medico) => medico.estado === 'ACTIVO');
          this.changeDetector.markForCheck();
          this.personasService.getPersonas().subscribe({
            next: (people) => { this.people = people; this.feedback.set('Puedes consultar todas las citas o aplicar uno o ambos filtros.'); this.changeDetector.markForCheck(); },
            error: () => { this.feedback.set('Los profesionales cargaron, pero no se pudieron cargar sus nombres.'); this.changeDetector.markForCheck(); },
          });
        },
        error: () => { this.feedback.set('No fue posible cargar los profesionales.'); this.changeDetector.markForCheck(); },
      });
    }
    if (this.isSchedulerOnly) this.loadSchedulerAppointmentDates();
    if (!this.isDoctorOnly) this.search();
  }

  private loadAppointmentDates(): void {
    this.isLoadingDates.set(true);
    this.calendarError.set('');
    this.isLoading.set(true);

    this.citasService.getAgendaMedico().subscribe({
      next: (response) => {
        this.doctorAppointments = response.resultados;
        this.availableAppointmentDates.set(new Set(
          this.doctorAppointments
            .filter((appointment) => appointment.estado !== 'CANCELADA')
            .map((appointment) => appointment.fecha_hora.slice(0, 10)),
        ));
        this.isLoadingDates.set(false);
        this.isLoading.set(false);
        this.search();
      },
      error: (error: { error?: { detail?: string } }) => {
        const message = error.error?.detail
          ?? 'No fue posible cargar tus citas para el calendario. Inténtalo de nuevo.';
        this.calendarError.set(message);
        this.isLoadingDates.set(false);
        this.isLoading.set(false);
        this.feedback.set(message);
        this.changeDetector.markForCheck();
      },
    });
  }

  private loadSchedulerAppointmentDates(): void {
    const month = this.currentCalendarMonth();
    const medicoId = this.selectedProfessional ? Number(this.selectedProfessional) : undefined;
    this.isLoadingDates.set(true);
    this.calendarError.set('');

    this.citasService.getFechasAgenda(month, medicoId).subscribe({
      next: (response) => {
        if (response.mes !== this.currentCalendarMonth()
          || medicoId !== (this.selectedProfessional ? Number(this.selectedProfessional) : undefined)) {
          return;
        }
        this.availableAppointmentDates.set(new Set(response.fechas));
        this.isLoadingDates.set(false);
        this.changeDetector.markForCheck();
      },
      error: (error: { error?: { detail?: string } }) => {
        if (month !== this.currentCalendarMonth()
          || medicoId !== (this.selectedProfessional ? Number(this.selectedProfessional) : undefined)) {
          return;
        }
        this.calendarError.set(error.error?.detail
          ?? 'No fue posible cargar las fechas con citas.');
        this.availableAppointmentDates.set(new Set());
        this.isLoadingDates.set(false);
        this.changeDetector.markForCheck();
      },
    });
  }

  private currentCalendarMonth(): string {
    return `${this.calendarMonth.getFullYear()}-${String(this.calendarMonth.getMonth() + 1).padStart(2, '0')}`;
  }

  professionalName(professional: Medico): string {
    const person = this.people.find((item) => item.id === professional.persona);
    return person ? `${person.primer_nombre} ${person.primer_apellido}` : 'Profesional';
  }

  exportAppointments(): void {
    if (!this.selectedDate) return;
    if (!this.isDoctorOnly && (!this.isSchedulerOnly || !this.selectedProfessional)) return;

    const medicoId = this.isSchedulerOnly ? Number(this.selectedProfessional) : undefined;
    const date = this.selectedDate;
    this.isExporting.set(true);

    const request = this.isDoctorOnly
      ? this.citasService.exportarAgendaMedico(date)
      : this.citasService.exportarAgenda(Number(this.selectedProfessional), date);

    request.subscribe({
      next: (file) => {
        const downloadUrl = URL.createObjectURL(file);
        const link = document.createElement('a');
        link.href = downloadUrl;
        const filename = this.isDoctorOnly
          ? this.doctorExportFilename(date)
          : this.schedulerExportFilename(Number(this.selectedProfessional), date);
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(downloadUrl);
        this.feedback.set('La agenda se exportó correctamente.');
        this.isExporting.set(false);
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.feedback.set('No fue posible exportar la agenda. Inténtalo nuevamente.');
        this.isExporting.set(false);
        this.changeDetector.markForCheck();
      },
    });
  }

  private doctorExportFilename(date: string): string {
    const persona = this.doctorAppointments[0]?.medico_detalle?.persona;
    const name = persona
      ? `${persona.primer_nombre ?? ''} ${persona.primer_apellido ?? ''}`
      : `medico-${this.auth.username()}`;
    return `citas-${this.slugify(name)}-${date}.csv`;
  }

  private schedulerExportFilename(medicoId: number, date: string): string {
    const professional = this.professionals.find((item) => item.persona === medicoId);
    const name = professional
      ? this.professionalName(professional)
      : `medico-${medicoId}`;
    return `citas-${this.slugify(name)}-${date}.csv`;
  }

  private slugify(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-|-$/g, '');
  }

  search(): void {
    this.isLoading.set(true);
    this.feedback.set('');
    if (this.isDoctorOnly) {
      const filteredAppointments = this.selectedDate
        ? this.doctorAppointments.filter(
          (appointment) => appointment.fecha_hora.slice(0, 10) === this.selectedDate,
        )
        : [...this.doctorAppointments];
      this.appointments.set(filteredAppointments);
      this.currentPage = 1;
      this.feedback.set(filteredAppointments.length
        ? `Se encontraron ${filteredAppointments.length} citas para ${this.selectedDate ? `el ${this.selectedDate}` : 'todas las fechas'}.`
        : `No hay citas registradas para ${this.selectedDate ? `el ${this.selectedDate}` : 'ninguna fecha'}.`);
      this.isLoading.set(false);
      return;
    }

    const fecha = this.selectedDate || undefined;
    const request = this.citasService.getAgenda(
      this.selectedProfessional ? Number(this.selectedProfessional) : undefined,
      fecha,
    );

    request.subscribe({
      next: (response) => {
        this.appointments.set(response.resultados);
        this.currentPage = 1;
        const filtro = [
          this.selectedDate ? `el ${this.selectedDate}` : 'todas las fechas',
          this.selectedProfessional ? 'el profesional seleccionado' : 'todos los profesionales',
        ].join(' y ');
        this.feedback.set(response.cantidad
          ? `Se encontraron ${response.cantidad} citas para ${filtro}.`
          : `No hay citas registradas para ${filtro}.`);
        this.isLoading.set(false);
        this.changeDetector.markForCheck();
      },
      error: (error: { name?: string; error?: { detail?: string } }) => {
        this.feedback.set(error.name === 'TimeoutError' ? 'La consulta tardó demasiado. Revisa que Django y PostgreSQL estén activos e inténtalo nuevamente.' : error.error?.detail ?? 'No fue posible consultar la agenda. Verifica el médico y la fecha.');
        this.isLoading.set(false);
        this.changeDetector.markForCheck();
      },
    });
  }

  patientName(appointment: Cita): string {
    const persona = appointment.paciente_detalle?.persona;
    return persona
      ? `${persona.primer_nombre} ${persona.primer_apellido}`
      : `Paciente #${appointment.paciente}`;
  }

  professionalNameFromAppointment(appointment: Cita): string {
    const persona = appointment.medico_detalle?.persona;
    return persona
      ? `${persona.primer_nombre} ${persona.primer_apellido}`
      : `Profesional #${appointment.medico}`;
  }

  previousPage(): void {
    if (this.currentPage > 1) this.currentPage -= 1;
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) this.currentPage += 1;
  }
}
