import { Component } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-medicos',
  styleUrl: './medicos.scss',
  templateUrl: './medicos.html',
})
export class Medicos {
  readonly professionals = [
    { initials: 'ER', name: 'Dra. Elena Restrepo', specialty: 'Medicina general', tone: '' },
    { initials: 'CM', name: 'Dr. Carlos Mora', specialty: 'Fisioterapia', tone: 'blue' },
  ];
}
