import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Disponibilidad } from './disponibilidad';
import { DisponibilidadService } from '../../core/services/disponibilidad.service';
import { MedicosService } from '../../core/services/medicos.service';
import { PersonasService } from '../../core/services/personas.service';

describe('Disponibilidad', () => {
  let component: Disponibilidad;
  let fixture: ComponentFixture<Disponibilidad>;
  let disponibilidadService: {
    getMedicoDisponibilidades: ReturnType<typeof vi.fn>;
    getDisponibilidades: ReturnType<typeof vi.fn>;
    crearDisponibilidad: ReturnType<typeof vi.fn>;
    asociarConMedico: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    disponibilidadService = {
      getMedicoDisponibilidades: vi.fn().mockReturnValue(of([])),
      getDisponibilidades: vi.fn().mockReturnValue(of([])),
      crearDisponibilidad: vi.fn().mockReturnValue(of({ id: 2 })),
      asociarConMedico: vi.fn().mockReturnValue(of({})),
    };
    await TestBed.configureTestingModule({
      imports: [Disponibilidad],
      providers: [
        { provide: DisponibilidadService, useValue: disponibilidadService },
        { provide: MedicosService, useValue: { getMedicos: () => of([]) } },
        { provide: PersonasService, useValue: { getPersonas: () => of([]) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Disponibilidad);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('rejects overlapping schedules for the same doctor and weekday', () => {
    component.selectedProfessional = '8';
    component.selectedDay = 'LUNES';
    component.startTime = '09:30';
    component.endTime = '10:30';
    component.relationsLoaded = true;
    component.relations = [{ medico: 8, disponibilidad: 1 }];
    component.schedules = [
      { id: 1, dia_semana: 'LUNES', hora_inicio: '09:00', hora_fin: '10:00', intervalo: 30 },
    ];

    component.saveSchedule();

    expect(component.fieldErrors.startTime).toContain('se cruza');
    expect(disponibilidadService.crearDisponibilidad).not.toHaveBeenCalled();
  });

  it('allows adjacent schedules and associates the created schedule', () => {
    component.selectedProfessional = '8';
    component.selectedDay = 'LUNES';
    component.startTime = '10:00';
    component.endTime = '11:00';
    component.relationsLoaded = true;
    component.relations = [{ medico: 8, disponibilidad: 1 }];
    component.schedules = [
      { id: 1, dia_semana: 'LUNES', hora_inicio: '09:00', hora_fin: '10:00', intervalo: 30 },
    ];

    component.saveSchedule();

    expect(disponibilidadService.crearDisponibilidad).toHaveBeenCalledWith({
      dia_semana: 'LUNES',
      hora_inicio: '10:00',
      hora_fin: '11:00',
      intervalo: 30,
    });
    expect(disponibilidadService.asociarConMedico).toHaveBeenCalledWith({
      medico: 8,
      disponibilidad: 2,
    });
    expect(component.feedback).toBe('Horario guardado correctamente.');
    expect(component.isSaving).toBe(false);
  });
});
