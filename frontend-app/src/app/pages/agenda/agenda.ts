import { DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
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
  private readonly changeDetector = inject(ChangeDetectorRef);
  professionals: Medico[] = [];
  people: Persona[] = [];
  appointments: Cita[] = [];
  selectedProfessional = '';
  selectedDate = '';
  isLoading = false;
  feedback = '';

  ngOnInit(): void {
    this.feedback = 'Cargando profesionales...';
    this.medicosService.getMedicos().subscribe({
      next: (medicos) => {
        this.professionals = medicos.filter((medico) => medico.estado === 'ACTIVO');
        this.changeDetector.markForCheck();
        this.personasService.getPersonas().subscribe({
          next: (people) => { this.people = people; this.feedback = 'Puedes consultar todas las citas o aplicar uno o ambos filtros.'; this.changeDetector.markForCheck(); },
          error: () => { this.feedback = 'Los profesionales cargaron, pero no se pudieron cargar sus nombres.'; this.changeDetector.markForCheck(); },
        });
      },
      error: () => { this.feedback = 'No fue posible cargar los profesionales.'; this.changeDetector.markForCheck(); },
    });
    this.search();
  }

  professionalName(professional: Medico): string {
    const person = this.people.find((item) => item.id === professional.persona);
    return person ? `${person.primer_nombre} ${person.primer_apellido}` : 'Profesional';
  }

  search(): void {
    this.isLoading = true;
    this.feedback = '';
    const medicoId = this.selectedProfessional
      ? Number(this.selectedProfessional)
      : undefined;
    const fecha = this.selectedDate || undefined;

    this.citasService.getAgenda(medicoId, fecha).subscribe({
      next: (response) => {
        this.appointments = response.resultados;
        const filtro = [
          this.selectedDate ? `el ${this.selectedDate}` : 'todas las fechas',
          this.selectedProfessional ? 'el profesional seleccionado' : 'todos los profesionales',
        ].join(' y ');
        this.feedback = response.cantidad
          ? `Se encontraron ${response.cantidad} citas para ${filtro}.`
          : `No hay citas registradas para ${filtro}.`;
        this.isLoading = false;
        this.changeDetector.markForCheck();
      },
      error: (error: { name?: string; error?: { detail?: string } }) => {
        this.feedback = error.name === 'TimeoutError' ? 'La consulta tardó demasiado. Revisa que Django y PostgreSQL estén activos e inténtalo nuevamente.' : error.error?.detail ?? 'No fue posible consultar la agenda. Verifica el médico y la fecha.';
        this.isLoading = false;
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
}
