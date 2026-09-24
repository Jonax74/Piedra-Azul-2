import { computed, inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import Keycloak, { KeycloakProfile } from 'keycloak-js';
import { environment } from '../../../environments/environment';

interface PiedrazulToken {
	preferred_username?: string;
	name?: string;
	email?: string;
	realm_access?: { roles?: string[] };
}

@Injectable({ providedIn: 'root' })
export class Auth {
	private readonly platformId = inject(PLATFORM_ID);
	private keycloak: Keycloak | null = null;
	private initialization: Promise<boolean> | null = null;

	readonly isInitialized = signal(false);
	readonly isAuthenticated = signal(false);
	readonly profile = signal<KeycloakProfile | null>(null);
	readonly username = computed(() => {
		const token = this.keycloak?.tokenParsed as PiedrazulToken | undefined;
		return token?.name || token?.preferred_username || 'Usuario';
	});
	readonly roles = computed(() => {
		const token = this.keycloak?.tokenParsed as PiedrazulToken | undefined;
		return token?.realm_access?.roles ?? [];
	});

	initialize(): Promise<boolean> {
		if (!isPlatformBrowser(this.platformId)) {
			return Promise.resolve(false);
		}

		if (this.initialization) {
			return this.initialization;
		}

		this.keycloak = new Keycloak({
			url: environment.keycloakUrl,
			realm: environment.keycloakRealm,
			clientId: environment.keycloakClientId,
		});

		this.initialization = this.keycloak.init({
			onLoad: 'check-sso',
			pkceMethod: 'S256',
			checkLoginIframe: false,
			silentCheckSsoRedirectUri: `${window.location.origin}/assets/silent-check-sso.html`,
		}).then(async (authenticated) => {
			this.isAuthenticated.set(authenticated);
			this.isInitialized.set(true);
			if (authenticated) {
				await this.loadProfile();
			}
			return authenticated;
		}).catch(() => {
			this.isInitialized.set(true);
			this.isAuthenticated.set(false);
			return false;
		});

		return this.initialization;
	}

	async login(): Promise<void> {
		await this.initialize();
		await this.keycloak?.login({ redirectUri: `${window.location.origin}/dashboard` });
	}

	async logout(): Promise<void> {
		if (this.keycloak) {
			await this.keycloak.logout({ redirectUri: `${window.location.origin}/login` });
		}
		this.profile.set(null);
		this.isAuthenticated.set(false);
	}

	async getToken(): Promise<string | null> {
		if (!this.keycloak || !this.isAuthenticated()) {
			return null;
		}

		try {
			await this.keycloak.updateToken(30);
			return this.keycloak.token ?? null;
		} catch {
			await this.logout();
			return null;
		}
	}

	hasAnyRole(requiredRoles: string[]): boolean {
		return requiredRoles.length === 0 || requiredRoles.some((role) => this.roles().includes(role));
	}

	private async loadProfile(): Promise<void> {
		try {
			this.profile.set(await this.keycloak?.loadUserProfile() ?? null);
		} catch {
			this.profile.set(null);
		}
	}
}
