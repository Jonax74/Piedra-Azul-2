import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Auth } from './auth';

describe('Auth', () => {
  let service: Auth;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: PLATFORM_ID, useValue: 'server' }],
    });
    service = TestBed.inject(Auth);
  });

  it('does not initialize Keycloak during server rendering', async () => {
    await expect(service.initialize()).resolves.toBe(false);
    expect(service.isAuthenticated()).toBe(false);
  });

  it('allows empty role requirements and rejects unmatched roles', () => {
    expect(service.hasAnyRole([])).toBe(true);
    expect(service.hasAnyRole(['ADMIN'])).toBe(false);
  });
});
