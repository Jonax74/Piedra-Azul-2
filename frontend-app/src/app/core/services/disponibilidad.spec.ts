import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { DisponibilidadService } from './disponibilidad.service';

describe('DisponibilidadService', () => {
  let service: DisponibilidadService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DisponibilidadService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('loads schedules and their doctor relations', () => {
    service.getDisponibilidades().subscribe();
    service.getMedicoDisponibilidades().subscribe();

    const schedules = httpTesting.expectOne(`${environment.apiUrl}/disponibilidades/`);
    const relations = httpTesting.expectOne(`${environment.apiUrl}/medico-disponibilidades/`);
    expect(schedules.request.method).toBe('GET');
    expect(relations.request.method).toBe('GET');
    schedules.flush([]);
    relations.flush([]);
  });

  it('creates, associates and removes availability records', () => {
    const schedule = { dia_semana: 'LUNES', hora_inicio: '09:00', hora_fin: '10:00', intervalo: 30 };
    service.crearDisponibilidad(schedule).subscribe();
    const create = httpTesting.expectOne(`${environment.apiUrl}/disponibilidades/`);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(schedule);
    create.flush({ id: 2, ...schedule });

    const relation = { medico: 7, disponibilidad: 2 };
    service.asociarConMedico(relation).subscribe();
    const associate = httpTesting.expectOne(`${environment.apiUrl}/medico-disponibilidades/`);
    expect(associate.request.method).toBe('POST');
    expect(associate.request.body).toEqual(relation);
    associate.flush(relation);

    service.eliminarDisponibilidad(2).subscribe();
    const remove = httpTesting.expectOne(`${environment.apiUrl}/disponibilidades/2/`);
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
  });
});
