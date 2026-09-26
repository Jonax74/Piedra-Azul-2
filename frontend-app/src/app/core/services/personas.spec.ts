import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { PersonasService } from './personas.service';

describe('PersonasService', () => {
  let service: PersonasService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PersonasService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('shares patient and person lookups through their respective endpoints', () => {
    const patients = vi.fn();
    const people = vi.fn();
    service.getPacientes().subscribe(patients);
    service.getPersonas().subscribe(people);

    const patientRequest = httpTesting.expectOne(`${environment.apiUrl}/pacientes/`);
    const peopleRequest = httpTesting.expectOne(`${environment.apiUrl}/personas/`);
    expect(patientRequest.request.method).toBe('GET');
    expect(peopleRequest.request.method).toBe('GET');
    patientRequest.flush([]);
    peopleRequest.flush([]);
    expect(patients).toHaveBeenCalledWith([]);
    expect(people).toHaveBeenCalledWith([]);
  });
});
