import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { ProjectFormComponent } from './features/project-form/project-form.component';
import { ProjectDetailComponent } from './features/project-detail/project-detail.component';
import { LoginComponent } from './features/login/login.component';
import { ChangePasswordComponent } from './features/change-password/change-password.component';
import { AdminApproveComponent } from './features/admin-approve/admin-approve.component';
import { ProjectEditComponent } from './features/project-edit/project-edit.component';
import { AdminDashboardComponent } from './features/admin-dashboard/admin-dashboard.component';
import { AdminUsersComponent } from './features/admin-users/admin-users.component';
import { AdminRefDataComponent } from './features/admin-ref-data/admin-ref-data.component';
import { ProjectManagementComponent } from './features/project-management/project-management.component';
import { authGuard, adminGuard, loginGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // ── Publique ──────────────────────────────────────────────────
  { path: 'login',           component: LoginComponent,          canActivate: [loginGuard] },

  // ── Forcer changement MDP ─────────────────────────────────────
  { path: 'change-password', component: ChangePasswordComponent, canActivate: [authGuard] },

  // ── Dashboard admin ───────────────────────────────────────────
  { path: 'admin',           component: AdminDashboardComponent, canActivate: [adminGuard] },
  { path: 'admin/users',     component: AdminUsersComponent,     canActivate: [adminGuard] },
  { path: 'admin/ref-data', component: AdminRefDataComponent,   canActivate: [adminGuard] },

  // ── Routes protégées ──────────────────────────────────────────
  { path: '',                component: DashboardComponent,      canActivate: [authGuard] },
  { path: 'projects/new',    component: ProjectFormComponent,    canActivate: [authGuard] },
  { path: 'projects/:id',            component: ProjectDetailComponent,    canActivate: [authGuard] },
  { path: 'projects/:id/management', component: ProjectManagementComponent, canActivate: [authGuard] },

  // ── Admin approbation (public — le token email fait office d'autorisation) ──
  { path: 'admin/approve/:token',  component: AdminApproveComponent },

  // ── Modification projet rejeté ────────────────────────────────
  { path: 'projects/edit/:token',  component: ProjectEditComponent,  canActivate: [authGuard] },

  // ── Fallback ──────────────────────────────────────────────────
  { path: '**', redirectTo: '' }
];
