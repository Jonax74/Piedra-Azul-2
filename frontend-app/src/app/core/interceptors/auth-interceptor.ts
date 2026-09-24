import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { Auth } from '../services/auth';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(Auth);

  if (!req.url.includes('/api/')) {
    return next(req);
  }

  return from(auth.getToken()).pipe(
    switchMap((token) => token
      ? next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }))
      : next(req)),
  );
};
