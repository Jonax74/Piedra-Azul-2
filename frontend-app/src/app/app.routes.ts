import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { roleGuard } from './core/guards/role-guard';
import { Layout } from './layout/layout';
import { Login } from './pages/login/login';
import { Dashboard } from './pages/dashboard/dashboard';

export const routes: Routes = [
	{ path: 'login', component: Login },
	{
		path: '',
		component: Layout,
		canActivate: [authGuard],
		children: [
			{ path: 'dashboard', component: Dashboard },
			{ path: 'agenda', loadComponent: () => import('./pages/agenda/agenda').then((m) => m.Agenda), canActivate: [roleGuard], data: { roles: ['ADMIN', 'AGENDADOR'] } },
			{ path: 'agendar-cita', loadComponent: () => import('./pages/agendar-cita/agendar-cita').then((m) => m.AgendarCita), canActivate: [roleGuard], data: { roles: ['PACIENTE', 'AGENDADOR'] } },
			{ path: 'mis-citas', loadComponent: () => import('./pages/mis-citas/mis-citas').then((m) => m.MisCitas), canActivate: [roleGuard], data: { roles: ['PACIENTE'] } },
			{ path: 'medicos', loadComponent: () => import('./pages/medicos/medicos').then((m) => m.Medicos), canActivate: [roleGuard], data: { roles: ['ADMIN'] } },
			{ path: 'disponibilidad', loadComponent: () => import('./pages/disponibilidad/disponibilidad').then((m) => m.Disponibilidad), canActivate: [roleGuard], data: { roles: ['ADMIN', 'MEDICO'] } },
			{ path: 'configuracion', loadComponent: () => import('./pages/configuracion/configuracion').then((m) => m.Configuracion), canActivate: [roleGuard], data: { roles: ['ADMIN'] } },
			{ path: '', pathMatch: 'full', redirectTo: 'dashboard' },
		],
	},
	{ path: '**', redirectTo: '' },
];
