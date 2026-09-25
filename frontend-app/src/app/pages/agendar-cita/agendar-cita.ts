import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CitasService } from '../../core/services/citas.service';
import { MedicosService } from '../../core/services/medicos.service';
import { PersonasService, Paciente } from '../../core/services/personas.service';
import { AuthService } from '../../core/services/auth.service';
import { Medico } from '../../shared/models/medico.model';
import { Especialidad } from '../../shared/models/especialidad.model';
import { Persona } from '../../shared/models/persona.model';

@Component({
  imports: [FormsModule],
  selector: 'app-agendar-cita',
  styleUrl: './agendar-cita.scss',
  templateUrl: './agendar-cita.html',
})
export class AgendarCita {
  private readonly medicosService = inject(MedicosService);
  private readonly personasService = inject(PersonasService);
  private readonly citasService = inject(CitasService);
  private readonly authService = inject(AuthService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  specialties: Especialidad[] = [];
  professionals: Medico[] = [];
  people: Persona[] = [];
  patients: Paciente[] = [];
  slots: Array<{ fecha_hora: string; hora: string }> = [];
  selectedSpecialty = '';
  selectedProfessional = '';
  selectedDate = new Date().toISOString().slice(0, 10);
  selectedPatient = '';
  selectedSlot = '';
  isLoading = false;
  isSaving = false;
  feedback = '';
  showConfirmation = false;
  confirmationMessage = '';
  patientOnly = false;
  currentPersonaId: number | null = null;

  get currentStep(): number {
    if (this.showConfirmation) return 1;
    if (this.selectedSlot || this.slots.length) return 2;
    return 1;
  }

  ngOnInit(): void {
    this.feedback = 'Cargando pacientes, profesionales y especialidades...';
    this.medicosService.getMedicos().subscribe({ next: (items) => { this.professionals = items.filter((item) => item.estado === 'ACTIVO'); this.personasService.getPersonas().subscribe({ next: (people) => { this.people = people; this.changeDetector.markForCheck(); }, error: () => { this.feedback = 'No fue posible cargar los nombres de los profesionales.'; this.changeDetector.markForCheck(); } }); this.changeDetector.markForCheck(); }, error: () => { this.feedback = 'No fue posible cargar los profesionales.'; this.changeDetector.markForCheck(); } });
    this.medicosService.getEspecialidades().subscribe({ next: (items) => { this.specialties = items; this.changeDetector.markForCheck(); }, error: () => { this.feedback = 'No fue posible cargar las especialidades.'; this.changeDetector.markForCheck(); } });
    this.authService.getProfile().subscribe({
      next: (profile) => {
        const elevatedRoles = ['ADMIN', 'AGENDADOR', 'MEDICO'];
        this.patientOnly = profile.roles.includes('PACIENTE')
          && !profile.roles.some((role) => elevatedRoles.includes(role));
        this.currentPersonaId = profile.persona_id;
        this.loadPatients();
        this.changeDetector.markForCheck();
      },
      error: () => { this.feedback = 'No fue posible cargar tu perfil.'; this.changeDetector.markForCheck(); },
    });
  }

  private loadPatients(): void {
    this.personasService.getPacientes().subscribe({ next: (items) => {
      this.patients = items;
      const ownPatient = this.currentPersonaId
        ? items.find((item) => item.persona.id === this.currentPersonaId)
        : undefined;
      this.selectedPatient = this.patientOnly
        ? ownPatient?.persona.id?.toString() ?? ''
        : '';
      this.feedback = items.length ? 'Selecciona una fecha y consulta los horarios disponibles.' : 'No hay pacientes registrados para agendar.';
      this.changeDetector.markForCheck();
    }, error: () => { this.feedback = 'No fue posible cargar los pacientes.'; this.changeDetector.markForCheck(); } });
  }

  patientName(): string {
    const patient = this.patients.find(
      (item) => item.persona.id?.toString() === this.selectedPatient,
    );
    return patient
      ? `${patient.persona.primer_nombre} ${patient.persona.primer_apellido}`
      : 'Paciente no vinculado';
  }

  get filteredProfessionals(): Medico[] {
    if (!this.selectedSpecialty) return this.professionals;
    return this.professionals.filter((professional) => professional.especialidades?.some((specialty) => specialty.id?.toString() === this.selectedSpecialty));
  }

  professionalName(professional?: Medico): string {
    if (!professional) return 'el profesional seleccionado';
    const person = this.people.find((item) => item.id === professional.persona);
    return person ? `${person.primer_nombre} ${person.primer_apellido}` : 'Profesional';
  }

  loadSlots(): void {
    if (!this.selectedProfessional || !this.selectedDate) {
      this.feedback = 'Selecciona un profesional y una fecha.';
      return;
    }
    this.isLoading = true;
    this.feedback = '';
    this.selectedSlot = '';
    this.citasService.getFranjasDisponibles(Number(this.selectedProfessional), this.selectedDate).subscribe({
      next: (response) => { this.slots = response.franjas; this.isLoading = false; this.feedback = response.franjas.length ? `Hay ${response.franjas.length} horarios disponibles para el ${this.selectedDate}.` : `No hay horarios disponibles para el ${this.selectedDate}.`; this.changeDetector.markForCheck(); },
      error: (error: { name?: string; error?: { detail?: string } }) => { this.feedback = error.name === 'TimeoutError' ? 'La consulta de horarios tardó demasiado. Verifica que Django y PostgreSQL estén activos.' : error.error?.detail ?? 'No fue posible consultar los horarios. Verifica el profesional y la fecha.'; this.isLoading = false; this.changeDetector.markForCheck(); },
    });
  }

  book(): void {
    if (!this.selectedPatient || !this.selectedProfessional || !this.selectedSlot) {
      this.feedback = 'Completa paciente, profesional y horario.';
      return;
    }
    this.isSaving = true;
    this.citasService.crearCita({ paciente: Number(this.selectedPatient), medico: Number(this.selectedProfessional), fecha_hora: this.selectedSlot, estado: 'PROGRAMADA' }).subscribe({
      next: () => {
        this.confirmationMessage = `Tu cita con ${this.professionalName(this.professionals.find((item) => item.persona.toString() === this.selectedProfessional)!) } quedó agendada para el ${this.selectedDate} a las ${this.slots.find((slot) => slot.fecha_hora === this.selectedSlot)?.hora ?? 'la hora seleccionada'}.`;
        this.resetForm();
        this.showConfirmation = true;
        this.feedback = '';
        this.isSaving = false;
        this.changeDetector.markForCheck();
      },
      error: (error: { error?: { detail?: string; non_field_errors?: string[] } }) => { this.feedback = error.error?.detail ?? error.error?.non_field_errors?.[0] ?? 'No fue posible agendar la cita. El horario puede haberse ocupado o la fecha no ser válida.'; this.isSaving = false; this.changeDetector.markForCheck(); },
    });
  }

  closeConfirmation(): void {
    this.showConfirmation = false;
    this.changeDetector.markForCheck();
  }

  private resetForm(): void {
    this.selectedSpecialty = '';
    this.selectedProfessional = '';
    this.selectedPatient = this.patientOnly
      ? this.currentPersonaId?.toString() ?? ''
      : '';
    this.selectedDate = new Date().toISOString().slice(0, 10);
    this.selectedSlot = '';
    this.slots = [];
  }
}
