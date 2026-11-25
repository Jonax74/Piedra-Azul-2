import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
import { roleGuard } from './role-guard';
import { Auth } from '../services/auth';

describe('roleGuard', () => {
  let auth: { hasAnyRole: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    auth = { hasAnyRole: vi.fn() };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: Auth, useValue: auth }],
    });
  });

  it('allows a user with an accepted role', () => {
    auth.hasAnyRole.mockReturnValue(true);
    const result = TestBed.runInInjectionContext(() =>
      roleGuard(
        { data: { roles: ['ADMIN', 'AGENDADOR'] } } as unknown as ActivatedRouteSnapshot,
        { url: '/agenda' } as RouterStateSnapshot,
      ),
    );

    expect(auth.hasAnyRole).toHaveBeenCalledWith(['ADMIN', 'AGENDADOR']);
    expect(result).toBe(true);
  });

  it('redirects users without a required role to the dashboard', () => {
    auth.hasAnyRole.mockReturnValue(false);
    const result = TestBed.runInInjectionContext(() =>
      roleGuard(
        { data: { roles: ['ADMIN'] } } as unknown as ActivatedRouteSnapshot,
        { url: '/configuracion' } as RouterStateSnapshot,
      ),
    );
    const router = TestBed.inject(Router);

    expect(router.serializeUrl(result as ReturnType<Router['createUrlTree']>))
      .toBe('/dashboard');
  });
});
