import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { Medicos } from './medicos';
import { MedicosService } from '../../core/services/medicos.service';
import { PersonasService } from '../../core/services/personas.service';

describe('Medicos', () => {
  let component: Medicos;
  let fixture: ComponentFixture<Medicos>;
  let createMedico: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    createMedico = vi.fn(() => of({ persona: 3, tipo_profesional: 'MEDICO', estado: 'ACTIVO' }));
    await TestBed.configureTestingModule({
      imports: [Medicos],
      providers: [
        provideRouter([]),
        {
          provide: MedicosService,
          useValue: {
            getMedicos: () => of([
              { persona: 1, tipo_profesional: 'MEDICO' },
              { persona: 2, tipo_profesional: 'TERAPISTA' },
            ]),
            getEspecialidades: () => of([{ id: 1, nombre: 'Fisioterapia' }]),
            crearMedico: createMedico,
          },
        },
        {
          provide: PersonasService,
          useValue: {
            getPersonas: () => of([
              { id: 1, primer_nombre: 'Ana', primer_apellido: 'Ruiz' },
            ]),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Medicos);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('filters professionals by person name or professional type', () => {
    component.searchTerm = '  ANA RUIZ ';
    expect(component.filteredProfessionals.map((item) => item.persona)).toEqual([1]);

    component.searchTerm = 'terapista';
    expect(component.filteredProfessionals.map((item) => item.persona)).toEqual([2]);
  });

  it('returns a stable fallback name when a person is not loaded', () => {
    expect(component.professionalName({ persona: 99 } as never)).toBe('Profesional #99');
  });

  it('creates a doctor and a linked login account from the same form', () => {
    Object.assign(component.professionalForm, {
      primer_nombre: 'Luis',
      primer_apellido: 'Mendez',
      fecha_nacimiento: '1985-01-01',
      telefono: '3000000001',
      dni: '98765432',
      correo: 'luis@example.com',
      especialidad_id: '1',
      username: 'medico.luis',
      password: 'ClaveSegura123',
      password_confirmation: 'ClaveSegura123',
    });

    component.saveProfessional();

    expect(createMedico).toHaveBeenCalledWith(expect.objectContaining({
      username: 'medico.luis',
      password: 'ClaveSegura123',
      password_confirmation: 'ClaveSegura123',
      especialidad_ids: [1],
      persona_data: expect.objectContaining({
        correo: 'luis@example.com',
        dni: 98765432,
      }),
    }));
    expect(component.feedback).toContain('cuenta de acceso');
    expect(component.professionalForm.password).toBe('');
  });
});
