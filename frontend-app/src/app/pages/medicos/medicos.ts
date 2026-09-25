import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MedicosService } from '../../core/services/medicos.service';
import { PersonasService } from '../../core/services/personas.service';
import { Persona } from '../../shared/models/persona.model';
import { Medico } from '../../shared/models/medico.model';

@Component({
  imports: [FormsModule],
  selector: 'app-medicos',
  styleUrl: './medicos.scss',
  templateUrl: './medicos.html',
})
export class Medicos {
  private readonly medicosService = inject(MedicosService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly personasService = inject(PersonasService);
  professionals: Medico[] = [];
  people: Persona[] = [];
  searchTerm = '';
  feedback = '';

  ngOnInit(): void {
    this.medicosService.getMedicos().subscribe({
      next: (items) => {
        this.professionals = items;
        this.changeDetector.markForCheck();
        this.personasService.getPersonas().subscribe({
          next: (people) => { this.people = people; this.changeDetector.markForCheck(); },
          error: () => { this.feedback = 'Los profesionales cargaron, pero no se pudieron cargar sus nombres.'; this.changeDetector.markForCheck(); },
        });
      },
      error: () => { this.feedback = 'No fue posible cargar los profesionales.'; this.changeDetector.markForCheck(); },
    });
  }

  get filteredProfessionals(): Medico[] {
    const term = this.searchTerm.trim().toLowerCase();
    return term ? this.professionals.filter((item) => `${this.professionalName(item)} ${item.tipo_profesional}`.toLowerCase().includes(term)) : this.professionals;
  }

  professionalName(professional: Medico): string {
    const person = this.people.find((item) => item.id === professional.persona);
    return person
      ? `${person.primer_nombre} ${person.primer_apellido}`
      : `Profesional #${professional.persona}`;
  }
}
