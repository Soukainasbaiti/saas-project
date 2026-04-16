import { Component, OnInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../core/services/api.service';
import { AdminSidebarComponent } from '../admin-sidebar/admin-sidebar.component';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, FormsModule, MatSnackBarModule, AdminSidebarComponent],
  templateUrl: './admin-users.component.html',
  styleUrls: ['./admin-users.component.scss']
})
export class AdminUsersComponent implements OnInit {

  users: any[] = [];
  loading = true;
  showForm   = false;
  saving     = false;

  searchText   = '';
  filterRole   = '';
  filterStatus = '';

  get filteredUsers(): any[] {
    return this.users.filter(u => {
      const search = this.searchText.toLowerCase();
      const matchSearch = !search ||
        u.fullName?.toLowerCase().includes(search) ||
        u.email?.toLowerCase().includes(search);
      const matchRole = !this.filterRole || u.role === this.filterRole;
      const matchStatus = !this.filterStatus ||
        (this.filterStatus === 'actif' && u.isActive) ||
        (this.filterStatus === 'inactif' && !u.isActive);
      return matchSearch && matchRole && matchStatus;
    });
  }

  // Edit
  showEditModal = false;
  editTarget: any = null;
  editSaving = false;
  editForm: FormGroup;

  // Delete
  showDeleteModal = false;
  deleteTarget: any = null;
  deleteSaving = false;

  form: FormGroup;

  private segulaEmailValidator = (c: AbstractControl): ValidationErrors | null => {
    const v: string = (c.value || '').toLowerCase();
    const allowed = ['@segulagrp.com', '@segula.com', '@segula.fr'];
    if (!v) return null;
    return allowed.some(d => v.endsWith(d)) ? null : { segulaEmail: true };
  };

  constructor(
    private api: ApiService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
    private snack: MatSnackBar
  ) {
    this.form = this.fb.group({
      fullName:   ['', Validators.required],
      email:      ['', [Validators.required, Validators.email, this.segulaEmailValidator]],
      password:   ['', [Validators.required, Validators.minLength(6)]],
      role:       ['PM', Validators.required],
      customRole: ['']
    });

    this.editForm = this.fb.group({
      fullName:   ['', Validators.required],
      email:      ['', [Validators.required, Validators.email, this.segulaEmailValidator]],
      role:       ['PM', Validators.required],
      isActive:   [true, Validators.required],
      customRole: ['']
    });
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.api.getUsers().subscribe({
      next: u => { this.users = u; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.loading = false; this.cdr.markForCheck(); }
    });
  }

  // ── Create ─────────────────────────────────────────────────────
  openForm(): void {
    this.form.reset({ role: 'PM' });
    this.showForm = true;
    this.cdr.markForCheck();
  }

  closeForm(): void {
    this.showForm = false;
    this.cdr.markForCheck();
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); this.cdr.markForCheck(); return; }
    const role = this.resolveRole(this.form);
    if (!role) {
      this.form.get('customRole')?.setErrors({ required: true });
      this.form.get('customRole')?.markAsTouched();
      this.cdr.markForCheck();
      return;
    }
    this.saving = true;
    this.cdr.markForCheck();
    const payload = { ...this.form.value, role };
    this.api.createUser(payload).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.snack.open('Utilisateur créé avec succès.', 'OK', { duration: 3000 });
        this.loadUsers();
      },
      error: err => {
        this.saving = false;
        this.cdr.markForCheck();
        this.snack.open(err.error?.error || 'Erreur lors de la création.', 'Fermer', { duration: 5000 });
      }
    });
  }

  // ── Helpers rôle personnalisé ──────────────────────────────────
  get isCustomRole(): boolean {
    return this.form.get('role')?.value === 'AUTRE';
  }

  get isEditCustomRole(): boolean {
    return this.editForm.get('role')?.value === 'AUTRE';
  }

  private resolveRole(formGroup: FormGroup): string {
    const role = formGroup.get('role')?.value;
    if (role === 'AUTRE') {
      return (formGroup.get('customRole')?.value || '').trim().toUpperCase();
    }
    return role;
  }

  // ── Edit ───────────────────────────────────────────────────────
  openEdit(user: any): void {
    this.editTarget = user;
    const knownRoles = ['PM', 'BUM', 'ADMIN'];
    const isKnown = knownRoles.includes(user.role);
    this.editForm.reset({
      fullName:   user.fullName,
      email:      user.email,
      role:       isKnown ? user.role : 'AUTRE',
      isActive:   user.isActive,
      customRole: isKnown ? '' : user.role
    });
    this.showEditModal = true;
    this.cdr.markForCheck();
  }

  closeEdit(): void {
    this.showEditModal = false;
    this.editTarget = null;
    this.cdr.markForCheck();
  }

  submitEdit(): void {
    if (this.editForm.invalid) { this.editForm.markAllAsTouched(); this.cdr.markForCheck(); return; }
    const role = this.resolveRole(this.editForm);
    if (!role) {
      this.editForm.get('customRole')?.setErrors({ required: true });
      this.editForm.get('customRole')?.markAsTouched();
      this.cdr.markForCheck();
      return;
    }
    this.editSaving = true;
    this.cdr.markForCheck();
    const payload = { ...this.editForm.value, role };
    this.api.updateUser(this.editTarget.id, payload).subscribe({
      next: () => {
        this.editSaving = false;
        this.showEditModal = false;
        this.snack.open('Utilisateur modifié avec succès.', 'OK', { duration: 3000 });
        this.loadUsers();
      },
      error: err => {
        this.editSaving = false;
        this.cdr.markForCheck();
        this.snack.open(err.error?.error || 'Erreur lors de la modification.', 'Fermer', { duration: 5000 });
      }
    });
  }

  // ── Delete ─────────────────────────────────────────────────────
  openDelete(user: any): void {
    this.deleteTarget = user;
    this.showDeleteModal = true;
    this.cdr.markForCheck();
  }

  closeDelete(): void {
    this.showDeleteModal = false;
    this.deleteTarget = null;
    this.cdr.markForCheck();
  }

  confirmDelete(): void {
    this.deleteSaving = true;
    this.cdr.markForCheck();
    this.api.deleteUser(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteSaving = false;
        this.showDeleteModal = false;
        this.snack.open('Utilisateur supprimé.', 'OK', { duration: 3000 });
        this.loadUsers();
      },
      error: err => {
        this.deleteSaving = false;
        this.cdr.markForCheck();
        this.snack.open(err.error?.error || 'Erreur lors de la suppression.', 'Fermer', { duration: 5000 });
      }
    });
  }

  // ── Helpers ────────────────────────────────────────────────────
  roleLabel(r: string): string {
    const m: Record<string, string> = { ADMIN: 'Administrateur', PM: 'Project Manager', BUM: 'BU Manager' };
    return m[r] ?? r; // rôle personnalisé affiché tel quel
  }

  roleClass(r: string): string {
    const m: Record<string, string> = { ADMIN: 'role-admin', PM: 'role-pm', BUM: 'role-bum' };
    return m[r] ?? 'role-custom'; // classe générique pour les rôles personnalisés
  }

  formatDate(iso: string): string {
    if (!iso) return '—';
    const d = new Date(iso);
    const date = d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
    const time = d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    return `${date} ${time}`;
  }
}
