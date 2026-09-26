import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { CitasService } from './citas.service';

describe('CitasService', () => {
  let service: CitasService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CitasService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('loads agenda with optional filters', () => {
    service.getAgenda(7, '2099-01-05').subscribe();

    const request = httpTesting.expectOne(
      (candidate) => candidate.url === `${environment.apiUrl}/citas/agenda/`,
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('medico')).toBe('7');
    expect(request.request.params.get('fecha')).toBe('2099-01-05');
    request.flush({ cantidad: 0, resultados: [] });
  });

  it('requests available slots and sends appointment writes', () => {
    service.getFranjasDisponibles(7, '2099-01-05').subscribe();
    const slotsRequest = httpTesting.expectOne(
      (candidate) => candidate.url === `${environment.apiUrl}/citas/disponibles/`,
    );
    expect(slotsRequest.request.params.get('medico')).toBe('7');
    expect(slotsRequest.request.params.get('fecha')).toBe('2099-01-05');
    slotsRequest.flush({ medico: 7, fecha: '2099-01-05', cantidad: 0, franjas: [] });

    const payload = { paciente: 3, medico: 7 };
    service.crearCita(payload).subscribe();
    service.actualizarCita(4, payload).subscribe();
    const createRequest = httpTesting.expectOne(
      (candidate) => candidate.url === `${environment.apiUrl}/citas/`
        && candidate.method === 'POST',
    );
    expect(createRequest.request.body).toEqual(payload);
    createRequest.flush({});
    const updateRequest = httpTesting.expectOne(
      (candidate) => candidate.url === `${environment.apiUrl}/citas/4/`
        && candidate.method === 'PUT',
    );
    expect(updateRequest.request.body).toEqual(payload);
    updateRequest.flush({});
  });
});
