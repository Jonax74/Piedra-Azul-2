import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  imports: [FormsModule],
  selector: 'app-disponibilidad',
  styleUrl: './disponibilidad.scss',
  templateUrl: './disponibilidad.html',
})
export class Disponibilidad {
  readonly weekdays = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes'];
}
