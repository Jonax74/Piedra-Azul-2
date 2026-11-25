import { TestBed } from '@angular/core/testing';
import { HttpRequest, HttpResponse, HttpHandlerFn } from '@angular/common/http';
import { firstValueFrom, of } from 'rxjs';
import { authInterceptor } from './auth-interceptor';
import { Auth } from '../services/auth';

describe('authInterceptor', () => {
  let auth: { getToken: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    auth = { getToken: vi.fn() };
    TestBed.configureTestingModule({
      providers: [{ provide: Auth, useValue: auth }],
    });
  });

  it('adds a bearer token to API requests', async () => {
    auth.getToken.mockResolvedValue('signed-token');
    const request = new HttpRequest('GET', 'http://localhost/api/citas/');
    const next: HttpHandlerFn = vi.fn(() => of(new HttpResponse({ status: 200 })));

    await firstValueFrom(
      TestBed.runInInjectionContext(() => authInterceptor(request, next)),
    );

    const forwardedRequest = vi.mocked(next).mock.calls[0][0];
    expect(forwardedRequest.headers.get('Authorization')).toBe('Bearer signed-token');
  });

  it('does not request a token for non-API URLs', async () => {
    const request = new HttpRequest('GET', 'https://example.test/health');
    const next: HttpHandlerFn = vi.fn(() => of(new HttpResponse({ status: 200 })));

    await firstValueFrom(
      TestBed.runInInjectionContext(() => authInterceptor(request, next)),
    );

    expect(auth.getToken).not.toHaveBeenCalled();
    expect(vi.mocked(next).mock.calls[0][0]).toBe(request);
  });
});
