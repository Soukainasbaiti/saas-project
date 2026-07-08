import { Component, OnInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../core/services/api.service';
import { AdminSidebarComponent } from '../admin-sidebar/admin-sidebar.component';

export type RefTab = 'bus' | 'industries' | 'customers' | 'disciplines' | 'engagements' | 'countries';

@Component({
  selector: 'app-admin-ref-data',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, MatSnackBarModule, AdminSidebarComponent],
  templateUrl: './admin-ref-data.component.html',
  styleUrls: ['./admin-ref-data.component.scss']
})
export class AdminRefDataComponent implements OnInit {

  activeTab: RefTab = 'bus';
  loading = false;
  saving  = false;

  items: any[] = [];

  showModal  = false;
  editTarget: any = null;  // null = création, sinon = modification
  form: FormGroup;

  tabs: { key: RefTab; label: string; icon: string }[] = [
    { key: 'bus',              label: 'BU',                   icon: '🏢' },
    { key: 'industries',       label: 'Industries',            icon: '🏭' },
    { key: 'customers',        label: 'Clients',               icon: '👥' },
    { key: 'disciplines',      label: 'Disciplines',           icon: '⚙️' },
    { key: 'engagements',      label: 'Engagements',           icon: '📋' },
    { key: 'countries',        label: 'Pays',                  icon: '🌍' },
  ];

  constructor(
    private api: ApiService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
    private snack: MatSnackBar
  ) {
    this.form = this.fb.group({
      // BU
      id:              [''],
      bumName:         [''],
      // Industry / Customer / FrontFinancier
      trigram:         [''],
      customerGroup:   [''],
      // Engagement
      engagementType:  [''],
      // Pays
      isoCode:         [''],
      // Commun
      name:            [''],
    });
  }

  ngOnInit(): void {
    this.loadTab(this.activeTab);
  }

  // ── Navigation entre onglets ──────────────────────────────────
  selectTab(tab: RefTab): void {
    this.activeTab = tab;
    this.loadTab(tab);
  }

  loadTab(tab: RefTab): void {
    this.loading = true;
    this.items = [];
    this.cdr.markForCheck();
    this.api.getAdminRef(tab).subscribe({
      next: d => { this.items = d; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.loading = false; this.cdr.markForCheck(); }
    });
  }

  // ── Colonnes à afficher selon l'onglet ───────────────────────
  get columns(): string[] {
    const map: Record<RefTab, string[]> = {
      'bus':              ['id', 'name', 'trigram', 'bumName'],
      'industries':       ['name', 'trigram'],
      'customers':        ['name', 'trigram', 'customerGroup'],
      'disciplines':      ['name'],
      'engagements':      ['name', 'engagementType'],
      'countries':        ['name', 'isoCode'],
    };
    return map[this.activeTab];
  }

  get colLabels(): Record<string, string> {
    return {
      id: 'ID BU', name: 'Nom', trigram: 'Trigramme',
      bumName: 'BU Manager', customerGroup: 'Groupe client',
      engagementType: 'Type', code: 'Code', label: 'Libellé',
      isoCode: 'Code ISO'
    };
  }

  // ── Ouvrir modale ajout ───────────────────────────────────────
  openAdd(): void {
    this.editTarget = null;
    this.form.reset();
    this.applyValidators();
    this.showModal = true;
    this.cdr.markForCheck();
  }

  // ── Ouvrir modale modification ────────────────────────────────
  openEdit(item: any): void {
    this.editTarget = item;
    this.form.reset();
    this.applyValidators();
    // Pré-remplir selon l'onglet
    const patch: any = {};
    this.columns.forEach(c => patch[c] = item[c] ?? '');
    this.form.patchValue(patch);
    this.showModal = true;
    this.cdr.markForCheck();
  }

  closeModal(): void {
    this.showModal = false;
    this.editTarget = null;
    this.cdr.markForCheck();
  }

  // ── Appliquer les validateurs requis selon l'onglet ──────────
  private applyValidators(): void {
    // Effacer tous les validateurs d'abord
    Object.keys(this.form.controls).forEach(k => {
      this.form.get(k)?.clearValidators();
      this.form.get(k)?.updateValueAndValidity();
    });

    const req = Validators.required;
    switch (this.activeTab) {
      case 'bus':
        if (!this.editTarget) this.form.get('id')?.setValidators([req]);
        this.form.get('name')?.setValidators([req]);
        this.form.get('trigram')?.setValidators([req]);
        this.form.get('bumName')?.setValidators([req]);
        break;
      case 'industries':
        this.form.get('name')?.setValidators([req]);
        this.form.get('trigram')?.setValidators([req]);
        break;
      case 'customers':
        this.form.get('name')?.setValidators([req]);
        this.form.get('trigram')?.setValidators([req]);
        break;
      case 'disciplines':
        this.form.get('name')?.setValidators([req]);
        break;
      case 'engagements':
        this.form.get('name')?.setValidators([req]);
        this.form.get('engagementType')?.setValidators([req]);
        break;
      case 'countries':
        this.form.get('name')?.setValidators([req]);
        this.form.get('isoCode')?.setValidators([req, Validators.maxLength(2)]);
        break;
    }
    Object.keys(this.form.controls).forEach(k => this.form.get(k)?.updateValueAndValidity());
  }

  // ── Soumettre (créer ou modifier) ─────────────────────────────
  save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) { this.cdr.markForCheck(); return; }

    const v = this.form.value;
    let body: any = {};

    switch (this.activeTab) {
      case 'bus':
        body = { id: v.id, name: v.name, trigram: v.trigram, bumName: v.bumName }; break;
      case 'industries':
        body = { name: v.name, trigram: v.trigram }; break;
      case 'customers':
        body = { name: v.name, trigram: v.trigram, customerGroup: v.customerGroup }; break;
      case 'disciplines':
        body = { name: v.name }; break;
      case 'engagements':
        body = { name: v.name, engagementType: v.engagementType }; break;
      case 'countries':
        body = { name: v.name, isoCode: v.isoCode }; break;
    }

    this.saving = true;
    this.cdr.markForCheck();

    const call$ = this.editTarget
      ? this.api.updateAdminRef(this.activeTab, this.editTarget.id ?? this.editTarget.code, body)
      : this.api.createAdminRef(this.activeTab, body);

    call$.subscribe({
      next: (res: any) => {
        this.saving = false;
        this.showModal = false;
        this.snack.open(res.message || 'Enregistré.', 'OK', { duration: 3000 });
        this.loadTab(this.activeTab);
      },
      error: err => {
        this.saving = false;
        this.cdr.markForCheck();
        this.snack.open(err.error?.error || 'Erreur lors de l\'enregistrement.', 'Fermer', { duration: 5000 });
      }
    });
  }

  // ── Activer / Désactiver ──────────────────────────────────────
  toggle(item: any): void {
    const id = item.id ?? item.code;
    this.api.toggleAdminRef(this.activeTab, id).subscribe({
      next: (res: any) => {
        this.snack.open(res.message || 'Statut modifié.', 'OK', { duration: 2500 });
        this.loadTab(this.activeTab);
      },
      error: () => this.snack.open('Erreur.', 'Fermer', { duration: 3000 })
    });
  }

  // ── Helpers ───────────────────────────────────────────────────
  get modalTitle(): string {
    const addLabels: Record<RefTab, string> = {
      'bus':         'Ajouter une nouvelle BU',
      'industries':  'Ajouter une nouvelle industrie',
      'customers':   'Ajouter un nouveau client',
      'disciplines': 'Ajouter une nouvelle discipline',
      'engagements': 'Ajouter un nouvel engagement',
      'countries':   'Ajouter un nouveau pays',
    };
    const editLabels: Record<RefTab, string> = {
      'bus':         'Modifier la BU',
      'industries':  'Modifier l\'industrie',
      'customers':   'Modifier le client',
      'disciplines': 'Modifier la discipline',
      'engagements': 'Modifier l\'engagement',
      'countries':   'Modifier le pays',
    };
    return this.editTarget ? editLabels[this.activeTab] : addLabels[this.activeTab];
  }

  showField(field: string): boolean {
    if (field === 'id') return this.activeTab === 'bus' && !this.editTarget;
    return this.columns.includes(field);
  }
}
