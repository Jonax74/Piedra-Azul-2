import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Auth } from '../../../core/services/auth';

@Component({
  imports: [RouterLink],
  selector: 'app-registrar-paciente-link',
  styleUrl: './registrar-paciente-link.scss',
  templateUrl: './registrar-paciente-link.html',
})
export class RegistrarPacienteLink {
  private readonly auth = inject(Auth);

  get isScheduler(): boolean {
    return this.auth.hasAnyRole(['AGENDADOR']);
  }
}
