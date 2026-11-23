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
        id: 2,
        paciente: 5,
        medico: 3,
        fecha_hora: '2099-01-10T10:00:00-05:00',
        estado: 'PROGRAMADA',
      },
      {
        id: 3,
        paciente: 5,
        medico: 4,
        fecha_hora: '2021-01-10T10:00:00-05:00',
        estado: 'ATENDIDA',
      },
    ];

    expect(component.pastAppointments.map((appointment) => appointment.id)).toEqual([3, 1]);
  });

  it('filters history by date and assigned professional', () => {
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
        medico: 4,
        fecha_hora: '2020-01-11T10:00:00-05:00',
        estado: 'ATENDIDA',
      },
    ] satisfies Cita[];
    component.selectedDate = '2020-01-10';
    component.selectedProfessional = '3';

    expect(component.filteredAppointments.map((appointment) => appointment.id)).toEqual([1]);
  });
});
