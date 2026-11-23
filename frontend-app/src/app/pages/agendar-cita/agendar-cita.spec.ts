import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { CitasService } from '../../core/services/citas.service';
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

  it('closes the other search menus when one selector opens', () => {
    component.patientDropdownOpen = true;
    component.professionalDropdownOpen = true;
    component.openSpecialtyDropdown();
    expect(component.patientDropdownOpen).toBe(false);
    expect(component.professionalDropdownOpen).toBe(false);
    expect(component.specialtyDropdownOpen).toBe(true);

    component.openPatientDropdown();
    expect(component.specialtyDropdownOpen).toBe(false);
    expect(component.patientDropdownOpen).toBe(true);

    component.openProfessionalDropdown();
    expect(component.patientDropdownOpen).toBe(false);
    expect(component.professionalDropdownOpen).toBe(true);
  });

  it('reloads available slots when the professional changes and the date is still valid', () => {
    const date = '2026-11-23';
    const getSlots = vi.spyOn(TestBed.inject(CitasService), 'getFranjasDisponibles')
      .mockReturnValue(of({ medico: 10, fecha: date, cantidad: 0, franjas: [] }));
    vi.spyOn(component, 'isDateSelectable').mockReturnValue(true);
    component.selectedDate = date;

    component.selectProfessional({
      persona: 10,
      tipo_profesional: 'MEDICO',
      estado: 'ACTIVO',
    });

    expect(getSlots).toHaveBeenCalledWith(10, date);
  });

  it('shows a helpful message when the selected professional slot was just booked', () => {
    vi.spyOn(component, 'dateValidationMessage').mockReturnValue('');
    vi.spyOn(TestBed.inject(CitasService), 'crearCita').mockReturnValue(
      throwError(() => ({
        error: {
          non_field_errors: ['The fields medico, fecha_hora must make a unique set.'],
        },
      })),
    );
    component.selectedPatient = '1';
    component.selectedProfessional = '10';
    component.selectedDate = '2099-01-05';
    component.selectedSlot = '2099-01-05T10:00:00-05:00';

    component.book();

    expect(component.feedback).toBe(
      'Este horario ya fue reservado para el médico seleccionado. Elige otro horario disponible.',
    );
  });
});
