import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ConfiguracionService } from '../../core/services/configuracion.service';

@Component({
  imports: [FormsModule],
  selector: 'app-configuracion',
  styleUrl: './configuracion.scss',
  templateUrl: './configuracion.html',
})
export class Configuracion {
  private readonly configuracionService = inject(ConfiguracionService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  bookingWeeks = 4;
  autonomousBooking = true;
  automaticConfirmation = true;
  isLoading = true;
  isSaving = false;
  feedback = '';

  ngOnInit(): void {
    this.configuracionService.getConfiguracion().subscribe({
      next: (configuracion) => {
        this.bookingWeeks = configuracion.semanas_agendamiento;
        this.isLoading = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.feedback = 'No fue posible cargar la configuración.';
        this.isLoading = false;
        this.changeDetector.markForCheck();
      },
    });
  }

  guardar(): void {
    this.isSaving = true;
    this.feedback = '';
    this.configuracionService.actualizarConfiguracion({
      semanas_agendamiento: this.bookingWeeks,
      activo: true,
    }).subscribe({
      next: () => {
        this.feedback = 'Configuración guardada correctamente.';
        this.isSaving = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.feedback = 'No fue posible guardar la configuración.';
        this.isSaving = false;
        this.changeDetector.markForCheck();
      },
    });
  }
}
