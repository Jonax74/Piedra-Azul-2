import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CitasService } from '../../core/services/citas.service';
import { Cita } from '../../shared/models/cita.model';
import { Historial } from './historial';

describe('Historial', () => {
  let component: Historial;
  let fixture: ComponentFixture<Historial>;
  const getCitas = vi.fn(() => of([]));

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Historial],
      providers: [
        { provide: CitasService, useValue: { getCitas } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Historial);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('shows only past appointments, newest first', () => {
    component.appointments = [
      {
        id: 1,
        paciente: 5,
        medico: 3,
        fecha_hora: '2020-01-10T10:00:00-05:00',
        estado: 'ATENDIDA',
      },
      {
        id: 3,
        paciente: 5,
        medico: 4,
        fecha_hora: '2021-01-10T10:00:00-05:00',
        estado: 'ATENDIDA',
      },
      {
        id: 4,
        paciente: 5,
        medico: 3,
        fecha_hora: '2030-01-10T10:00:00-05:00',
        estado: 'CANCELADA',
      },
      {
        id: 2,
        paciente: 5,
        medico: 3,
        fecha_hora: '2099-01-10T10:00:00-05:00',
        estado: 'PROGRAMADA',
      },
    ];

    expect(component.historyAppointments.map((appointment) => appointment.id)).toEqual([4, 3, 1]);
  });

  it('includes future cancellations in history filters', () => {
    component.appointments = [
      {
        id: 1,
        paciente: 5,
        medico: 3,
        fecha_hora: '2020-01-10T10:00:00-05:00',
        estado: 'ATENDIDA',
      },
      {
        id: 2,
        paciente: 5,
        medico: 3,
        fecha_hora: '2099-01-11T10:00:00-05:00',
        estado: 'CANCELADA',
      },
      {
        id: 3,
        paciente: 5,
        medico: 3,
        fecha_hora: '2099-01-11T11:00:00-05:00',
        estado: 'PROGRAMADA',
      },
    ] satisfies Cita[];
    component.selectedDate = '2099-01-11';
    component.selectedProfessional = '3';

    expect(component.filteredAppointments.map((appointment) => appointment.id)).toEqual([2]);
  });

  it('builds searchable doctor options from historical appointments', () => {
    component.appointments = [{
      id: 1,
      paciente: 5,
      medico: 3,
      fecha_hora: '2020-01-10T10:00:00-05:00',
      estado: 'ATENDIDA',
      medico_detalle: {
        persona: { primer_nombre: 'Lucía', primer_apellido: 'Médica' },
      },
    }];

    expect(component.professionalOptions).toEqual([
      { value: '', label: 'Todos los médicos' },
      { value: '3', label: 'Lucía Médica', searchText: 'Lucía Médica' },
    ]);
  });
});
