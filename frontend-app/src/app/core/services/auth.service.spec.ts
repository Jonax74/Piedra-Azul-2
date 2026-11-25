import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('loads the authenticated user from the profile endpoint', () => {
    service.getCurrentUser().subscribe();
    const request = httpTesting.expectOne(`${environment.apiUrl}/me/`);

    expect(request.request.method).toBe('GET');
    request.flush({ user_id: 'kc-id', username: 'ana', roles: ['PACIENTE'] });
  });

  it('returns the profile and handles profile request failures', async () => {
    const profilePromise = service.getProfileSync();
    const request = httpTesting.expectOne(`${environment.apiUrl}/me/profile/`);
    expect(request.request.method).toBe('GET');
    request.flush({ id: 5, username: 'ana', roles: ['PACIENTE'], persona_id: 9 });

    await expect(profilePromise).resolves.toMatchObject({
      id: 5,
      username: 'ana',
      persona_id: 9,
    });

    const failedProfilePromise = service.getProfileSync();
    const failedRequest = httpTesting.expectOne(`${environment.apiUrl}/me/profile/`);
    failedRequest.flush('unavailable', { status: 503, statusText: 'Unavailable' });
    await expect(failedProfilePromise).resolves.toBeNull();
  });
});
