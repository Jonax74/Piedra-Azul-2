import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Auth } from '../services/auth';

export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(Auth);
  const router = inject(Router);
  const roles = route.data['roles'] as string[] | undefined;

  return auth.hasAnyRole(roles ?? []) || router.createUrlTree(['/dashboard']);
};
