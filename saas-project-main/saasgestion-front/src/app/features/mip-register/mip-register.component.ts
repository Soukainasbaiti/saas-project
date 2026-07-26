import { Component, OnInit, Input, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';

export interface MipRow {
  id: number; projectId: number; mipId: string;
  identificationDate: string | null; lever: string;
  actionType: string; actionDescription: string; accountable: string;
  clientImpact: string; dueDate: string | null;
  priority: string; risquesPrerequis: string;
  plannedGain: number | null; realizedGain: number | null; status: string;
}

@Component({
  selector: 'app-mip-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mip-register.component.html',
  styleUrls: ['./mip-register.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MipRegisterComponent implements OnInit {

  @Input() projectId!: number;

  mips: MipRow[] = [];
  loading = false;
  selectedMip: MipRow | null = null;
  showModal = false;
  isViewMode = false;
  saving = false;
  showCreateModal = false;
  newMip = this.emptyMip();
  showDeleteConfirm = false;
  pendingDelete: MipRow | null = null;

  readonly levers     = ['Costs','Delivery','Price','Productivity','Resources'];
  readonly priorities = ['P1-High','P2-Medium','P3-Low'];
  readonly statuses   = ['Identified','In Progress','Completed','Cancelled'];
  readonly clientImpacts = ['Yes','No'];

  // Actions possibles par levier (2e niveau de la cascade, liste fixe)
  readonly leverActions: Record<string, string[]> = {
    'Costs': [
      'Limiter les déplacements',
      'Réduire les coûts non salariaux',
      'Optimiser les achats fournisseurs',
      'Anticiper les besoins budgétaires',
      'Suivre et renégocier les fournisseurs',
      'Réduire le nombre de sous-traitants',
    ],
    'Delivery': [
      'Sécuriser le respect des délais',
      'Réduire les risques de retard',
      'Améliorer la qualité de livraison',
    ],
    'Productivity': [
      'Automatiser les tâches répétitives',
      'Améliorer les process',
      'Optimiser le time efficiency',
    ],
    'Resources': [
      'Optimiser l\'allocation des ressources',
      'Rééquilibrer la charge entre consultants',
    ],
    'Price': [
      'Ajuster les prix du contrat',
      'Émettre un change request',
    ],
  };

  actionsForLever(lever: string): string[] {
    return this.leverActions[lever] || [];
  }

  // ── Cascade Levier -> Action -> Commentaire pré-rempli ─────────────
  onLeverChange(target: { lever: string; actionType: string }): void {
    target.actionType = '';
  }

  onActionTypeChange(target: { actionType: string; actionDescription: string }): void {
    if (!target.actionType) return;
    if (!target.actionDescription?.trim()) {
      target.actionDescription = `Quelle action mettre en place pour : ${target.actionType.toLowerCase()} ?`;
    }
  }

  constructor(private api: ApiService, private cdr: ChangeDetectorRef, public i18n: I18nService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.api.getMips(this.projectId).subscribe({
      next: d => { this.mips = d; this.loading = false; this.cdr.markForCheck(); }
    });
  }

  // ── KPIs ──────────────────────────────────────────────────────
  get totalMips()       { return this.mips.length; }
  get completedCount()  { return this.mips.filter(m => m.status === 'Completed').length; }
  get totalPlanned()    { return this.mips.reduce((s,m) => s + (m.plannedGain||0), 0); }
  get totalRealized()   { return this.mips.reduce((s,m) => s + (m.realizedGain||0), 0); }

  leverClass(l: string): string {
    return { 'Costs':'lev-costs','Delivery':'lev-delivery','Price':'lev-price','Productivity':'lev-productivity','Resources':'lev-resources' }[l] ?? '';
  }
  statusClass(s: string): string {
    return { 'Identified':'mip-identified','In Progress':'mip-inprogress','Completed':'mip-completed','Cancelled':'mip-cancelled' }[s] ?? '';
  }
  priorityClass(p: string): string {
    return { 'P1-High':'prio-high','P2-Medium':'prio-medium','P3-Low':'prio-low' }[p] ?? '';
  }
  clientImpactClass(c: string): string {
    return c === 'Yes' ? 'ci-yes' : 'ci-no';
  }

  fmt(v: number|null): string {
    if (v == null) return '—';
    return new Intl.NumberFormat('fr-FR',{minimumFractionDigits:0,maximumFractionDigits:0}).format(v) + ' €';
  }
  truncate(s: string, max = 80): string {
    return s && s.length > max ? s.substring(0, max) + '…' : s;
  }

  // ── Create ────────────────────────────────────────────────────
  openCreate(): void { this.newMip = this.emptyMip(); this.showCreateModal = true; this.cdr.markForCheck(); }

  confirmCreate(): void {
    if (!this.newMip.actionDescription?.trim()) return;
    this.saving = true;
    this.api.createMip(this.projectId, this.newMip).subscribe({
      next: m => { this.mips = [...this.mips, m]; this.showCreateModal = false; this.saving = false; this.cdr.markForCheck(); },
      error: () => { this.saving = false; this.cdr.markForCheck(); }
    });
  }

  // ── Detail ────────────────────────────────────────────────────
  openView(m: MipRow): void { this.selectedMip = {...m}; this.isViewMode = true; this.showModal = true; this.cdr.markForCheck(); }
  openEdit(m: MipRow): void { this.selectedMip = {...m}; this.isViewMode = false; this.showModal = true; this.cdr.markForCheck(); }
  closeModal(): void { this.showModal = false; this.selectedMip = null; this.cdr.markForCheck(); }

  saveModal(): void {
    if (!this.selectedMip || this.saving) return;
    this.saving = true;
    this.api.updateMip(this.projectId, this.selectedMip.id, this.selectedMip).subscribe({
      next: updated => { this.mips = this.mips.map(m => m.id === updated.id ? updated : m); this.saving = false; this.closeModal(); this.cdr.markForCheck(); },
      error: () => { this.saving = false; this.cdr.markForCheck(); }
    });
  }

  // ── Delete ────────────────────────────────────────────────────
  openDelete(m: MipRow, event?: Event): void { event?.stopPropagation(); this.pendingDelete = m; this.showDeleteConfirm = true; this.cdr.markForCheck(); }
  confirmDelete(): void {
    if (!this.pendingDelete) return;
    const m = this.pendingDelete;
    this.showDeleteConfirm = false; this.pendingDelete = null;
    this.api.deleteMip(this.projectId, m.id).subscribe({ next: () => { this.mips = this.mips.filter(x => x.id !== m.id); this.cdr.markForCheck(); } });
  }
  cancelDelete(): void { this.showDeleteConfirm = false; this.pendingDelete = null; this.cdr.markForCheck(); }

  private emptyMip(): any {
    return { actionDescription:'', identificationDate: null, lever:'', actionType:'', accountable:'', clientImpact:'No', dueDate:null, priority:'P1-High', risquesPrerequis:'', plannedGain:null, realizedGain:null, status:'Identified' };
  }
}
