import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Medicos } from './medicos';
import { MedicosService } from '../../core/services/medicos.service';
import { PersonasService } from '../../core/services/personas.service';

describe('Medicos', () => {
  let component: Medicos;
  let fixture: ComponentFixture<Medicos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Medicos],
      providers: [
        {
          provide: MedicosService,
          useValue: {
            getMedicos: () => of([
              { persona: 1, tipo_profesional: 'MEDICO' },
              { persona: 2, tipo_profesional: 'TERAPISTA' },
            ]),
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
});
