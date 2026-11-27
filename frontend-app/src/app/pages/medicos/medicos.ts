import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
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
  submitted = false;
  readonly maxBirthDate = this.today();
  private serverFieldErrors: Record<string, string> = {};
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

  toggleCreationForm(): void {
    this.isCreating = !this.isCreating;
    this.submitted = false;
    this.serverFieldErrors = {};
    this.feedback = '';
  }

  saveProfessional(form: NgForm): void {
    if (this.isSaving) return;

    this.submitted = true;
    this.serverFieldErrors = {};
    form.control.markAllAsTouched();
    const requiredTextFields = [
      this.professionalForm.primer_nombre,
      this.professionalForm.primer_apellido,
      this.professionalForm.fecha_nacimiento,
      this.professionalForm.telefono,
      this.professionalForm.dni,
      this.professionalForm.correo,
      this.professionalForm.username,
      this.professionalForm.password,
      this.professionalForm.password_confirmation,
    ];
    if (
      form.invalid
      || requiredTextFields.some((value) => !value.trim())
      || !/\d/.test(this.professionalForm.telefono)
      || !Number.isInteger(Number(this.professionalForm.dni))
      || Number(this.professionalForm.dni) < 1
      || Number(this.professionalForm.dni) > 2147483647
      || this.professionalForm.password !== this.professionalForm.password_confirmation
    ) {
      this.feedback = 'Revisa los campos marcados antes de registrar.';
      this.changeDetector.markForCheck();
      return;
    }

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
        this.submitted = false;
        this.serverFieldErrors = {};
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
        this.serverFieldErrors = this.fieldErrorsFrom(error);
        this.feedback = this.registrationError(error);
        this.changeDetector.markForCheck();
      },
    });
  }

  fieldError(form: NgForm, fieldName: string): string {
    const control = form.controls[fieldName];
    if (!this.submitted && !control?.touched) return '';
    if (this.serverFieldErrors[fieldName]) return this.serverFieldErrors[fieldName];
    if (
      [
        'primer_nombre',
        'primer_apellido',
        'fecha_nacimiento',
        'telefono',
        'dni',
        'correo',
        'username',
        'password',
        'password_confirmation',
      ].includes(fieldName)
      && typeof control?.value === 'string'
      && !control.value.trim()
    ) {
      return 'Este campo es obligatorio.';
    }

    if (
      fieldName === 'password_confirmation'
      && this.professionalForm.password_confirmation
      && this.professionalForm.password !== this.professionalForm.password_confirmation
    ) {
      return 'Las contraseñas no coinciden.';
    }
    if (
      fieldName === 'dni'
      && control?.value
      && (!Number.isInteger(Number(control.value)) || Number(control.value) > 2147483647)
    ) {
      return 'Ingresa un documento entero válido.';
    }
    if (fieldName === 'telefono' && control?.value && !/\d/.test(control.value)) {
      return 'El teléfono debe contener al menos un número.';
    }

    const errors = control?.errors;
    if (!errors) return '';
    if (errors['required']) return 'Este campo es obligatorio.';
    if (errors['email']) return 'Ingresa un correo electrónico válido.';
    if (errors['minlength']) {
      const minimum = errors['minlength'].requiredLength;
      if (fieldName === 'password' || fieldName === 'password_confirmation') {
        return `La contraseña debe tener al menos ${minimum} caracteres.`;
      }
      if (fieldName === 'username') return `El usuario debe tener al menos ${minimum} caracteres.`;
      if (fieldName === 'telefono') return `El teléfono debe tener al menos ${minimum} caracteres.`;
    }
    if (errors['maxlength']) {
      return `No puede superar ${errors['maxlength'].requiredLength} caracteres.`;
    }
    if (errors['pattern']) {
      if (fieldName === 'username') return 'Usa solo letras, números, puntos, guiones y guiones bajos.';
      if (fieldName === 'telefono') return 'Usa de 7 a 15 números, espacios, +, paréntesis o guiones.';
      if (fieldName === 'dni') return 'El documento solo puede contener números.';
    }
    if (errors['max']) {
      return 'La fecha de nacimiento no puede ser futura.';
    }

    return 'Revisa este campo.';
  }

  clearServerError(fieldName: string): void {
    const hadError = Boolean(this.serverFieldErrors[fieldName]);
    delete this.serverFieldErrors[fieldName];
    if (hadError && Object.keys(this.serverFieldErrors).length === 0) {
      this.feedback = '';
    }
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

  private fieldErrorsFrom(error: unknown): Record<string, string> {
    if (!(error instanceof HttpErrorResponse) || !error.error || typeof error.error !== 'object') {
      return {};
    }

    const fieldErrors: Record<string, string> = {};
    for (const [field, value] of Object.entries(error.error as Record<string, unknown>)) {
      if (field === 'persona_data' && value && typeof value === 'object') {
        for (const [personField, personError] of Object.entries(value)) {
          const messages = this.collectErrorMessages(personError);
          if (messages.length) fieldErrors[personField] = messages[0];
        }
      } else {
        const messages = this.collectErrorMessages(value);
        if (messages.length) fieldErrors[field] = messages[0];
      }
    }
    return fieldErrors;
  }

  private collectErrorMessages(value: unknown): string[] {
    if (typeof value === 'string') return [value];
    if (Array.isArray(value)) return value.flatMap((item) => this.collectErrorMessages(item));
    if (value && typeof value === 'object') {
      return Object.values(value).flatMap((item) => this.collectErrorMessages(item));
    }
    return [];
  }

  private today(): string {
    const today = new Date();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${today.getFullYear()}-${month}-${day}`;
  }
}
