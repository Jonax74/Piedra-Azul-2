import { NgZone } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { Auth } from '../../core/services/auth';
import { CitasService } from '../../core/services/citas.service';
import { MedicosService } from '../../core/services/medicos.service';
import { PersonasService } from '../../core/services/personas.service';
import { Agenda } from './agenda';
import { Cita } from '../../shared/models/cita.model';

describe('Agenda', () => {
  let component: Agenda;
  let fixture: ComponentFixture<Agenda>;
  let citasService: { getAgenda: ReturnType<typeof vi.fn> };
  let getAgendaMedico: ReturnType<typeof vi.fn>;
  let getAgenda: ReturnType<typeof vi.fn>;
  let getFechasAgenda: ReturnType<typeof vi.fn>;
  let exportarAgenda: ReturnType<typeof vi.fn>;
  let exportarAgendaMedico: ReturnType<typeof vi.fn>;
  let getHistorialPaciente: ReturnType<typeof vi.fn>;
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
    getFechasAgenda = vi.fn((requestedMonth: string) => of({
      mes: requestedMonth,
      fechas: [`${requestedMonth}-12`],
    }));
    getAgenda = vi.fn(() => of({ cantidad: 0, resultados: [] }));
    exportarAgenda = vi.fn(() => of(new Blob(['Fecha,Hora'])));
    exportarAgendaMedico = vi.fn(() => of(new Blob(['Fecha;Hora'])));
    getHistorialPaciente = vi.fn(() => of({
      paciente: {
        nombre: 'Ana Paciente',
        cedula: 10000001,
        fecha_nacimiento: '1992-01-01',
        genero: 'Mujer',
        telefono: '3000000000',
        correo: null,
      },
      cantidad: 1,
      citas: [{
        id: 1,
        fecha_hora: '2099-01-05T10:00:00-05:00',
        medico: 'Carlos Medico',
        tipo_profesional: 'MEDICO',
        estado: 'Programada',
      }],
    }));
    getMedicos = vi.fn(() => of([]));
    getPersonas = vi.fn(() => of([]));
    roles = ['MEDICO'];
    citasService = { getAgenda };
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
            getHistorialPaciente,
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

  it('requests the selected doctor/date filters and resets pagination', () => {
    roles = ['ADMIN'];
    fixture.destroy();
    fixture = TestBed.createComponent(Agenda);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const appointment = { id: 3, paciente: 10, medico: 8 } as Cita;
    citasService.getAgenda.mockReturnValue(
      of({ cantidad: 1, resultados: [appointment] }),
    );
    component.currentPage = 4;
    component.selectedProfessional = '8';
    component.selectedDate = '2099-01-05';

    component.search();

    expect(citasService.getAgenda).toHaveBeenCalledWith(8, '2099-01-05');
    expect(component.appointments()).toEqual([appointment]);
    expect(component.currentPage).toBe(1);
    expect(component.feedback()).toContain('Se encontraron 1 citas');
  });

  it('paginates appointments without moving beyond either end', () => {
    component.appointments.set(Array.from(
      { length: 21 },
      (_, index) => ({ id: index + 1 }) as Cita,
    ));

    expect(component.totalPages).toBe(3);
    expect(component.paginatedAppointments).toHaveLength(10);
    component.previousPage();
    expect(component.currentPage).toBe(1);
    component.nextPage();
    component.nextPage();
    component.nextPage();
    expect(component.currentPage).toBe(3);
    expect(component.paginatedAppointments).toHaveLength(1);
  });

  it('uses appointment identifiers when patient details are absent', () => {
    const appointment = { paciente: 10 } as Cita;

    expect(component.patientName(appointment)).toBe('Paciente #10');
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

  it('lets an agendador search a patient by ID and displays appointment history', async () => {
    roles = ['AGENDADOR'];
    fixture.destroy();
    fixture = TestBed.createComponent(Agenda);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('Consultar paciente');
    fixture.nativeElement.querySelector('.patient-history-entry button').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).not.toBeNull();

    component.patientDocument = ' 10000001 ';
    component.searchPatientHistory();
    fixture.detectChanges();

    expect(getHistorialPaciente).toHaveBeenCalledWith('10000001');
    expect(fixture.nativeElement.textContent).toContain('Ana Paciente');
    expect(fixture.nativeElement.textContent).toContain('Carlos Medico');
    expect(fixture.nativeElement.textContent).toContain('Programada');
  });

  it('shows the API error when a patient cannot be found by ID', async () => {
    roles = ['AGENDADOR'];
    getHistorialPaciente.mockReturnValue(throwError(() => ({
      error: { detail: 'No se encontró un paciente con esa cédula.' },
    })));
    fixture.destroy();
    fixture = TestBed.createComponent(Agenda);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    component.openPatientHistory();
    component.patientDocument = '10000001';
    component.searchPatientHistory();
    fixture.detectChanges();

    expect(getHistorialPaciente).toHaveBeenCalledWith('10000001');
    expect(component.isSearchingPatient()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain(
      'No se encontró un paciente con esa cédula.',
    );
  });

  it('shows a clear message when patient ID contains non-digits', async () => {
    roles = ['AGENDADOR'];
    fixture.destroy();
    fixture = TestBed.createComponent(Agenda);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    component.openPatientHistory();
    component.patientDocument = 'ABC123';
    component.searchPatientHistory();
    fixture.detectChanges();

    expect(getHistorialPaciente).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'Escribe una cédula válida usando solo números.',
    );
  });

  it('lets a doctor open patient lookup and view appointment history', async () => {
    roles = ['MEDICO'];
    fixture.destroy();
    fixture = TestBed.createComponent(Agenda);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('Consultar paciente');
    fixture.nativeElement.querySelector('.patient-history-entry button').click();
    fixture.detectChanges();
    component.patientDocument = '10000001';
    component.searchPatientHistory();
    fixture.detectChanges();

    expect(getHistorialPaciente).toHaveBeenCalledWith('10000001');
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Ana Paciente');
    expect(fixture.nativeElement.textContent).toContain('Carlos Medico');
  });
});
