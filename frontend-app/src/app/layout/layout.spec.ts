import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Layout } from './layout';
import { Auth } from '../core/services/auth';

describe('Layout', () => {
  let component: Layout;
  let fixture: ComponentFixture<Layout>;
  let auth: {
    username: () => string;
    roles: () => string[];
    hasAnyRole: (roles: string[]) => boolean;
    logout: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    auth = {
      username: () => 'Ana',
      roles: () => ['ADMIN'],
      hasAnyRole: (roles) => roles.length === 0 || roles.includes('ADMIN'),
      logout: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [Layout],
      providers: [provideRouter([]), { provide: Auth, useValue: auth }],
    }).compileComponents();

    fixture = TestBed.createComponent(Layout);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('shows admin navigation and delegates logout', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(component.isVisible(['ADMIN'])).toBe(true);
    expect(element.textContent).toContain('Configuración');
    component.logout();
    expect(auth.logout).toHaveBeenCalledOnce();
  });
});
