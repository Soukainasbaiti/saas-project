import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { ProjectFormComponent } from './features/project-form/project-form.component';
import { ProjectDetailComponent } from './features/project-detail/project-detail.component';
import { LoginComponent } from './features/login/login.component';
import { ChangePasswordComponent } from './features/change-password/change-password.component';
import { AdminApproveComponent } from './features/admin-approve/admin-approve.component';
import { authGuard, adminGuard, loginGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // ── Publique ──────────────────────────────────────────────────
  { path: 'login',           component: LoginComponent,          canActivate: [loginGuard] },

  // ── Forcer changement MDP (accessible même avec forceChange) ─
  { path: 'change-password', component: ChangePasswordComponent, canActivate: [authGuard] },

  // ── Routes protégées ──────────────────────────────────────────
  { path: '',                component: DashboardComponent,      canActivate: [authGuard] },
  { path: 'projects/new',    component: ProjectFormComponent,    canActivate: [authGuard] },
  { path: 'projects/:id',    component: ProjectDetailComponent,  canActivate: [authGuard] },

  // ── Admin uniquement ──────────────────────────────────────────
  { path: 'admin/approve/:token', component: AdminApproveComponent, canActivate: [adminGuard] },

  // ── Fallback ──────────────────────────────────────────────────
  { path: '**', redirectTo: '' }
];
