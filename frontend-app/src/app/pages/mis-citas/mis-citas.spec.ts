import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { MisCitas } from './mis-citas';
import { CitasService } from '../../core/services/citas.service';

describe('MisCitas', () => {
  let component: MisCitas;
  let fixture: ComponentFixture<MisCitas>;

  beforeEach(async () => {
    const appointments = Array.from({ length: 11 }, (_, index) => ({
      id: index + 1,
      fecha_hora: '2099-01-05T10:00:00Z',
      estado: 'PROGRAMADA',
      medico: 2,
      paciente: 3,
    }));
    await TestBed.configureTestingModule({
      imports: [MisCitas],
      providers: [
        provideRouter([]),
        { provide: CitasService, useValue: { getCitas: () => of(appointments) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MisCitas);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('loads appointments and paginates the personal list', () => {
    expect(component.appointments).toHaveLength(11);
    expect(component.totalPages).toBe(2);
    expect(component.paginatedAppointments).toHaveLength(10);
    component.nextPage();
    expect(component.currentPage).toBe(2);
    expect(component.paginatedAppointments).toHaveLength(1);
  });
});
