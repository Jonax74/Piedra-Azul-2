import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { CitasService } from '../../core/services/citas.service';
import { Cita } from '../../shared/models/cita.model';
import { DatePicker } from '../../shared/components/date-picker/date-picker';
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

  it('shows only future appointments and filters by date and assigned professional', () => {
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
        medico_detalle: {
          persona: { primer_nombre: 'Carlos', primer_apellido: 'Médico' },
        },
      },
      {
        id: 3,
        paciente: 5,
        medico: 4,
        fecha_hora: '2099-01-10T11:00:00-05:00',
        estado: 'CONFIRMADA',
      },
      {
        id: 4,
        paciente: 5,
        medico: 5,
        fecha_hora: '2099-01-11T10:00:00-05:00',
        estado: 'CANCELADA',
      },
    ];

    expect(component.upcomingAppointments.map((appointment) => appointment.id)).toEqual([2, 3]);
    expect(component.professionals.map((professional) => professional.id)).toEqual([3, 4]);

    component.selectedDate = '2099-01-10';
    component.selectedProfessional = '3';
    expect(component.filteredAppointments.map((appointment) => appointment.id)).toEqual([2]);
  });

  it('builds searchable doctor options from upcoming appointments', () => {
    component.appointments = [{
      id: 2,
      paciente: 5,
      medico: 3,
      fecha_hora: '2099-01-10T10:00:00-05:00',
      estado: 'PROGRAMADA',
      medico_detalle: {
        persona: { primer_nombre: 'Lucía', primer_apellido: 'Médica' },
      },
    }];

    expect(component.professionalOptions).toEqual([
      { value: '', label: 'Todos los médicos' },
      { value: '3', label: 'Lucía Médica', searchText: 'Lucía Médica' },
    ]);
  });

  it('filters appointments when choosing a day from the custom calendar', () => {
    component.appointments = [
      {
        id: 1,
        paciente: 5,
        medico: 3,
        fecha_hora: '2099-01-10T10:00:00-05:00',
        estado: 'PROGRAMADA',
      },
      {
        id: 2,
        paciente: 5,
        medico: 3,
        fecha_hora: '2099-01-11T10:00:00-05:00',
        estado: 'PROGRAMADA',
      },
    ];
    const datePicker = fixture.debugElement.query(By.directive(DatePicker))
      .componentInstance as DatePicker;
    datePicker.writeValue('2099-01-10');
    fixture.detectChanges();

    fixture.nativeElement.querySelector('[aria-label="Filtrar por fecha"]').click();
    fixture.detectChanges();
    fixture.nativeElement.querySelector(
      '#upcoming-date-calendar [aria-label="2099-01-11"]',
    ).click();
    fixture.detectChanges();

    expect(component.selectedDate).toBe('2099-01-11');
    expect(component.filteredAppointments.map((appointment) => appointment.id)).toEqual([2]);
  });

  it('resets pagination when filters change', () => {
    component.appointments = [
      ...Array.from({ length: 12 }, (_, index) => ({
        id: index + 1,
        paciente: 5,
        medico: 3,
        fecha_hora: `2099-01-${String(index + 1).padStart(2, '0')}T10:00:00-05:00`,
        estado: 'PROGRAMADA',
      })),
    ];

    component.nextPage();
    expect(component.currentPage).toBe(2);
    component.selectedDate = '2099-01-12';
    component.onFilterChange();

    expect(component.currentPage).toBe(1);
    expect(component.paginatedAppointments.map((appointment) => appointment.id)).toEqual([12]);
  });
});
