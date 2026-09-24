import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Auth } from '../../core/services/auth';

@Component({
  imports: [],
  selector: 'app-login',
  styleUrl: './login.scss',
  templateUrl: './login.html',
})
export class Login {
  readonly auth = inject(Auth);
  readonly returnUrl = inject(ActivatedRoute).snapshot.queryParamMap.get('returnUrl') ?? '/dashboard';
  readonly router = inject(Router);
  isLoading = false;
  errorMessage = '';

  async ngOnInit(): Promise<void> {
    if (await this.auth.initialize()) {
      await this.router.navigateByUrl(this.returnUrl);
    }
  }

  async login(): Promise<void> {
    this.isLoading = true;
    this.errorMessage = '';
    try {
      await this.auth.login();
    } catch {
      this.errorMessage = 'No fue posible iniciar la sesión. Intenta nuevamente.';
      this.isLoading = false;
    }
  }
}
