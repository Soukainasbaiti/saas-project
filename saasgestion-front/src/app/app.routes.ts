// src/app/app.routes.ts

import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { ProjectFormComponent } from './features/project-form/project-form.component';
import { ProjectDetailComponent } from './features/project-detail/project-detail.component';

export const routes: Routes = [
  { path: '',              component: DashboardComponent },
  { path: 'projects/new',  component: ProjectFormComponent },
  { path: 'projects/:id',  component: ProjectDetailComponent },
  { path: '**',            redirectTo: '' }
];