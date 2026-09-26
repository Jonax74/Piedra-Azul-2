import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let service: ApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('adds supported query parameters and omits empty values', () => {
    service.get('/citas/', {
      pagina: 2,
      activa: false,
      vacia: '',
      nula: null,
      ausente: undefined,
    }).subscribe();

    const request = httpTesting.expectOne(
      (candidate) => candidate.url === `${environment.apiUrl}/citas/`,
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('pagina')).toBe('2');
    expect(request.request.params.get('activa')).toBe('false');
    expect(request.request.params.has('vacia')).toBe(false);
    expect(request.request.params.has('nula')).toBe(false);
    expect(request.request.params.has('ausente')).toBe(false);
    request.flush([]);
  });

  it('sends create, update and delete requests to the requested paths', () => {
    const payload = { semanas_agendamiento: 8 };
    service.post('/configuracion/', payload).subscribe();
    service.put('/configuracion/', payload).subscribe();
    service.delete('/citas/4/').subscribe();

    const createRequest = httpTesting.expectOne(
      (request) => request.url === `${environment.apiUrl}/configuracion/`
        && request.method === 'POST',
    );
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toEqual(payload);
    createRequest.flush({});

    const updateRequest = httpTesting.expectOne(
      (request) => request.url === `${environment.apiUrl}/configuracion/`
        && request.method === 'PUT',
    );
    expect(updateRequest.request.method).toBe('PUT');
    expect(updateRequest.request.body).toEqual(payload);
    updateRequest.flush({});

    const deleteRequest = httpTesting.expectOne(
      (request) => request.url === `${environment.apiUrl}/citas/4/`
        && request.method === 'DELETE',
    );
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush({});
  });
});
