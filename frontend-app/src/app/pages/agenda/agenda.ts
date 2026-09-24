import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  imports: [FormsModule],
  selector: 'app-agenda',
  styleUrl: './agenda.scss',
  templateUrl: './agenda.html',
})
export class Agenda {
  readonly professionals = ['Dra. Elena Restrepo', 'Dr. Carlos Mora'];
  selectedProfessional = '';
  selectedDate = '2026-09-24';
}
