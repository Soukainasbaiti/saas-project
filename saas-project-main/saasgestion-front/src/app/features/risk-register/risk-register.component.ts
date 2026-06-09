import { Component, OnInit, Input, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';

export interface RiskRow {
  id: number;
  projectId: number;
  rId: string;
  identificationDate: string | null;
  phase: string;
  risk: string;
  category: string;
  probability: string;
  probEval: number | null;
  percentProbability: number | null;
  impact: string;
  impactEval: number | null;
  rating: string;
  managementStrategy: string;
  owner: string;
  mitigationAction: string;
  costs: number | null;
  probabilityResidual: number | null;
  net: number | null;
  contingencyAction: string;
  trigger: string;
  residualAction: string;
  deadline: string | null;
  status: string;
  closureDate: string | null;
}

@Component({
  selector: 'app-risk-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './risk-register.component.html',
  styleUrls: ['./risk-register.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RiskRegisterComponent implements OnInit {

  @Input() projectId!: number;
  @Input() projectName = '';

  risks: RiskRow[] = [];
  loading = false;

  // Drawer (detail panel)
  selectedRisk: RiskRow | null = null;
  showDrawer = false;
  isViewMode = false; // true = lecture seule, false = édition

  // Create modal
  showCreateModal = false;
  saving = false;

  newRisk = this.emptyRisk();

  // Reference data
  readonly phases = [
    '1 - Tender (RFI/RFP/RFQ)',
    '2 - Due Diligence & Contracting',
    '3 - Kickoff & Planning',
    '4 - Design / Architecture',
    '5 - Build / Implementation',
    '6 - Handover to Operations',
    '7 - Run',
    '8 - Project Closure'
  ];

  readonly categories = [
    'Contract & Financials',
    'SLA / Service Quality',
    'Resources & Staffing',
    'Operations & Client Tools',
    'Client Dependencies',
    'Suppliers / Partners',
    'Program / Scheduling',
    'Security & Compliance'
  ];

  readonly probabilities = [
    { label: 'Very Likely', value: 5, pct: 90 },
    { label: 'Likely',      value: 4, pct: 70 },
    { label: 'Possible',    value: 3, pct: 50 },
    { label: 'Unlikely',    value: 2, pct: 30 },
    { label: 'Very unlikely', value: 1, pct: 10 }
  ];

  readonly impacts = [
    { label: 'Severe',      value: 5 },
    { label: 'Significant', value: 4 },
    { label: 'Moderate',    value: 3 },
    { label: 'Minor',       value: 2 },
    { label: 'Negligible',  value: 1 }
  ];

  readonly strategies = ['Accept', 'Mitigate', 'Transfer', 'Eliminate', 'Contingency'];

  readonly statuses = ['Identified', 'Mitigation in progress', 'Became an issue', 'Closed'];

  // Rating matrix
  private readonly RATING_MATRIX: Record<string, Record<string, string>> = {
    'Very unlikely': { Negligible:'Low', Minor:'Low', Moderate:'Medium', Significant:'High', Severe:'High' },
    'Unlikely':      { Negligible:'Low', Minor:'Medium', Moderate:'Medium', Significant:'High', Severe:'High' },
    'Possible':      { Negligible:'Medium', Minor:'Medium', Moderate:'High', Significant:'High', Severe:'Critical' },
    'Likely':        { Negligible:'Medium', Minor:'High', Moderate:'High', Significant:'Critical', Severe:'Critical' },
    'Very Likely':   { Negligible:'High', Minor:'High', Moderate:'Critical', Significant:'Critical', Severe:'Critical' }
  };

  constructor(private api: ApiService, private cdr: ChangeDetectorRef, public i18n: I18nService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.api.getRisks(this.projectId).subscribe({
      next: (data) => { this.risks = data; this.loading = false; this.cdr.markForCheck(); }
    });
  }

  // ── KPIs ──────────────────────────────────────────────────────
  get totalRisks()    { return this.risks.length; }
  get criticalCount() { return this.risks.filter(r => r.rating === 'Critical').length; }
  get highCount()     { return this.risks.filter(r => r.rating === 'High').length; }
  get openCount()     { return this.risks.filter(r => r.status !== 'Closed').length; }

  // ── Rating helpers ────────────────────────────────────────────
  computeRating(prob: string, impact: string): string {
    return this.RATING_MATRIX[prob]?.[impact] ?? '';
  }

  ratingClass(rating: string): string {
    const map: Record<string, string> = {
      Critical: 'rating-critical', High: 'rating-high',
      Medium: 'rating-medium', Low: 'rating-low'
    };
    return map[rating] ?? '';
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      'Identified': 'status-identified',
      'Mitigation in progress': 'status-mitigation',
      'Became an issue': 'status-issue',
      'Closed': 'status-closed'
    };
    return map[status] ?? '';
  }

  computeNet(costs: number | null, residual: number | null): number | null {
    if (costs == null || residual == null) return null;
    return +(costs * residual / 100).toFixed(2);
  }

  // ── Create modal ──────────────────────────────────────────────
  openCreateModal(): void {
    this.newRisk = this.emptyRisk();
    this.showCreateModal = true;
    this.cdr.markForCheck();
  }

  confirmCreate(): void {
    if (!this.newRisk.identificationDate || !this.newRisk.risk.trim()) return;
    this.saving = true;
    this.api.createRisk(this.projectId, {
      ...this.newRisk,
      rating: this.computeRating(this.newRisk.probability, this.newRisk.impact),
      net: this.computeNet(this.newRisk.costs, this.newRisk.probabilityResidual)
    }).subscribe({
      next: (r) => {
        this.risks.push(r);
        this.showCreateModal = false;
        this.saving = false;
        this.cdr.markForCheck();
      },
      error: () => { this.saving = false; this.cdr.markForCheck(); }
    });
  }

  // ── Drawer ────────────────────────────────────────────────────
  openDrawer(risk: RiskRow, viewMode = false): void {
    this.selectedRisk = { ...risk };
    this.isViewMode = viewMode;
    this.showDrawer = true;
    this.cdr.markForCheck();
  }

  openViewMode(risk: RiskRow): void {
    this.openDrawer(risk, true);
  }

  openEditMode(risk: RiskRow): void {
    this.openDrawer(risk, false);
  }

  closeDrawer(): void {
    this.showDrawer = false;
    this.selectedRisk = null;
    this.cdr.markForCheck();
  }

  // Helpers pour les valeurs auto dans le drawer
  getProbEval(prob: string): number | null {
    return prob ? (this.probabilities.find(p => p.label === prob)?.value ?? null) : null;
  }
  getProbPct(prob: string): number | null {
    return prob ? (this.probabilities.find(p => p.label === prob)?.pct ?? null) : null;
  }
  getImpactEval(impact: string): number | null {
    return impact ? (this.impacts.find(i => i.label === impact)?.value ?? null) : null;
  }

  saveDrawer(): void {
    if (!this.selectedRisk || this.saving) return;
    this.saving = true;
    const payload = {
      identificationDate: this.selectedRisk.identificationDate,
      risk:               this.selectedRisk.risk,
      phase:              this.selectedRisk.phase,
      category:           this.selectedRisk.category,
      probability:        this.selectedRisk.probability,
      impact:             this.selectedRisk.impact,
      managementStrategy: this.selectedRisk.managementStrategy,
      owner:              this.selectedRisk.owner,
      mitigationAction:   this.selectedRisk.mitigationAction,
      costs:              this.selectedRisk.costs,
      probabilityResidual: this.selectedRisk.probabilityResidual,
      contingencyAction:  this.selectedRisk.contingencyAction,
      trigger:            this.selectedRisk.trigger,
      residualAction:     this.selectedRisk.residualAction,
      deadline:           this.selectedRisk.deadline,
      status:             this.selectedRisk.status,
      closureDate:        this.selectedRisk.closureDate,
    };
    this.api.updateRisk(this.projectId, this.selectedRisk.id, payload).subscribe({
      next: (updated) => {
        this.risks = this.risks.map(r => r.id === updated.id ? updated : r);
        this.saving = false;
        this.closeDrawer(); // fermer le modal après enregistrement
        this.cdr.markForCheck();
      },
      error: () => { this.saving = false; this.cdr.markForCheck(); }
    });
  }

  showDeleteConfirm = false;
  pendingDeleteRisk: RiskRow | null = null;

  openDeleteConfirm(risk: RiskRow, event?: Event): void {
    event?.stopPropagation();
    this.pendingDeleteRisk = risk;
    this.showDeleteConfirm = true;
    this.cdr.markForCheck();
  }

  confirmDelete(): void {
    if (!this.pendingDeleteRisk) return;
    const risk = this.pendingDeleteRisk;
    this.showDeleteConfirm = false;
    this.pendingDeleteRisk = null;
    this.api.deleteRisk(this.projectId, risk.id).subscribe({
      next: () => {
        this.risks = this.risks.filter(r => r.id !== risk.id);
        if (this.selectedRisk?.id === risk.id) this.closeDrawer();
        this.cdr.markForCheck();
      }
    });
  }

  cancelDelete(): void {
    this.showDeleteConfirm = false;
    this.pendingDeleteRisk = null;
    this.cdr.markForCheck();
  }

  deleteRisk(risk: RiskRow): void {
    this.openDeleteConfirm(risk);
  }

  private emptyRisk(): any {
    return {
      identificationDate: new Date().toISOString().split('T')[0],
      risk: '', phase: '', category: '', probability: '',
      impact: '', managementStrategy: '', owner: '',
      mitigationAction: '', costs: null, probabilityResidual: null,
      contingencyAction: '', trigger: '', residualAction: '',
      deadline: null, status: 'Identified', closureDate: null
    };
  }

  fmt(v: number | null): string {
    if (v == null) return '—';
    return new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v);
  }

  truncate(s: string, max = 60): string {
    return s && s.length > max ? s.substring(0, max) + '…' : s;
  }
}
