import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
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
  professionals: Medico[] = [];
  people: Persona[] = [];
  appointments: Cita[] = [];
  selectedProfessional = '';
  selectedDate = new Date().toISOString().slice(0, 10);
  isLoading = false;
  feedback = '';

  ngOnInit(): void {
    this.feedback = 'Cargando profesionales...';
    this.medicosService.getMedicos().subscribe({
      next: (medicos) => {
        this.professionals = medicos.filter((medico) => medico.estado === 'ACTIVO');
        this.personasService.getPersonas().subscribe({
          next: (people) => { this.people = people; this.feedback = 'Selecciona un profesional y una fecha para consultar la agenda.'; },
          error: () => this.feedback = 'Los profesionales cargaron, pero no se pudieron cargar sus nombres.',
        });
      },
      error: () => this.feedback = 'No fue posible cargar los profesionales.',
    });
  }

  professionalName(professional: Medico): string {
    const person = this.people.find((item) => item.id === professional.persona);
    return person ? `${person.primer_nombre} ${person.primer_apellido}` : 'Profesional';
  }

  search(): void {
    if (!this.selectedProfessional || !this.selectedDate) {
      this.feedback = 'Selecciona un profesional y una fecha.';
      return;
    }

    this.isLoading = true;
    this.feedback = '';
    this.citasService.getAgenda(Number(this.selectedProfessional), this.selectedDate).subscribe({
      next: (response) => {
        this.appointments = response.resultados;
        this.feedback = response.cantidad ? `Se encontraron ${response.cantidad} citas para el ${this.selectedDate}.` : `No hay citas registradas para el ${this.selectedDate}.`;
        this.isLoading = false;
      },
      error: (error: { name?: string; error?: { detail?: string } }) => {
        this.feedback = error.name === 'TimeoutError' ? 'La consulta tardó demasiado. Revisa que Django y PostgreSQL estén activos e inténtalo nuevamente.' : error.error?.detail ?? 'No fue posible consultar la agenda. Verifica el médico y la fecha.';
        this.isLoading = false;
      },
    });
  }
}
