import { NgZone } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { Auth } from '../../core/services/auth';
import { CitasService } from '../../core/services/citas.service';
import { MedicosService } from '../../core/services/medicos.service';
import { PersonasService } from '../../core/services/personas.service';
import { Agenda } from './agenda';

describe('Agenda', () => {
  let component: Agenda;
  let fixture: ComponentFixture<Agenda>;
  let getAgendaMedico: ReturnType<typeof vi.fn>;
  let getAgenda: ReturnType<typeof vi.fn>;
  let getFechasAgenda: ReturnType<typeof vi.fn>;
  let exportarAgenda: ReturnType<typeof vi.fn>;
  let exportarAgendaMedico: ReturnType<typeof vi.fn>;
  let getMedicos: ReturnType<typeof vi.fn>;
  let getPersonas: ReturnType<typeof vi.fn>;
  let roles: string[];
  let agendaResponse: Subject<{
    cantidad: number;
    resultados: Array<{
      paciente: number;
      medico: number;
      fecha_hora: string;
      estado: string;
      medico_detalle?: {
        persona?: {
          primer_nombre?: string;
          primer_apellido?: string;
        };
      };
    }>;
  }>;

  beforeEach(async () => {
    agendaResponse = new Subject();
    getAgendaMedico = vi.fn(() => agendaResponse.asObservable());
    const now = new Date();
    const month = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    getFechasAgenda = vi.fn((requestedMonth: string) => of({
      mes: requestedMonth,
      fechas: [`${requestedMonth}-12`],
    }));
    getAgenda = vi.fn(() => of({ cantidad: 0, resultados: [] }));
    exportarAgenda = vi.fn(() => of(new Blob(['Fecha,Hora'])));
    exportarAgendaMedico = vi.fn(() => of(new Blob(['Fecha;Hora'])));
    getMedicos = vi.fn(() => of([]));
    getPersonas = vi.fn(() => of([]));
    roles = ['MEDICO'];
    await TestBed.configureTestingModule({
      imports: [Agenda],
      providers: [
        { provide: Auth, useValue: { roles: () => roles } },
        {
          provide: CitasService,
          useValue: {
            getAgendaMedico,
            getAgenda,
            getFechasAgenda,
            exportarAgenda,
            exportarAgendaMedico,
          },
        },
        { provide: MedicosService, useValue: { getMedicos } },
        { provide: PersonasService, useValue: { getPersonas } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Agenda);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders the initial agenda as soon as the request completes', async () => {
    const month = `${component.calendarMonth.getFullYear()}-${String(component.calendarMonth.getMonth() + 1).padStart(2, '0')}`;
    expect(fixture.nativeElement.textContent).toContain('Cargando tus citas');

    agendaResponse.next({
      cantidad: 1,
      resultados: [{
        paciente: 1,
        medico: 1,
        fecha_hora: `${month}-12T08:00:00-05:00`,
        estado: 'PROGRAMADA',
      }],
    });
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelectorAll('.appointment-row')).toHaveLength(1);
    expect(fixture.nativeElement.textContent).toContain('Se encontraron 1 citas');
  });

  it('renders the initial agenda when the request completes outside Angular', async () => {
    const month = `${component.calendarMonth.getFullYear()}-${String(component.calendarMonth.getMonth() + 1).padStart(2, '0')}`;

    TestBed.inject(NgZone).runOutsideAngular(() => {
      agendaResponse.next({
        cantidad: 1,
        resultados: [{
          paciente: 1,
          medico: 1,
          fecha_hora: `${month}-12T08:00:00-05:00`,
          estado: 'PROGRAMADA',
        }],
      });
    });
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelectorAll('.appointment-row')).toHaveLength(1);
    expect(fixture.nativeElement.textContent).toContain('Se encontraron 1 citas');
  });

  it('shows doctors only their own agenda and enables only days with appointments', () => {
    const now = new Date();
    const month = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    agendaResponse.next({
      cantidad: 3,
      resultados: [
        {
          paciente: 1,
          medico: 1,
          fecha_hora: `${month}-12T08:00:00-05:00`,
          estado: 'PROGRAMADA',
          medico_detalle: {
            persona: { primer_nombre: 'Valentina', primer_apellido: 'Pabon' },
          },
        },
        {
          paciente: 2,
          medico: 1,
          fecha_hora: `${month}-14T08:00:00-05:00`,
          estado: 'CONFIRMADA',
        },
        {
          paciente: 3,
          medico: 1,
          fecha_hora: `${month}-15T08:00:00-05:00`,
          estado: 'CANCELADA',
        },
      ],
    });
    fixture.detectChanges();

    expect(component.isDoctorOnly).toBe(true);
    expect(getAgendaMedico).toHaveBeenCalledTimes(1);
    expect(getMedicos).not.toHaveBeenCalled();
    expect(getPersonas).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('select')).toBeNull();
    expect(fixture.nativeElement.querySelector('input[type="date"]')).toBeNull();
    const availableDate = `${month}-12`;
    const unavailableDate = `${month}-13`;
    const cancelledDate = `${month}-15`;
    expect(component.availableAppointmentDates().has(availableDate)).toBe(true);
    expect(component.availableAppointmentDates().has(cancelledDate)).toBe(false);
    fixture.nativeElement.querySelector('.calendar-trigger').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.calendar-popup')).not.toBeNull();
    expect(component.calendarCells().find((day) => day?.iso === availableDate)?.hasAppointments).toBe(true);
    expect(component.calendarCells().find((day) => day?.iso === unavailableDate)?.hasAppointments).toBe(false);
    expect(fixture.nativeElement.querySelector('.calendar-day.available-day')).not.toBeNull();

    const unavailableDay = fixture.nativeElement.querySelector(
      `.calendar-day[aria-label="${unavailableDate}, sin citas"]`,
    ) as HTMLButtonElement;
    expect(unavailableDay.disabled).toBe(true);

    const availableDay = fixture.nativeElement.querySelector(
      `.calendar-day[aria-label="${availableDate}, tiene citas"]`,
    ) as HTMLButtonElement;
    const callsBeforeSelection = getAgendaMedico.mock.calls.length;
    availableDay.click();
    fixture.detectChanges();

    expect(component.selectedDate).toBe(availableDate);
    expect(component.isCalendarOpen).toBe(false);
    expect(component.appointments()).toHaveLength(1);
    expect(component.appointments()[0].fecha_hora.startsWith(availableDate)).toBe(true);
    expect(getAgendaMedico).toHaveBeenCalledTimes(callsBeforeSelection);
    expect(fixture.nativeElement.querySelector('.export-button')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Exportar mi agenda');

    const downloadClick = vi.spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(function (this: HTMLAnchorElement) {
        expect(this.download).toBe(`citas-valentina-pabon-${availableDate}.csv`);
      });
    component.exportAppointments();
    expect(exportarAgendaMedico).toHaveBeenCalledWith(availableDate);
    expect(downloadClick).toHaveBeenCalledOnce();
    downloadClick.mockRestore();
  });

  it('filters scheduler calendar by selected professional and shows CSV export only with both filters', () => {
    roles = ['AGENDADOR'];
    getMedicos.mockReturnValue(of([{ persona: 12, estado: 'ACTIVO' }]));
    getPersonas.mockReturnValue(of([{
      id: 12,
      primer_nombre: 'Carlos',
      primer_apellido: 'Medico',
    }]));
    fixture.destroy();
    fixture = TestBed.createComponent(Agenda);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const month = `${component.calendarMonth.getFullYear()}-${String(component.calendarMonth.getMonth() + 1).padStart(2, '0')}`;
    expect(component.isSchedulerOnly).toBe(true);
    expect(getFechasAgenda).toHaveBeenCalledWith(month, undefined);
    expect(fixture.nativeElement.querySelector('.export-button')).toBeNull();

    component.selectedProfessional = '12';
    component.onProfessionalChange();
    expect(getFechasAgenda).toHaveBeenLastCalledWith(month, 12);
    expect(component.availableAppointmentDates().has(`${month}-12`)).toBe(true);

    fixture.nativeElement.querySelector('.calendar-trigger').click();
    fixture.detectChanges();
    const appointmentDay = fixture.nativeElement.querySelector(
      `.calendar-day[aria-label="${month}-12, tiene citas"]`,
    ) as HTMLButtonElement;
    appointmentDay.click();
    fixture.detectChanges();
    expect(component.selectedDate).toBe(`${month}-12`);
    expect(getAgenda).toHaveBeenLastCalledWith(12, `${month}-12`);
    expect(fixture.nativeElement.querySelector('.export-button')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Exportar CSV');

    const downloadClick = vi.spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(function (this: HTMLAnchorElement) {
        expect(this.download).toBe(`citas-carlos-medico-${month}-12.csv`);
      });
    component.exportAppointments();
    expect(exportarAgenda).toHaveBeenCalledWith(12, `${month}-12`);
    expect(downloadClick).toHaveBeenCalledOnce();
  });
});
