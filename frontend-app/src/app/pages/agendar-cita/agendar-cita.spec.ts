import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AgendarCita } from './agendar-cita';
import { CitasService } from '../../core/services/citas.service';
import { MedicosService } from '../../core/services/medicos.service';
import { PersonasService } from '../../core/services/personas.service';
import { AuthService } from '../../core/services/auth.service';
import { ConfiguracionService } from '../../core/services/configuracion.service';
import { DisponibilidadService } from '../../core/services/disponibilidad.service';

describe('AgendarCita', () => {
  let component: AgendarCita;
  let fixture: ComponentFixture<AgendarCita>;
  let citasService: {
    getFranjasDisponibles: ReturnType<typeof vi.fn>;
    crearCita: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    citasService = {
      getFranjasDisponibles: vi.fn().mockReturnValue(of({ franjas: [] })),
      crearCita: vi.fn().mockReturnValue(of({})),
    };
    await TestBed.configureTestingModule({
      imports: [AgendarCita],
      providers: [
        { provide: CitasService, useValue: citasService },
        {
          provide: MedicosService,
          useValue: {
            getMedicos: () => of([{ persona: 8, estado: 'ACTIVO', especialidades: [] }]),
            getEspecialidades: () => of([]),
          },
        },
        {
          provide: PersonasService,
          useValue: {
            getPersonas: () => of([{ id: 8, primer_nombre: 'Carlos', primer_apellido: 'Médico' }]),
            getPacientes: () => of([
              { persona: { id: 10, primer_nombre: 'Ana', primer_apellido: 'Paciente' } },
              { persona: { id: 11, primer_nombre: 'Otra', primer_apellido: 'Persona' } },
            ]),
          },
        },
        { provide: AuthService, useValue: { getProfile: () => of({ roles: ['PACIENTE'], persona_id: 10 }) } },
        {
          provide: ConfiguracionService,
          useValue: { getConfiguracion: () => of({ semanas_agendamiento: 4, activo: true }) },
        },
        {
          provide: DisponibilidadService,
          useValue: {
            getDisponibilidades: () => of([{ id: 1, dia_semana: 'LUNES' }]),
            getMedicoDisponibilidades: () => of([{ medico: 8, disponibilidad: 1 }]),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AgendarCita);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('limits a patient to their own linked patient record', () => {
    expect(component.patientOnly).toBe(true);
    expect(component.selectedPatient).toBe('10');
    expect(component.patientName()).toBe('Ana Paciente');
  });

  it('enables calendar dates only on configured weekdays within the booking window', () => {
    component.selectedProfessional = '8';
    const nextMonday = new Date();
    nextMonday.setDate(nextMonday.getDate() + 1);
    while (nextMonday.getDay() !== 1) {
      nextMonday.setDate(nextMonday.getDate() + 1);
    }
    const date = [
      nextMonday.getFullYear(),
      String(nextMonday.getMonth() + 1).padStart(2, '0'),
      String(nextMonday.getDate()).padStart(2, '0'),
    ].join('-');

    expect(component.isDateSelectable(date)).toBe(true);
    expect(component.isDateSelectable('2000-01-01')).toBe(false);
  });

  it('does not request slots until both date and professional are selected', () => {
    component.selectedDate = '';

    component.loadSlots();

    expect(component.fieldErrors.professional).toBe('Selecciona un profesional.');
    expect(component.fieldErrors.date).toBe('Selecciona una fecha.');
    expect(citasService.getFranjasDisponibles).not.toHaveBeenCalled();
  });

  it('books the selected slot and shows confirmation', () => {
    component.selectedPatient = '10';
    component.selectedProfessional = '8';
    component.selectedDate = new Date(Date.now() + 86400000).toISOString().slice(0, 10);
    component.selectedSlot = `${component.selectedDate}T09:00:00-05:00`;
    component.slots = [{ fecha_hora: component.selectedSlot, hora: '09:00' }];
    const selectedSlot = component.selectedSlot;

    component.book();

    expect(citasService.crearCita).toHaveBeenCalledWith({
      paciente: 10,
      medico: 8,
      fecha_hora: selectedSlot,
      estado: 'PROGRAMADA',
    });
    expect(component.showConfirmation).toBe(true);
    expect(component.confirmationMessage).toContain('Carlos Médico');
    expect(component.selectedProfessional).toBe('');
  });
});
