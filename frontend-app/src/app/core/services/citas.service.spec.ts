import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { CitasService } from './citas.service';

describe('CitasService', () => {
	let service: CitasService;
	let httpTesting: HttpTestingController;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [provideHttpClient(), provideHttpClientTesting()],
		});
		service = TestBed.inject(CitasService);
		httpTesting = TestBed.inject(HttpTestingController);
	});

	afterEach(() => httpTesting.verify());

	it('requests the authenticated doctor agenda with only the optional date filter', () => {
		service.getAgendaMedico('2026-10-12').subscribe();

		const request = httpTesting.expectOne(
			`${environment.apiUrl}/citas/medico/agenda/?fecha=2026-10-12`,
		);
		expect(request.request.method).toBe('GET');
		request.flush({ cantidad: 0, resultados: [] });
	});

	it('requests appointment dates for a month and optional selected professional', () => {
		service.getFechasAgenda('2026-10', 12).subscribe((response) => {
			expect(response.fechas).toEqual(['2026-10-05']);
		});

		const request = httpTesting.expectOne(
			`${environment.apiUrl}/citas/agenda/fechas/?mes=2026-10&medico=12`,
		);
		expect(request.request.method).toBe('GET');
		request.flush({ mes: '2026-10', fechas: ['2026-10-05'] });
	});

	it('requests a CSV export for the selected professional and date', () => {
		service.exportarAgenda(12, '2026-10-05').subscribe((file) => {
			expect(file).toBeInstanceOf(Blob);
		});

		const request = httpTesting.expectOne(
			`${environment.apiUrl}/citas/agenda/exportar/?medico=12&fecha=2026-10-05`,
		);
		expect(request.request.method).toBe('GET');
		expect(request.request.responseType).toBe('blob');
		request.flush(new Blob(['Fecha,Hora']));
	});

	it('requests a CSV export for the authenticated doctor and selected date', () => {
		service.exportarAgendaMedico('2026-10-12').subscribe((file) => {
			expect(file).toBeInstanceOf(Blob);
		});

		const request = httpTesting.expectOne(
			`${environment.apiUrl}/citas/medico/agenda/exportar/?fecha=2026-10-12`,
		);
		expect(request.request.method).toBe('GET');
		expect(request.request.responseType).toBe('blob');
		request.flush(new Blob(['Fecha;Hora']));
	});

	it('requests patient appointment history by national ID', () => {
		service.getHistorialPaciente('10000001').subscribe((history) => {
			expect(history.paciente.nombre).toBe('Ana Paciente');
			expect(history.citas).toHaveLength(1);
		});

		const request = httpTesting.expectOne(
			`${environment.apiUrl}/citas/agenda/paciente/?cedula=10000001`,
		);
		expect(request.request.method).toBe('GET');
		request.flush({
			paciente: {
				nombre: 'Ana Paciente',
				cedula: 10000001,
				fecha_nacimiento: '1992-01-01',
				genero: 'Mujer',
				telefono: '3000000000',
				correo: null,
			},
			cantidad: 1,
			citas: [{
				id: 1,
				fecha_hora: '2099-01-05T05:00:00-05:00',
				medico: 'Carlos Medico',
				tipo_profesional: 'MEDICO',
				estado: 'Programada',
			}],
		});
	});

	it('updates appointment status and schedule with PATCH', () => {
		service.actualizarEstadoCita(14, {
			estado: 'REAGENDADA',
			fecha_hora: '2026-10-12T10:00:00-05:00',
		}).subscribe((appointment) => {
			expect(appointment.estado).toBe('REAGENDADA');
		});

		const request = httpTesting.expectOne(
			`${environment.apiUrl}/citas/14/`,
		);
		expect(request.request.method).toBe('PATCH');
		expect(request.request.body).toEqual({
			estado: 'REAGENDADA',
			fecha_hora: '2026-10-12T10:00:00-05:00',
		});
		request.flush({
			id: 14,
			paciente: 8,
			medico: 4,
			fecha_hora: '2026-10-12T10:00:00-05:00',
			estado: 'REAGENDADA',
		});
	});

	it('requests only dates with available slots for a professional', () => {
		service.getFechasDisponibles(14, '2026-10').subscribe((response) => {
			expect(response.fechas).toEqual(['2026-10-12']);
		});

		const request = httpTesting.expectOne(
			`${environment.apiUrl}/citas/disponibles/fechas/?medico=14&mes=2026-10`,
		);
		expect(request.request.method).toBe('GET');
		request.flush({
			medico: 14,
			mes: '2026-10',
			fechas: ['2026-10-12'],
		});
	});

});
