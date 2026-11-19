import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Disponibilidad } from './disponibilidad';

describe('Disponibilidad', () => {
  let component: Disponibilidad;
  let fixture: ComponentFixture<Disponibilidad>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Disponibilidad],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Disponibilidad);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
