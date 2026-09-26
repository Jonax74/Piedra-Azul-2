import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
import { authGuard } from './auth-guard';
import { Auth } from '../services/auth';

describe('authGuard', () => {
  let auth: { initialize: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    auth = { initialize: vi.fn() };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: Auth, useValue: auth }],
    });
  });

  it('allows an authenticated session', async () => {
    auth.initialize.mockResolvedValue(true);
    const result = await TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/agenda' } as RouterStateSnapshot),
    );

    expect(result).toBe(true);
  });

  it('redirects unauthenticated users to login with their return path', async () => {
    auth.initialize.mockResolvedValue(false);
    const result = await TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/agenda' } as RouterStateSnapshot),
    );
    const router = TestBed.inject(Router);

    expect(router.serializeUrl(result as ReturnType<Router['createUrlTree']>))
      .toContain('returnUrl=%2Fagenda');
  });
});
