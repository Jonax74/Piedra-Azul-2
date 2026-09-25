import { TestBed } from '@angular/core/testing';
import { Medicos } from './medicos';

describe('Medicos', () => {
  let service: Medicos;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Medicos);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
