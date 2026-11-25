import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CitasService } from '../../../core/services/citas.service';
import { ConfiguracionService } from '../../../core/services/configuracion.service';
import { Auth } from '../../../core/services/auth';
import { Cita } from '../../models/cita.model';

type AppointmentStatus = 'CONFIRMADA' | 'CANCELADA';

@Component({
  imports: [FormsModule],
  selector: 'app-appointment-actions',
  styleUrl: './appointment-actions.scss',
  templateUrl: './appointment-actions.html',
})
export class AppointmentActions {
  private readonly citasService = inject(CitasService);
  private readonly configuracionService = inject(ConfiguracionService);
  private readonly auth = inject(Auth);
  private readonly changeDetector = inject(ChangeDetectorRef);

  @Input({ required: true }) cita!: Cita;
  @Output() readonly citaActualizada = new EventEmitter<Cita>();

  pendingStatus: AppointmentStatus | null = null;
  cancellationReason = '';
  cancellationBlocked = false;
  isRescheduleOpen = false;
  isLoadingDates = false;
  isLoadingSlots = false;
  isSaving = false;
  selectedDate = '';
  selectedSlot = '';
  feedback = '';
  calendarError = '';
  bookingWeeks = 4;
  maxBookingDate = this.addDays(this.todayIso(), this.bookingWeeks * 7);
  calendarMonth = this.dateFromIso(this.todayIso());
  availableDates = new Set<string>();
  slots: Array<{ fecha_hora: string; hora: string }> = [];

  get canEdit(): boolean {
    return ['PROGRAMADA', 'CONFIRMADA', 'REAGENDADA'].includes(this.cita.estado);
  }

  get requiresCancellationReason(): boolean {
    return this.pendingStatus === 'CANCELADA'
      && this.isPatient()
      && !this.cancellationBlocked;
  }

  openStatusConfirmation(status: AppointmentStatus): void {
    this.cancellationBlocked = status === 'CANCELADA'
      && this.isPatient()
      && Date.parse(this.cita.fecha_hora) - Date.now() < 48 * 60 * 60 * 1000;
    this.pendingStatus = status;
    this.cancellationReason = '';
    this.feedback = '';
  }

  cancellationDeadlineLabel(): string {
    const deadline = new Date(
      Date.parse(this.cita.fecha_hora) - 48 * 60 * 60 * 1000,
    );
    return deadline.toLocaleString('es-CO', {
      dateStyle: 'long',
      timeStyle: 'short',
    });
  }

  openReschedule(): void {
    this.selectedDate = '';
    this.selectedSlot = '';
    this.slots = [];
    this.feedback = '';
    this.calendarError = '';
    this.isLoadingDates = true;
    this.calendarMonth = this.dateFromIso(this.todayIso());
    this.loadAvailableDates();
    this.configuracionService.getConfiguracion().subscribe({
      next: (configuracion) => {
        this.bookingWeeks = configuracion.semanas_agendamiento;
        this.maxBookingDate = this.addDays(this.todayIso(), this.bookingWeeks * 7);
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.calendarError = 'No fue posible consultar el límite de fechas para reagendar.';
        this.changeDetector.markForCheck();
      },
    });
    this.isRescheduleOpen = true;
  }

  calendarMonthLabel(): string {
    return this.calendarMonth.toLocaleDateString('es-CO', {
      month: 'long',
      year: 'numeric',
    });
  }

  calendarCells(): Array<{ iso: string; number: number } | null> {
    const year = this.calendarMonth.getFullYear();
    const month = this.calendarMonth.getMonth();
    const firstWeekday = (new Date(year, month, 1).getDay() + 6) % 7;
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const cells: Array<{ iso: string; number: number } | null> = Array(firstWeekday).fill(null);

    for (let day = 1; day <= daysInMonth; day += 1) {
      const iso = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      cells.push({ iso, number: day });
    }

    while (cells.length % 7 !== 0) cells.push(null);
    return cells;
  }

  canPreviousMonth(): boolean {
    const today = this.dateFromIso(this.todayIso());
    return this.calendarMonth.getFullYear() > today.getFullYear()
      || (this.calendarMonth.getFullYear() === today.getFullYear()
        && this.calendarMonth.getMonth() > today.getMonth());
  }

  canNextMonth(): boolean {
    const maxDate = this.dateFromIso(this.maxBookingDate);
    return this.calendarMonth.getFullYear() < maxDate.getFullYear()
      || (this.calendarMonth.getFullYear() === maxDate.getFullYear()
        && this.calendarMonth.getMonth() < maxDate.getMonth());
  }

  changeCalendarMonth(offset: number): void {
    if (offset < 0 && !this.canPreviousMonth()) return;
    if (offset > 0 && !this.canNextMonth()) return;

    this.calendarMonth = new Date(
      this.calendarMonth.getFullYear(),
      this.calendarMonth.getMonth() + offset,
      1,
    );
    this.selectedDate = '';
    this.selectedSlot = '';
    this.slots = [];
    this.loadAvailableDates();
  }

  isDateAvailable(date: string): boolean {
    return this.availableDates.has(date);
  }

  selectDate(date: string): void {
    if (!this.isDateAvailable(date) || this.isLoadingDates || this.isLoadingSlots) return;
    this.selectedDate = date;
    this.selectedSlot = '';
    this.slots = [];
    this.loadSlots();
  }

  closeDialog(): void {
    if (this.isSaving) return;
    this.pendingStatus = null;
    this.cancellationBlocked = false;
    this.isRescheduleOpen = false;
    this.cancellationReason = '';
    this.feedback = '';
  }

  loadSlots(): void {
    this.feedback = '';
    if (!this.selectedDate) return;

    this.isLoadingSlots = true;
    this.citasService.getFranjasDisponibles(this.cita.medico, this.selectedDate).subscribe({
      next: (response) => {
        this.slots = response.franjas.filter(
          (slot) => slot.fecha_hora !== this.cita.fecha_hora,
        );
        this.isLoadingSlots = false;
        if (!this.slots.length) {
          this.feedback = 'Ya no quedan horarios disponibles en esta fecha. Selecciona otro día.';
          this.availableDates.delete(this.selectedDate);
        }
        this.changeDetector.markForCheck();
      },
      error: (error: unknown) => {
        this.feedback = this.errorMessage(
          error,
          'No fue posible consultar los horarios disponibles.',
        );
        this.isLoadingSlots = false;
        this.changeDetector.markForCheck();
      },
    });
  }

  private loadAvailableDates(): void {
    const month = this.currentCalendarMonth();
    this.isLoadingDates = true;
    this.calendarError = '';
    this.availableDates = new Set();
    this.citasService.getFechasDisponibles(this.cita.medico, month).subscribe({
      next: (response) => {
        if (month !== this.currentCalendarMonth()) return;
        this.availableDates = new Set(response.fechas);
        this.isLoadingDates = false;
        this.changeDetector.markForCheck();
      },
      error: (error: unknown) => {
        if (month !== this.currentCalendarMonth()) return;
        this.calendarError = this.errorMessage(
          error,
          'No fue posible cargar las fechas con horarios disponibles.',
        );
        this.isLoadingDates = false;
        this.changeDetector.markForCheck();
      },
    });
  }

  confirmStatusChange(): void {
    if (!this.pendingStatus || this.cancellationBlocked || this.cita.id === undefined) return;
    const status = this.pendingStatus;
    const reason = this.cancellationReason.trim();
    if (status === 'CANCELADA' && this.isPatient() && !reason) {
      this.feedback = 'Escribe el motivo de la cancelación para continuar.';
      return;
    }

    this.isSaving = true;
    this.feedback = '';

    const payload: Pick<Cita, 'estado'>
      & Partial<Pick<Cita, 'motivo_cancelacion'>> = { estado: status };
    if (status === 'CANCELADA' && reason) {
      payload.motivo_cancelacion = reason;
    }

    this.citasService.actualizarEstadoCita(this.cita.id, payload).subscribe({
      next: (updated) => {
        this.citaActualizada.emit(updated);
        this.isSaving = false;
        this.pendingStatus = null;
        this.cancellationReason = '';
      },
      error: (error: unknown) => {
        this.feedback = this.errorMessage(
          error,
          'No fue posible actualizar el estado de la cita.',
        );
        this.isSaving = false;
      },
    });
  }

  reschedule(): void {
    if (this.cita.id === undefined || !this.selectedSlot) return;
    this.isSaving = true;
    this.feedback = '';

    this.citasService.actualizarEstadoCita(this.cita.id, {
      estado: 'REAGENDADA',
      fecha_hora: this.selectedSlot,
    }).subscribe({
      next: (updated) => {
        this.citaActualizada.emit(updated);
        this.isSaving = false;
        this.isRescheduleOpen = false;
      },
      error: (error: unknown) => {
        this.feedback = this.errorMessage(
          error,
          'No fue posible reagendar la cita. Elige otro horario.',
        );
        this.isSaving = false;
      },
    });
  }

  private errorMessage(error: unknown, fallback: string): string {
    const response = error instanceof HttpErrorResponse ? error.error : error;
    if (typeof response === 'string') return response;
    if (!response || typeof response !== 'object') return fallback;

    const details = response as Record<string, unknown>;
    for (const key of [
      'detail',
      'estado',
      'fecha_hora',
      'motivo_cancelacion',
      'non_field_errors',
    ]) {
      const message = this.flattenMessage(details[key]);
      if (message) return message;
    }

    return fallback;
  }

  private flattenMessage(value: unknown): string {
    if (typeof value === 'string') return value;
    if (Array.isArray(value)) {
      return value.map((item) => this.flattenMessage(item)).filter(Boolean).join(' ');
    }
    return '';
  }

  private isPatient(): boolean {
    const roles = this.auth.roles();
    return roles.includes('PACIENTE')
      && !roles.some((role) => ['ADMIN', 'AGENDADOR', 'MEDICO'].includes(role));
  }

  private currentCalendarMonth(): string {
    return `${this.calendarMonth.getFullYear()}-${String(this.calendarMonth.getMonth() + 1).padStart(2, '0')}`;
  }

  private todayIso(): string {
    const today = new Date();
    return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
  }

  private addDays(date: string, days: number): string {
    const result = this.dateFromIso(date);
    result.setDate(result.getDate() + days);
    return this.toIso(result);
  }

  private dateFromIso(date: string): Date {
    return new Date(`${date}T12:00:00`);
  }

  private toIso(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
}
