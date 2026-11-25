import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Auth } from '../../../core/services/auth';
import { RegistrarPacienteLink } from './registrar-paciente-link';

describe('RegistrarPacienteLink', () => {
  it('shows the shortcut only to schedulers', async () => {
    let roles = ['PACIENTE'];
    await TestBed.configureTestingModule({
      imports: [RegistrarPacienteLink],
      providers: [
        provideRouter([]),
        {
          provide: Auth,
          useValue: { hasAnyRole: (allowed: string[]) => roles.some((role) => allowed.includes(role)) },
        },
      ],
    }).compileComponents();

    let fixture = TestBed.createComponent(RegistrarPacienteLink);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Registrar Paciente');

    roles = ['AGENDADOR'];
    fixture.destroy();
    fixture = TestBed.createComponent(RegistrarPacienteLink);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Registrar Paciente');
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe('/registrar-paciente');
  });
});
