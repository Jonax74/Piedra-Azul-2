import { TestBed } from '@angular/core/testing';
import { Disponibilidad } from './disponibilidad';

describe('Disponibilidad', () => {
  let service: Disponibilidad;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Disponibilidad);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
