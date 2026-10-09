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

});
