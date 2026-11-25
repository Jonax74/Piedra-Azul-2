import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Configuracion } from './configuracion';
import { ConfiguracionService } from '../../core/services/configuracion.service';

describe('Configuracion', () => {
  let component: Configuracion;
  let fixture: ComponentFixture<Configuracion>;
  let configuracionService: {
    getConfiguracion: ReturnType<typeof vi.fn>;
    actualizarConfiguracion: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    configuracionService = {
      getConfiguracion: vi.fn().mockReturnValue(of({ semanas_agendamiento: 6, activo: true })),
      actualizarConfiguracion: vi.fn().mockReturnValue(of({ semanas_agendamiento: 6, activo: true })),
    };
    await TestBed.configureTestingModule({
      imports: [Configuracion],
      providers: [{ provide: ConfiguracionService, useValue: configuracionService }],
    }).compileComponents();

    fixture = TestBed.createComponent(Configuracion);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('loads the persisted configuration', () => {
    expect(configuracionService.getConfiguracion).toHaveBeenCalledOnce();
    expect(component.bookingWeeks).toBe(6);
    expect(component.lastSavedWeeks).toBe(6);
    expect(component.isLoading).toBe(false);
  });

  it('rejects values outside the supported week range without saving', () => {
    component.bookingWeeks = 53;

    component.guardar();

    expect(component.fieldError).toBe('Usa un número entero entre 1 y 52.');
    expect(configuracionService.actualizarConfiguracion).not.toHaveBeenCalled();
  });

  it('saves the selected value and updates the reset value', () => {
    component.bookingWeeks = 12;
    component.autonomousBooking = false;

    component.guardar();

    expect(configuracionService.actualizarConfiguracion).toHaveBeenCalledWith({
      semanas_agendamiento: 12,
      activo: false,
    });
    expect(component.lastSavedWeeks).toBe(12);
    expect(component.feedback).toBe('Configuración guardada correctamente.');
    expect(component.isSaving).toBe(false);
  });
});
