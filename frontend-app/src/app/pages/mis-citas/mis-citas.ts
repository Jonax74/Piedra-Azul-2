import { Component } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { inject } from '@angular/core';
import { CitasService } from '../../core/services/citas.service';
import { Cita } from '../../shared/models/cita.model';

@Component({
  imports: [RouterLink, DatePipe],
  selector: 'app-mis-citas',
  styleUrl: './mis-citas.scss',
  templateUrl: './mis-citas.html',
})
export class MisCitas {
  private readonly citasService = inject(CitasService);
  appointments: Cita[] = [];
  feedback = '';

  private sortAppointments(appointments: Cita[]): Cita[] {
    return [...appointments].sort((left, right) => {
      const leftTime = Date.parse(left.fecha_hora);
      const rightTime = Date.parse(right.fecha_hora);
      return leftTime - rightTime || (left.id ?? 0) - (right.id ?? 0);
    });
  }

  ngOnInit(): void {
    this.citasService.getCitas().subscribe({
<<<<<<< Updated upstream
      next: (items) => this.appointments = items,
      error: () => this.feedback = 'No fue posible cargar tus citas.',
=======
      next: (items) => { this.appointments = this.sortAppointments(items); this.currentPage = 1; this.changeDetector.markForCheck(); },
      error: () => { this.feedback = 'No fue posible cargar tus citas.'; this.changeDetector.markForCheck(); },
>>>>>>> Stashed changes
    });
  }
}
