import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NgForm } from '@angular/forms';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Medicos } from './medicos';
import { MedicosService } from '../../core/services/medicos.service';
import { PersonasService } from '../../core/services/personas.service';

async function openCreateForm(fixture: ComponentFixture<Medicos>): Promise<NgForm> {
  fixture.nativeElement.querySelector('.page-heading button').click();
  fixture.detectChanges();
  await fixture.whenStable();
  return fixture.debugElement.query(By.css('form')).injector.get(NgForm);
}

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

  it('creates a doctor and a linked login account from the same form', async () => {
    const form = await openCreateForm(fixture);
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
    fixture.detectChanges();
    await fixture.whenStable();

    component.saveProfessional(form);

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

  it('marks required fields and prevents submission while the form is incomplete', async () => {
    const form = await openCreateForm(fixture);

    component.saveProfessional(form);

    expect(createMedico).not.toHaveBeenCalled();
    expect(form.controls['primer_nombre'].touched).toBe(true);
    expect(component.fieldError(form, 'primer_nombre')).toBe('Este campo es obligatorio.');
    expect(component.feedback).toContain('campos marcados');
  });

  it('marks a mismatched password confirmation before sending the request', async () => {
    const form = await openCreateForm(fixture);
    Object.assign(component.professionalForm, {
      primer_nombre: 'Luis',
      primer_apellido: 'Mendez',
      fecha_nacimiento: '1985-01-01',
      telefono: '3000000001',
      dni: '98765432',
      correo: 'luis@example.com',
      username: 'medico.luis',
      password: 'ClaveSegura123',
      password_confirmation: 'OtraClave123',
    });
    fixture.detectChanges();
    await fixture.whenStable();

    component.saveProfessional(form);

    expect(createMedico).not.toHaveBeenCalled();
    expect(component.fieldError(form, 'password_confirmation')).toBe('Las contraseñas no coinciden.');
  });

  it('shows field-level messages for invalid email, phone, and document input', async () => {
    const form = await openCreateForm(fixture);
    Object.assign(component.professionalForm, {
      primer_nombre: 'Luis',
      primer_apellido: 'Mendez',
      fecha_nacimiento: '1985-01-01',
      telefono: 'abcdefgh',
      dni: '12x',
      correo: 'no-es-correo',
      username: 'medico.luis',
      password: 'ClaveSegura123',
      password_confirmation: 'ClaveSegura123',
    });
    fixture.detectChanges();
    await fixture.whenStable();

    component.saveProfessional(form);

    expect(createMedico).not.toHaveBeenCalled();
    expect(component.fieldError(form, 'correo')).toBe('Ingresa un correo electrónico válido.');
    expect(component.fieldError(form, 'telefono')).toContain('número');
    expect(component.fieldError(form, 'dni')).toBe('Ingresa un documento entero válido.');
  });

  it('enforces name, username, and password length limits inline', async () => {
    const form = await openCreateForm(fixture);
    Object.assign(component.professionalForm, {
      primer_nombre: 'L'.repeat(51),
      primer_apellido: 'Mendez',
      fecha_nacimiento: '1985-01-01',
      telefono: '3000000001',
      dni: '98765432',
      correo: 'luis@example.com',
      username: 'ab',
      password: 'corta',
      password_confirmation: 'corta',
    });
    fixture.detectChanges();
    await fixture.whenStable();

    component.saveProfessional(form);

    expect(createMedico).not.toHaveBeenCalled();
    expect(component.fieldError(form, 'primer_nombre')).toBe('No puede superar 50 caracteres.');
    expect(component.fieldError(form, 'username')).toBe('El usuario debe tener al menos 3 caracteres.');
    expect(component.fieldError(form, 'password')).toBe('La contraseña debe tener al menos 8 caracteres.');
  });

  it('attaches backend username errors to the matching form field', async () => {
    const form = await openCreateForm(fixture);
    Object.assign(component.professionalForm, {
      primer_nombre: 'Luis',
      primer_apellido: 'Mendez',
      fecha_nacimiento: '1985-01-01',
      telefono: '3000000001',
      dni: '98765432',
      correo: 'luis@example.com',
      username: 'medico.luis',
      password: 'ClaveSegura123',
      password_confirmation: 'ClaveSegura123',
    });
    fixture.detectChanges();
    await fixture.whenStable();
    createMedico.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409,
      statusText: 'Conflict',
      error: { username: ['Este nombre de usuario ya está registrado.'] },
    })));

    component.saveProfessional(form);

    expect(component.fieldError(form, 'username')).toBe(
      'Este nombre de usuario ya está registrado.',
    );
  });
});
