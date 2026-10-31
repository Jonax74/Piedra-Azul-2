import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DisponibilidadService } from '../../core/services/disponibilidad.service';
import { MedicosService } from '../../core/services/medicos.service';
import { PersonasService } from '../../core/services/personas.service';
import { Persona } from '../../shared/models/persona.model';
import { Medico } from '../../shared/models/medico.model';
import { Disponibilidad as DisponibilidadModel } from '../../shared/models/disponibilidad.model';

@Component({
  imports: [FormsModule, RouterLink],
  selector: 'app-medicos',
  styleUrl: './medicos.scss',
  templateUrl: './medicos.html',
})
export class Medicos {
  private readonly medicosService = inject(MedicosService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly personasService = inject(PersonasService);
  private readonly disponibilidadService = inject(DisponibilidadService);
  professionals: Medico[] = [];
  people: Persona[] = [];
  schedules: DisponibilidadModel[] = [];
  relations: Array<{ medico: number; disponibilidad: number }> = [];
  searchTerm = '';
  feedback = '';
  isCreating = false;
  isSaving = false;
  professionalForm = {
    primer_nombre: '',
    segundo_nombre: '',
    primer_apellido: '',
    segundo_apellido: '',
    genero: 'MUJER',
    fecha_nacimiento: '',
    telefono: '',
    dni: '',
    correo: '',
    tipo_profesional: 'MEDICO',
  };

  ngOnInit(): void {
    this.loadProfessionals();
    this.personasService.getPersonas().subscribe({
      next: (people) => { this.people = people; this.changeDetector.markForCheck(); },
      error: () => { this.feedback = 'No se pudieron cargar los nombres de las personas.'; this.changeDetector.markForCheck(); },
    });
    this.disponibilidadService.getDisponibilidades().subscribe({
      next: (items) => { this.schedules = items; this.changeDetector.markForCheck(); },
      error: () => { this.feedback = 'No fue posible cargar las disponibilidades.'; this.changeDetector.markForCheck(); },
    });
    this.disponibilidadService.getMedicoDisponibilidades().subscribe({
      next: (items) => { this.relations = items; this.changeDetector.markForCheck(); },
      error: () => { this.feedback = 'No fue posible cargar las relaciones entre profesionales y horarios.'; this.changeDetector.markForCheck(); },
    });
  }

  private loadProfessionals(): void {
    this.medicosService.getMedicos().subscribe({
      next: (items) => { this.professionals = items; this.changeDetector.markForCheck(); },
      error: () => { this.feedback = 'No fue posible cargar los profesionales. Verifica tu sesión y la conexión con el backend.'; this.changeDetector.markForCheck(); },
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

  professionalSchedules(professional: Medico): DisponibilidadModel[] {
    const scheduleIds = new Set(
      this.relations
        .filter((relation) => relation.medico === professional.persona)
        .map((relation) => relation.disponibilidad),
    );
    return this.schedules.filter((schedule) => schedule.id !== undefined && scheduleIds.has(schedule.id));
  }

  saveProfessional(): void {
    this.isSaving = true;
    this.feedback = '';
    this.medicosService.crearMedico({
      tipo_profesional: this.professionalForm.tipo_profesional,
      estado: 'ACTIVO',
      persona_data: {
        primer_nombre: this.professionalForm.primer_nombre.trim(),
        segundo_nombre: this.professionalForm.segundo_nombre.trim() || null,
        primer_apellido: this.professionalForm.primer_apellido.trim(),
        segundo_apellido: this.professionalForm.segundo_apellido.trim() || null,
        genero: this.professionalForm.genero,
        fecha_nacimiento: this.professionalForm.fecha_nacimiento,
        telefono: this.professionalForm.telefono.trim(),
        dni: Number(this.professionalForm.dni),
        correo: this.professionalForm.correo.trim() || null,
      },
    }).subscribe({
      next: () => {
        this.isSaving = false;
        this.isCreating = false;
        this.professionalForm = {
          primer_nombre: '',
          segundo_nombre: '',
          primer_apellido: '',
          segundo_apellido: '',
          genero: 'MUJER',
          fecha_nacimiento: '',
          telefono: '',
          dni: '',
          correo: '',
          tipo_profesional: 'MEDICO',
        };
        this.feedback = 'Profesional registrado correctamente. Ahora puedes configurar su disponibilidad.';
        this.loadProfessionals();
        this.personasService.getPersonas().subscribe({
          next: (people) => { this.people = people; this.changeDetector.markForCheck(); },
          error: () => { this.feedback = 'El profesional se registró, pero no se pudo actualizar su nombre en el directorio.'; this.changeDetector.markForCheck(); },
        });
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.isSaving = false;
        this.feedback = 'No fue posible registrar al profesional. Revisa que el documento no esté ya registrado.';
        this.changeDetector.markForCheck();
      },
    });
  }
}
