import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { ConfiguracionService } from './configuracion.service';

describe('ConfiguracionService', () => {
  let service: ConfiguracionService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ConfiguracionService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('reads and updates the system booking configuration', () => {
    service.getConfiguracion().subscribe();
    const readRequest = httpTesting.expectOne(`${environment.apiUrl}/configuracion/`);
    expect(readRequest.request.method).toBe('GET');
    readRequest.flush({ semanas_agendamiento: 8, activo: true });

    const payload = { semanas_agendamiento: 12, activo: false };
    service.actualizarConfiguracion(payload).subscribe();
    const updateRequest = httpTesting.expectOne(`${environment.apiUrl}/configuracion/`);
    expect(updateRequest.request.method).toBe('PUT');
    expect(updateRequest.request.body).toEqual(payload);
    updateRequest.flush(payload);
  });
});
