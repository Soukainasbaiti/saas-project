import { Component, OnInit, Input, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';

export interface OpportunityRow {
  id: number; projectId: number; oId: string;
  identificationDate: string | null;
  opportunityDescription: string;
  category: string; costs: number | null; price: number | null;
  estimatedBenefit: number | null; percentNewPm: number | null;
  actionToBeTaken: string; owner: string;
  deadline: string | null; overdue: boolean | null;
  onHold: boolean | null; status: string;
  copilValidation: string; comments: string;
}

@Component({
  selector: 'app-opportunity-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './opportunity-register.component.html',
  styleUrls: ['./opportunity-register.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OpportunityRegisterComponent implements OnInit {

  @Input() projectId!: number;

  opportunities: OpportunityRow[] = [];
  loading = false;
  selectedOpp: OpportunityRow | null = null;
  showModal = false;
  isViewMode = false;
  saving = false;
  showCreateModal = false;
  newOpp = this.emptyOpp();
  showDeleteConfirm = false;
  pendingDelete: OpportunityRow | null = null;

  readonly categories = ['Scope Extension','Additional Services','Customer Upsell','New Stream','Change Request','Performance Bonus'];
  readonly statuses   = ['Open','In Progress','Won','Lost'];
  readonly copilOpts  = ['Pending','Validated','Rejected','Deferred'];

  constructor(private api: ApiService, private cdr: ChangeDetectorRef, public i18n: I18nService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.api.getOpportunities(this.projectId).subscribe({
      next: d => { this.opportunities = d; this.loading = false; this.cdr.markForCheck(); }
    });
  }

  // ── KPIs ──────────────────────────────────────────────────────
  get totalOpps()     { return this.opportunities.length; }
  get totalBenefit()  { return this.opportunities.reduce((s,o) => s + (o.estimatedBenefit||0), 0); }
  get wonCount()      { return this.opportunities.filter(o => o.status === 'Won').length; }
  get overdueCount()  { return this.opportunities.filter(o => o.overdue).length; }

  // ── Helpers ───────────────────────────────────────────────────
  computeBenefit(price: number|null, costs: number|null): number|null {
    if (price == null) return null;
    return price - (costs || 0);
  }

  statusClass(s: string): string {
    return { 'Open':'opp-open','In Progress':'opp-inprogress','Won':'opp-won','Lost':'opp-lost' }[s] ?? '';
  }
  copilClass(c: string): string {
    return { 'Pending':'copil-pending','Validated':'copil-validated','Rejected':'copil-rejected','Deferred':'copil-deferred' }[c] ?? '';
  }

  fmt(v: number|null): string {
    if (v == null) return '—';
    return new Intl.NumberFormat('fr-FR',{minimumFractionDigits:0,maximumFractionDigits:0}).format(v) + ' €';
  }

  truncate(s: string, max = 70): string {
    return s && s.length > max ? s.substring(0,max) + '…' : s;
  }

  // ── Create ────────────────────────────────────────────────────
  openCreate(): void {
    this.newOpp = this.emptyOpp();
    this.showCreateModal = true;
    this.cdr.markForCheck();
  }

  confirmCreate(): void {
    if (!this.newOpp.opportunityDescription?.trim() || !this.newOpp.deadline) return;
    this.saving = true;
    this.api.createOpportunity(this.projectId, this.newOpp).subscribe({
      next: o => { this.opportunities = [...this.opportunities, o]; this.showCreateModal = false; this.saving = false; this.cdr.markForCheck(); },
      error: () => { this.saving = false; this.cdr.markForCheck(); }
    });
  }

  // ── Detail modal ──────────────────────────────────────────────
  openView(o: OpportunityRow): void { this.selectedOpp = {...o}; this.isViewMode = true; this.showModal = true; this.cdr.markForCheck(); }
  openEdit(o: OpportunityRow): void { this.selectedOpp = {...o}; this.isViewMode = false; this.showModal = true; this.cdr.markForCheck(); }

  closeModal(): void { this.showModal = false; this.selectedOpp = null; this.cdr.markForCheck(); }

  saveModal(): void {
    if (!this.selectedOpp || this.saving) return;
    this.saving = true;
    this.selectedOpp.estimatedBenefit = this.computeBenefit(this.selectedOpp.price, this.selectedOpp.costs);
    this.api.updateOpportunity(this.projectId, this.selectedOpp.id, this.selectedOpp).subscribe({
      next: updated => {
        this.opportunities = this.opportunities.map(o => o.id === updated.id ? updated : o);
        this.saving = false; this.closeModal(); this.cdr.markForCheck();
      },
      error: () => { this.saving = false; this.cdr.markForCheck(); }
    });
  }

  // ── Delete ────────────────────────────────────────────────────
  openDelete(o: OpportunityRow, event?: Event): void {
    event?.stopPropagation(); this.pendingDelete = o; this.showDeleteConfirm = true; this.cdr.markForCheck();
  }
  confirmDelete(): void {
    if (!this.pendingDelete) return;
    const o = this.pendingDelete;
    this.showDeleteConfirm = false; this.pendingDelete = null;
    this.api.deleteOpportunity(this.projectId, o.id).subscribe({
      next: () => { this.opportunities = this.opportunities.filter(x => x.id !== o.id); this.cdr.markForCheck(); }
    });
  }
  cancelDelete(): void { this.showDeleteConfirm = false; this.pendingDelete = null; this.cdr.markForCheck(); }

  private emptyOpp(): any {
    return {
      opportunityDescription: '', deadline: null, identificationDate: null,
      category: '', costs: null, price: null, percentNewPm: null,
      actionToBeTaken: '', owner: '', onHold: false,
      status: 'Open', copilValidation: 'Pending', comments: ''
    };
  }
}
