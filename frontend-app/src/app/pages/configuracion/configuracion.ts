import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  imports: [FormsModule],
  selector: 'app-configuracion',
  styleUrl: './configuracion.scss',
  templateUrl: './configuracion.html',
})
export class Configuracion {
  bookingWeeks = 4;
  autonomousBooking = true;
  automaticConfirmation = true;
}
