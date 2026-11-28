import { routes } from './app.routes';

describe('application routes', () => {
  it('restricts availability management to administrators', () => {
    const layoutRoute = routes.find((route) => route.path === '');
    const availabilityRoute = layoutRoute?.children?.find(
      (route) => route.path === 'disponibilidad',
    );

    expect(availabilityRoute?.data?.['roles']).toEqual(['ADMIN']);
  });
});
