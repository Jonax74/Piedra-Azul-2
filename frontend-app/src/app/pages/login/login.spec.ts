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
});
