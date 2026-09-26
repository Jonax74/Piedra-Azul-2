import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { MedicosService } from './medicos.service';

describe('MedicosService', () => {
  let service: MedicosService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MedicosService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('shares the doctors request between subscribers', () => {
    const first = vi.fn();
    const second = vi.fn();
    service.getMedicos().subscribe(first);
    service.getMedicos().subscribe(second);

    const request = httpTesting.expectOne(`${environment.apiUrl}/medicos/`);
    expect(request.request.method).toBe('GET');
    request.flush([{ persona: 7, estado: 'ACTIVO' }]);
    expect(first).toHaveBeenCalledWith([{ persona: 7, estado: 'ACTIVO' }]);
    expect(second).toHaveBeenCalledWith([{ persona: 7, estado: 'ACTIVO' }]);
  });

  it('creates and updates doctors through the expected endpoints', () => {
    const payload = { persona: 7, tipo_profesional: 'MEDICO' };
    service.crearMedico(payload).subscribe();
    service.actualizarMedico(7, payload).subscribe();

    const create = httpTesting.expectOne(
      (request) => request.url === `${environment.apiUrl}/medicos/`
        && request.method === 'POST',
    );
    expect(create.request.body).toEqual(payload);
    create.flush(payload);

    const update = httpTesting.expectOne(
      (request) => request.url === `${environment.apiUrl}/medicos/7/`
        && request.method === 'PUT',
    );
    expect(update.request.body).toEqual(payload);
    update.flush(payload);
  });
});
