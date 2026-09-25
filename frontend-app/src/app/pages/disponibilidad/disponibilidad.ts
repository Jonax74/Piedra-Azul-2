import { ChangeDetectorRef, Component, inject } from '@angular/core';
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
  private readonly changeDetector = inject(ChangeDetectorRef);
  readonly weekdays = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes'];
  readonly weekdayValues = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES'];
  professionals: Medico[] = [];
  schedules: DisponibilidadModel[] = [];
  selectedProfessional = '';
  selectedDay = 'LUNES';
  startTime = '08:00';
  endTime = '17:00';
  interval = 30;
  isSaving = false;
  feedback = '';

  ngOnInit(): void {
    this.medicosService.getMedicos().subscribe({ next: (items) => { this.professionals = items.filter((item) => item.estado === 'ACTIVO'); this.changeDetector.markForCheck(); } });
    this.loadSchedules();
  }

  loadSchedules(): void {
    this.disponibilidadService.getDisponibilidades().subscribe({
      next: (items) => { this.schedules = items; this.changeDetector.markForCheck(); },
      error: () => { this.feedback = 'No fue posible cargar los horarios.'; this.changeDetector.markForCheck(); },
    });
  }

  saveSchedule(): void {
    if (!this.selectedProfessional) {
      this.feedback = 'Selecciona un profesional.';
      return;
    }
    this.isSaving = true;
    this.feedback = '';
    this.disponibilidadService.crearDisponibilidad({ dia_semana: this.selectedDay, hora_inicio: this.startTime, hora_fin: this.endTime, intervalo: this.interval }).subscribe({
      next: (schedule) => this.disponibilidadService.asociarConMedico({ medico: Number(this.selectedProfessional), disponibilidad: schedule.id! }).subscribe({
        next: () => { this.feedback = 'Horario guardado correctamente.'; this.isSaving = false; this.loadSchedules(); this.changeDetector.markForCheck(); },
        error: () => { this.feedback = 'El horario se creó, pero no pudo asociarse al profesional.'; this.isSaving = false; this.changeDetector.markForCheck(); },
      }),
      error: () => { this.feedback = 'No fue posible guardar el horario.'; this.isSaving = false; this.changeDetector.markForCheck(); },
    });
  }
}
