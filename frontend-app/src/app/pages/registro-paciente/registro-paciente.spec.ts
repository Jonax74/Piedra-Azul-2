import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { RegistroPaciente } from './registro-paciente';

describe('RegistroPaciente', () => {
	let httpTesting: HttpTestingController;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [RegistroPaciente],
			providers: [
				provideHttpClient(),
				provideHttpClientTesting(),
				provideRouter([]),
			],
		}).compileComponents();

		httpTesting = TestBed.inject(HttpTestingController);
	});

	afterEach(() => httpTesting.verify());

	it('submits the account and patient data to the public registration endpoint', () => {
		const fixture = TestBed.createComponent(RegistroPaciente);
		const component = fixture.componentInstance;
		const router = TestBed.inject(Router);
		const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
		component.form.username = 'paciente.ana';
		component.form.password = 'ClaveSegura123';
		component.form.passwordConfirmation = 'ClaveSegura123';
		component.form.persona = {
			primer_nombre: 'Ana',
			segundo_nombre: '',
			primer_apellido: 'Torres',
			segundo_apellido: '',
			genero: 'MUJER',
			fecha_nacimiento: '1995-04-20',
			telefono: '3001234567',
			dni: 987654321,
			correo: 'ana@example.com',
		};

		component.register();

		const request = httpTesting.expectOne(`${environment.apiUrl}/registro/`);
		expect(request.request.method).toBe('POST');
		expect(request.request.body.username).toBe('paciente.ana');
		expect(request.request.body.password_confirmation).toBe('ClaveSegura123');
		expect(request.request.body.persona.dni).toBe(987654321);
		request.flush({
			detail: 'Cuenta creada.',
			username: 'paciente.ana',
		});

		expect(component.form.password).toBe('');
		expect(navigate).toHaveBeenCalledWith(
			['/login'],
			{
				queryParams: {
					registro: 'exitoso',
					usuario: 'paciente.ana',
				},
			},
		);
	});

	it('shows the backend reason when registration is rejected', () => {
		const fixture = TestBed.createComponent(RegistroPaciente);
		const component = fixture.componentInstance;
		component.form.username = 'paciente.ana';
		component.form.password = 'ClaveSegura123';
		component.form.passwordConfirmation = 'ClaveSegura123';
		component.register();

		const request = httpTesting.expectOne(`${environment.apiUrl}/registro/`);
		request.flush(
			{ password: ['La contraseña debe tener al menos 8 caracteres.'] },
			{ status: 400, statusText: 'Bad Request' },
		);

		expect(component.errorMessage).toBe(
			'La contraseña debe tener al menos 8 caracteres.',
		);
		expect(component.isSubmitting).toBe(false);
	});

	it('stops waiting and reports a clear timeout message', async () => {
		const fixture = TestBed.createComponent(RegistroPaciente);
		const component = fixture.componentInstance;
		component.form.username = 'paciente.ana';
		component.form.password = 'ClaveSegura123';
		component.form.passwordConfirmation = 'ClaveSegura123';
		component.register();

		const request = httpTesting.expectOne(`${environment.apiUrl}/registro/`);
		await new Promise((resolve) => setTimeout(resolve, 20010));
		httpTesting.expectNone(`${environment.apiUrl}/registro/`);

		expect(request.cancelled).toBe(true);
		expect(component.isSubmitting).toBe(false);
		expect(component.errorMessage).toContain('tardó demasiado');
	}, 25000);
});
