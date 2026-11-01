import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { timeout } from 'rxjs';
import { ApiService } from '../../core/services/api.service';

interface RegistroPacienteResponse {
	detail: string;
	username: string;
}

interface RegistroError {
	detail?: unknown;
	[key: string]: unknown;
}

@Component({
	imports: [FormsModule, RouterLink],
	selector: 'app-registro-paciente',
	styleUrl: './registro-paciente.scss',
	templateUrl: './registro-paciente.html',
})
export class RegistroPaciente {
	private readonly api = inject(ApiService);
	private readonly router = inject(Router);
	readonly maxBirthDate = new Date().toISOString().slice(0, 10);
	isSubmitting = false;
	errorMessage = '';
	readonly form = {
		username: '',
		password: '',
		passwordConfirmation: '',
		persona: {
			primer_nombre: '',
			segundo_nombre: '',
			primer_apellido: '',
			segundo_apellido: '',
			genero: '',
			fecha_nacimiento: '',
			telefono: '',
			dni: null as number | null,
			correo: '',
		},
	};

	register(): void {
		this.errorMessage = '';
		this.isSubmitting = true;
		this.api.post<RegistroPacienteResponse>('/registro/', {
			username: this.form.username.trim(),
			password: this.form.password,
			password_confirmation: this.form.passwordConfirmation,
			persona: {
				...this.form.persona,
				segundo_nombre: this.form.persona.segundo_nombre.trim() || null,
				segundo_apellido: this.form.persona.segundo_apellido.trim() || null,
			},
		}).pipe(timeout({ first: 20000 })).subscribe({
			next: () => {
				this.isSubmitting = false;
				this.form.password = '';
				this.form.passwordConfirmation = '';
				void this.router.navigate(['/login'], {
					queryParams: {
						registro: 'exitoso',
						usuario: this.form.username.trim(),
					},
				});
			},
			error: (error: unknown) => {
				this.errorMessage = this.errorMessageFrom(error);
				this.isSubmitting = false;
			},
		});
	}

	clearError(): void {
		this.errorMessage = '';
	}

	private errorMessageFrom(error: unknown): string {
		if (error instanceof HttpErrorResponse) {
			const response = error.error as RegistroError | null;
			const messages = response ? this.collectMessages(response) : [];
			if (messages.length) return messages[0];
			if (error.status === 0) {
				return 'No se pudo conectar con el servidor. Verifica que el backend esté iniciado e inténtalo nuevamente.';
			}
			if (error.status >= 500) {
				return 'Ocurrió un problema al guardar tu cuenta. Inténtalo de nuevo más tarde o contacta al administrador.';
			}
		}

		if (error instanceof Error && error.name === 'TimeoutError') {
			return 'El registro tardó demasiado y no recibimos respuesta. Verifica que Keycloak y el backend estén activos antes de volver a intentarlo.';
		}

		return 'Revisa los datos ingresados e inténtalo nuevamente.';
	}

	private collectMessages(value: unknown): string[] {
		if (typeof value === 'string') return [value];
		if (Array.isArray(value)) return value.flatMap((item) => this.collectMessages(item));
		if (value && typeof value === 'object') {
			return Object.values(value).flatMap((item) => this.collectMessages(item));
		}
		return [];
	}
}
