import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// ── Guard routes protégées (dashboard, projects, etc.) ────────
export const authGuard: CanActivateFn = (route, state) => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  if (!auth.isTokenValid()) {
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  // Si forceChange=true → seul /change-password est accessible
  if (auth.isForceChange() && state.url !== '/change-password') {
    router.navigate(['/change-password']);
    return false;
  }

  return true;
};

// ── Guard routes admin uniquement ─────────────────────────────
export const adminGuard: CanActivateFn = (route, state) => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  if (!auth.isTokenValid()) {
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  if (!auth.isAdmin()) {
    router.navigate(['/']);
    return false;
  }

  return true;
};

// ── Guard page login (redirige si déjà connecté) ──────────────
export const loginGuard: CanActivateFn = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  if (auth.isTokenValid() && !auth.isForceChange()) {
    router.navigate(['/']);
    return false;
  }

  return true;
};
