import { Component } from '@angular/core';
import { inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Auth } from '../core/services/auth';

@Component({
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  selector: 'app-layout',
  styleUrl: './layout.scss',
  templateUrl: './layout.html',
})
export class Layout {
  readonly auth = inject(Auth);

  readonly navigation = [
    { label: 'Resumen', path: '/dashboard', roles: [] },
    { label: 'Agendar cita', path: '/agendar-cita', roles: ['PACIENTE', 'AGENDADOR'] },
    { label: 'Mis citas', path: '/mis-citas', roles: ['PACIENTE'] },
    { label: 'Agenda', path: '/agenda', roles: ['ADMIN', 'AGENDADOR'] },
    { label: 'Médicos', path: '/medicos', roles: ['ADMIN'] },
    { label: 'Disponibilidad', path: '/disponibilidad', roles: ['ADMIN', 'MEDICO'] },
    { label: 'Configuración', path: '/configuracion', roles: ['ADMIN'] },
  ];

  isVisible(roles: string[]): boolean {
    return this.auth.hasAnyRole(roles);
  }

  logout(): void {
    void this.auth.logout();
  }
}
