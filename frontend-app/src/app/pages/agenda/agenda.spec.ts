import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { of } from 'rxjs';
import { Agenda } from './agenda';
import { CitasService } from '../../core/services/citas.service';
import { MedicosService } from '../../core/services/medicos.service';
import { PersonasService } from '../../core/services/personas.service';
import { Cita } from '../../shared/models/cita.model';

describe('Agenda', () => {
  let component: Agenda;
  let fixture: ComponentFixture<Agenda>;
  let citasService: { getAgenda: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    citasService = {
      getAgenda: vi.fn().mockReturnValue(of({ cantidad: 0, resultados: [] })),
    };
    await TestBed.configureTestingModule({
      imports: [Agenda],
      providers: [
        { provide: PLATFORM_ID, useValue: 'server' },
        { provide: CitasService, useValue: citasService },
        { provide: MedicosService, useValue: { getMedicos: vi.fn(), getEspecialidades: vi.fn() } },
        { provide: PersonasService, useValue: { getPersonas: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Agenda);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('requests the selected doctor/date filters and resets pagination', () => {
    const appointment = { id: 3, paciente: 10, medico: 8 } as Cita;
    citasService.getAgenda.mockReturnValue(
      of({ cantidad: 1, resultados: [appointment] }),
    );
    component.currentPage = 4;
    component.selectedProfessional = '8';
    component.selectedDate = '2099-01-05';

    component.search();

    expect(citasService.getAgenda).toHaveBeenCalledWith(8, '2099-01-05');
    expect(component.appointments).toEqual([appointment]);
    expect(component.currentPage).toBe(1);
    expect(component.feedback).toContain('Se encontraron 1 citas');
  });

  it('paginates appointments without moving beyond either end', () => {
    component.appointments = Array.from(
      { length: 21 },
      (_, index) => ({ id: index + 1 }) as Cita,
    );

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
});
