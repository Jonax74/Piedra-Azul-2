import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MedicosService } from '../../core/services/medicos.service';
import { Medico } from '../../shared/models/medico.model';

@Component({
  imports: [FormsModule],
  selector: 'app-medicos',
  styleUrl: './medicos.scss',
  templateUrl: './medicos.html',
})
export class Medicos {
  private readonly medicosService = inject(MedicosService);
  professionals: Medico[] = [];
  searchTerm = '';
  feedback = '';

  ngOnInit(): void {
    this.medicosService.getMedicos().subscribe({
      next: (items) => this.professionals = items,
      error: () => this.feedback = 'No fue posible cargar los profesionales.',
    });
  }

  get filteredProfessionals(): Medico[] {
    const term = this.searchTerm.trim().toLowerCase();
    return term ? this.professionals.filter((item) => `${item.tipo_profesional} ${item.persona}`.toLowerCase().includes(term)) : this.professionals;
  }
}
