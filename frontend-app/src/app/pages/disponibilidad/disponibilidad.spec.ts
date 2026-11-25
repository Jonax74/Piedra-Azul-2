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
});
