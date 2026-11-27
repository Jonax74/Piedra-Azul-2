import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { timeout } from 'rxjs';
import { DisponibilidadService } from '../../core/services/disponibilidad.service';
import { MedicosService } from '../../core/services/medicos.service';
import { PersonasService } from '../../core/services/personas.service';
import { Persona } from '../../shared/models/persona.model';
import { Medico } from '../../shared/models/medico.model';
import { Disponibilidad as DisponibilidadModel } from '../../shared/models/disponibilidad.model';
import { Especialidad } from '../../shared/models/especialidad.model';

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
  specialties: Especialidad[] = [];
  relations: Array<{ medico: number; disponibilidad: number }> = [];
  specialtyLoadError = '';
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
    especialidad_id: '',
    username: '',
    password: '',
    password_confirmation: '',
  };

  ngOnInit(): void {
    this.loadProfessionals();
    this.medicosService.getEspecialidades().subscribe({
      next: (items) => { this.specialties = items; this.changeDetector.markForCheck(); },
      error: () => {
        this.specialtyLoadError = 'No fue posible cargar las opciones de especialidad.';
        this.changeDetector.markForCheck();
      },
    });
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
      especialidad_ids: this.professionalForm.especialidad_id
        ? [Number(this.professionalForm.especialidad_id)]
        : [],
      persona_data: {
        primer_nombre: this.professionalForm.primer_nombre.trim(),
        segundo_nombre: this.professionalForm.segundo_nombre.trim() || null,
        primer_apellido: this.professionalForm.primer_apellido.trim(),
        segundo_apellido: this.professionalForm.segundo_apellido.trim() || null,
        genero: this.professionalForm.genero,
        fecha_nacimiento: this.professionalForm.fecha_nacimiento,
        telefono: this.professionalForm.telefono.trim(),
        dni: Number(this.professionalForm.dni),
        correo: this.professionalForm.correo.trim(),
      },
      username: this.professionalForm.username.trim(),
      password: this.professionalForm.password,
      password_confirmation: this.professionalForm.password_confirmation,
    }).pipe(timeout({ first: 45000 })).subscribe({
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
          especialidad_id: '',
          username: '',
          password: '',
          password_confirmation: '',
        };
        this.feedback = 'Profesional y cuenta de acceso registrados correctamente. Ahora puedes configurar su disponibilidad.';
        this.loadProfessionals();
        this.personasService.getPersonas().subscribe({
          next: (people) => { this.people = people; this.changeDetector.markForCheck(); },
          error: () => { this.feedback = 'El profesional se registró, pero no se pudo actualizar su nombre en el directorio.'; this.changeDetector.markForCheck(); },
        });
        this.changeDetector.markForCheck();
      },
      error: (error: unknown) => {
        this.isSaving = false;
        this.feedback = this.registrationError(error);
        this.changeDetector.markForCheck();
      },
    });
  }

  private registrationError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const messages = this.collectErrorMessages(error.error);
      if (messages.length) return messages[0];
      if (error.status === 0) {
        return 'No se pudo conectar con el servidor. Verifica que el backend y Keycloak estén activos.';
      }
      if (error.status >= 500) {
        return 'No fue posible crear el profesional y su cuenta. Inténtalo nuevamente más tarde.';
      }
    }

    if (error instanceof Error && error.name === 'TimeoutError') {
      return 'El registro tardó demasiado. Verifica que el backend y Keycloak estén activos antes de volver a intentarlo.';
    }

    return 'No fue posible registrar al profesional. Revisa los datos ingresados e inténtalo nuevamente.';
  }

  private collectErrorMessages(value: unknown): string[] {
    if (typeof value === 'string') return [value];
    if (Array.isArray(value)) return value.flatMap((item) => this.collectErrorMessages(item));
    if (value && typeof value === 'object') {
      return Object.values(value).flatMap((item) => this.collectErrorMessages(item));
    }
    return [];
  }
}
