import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CitasService } from '../../../core/services/citas.service';
import { ConfiguracionService } from '../../../core/services/configuracion.service';
import { Cita } from '../../models/cita.model';
import { AppointmentActions } from './appointment-actions';

describe('AppointmentActions', () => {
  let fixture: ComponentFixture<AppointmentActions>;
  const updatedAppointment: Cita = {
    id: 12,
    paciente: 5,
    medico: 3,
    fecha_hora: '2099-01-05T10:00:00-05:00',
    estado: 'CONFIRMADA',
  };
  const actualizarEstadoCita = vi.fn(() => of(updatedAppointment));
  const getFechasDisponibles = vi.fn(() => of({
    medico: 3,
    mes: '2099-01',
    fechas: ['2099-01-05'],
  }));
  const getFranjasDisponibles = vi.fn(() => of({
    medico: 3,
    fecha: '2099-01-05',
    cantidad: 1,
    franjas: [{
      fecha: '2099-01-05',
      hora: '11:00',
      fecha_hora: '2099-01-05T11:00:00-05:00',
    }],
  }));

  beforeEach(async () => {
    getFechasDisponibles.mockClear();
    getFranjasDisponibles.mockClear();
    actualizarEstadoCita.mockClear();
    await TestBed.configureTestingModule({
      imports: [AppointmentActions],
      providers: [
        {
          provide: CitasService,
          useValue: {
            actualizarEstadoCita,
            getFranjasDisponibles,
            getFechasDisponibles,
          },
        },
        {
          provide: ConfiguracionService,
          useValue: {
            getConfiguracion: vi.fn(() => of({
              semanas_agendamiento: 4,
              activo: true,
            })),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppointmentActions);
    fixture.componentRef.setInput('cita', {
      id: 12,
      paciente: 5,
      medico: 3,
      fecha_hora: '2099-01-05T10:00:00-05:00',
      estado: 'PROGRAMADA',
    } satisfies Cita);
    fixture.detectChanges();
  });

  it('renders icon-only buttons with accessible labels', () => {
    const buttons = fixture.nativeElement.querySelectorAll('.icon-action');
    expect(buttons).toHaveLength(3);
    expect(
      Array.from(buttons as NodeListOf<HTMLButtonElement>).map((button) =>
        button.getAttribute('aria-label'),
      ),
    ).toEqual(['Confirmar cita', 'Reagendar cita', 'Cancelar cita']);
  });

  it('confirms an appointment and emits the updated record', () => {
    const onUpdated = vi.fn();
    fixture.componentInstance.citaActualizada.subscribe(onUpdated);

    const confirmButton = fixture.nativeElement.querySelector(
      '[aria-label="Confirmar cita"]',
    ) as HTMLButtonElement;
    confirmButton.click();
    fixture.detectChanges();
    const submitButton = fixture.nativeElement.querySelector(
      '.dialog-actions .primary-button',
    ) as HTMLButtonElement;
    submitButton.click();
    fixture.detectChanges();

    expect(actualizarEstadoCita).toHaveBeenCalledWith(12, {
      estado: 'CONFIRMADA',
    });
    expect(onUpdated).toHaveBeenCalledWith(updatedAppointment);
  });

  it('loads the availability calendar and fetches slots when a date is selected', () => {
    fixture.componentInstance.openReschedule();
    expect(getFechasDisponibles).toHaveBeenCalledOnce();

    fixture.componentInstance.availableDates = new Set(['2099-01-05']);
    fixture.componentInstance.selectDate('2099-01-05');

    expect(getFranjasDisponibles).toHaveBeenCalledWith(3, '2099-01-05');
    expect(fixture.componentInstance.slots).toEqual([
      {
        fecha: '2099-01-05',
        hora: '11:00',
        fecha_hora: '2099-01-05T11:00:00-05:00',
      },
    ]);
  });
});
