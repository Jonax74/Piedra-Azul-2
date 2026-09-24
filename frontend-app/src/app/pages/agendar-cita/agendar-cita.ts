import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  imports: [FormsModule],
  selector: 'app-agendar-cita',
  styleUrl: './agendar-cita.scss',
  templateUrl: './agendar-cita.html',
})
export class AgendarCita {
  readonly specialties = ['Medicina general', 'Fisioterapia', 'Psicología'];
  readonly professionals = ['Dra. Elena Restrepo', 'Dr. Carlos Mora'];
  readonly slots = ['08:00 a. m.', '09:30 a. m.', '11:00 a. m.', '02:00 p. m.'];
  selectedSlot = '';
}
