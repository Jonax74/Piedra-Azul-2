import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';
import { Disponibilidad } from './disponibilidad';
import { DisponibilidadService } from '../../core/services/disponibilidad.service';
import { MedicosService } from '../../core/services/medicos.service';
import { PersonasService } from '../../core/services/personas.service';

describe('Disponibilidad', () => {
  let component: Disponibilidad;
  let fixture: ComponentFixture<Disponibilidad>;
  let disponibilidadService: {
    getMedicoDisponibilidades: ReturnType<typeof vi.fn>;
    getDisponibilidades: ReturnType<typeof vi.fn>;
    crearDisponibilidad: ReturnType<typeof vi.fn>;
    asociarConMedico: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    disponibilidadService = {
      getMedicoDisponibilidades: vi.fn().mockReturnValue(of([])),
      getDisponibilidades: vi.fn().mockReturnValue(of([])),
      crearDisponibilidad: vi.fn().mockReturnValue(of({ id: 2 })),
      asociarConMedico: vi.fn().mockReturnValue(of({})),
    };
    await TestBed.configureTestingModule({
      imports: [Disponibilidad],
      providers: [
        { provide: DisponibilidadService, useValue: disponibilidadService },
        { provide: MedicosService, useValue: { getMedicos: () => of([]) } },
        { provide: PersonasService, useValue: { getPersonas: () => of([]) } },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Disponibilidad);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('rejects overlapping schedules for the same doctor and weekday', () => {
    component.selectedProfessional = '8';
    component.selectedDay = 'LUNES';
    component.startTime = '09:30';
    component.endTime = '10:30';
    component.relationsLoaded = true;
    component.relations = [{ medico: 8, disponibilidad: 1 }];
    component.schedules = [
      { id: 1, dia_semana: 'LUNES', hora_inicio: '09:00', hora_fin: '10:00', intervalo: 30 },
    ];

    component.saveSchedule();

    expect(component.fieldErrors.startTime).toContain('se cruza');
    expect(disponibilidadService.crearDisponibilidad).not.toHaveBeenCalled();
  });

  it('allows adjacent schedules and associates the created schedule', () => {
    component.selectedProfessional = '8';
    component.selectedDay = 'LUNES';
    component.startTime = '10:00';
    component.endTime = '11:00';
    component.relationsLoaded = true;
    component.relations = [{ medico: 8, disponibilidad: 1 }];
    component.schedules = [
      { id: 1, dia_semana: 'LUNES', hora_inicio: '09:00', hora_fin: '10:00', intervalo: 30 },
    ];

    component.saveSchedule();

    expect(disponibilidadService.crearDisponibilidad).toHaveBeenCalledWith({
      dia_semana: 'LUNES',
      hora_inicio: '10:00',
      hora_fin: '11:00',
      intervalo: 30,
    });
    expect(disponibilidadService.asociarConMedico).toHaveBeenCalledWith({
      medico: 8,
      disponibilidad: 2,
    });
    expect(component.feedback).toBe('Horario guardado correctamente.');
    expect(component.isSaving).toBe(false);
  });

  it('builds searchable professional options from names and documents', () => {
    component.professionals = [{
      persona: 10,
      tipo_profesional: 'MEDICO',
      estado: 'ACTIVO',
    }];
    component.people = [{
      id: 10,
      primer_nombre: 'Lucía',
      primer_apellido: 'Médica',
      dni: 12345678,
      genero: 'F',
      fecha_nacimiento: '1990-01-01',
      telefono: '',
    }];

    expect(component.professionalOptions).toEqual([
      { value: '', label: 'Selecciona un profesional' },
      {
        value: '10',
        label: 'Lucía Médica',
        detail: 'CC 12345678',
        searchText: '12345678 MEDICO',
      },
    ]);
  });

  it('sorts schedules by professional, weekday, and start time', () => {
    component.professionals = [
      { persona: 10, tipo_profesional: 'MEDICO', estado: 'ACTIVO' },
      { persona: 20, tipo_profesional: 'TERAPEUTA', estado: 'ACTIVO' },
    ];
    component.people = [
      { id: 10, primer_nombre: 'Ana', primer_apellido: 'Zuluaga', dni: 12345678, genero: 'F', fecha_nacimiento: '1990-01-01', telefono: '' },
      { id: 20, primer_nombre: 'Beatriz', primer_apellido: 'Arias', dni: 23456789, genero: 'F', fecha_nacimiento: '1990-01-01', telefono: '' },
    ];
    component.relations = [
      { medico: 20, disponibilidad: 4 },
      { medico: 10, disponibilidad: 3 },
      { medico: 10, disponibilidad: 2 },
      { medico: 10, disponibilidad: 1 },
    ];
    component.schedules = [
      { id: 4, dia_semana: 'LUNES', hora_inicio: '08:00', hora_fin: '12:00', intervalo: 30 },
      { id: 3, dia_semana: 'MIERCOLES', hora_inicio: '08:00', hora_fin: '12:00', intervalo: 30 },
      { id: 2, dia_semana: 'LUNES', hora_inicio: '10:00', hora_fin: '12:00', intervalo: 30 },
      { id: 1, dia_semana: 'LUNES', hora_inicio: '08:00', hora_fin: '10:00', intervalo: 30 },
    ];

    expect(component.filteredSchedules.map((schedule) => schedule.id)).toEqual([1, 2, 3, 4]);
  });

  it('keeps the selected professional filter while sorting schedules', () => {
    component.professionals = [
      { persona: 10, tipo_profesional: 'MEDICO', estado: 'ACTIVO' },
      { persona: 20, tipo_profesional: 'TERAPEUTA', estado: 'ACTIVO' },
    ];
    component.people = [
      { id: 10, primer_nombre: 'Ana', primer_apellido: 'Zuluaga', dni: 12345678, genero: 'F', fecha_nacimiento: '1990-01-01', telefono: '' },
      { id: 20, primer_nombre: 'Beatriz', primer_apellido: 'Arias', dni: 23456789, genero: 'F', fecha_nacimiento: '1990-01-01', telefono: '' },
    ];
    component.relations = [
      { medico: 10, disponibilidad: 1 },
      { medico: 20, disponibilidad: 2 },
      { medico: 20, disponibilidad: 3 },
    ];
    component.schedules = [
      { id: 1, dia_semana: 'LUNES', hora_inicio: '08:00', hora_fin: '10:00', intervalo: 30 },
      { id: 3, dia_semana: 'MARTES', hora_inicio: '08:00', hora_fin: '10:00', intervalo: 30 },
      { id: 2, dia_semana: 'LUNES', hora_inicio: '08:00', hora_fin: '10:00', intervalo: 30 },
    ];
    component.selectedProfessional = '20';

    expect(component.filteredSchedules.map((schedule) => schedule.id)).toEqual([2, 3]);
  });
});
