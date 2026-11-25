import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Login } from './login';
import { Auth } from '../../core/services/auth';

describe('Login', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;

  beforeEach(async () => {
    const auth = {
      initialize: vi.fn().mockResolvedValue(false),
      login: vi.fn().mockRejectedValue(new Error('login failed')),
    };
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), { provide: Auth, useValue: auth }],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('uses the dashboard as default return path and reports login failures', async () => {
    expect(component.returnUrl).toBe('/dashboard');

    await component.login();

    expect(component.errorMessage).toBe('No fue posible iniciar la sesión. Intenta nuevamente.');
    expect(component.isLoading).toBe(false);
  });

  it('shows a concise welcome message and Piedrazul and developer footer', () => {
    const page = fixture.nativeElement as HTMLElement;

    expect(page.querySelector('h1')?.textContent).toContain(
      'Tu bienestar, en buenas manos.',
    );
    expect(page.textContent).toContain('Piedrazul');
    expect(page.textContent).toContain('Clínica de bienestar');
    expect(page.textContent).toContain('contacto@piedrazul.example');
    expect(page.textContent).toContain('Horario de atención');
    expect(page.textContent).toContain('DEVCORE SOFTWARE STUDIO');
    expect(page.textContent).toContain('Estudio colombiano de tecnología');
    expect(page.textContent).toContain('Desarrollo web');
    expect(page.textContent).toContain('hola@devcore.studio');
    expect(page.textContent).toContain('Cuidamos de ti, cada día.');
    expect(page.textContent).not.toContain('pendiente de actualizar');
    expect(page.textContent).not.toContain(
      'Tu bienestar empieza con una conversación.',
    );
  });
});
