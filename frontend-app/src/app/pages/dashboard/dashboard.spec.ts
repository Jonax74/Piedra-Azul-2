import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { Dashboard } from './dashboard';
import { Auth } from '../../core/services/auth';
import { CitasService } from '../../core/services/citas.service';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;

  beforeEach(async () => {
    const auth = {
      username: () => 'Ana',
      roles: () => ['PACIENTE'],
    };
    const citasService = {
      getCitas: () => of([
        { id: 1, fecha_hora: new Date(Date.now() - 86400000).toISOString(), estado: 'PROGRAMADA', medico: 2, paciente: 3 },
        { id: 2, fecha_hora: new Date(Date.now() + 86400000).toISOString(), estado: 'CANCELADA', medico: 2, paciente: 3 },
        { id: 3, fecha_hora: new Date(Date.now() + 172800000).toISOString(), estado: 'PROGRAMADA', medico: 2, paciente: 3 },
        { id: 4, fecha_hora: new Date(Date.now() + 259200000).toISOString(), estado: 'PROGRAMADA', medico: 2, paciente: 3 },
      ]),
    };
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideRouter([]),
        { provide: Auth, useValue: auth },
        { provide: CitasService, useValue: citasService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('selects the nearest future appointment that is not cancelled', () => {
    expect(component.nextAppointment?.id).toBe(3);
    expect(component.isPatientOnly()).toBe(true);
  });
});
