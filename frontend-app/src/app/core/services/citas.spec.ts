import { TestBed } from '@angular/core/testing';
import { Citas } from './citas';

describe('Citas', () => {
  let service: Citas;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Citas);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
