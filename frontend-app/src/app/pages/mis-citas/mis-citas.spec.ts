import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { CitasService } from '../../core/services/citas.service';
import { Cita } from '../../shared/models/cita.model';
import { MisCitas } from './mis-citas';

describe('MisCitas', () => {
  let component: MisCitas;
  let fixture: ComponentFixture<MisCitas>;
  const getCitas = vi.fn(() => of([]));

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MisCitas],
      providers: [
        provideRouter([]),
        { provide: CitasService, useValue: { getCitas } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MisCitas);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('does not show status actions when there are no appointments', () => {
    expect(fixture.nativeElement.querySelector('app-appointment-actions')).toBeNull();
    expect(getCitas).toHaveBeenCalled();
  });

  it('reorders the appointment list immediately after changing its date', () => {
    const laterAppointment: Cita = {
      id: 2,
      paciente: 5,
      medico: 3,
      fecha_hora: '2099-01-10T10:00:00-05:00',
      estado: 'PROGRAMADA',
    };
    const earlierAppointment: Cita = {
      id: 1,
      paciente: 5,
      medico: 3,
      fecha_hora: '2099-01-12T10:00:00-05:00',
      estado: 'PROGRAMADA',
    };
    component.appointments = [earlierAppointment, laterAppointment];

    component.updateAppointment({
      ...earlierAppointment,
      fecha_hora: '2099-01-05T10:00:00-05:00',
      estado: 'REAGENDADA',
    });

    expect(component.appointments.map((appointment) => appointment.id)).toEqual([1, 2]);
    expect(component.appointments[0].fecha_hora).toBe('2099-01-05T10:00:00-05:00');
  });

  it('separates future appointments from past appointments, newest past first', () => {
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
        medico: 3,
        fecha_hora: '2021-01-10T10:00:00-05:00',
        estado: 'ATENDIDA',
      },
    ];

    expect(component.upcomingAppointments.map((appointment) => appointment.id)).toEqual([2]);
    expect(component.pastAppointments.map((appointment) => appointment.id)).toEqual([3, 1]);
  });

  it('paginates the appointment panels independently', () => {
    component.appointments = [
      ...Array.from({ length: 11 }, (_, index) => ({
        id: index + 1,
        paciente: 5,
        medico: 3,
        fecha_hora: `2099-01-${String(index + 1).padStart(2, '0')}T10:00:00-05:00`,
        estado: 'PROGRAMADA',
      })),
      ...Array.from({ length: 11 }, (_, index) => ({
        id: index + 12,
        paciente: 5,
        medico: 3,
        fecha_hora: `2000-01-${String(index + 1).padStart(2, '0')}T10:00:00-05:00`,
        estado: 'ATENDIDA',
      })),
    ];

    component.nextUpcomingPage();
    expect(component.currentPastPage).toBe(1);
    component.nextPastPage();

    expect(component.currentUpcomingPage).toBe(2);
    expect(component.currentPastPage).toBe(2);
    expect(component.paginatedUpcomingAppointments).toHaveLength(1);
    expect(component.paginatedPastAppointments).toHaveLength(1);
  });
});
