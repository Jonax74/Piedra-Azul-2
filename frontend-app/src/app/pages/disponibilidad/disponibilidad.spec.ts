import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Disponibilidad } from './disponibilidad';

describe('Disponibilidad', () => {
  let component: Disponibilidad;
  let fixture: ComponentFixture<Disponibilidad>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Disponibilidad],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Disponibilidad);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
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
