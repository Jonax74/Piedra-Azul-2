import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AgendarCita } from './agendar-cita';

describe('AgendarCita', () => {
  let component: AgendarCita;
  let fixture: ComponentFixture<AgendarCita>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgendarCita],
    }).compileComponents();

    fixture = TestBed.createComponent(AgendarCita);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
