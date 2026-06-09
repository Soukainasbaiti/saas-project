import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  forceChange: boolean;
  role: string;
  fullName: string;
}

export interface TokenPayload {
  sub: string;
  email: string;
  role: string;
  forceChange: boolean;
  exp: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly BASE = '/api';
  private readonly ACCESS_KEY  = 'access_token';
  private readonly REFRESH_KEY = 'refresh_token';
  private readonly USER_KEY    = 'user_info';

  // Signal réactif pour l'état de connexion
  private _currentUser = signal<{ fullName: string; role: string; email: string } | null>(
    this.loadUserFromStorage()
  );

  currentUser  = this._currentUser.asReadonly();
  isLoggedIn   = computed(() => this._currentUser() !== null);
  isAdmin      = computed(() => this._currentUser()?.role === 'ADMIN');

  constructor(private http: HttpClient, private router: Router) {}

  // ── Login ─────────────────────────────────────────────────────
  login(req: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.BASE}/auth/login`, req).pipe(
      tap(res => {
        localStorage.setItem(this.ACCESS_KEY,  res.accessToken);
        localStorage.setItem(this.REFRESH_KEY, res.refreshToken);
        localStorage.setItem(this.USER_KEY, JSON.stringify({
          fullName: res.fullName,
          role:     res.role,
          email:    this.decodeToken(res.accessToken)?.email ?? ''
        }));
        this._currentUser.set({
          fullName: res.fullName,
          role:     res.role,
          email:    this.decodeToken(res.accessToken)?.email ?? ''
        });
      })
    );
  }

  // ── Change password ───────────────────────────────────────────
  changePassword(newPassword: string): Observable<any> {
    return this.http.post(`${this.BASE}/auth/change-password`, { newPassword });
  }

  // ── Refresh token ─────────────────────────────────────────────
  refreshToken(): Observable<LoginResponse> {
    const refreshToken = localStorage.getItem(this.REFRESH_KEY) ?? '';
    return this.http.post<LoginResponse>(`${this.BASE}/auth/refresh`, { refreshToken }).pipe(
      tap(res => {
        localStorage.setItem(this.ACCESS_KEY,  res.accessToken);
        localStorage.setItem(this.REFRESH_KEY, res.refreshToken);
      })
    );
  }

  // ── Logout ────────────────────────────────────────────────────
  logout(): void {
    localStorage.removeItem(this.ACCESS_KEY);
    localStorage.removeItem(this.REFRESH_KEY);
    localStorage.removeItem(this.USER_KEY);
    this._currentUser.set(null);
    this.router.navigate(['/login']);
  }

  // ── Getters token ─────────────────────────────────────────────
  getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_KEY);
  }

  isTokenValid(): boolean {
    const token = this.getAccessToken();
    if (!token) return false;
    const payload = this.decodeToken(token);
    if (!payload) return false;
    return payload.exp * 1000 > Date.now();
  }

  isForceChange(): boolean {
    const token = this.getAccessToken();
    if (!token) return false;
    return this.decodeToken(token)?.forceChange === true;
  }

  // ── Décode JWT sans librairie ─────────────────────────────────
  decodeToken(token: string): TokenPayload | null {
    try {
      const base64 = token.split('.')[1];
      const json   = atob(base64.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(json) as TokenPayload;
    } catch {
      return null;
    }
  }

  // ── Chargement depuis localStorage au démarrage ───────────────
  private loadUserFromStorage(): { fullName: string; role: string; email: string } | null {
    try {
      const raw = localStorage.getItem(this.USER_KEY);
      if (!raw) return null;
      // Vérifier que le token est encore valide
      const token = localStorage.getItem(this.ACCESS_KEY);
      if (!token) return null;
      const payload = this.decodeToken(token);
      if (!payload || payload.exp * 1000 < Date.now()) {
        localStorage.clear();
        return null;
      }
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }
}
