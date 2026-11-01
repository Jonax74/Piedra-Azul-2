import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { MisCitas } from './mis-citas';
import { CitasService } from '../../core/services/citas.service';
import { Cita } from '../../shared/models/cita.model';

describe('MisCitas', () => {
  let component: MisCitas;
  let fixture: ComponentFixture<MisCitas>;
  const getCitas = vi.fn(() => of([]));

  beforeEach(async () => {
    const appointments = Array.from({ length: 11 }, (_, index) => ({
      id: index + 1,
      fecha_hora: '2099-01-05T10:00:00Z',
      estado: 'PROGRAMADA',
      medico: 2,
      paciente: 3,
    }));
    getCitas.mockReturnValue(of(appointments));
    await TestBed.configureTestingModule({
      imports: [MisCitas],
      providers: [
        provideRouter([]),
        { provide: CitasService, useValue: { getCitas } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MisCitas);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('loads appointments and paginates the personal list', () => {
    expect(component.appointments).toHaveLength(11);
    expect(component.totalPages).toBe(2);
    expect(component.paginatedAppointments).toHaveLength(10);
    component.nextPage();
    expect(component.currentPage).toBe(2);
    expect(component.paginatedAppointments).toHaveLength(1);
  });

  it('does not show status actions when there are no appointments', async () => {
    getCitas.mockReturnValue(of([]));
    fixture.destroy();
    fixture = TestBed.createComponent(MisCitas);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

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
});
