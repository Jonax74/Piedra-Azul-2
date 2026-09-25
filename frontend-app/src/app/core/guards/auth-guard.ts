import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Auth } from '../services/auth';

export const authGuard: CanActivateFn = async (_, state) => {
  const auth = inject(Auth);
  const router = inject(Router);
  const authenticated = await auth.initialize();

  return authenticated || router.createUrlTree(['/login'], {
    queryParams: { returnUrl: state.url },
  });
};
