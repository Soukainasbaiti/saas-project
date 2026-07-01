import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Location } from '@angular/common';
import { lastValueFrom } from 'rxjs';
import { ChartData, ChartOptions } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { I18nService } from '../../core/services/i18n.service';
import { RiskRegisterComponent } from '../risk-register/risk-register.component';
import { IssueRegisterComponent } from '../issue-register/issue-register.component';
import { OpportunityRegisterComponent } from '../opportunity-register/opportunity-register.component';
import { MipRegisterComponent } from '../mip-register/mip-register.component';
import { WipRegisterComponent } from '../wip-register/wip-register.component';

interface ResourceRow {
  id: number;
  matricule: string;
  personName: string;
  contractType: string;
  isActive: boolean;
  dailyCosts: Record<string, number>;
  workedDays: Record<string, number>;
  billedDays: Record<string, number>;
  dailyRates: Record<string, number>;
}

interface OtherCostRow {
  category: string;
  amounts: Record<string, number>;
  isRebill: boolean;
}

interface WorkTypeRow {
  id: number;
  name: string;
  unitLabel: string;
  unitPrice: number | null;
  plannedQty: number | null;
  durationDays: number | null;
  deliveredQty: number;
  plannedRevenue: number;
  actualRevenue: number;
  completionRate: number;
}

interface WorkTicketRow {
  id: number;
  ticketId: string;
  workTypeId: number;
  workTypeName: string;
  unitLabel: string;
  quantity: number | null;
  consultant: string;
  assignedDate: string | null;
  startDate: string | null;
  endDate: string | null;
  deliveryDate: string | null;
  firstPass: boolean | null;
  onTime: string;
  status: string;
  comments: string;
  revenue: number;
}

interface OnePagerRow {
  label: string;
  real: number;
  lastMonth: number;
  forecast: number;
  landing: number;
  format: 'currency' | 'percent' | 'days' | 'rate';
  marginRow?: boolean;
}

interface DeliverableRow {
  id: number;
  deliverableId: string;
  lotName: string;
  deliverableName: string;
  discipline: string;
  owner: string;
  plannedDate: string | null;
  deliveryDate: string | null;
  status: string;
  plannedRevenue: number | null;
  rfRevenue: number | null;
  gap: number | null;
  firstPass: boolean | null;
  onTime: string; // OTD | OVERDUE | PENDING
  priority: string;
  comments: string;
}

@Component({
  selector: 'app-project-management',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, BaseChartDirective, RiskRegisterComponent, IssueRegisterComponent, OpportunityRegisterComponent, MipRegisterComponent, WipRegisterComponent],
  templateUrl: './project-management.component.html',
  styleUrls: ['./project-management.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProjectManagementComponent implements OnInit {

  projectId!: number;
  projectName = '';
  projectEngagementType: string | null = null;

  // Champs projet pour génération BL
  projectClientName = '';
  projectBuTrigram = '';
  projectPmName = '';
  projectCode = '';
  projectBusinessId = '';

  // ── One Pager ─────────────────────────────────────────────────────
  startDate: string | null = null;
  endDate: string | null = null;
  monthlyForecasts: { [month: string]: { revenue: number; cost: number; cov: number | null } } = {};

  // ── One Pager : Health & Delivery (saisie PM) ──────────────────
  deliveryConfidenceLevel: string | null = null;
  healthScoreValue: number | null = null;
  healthScoreStatus: string | null = null;
  pmRemarks = '';
  varianceActualComment = '';
  varianceTrendComment = '';
  varianceLandingComment = '';
  tops = '';
  flops = '';

  readonly ragOptions: { value: string; label: string }[] = [
    { value: 'ON_TRACK',      label: 'On Track' },
    { value: 'MINOR_RISKS',   label: 'Minor Risks' },
    { value: 'AT_RISK',       label: 'At Risk' },
    { value: 'OFF_TRACK',     label: 'Off Track' },
    { value: 'RECOVERY_MODE', label: 'Recovery Mode' },
  ];

  ragLabel(value: string | null): string {
    if (!value) return '—';
    const opt = this.ragOptions.find(o => o.value === value);
    return opt ? opt.label : value;
  }

  ragClass(value: string | null): string {
    switch (value) {
      case 'ON_TRACK':      return 'rag-on-track';
      case 'MINOR_RISKS':   return 'rag-minor-risks';
      case 'AT_RISK':       return 'rag-at-risk';
      case 'OFF_TRACK':     return 'rag-off-track';
      case 'RECOVERY_MODE': return 'rag-recovery-mode';
      default:              return '';
    }
  }

  saveOnePagerExtras(): void {
    this.api.updateOnePagerExtras(this.projectId, {
      deliveryConfidenceLevel: this.deliveryConfidenceLevel,
      healthScoreValue: this.healthScoreValue,
      healthScoreStatus: this.healthScoreStatus,
      pmRemarks: this.pmRemarks,
      varianceActualComment: this.varianceActualComment,
      varianceTrendComment: this.varianceTrendComment,
      varianceLandingComment: this.varianceLandingComment,
      tops: this.tops,
      flops: this.flops,
    }).subscribe();
  }

  // ── Génération BL ─────────────────────────────────────────────
  showBlModal = false;
  blSelectedPeriod = '';
  blForm = {
    blDate: '',
    blNumber: '',
    period: '',
    orderNumber: '',
    clientProjectId: '',
    supplierContact: '',
    clientContact: '',
    additionalContact: '',
    clientName: '',
    buCode: '',
    projectDescription: '',
    projectCode: ''
  };
  blLines: { designation: string; quantity: number; unitPrice: number; total: number }[] = [];
  showChangeEngagementModal = false;
  targetEngagementType: string | null = null;
  changingEngagement = false;

  // Tous les types disponibles pour le switch
  readonly allEngagementTypes = [
    { code: 'AT',  label: 'AT — Technical Assistance',     category: 'moyens' },
    { code: 'T&M', label: 'T&M — Time & Material',         category: 'moyens' },
    { code: 'TK',  label: 'TK — Turnkey',                  category: 'moyens' },
    { code: 'UoW', label: 'UoW — Unit of Work',            category: 'resultats' },
    { code: 'WP',  label: 'WP — Work Package',             category: 'resultats' },
  ];

  get moyensEngagementTypes()    { return this.allEngagementTypes.filter(e => e.category === 'moyens'); }
  get resultatsEngagementTypes() { return this.allEngagementTypes.filter(e => e.category === 'resultats'); }

  get currentEngagementLabel(): string {
    return this.allEngagementTypes.find(e => e.code === this.projectEngagementType)?.label ?? (this.projectEngagementType || '—');
  }
  get targetEngagementLabel(): string {
    return this.allEngagementTypes.find(e => e.code === this.targetEngagementType)?.label ?? '';
  }
  get targetIsResultats(): boolean {
    return this.allEngagementTypes.find(e => e.code === this.targetEngagementType)?.category === 'resultats';
  }
  get targetIsMoyens(): boolean {
    return this.allEngagementTypes.find(e => e.code === this.targetEngagementType)?.category === 'moyens';
  }

  openChangeEngagementModal(): void {
    this.targetEngagementType = null;
    this.showChangeEngagementModal = true;
    this.cdr.markForCheck();
  }

  confirmChangeEngagement(): void {
    if (!this.targetEngagementType || this.changingEngagement) return;
    this.changingEngagement = true;
    this.api.changeEngagement(this.projectId, this.targetEngagementType).subscribe({
      next: (res) => {
        this.projectEngagementType = res.engagementType;
        // Auto-reconfigurer les modules si passage en Résultats
        if (this.isResultatsEngagement) {
          const t = this.projectEngagementType!.toLowerCase();
          this.engagementModules = {
            workPackage: t.includes('wp'),
            unitOfWork:  t.includes('uow')
          };
          this.activeResultTab = this.engagementModules.workPackage ? 'wp' : 'uow';
          localStorage.setItem(`pm-modules-${this.projectId}`, JSON.stringify(this.engagementModules));
        }
        this.showChangeEngagementModal = false;
        this.changingEngagement = false;
        this.cdr.markForCheck();
      },
      error: () => { this.changingEngagement = false; this.cdr.markForCheck(); }
    });
  }

  // Résultats : UoW ou WP → masquer Billed Days, ADR, Revenus—Moyens
  get isResultatsEngagement(): boolean {
    const t = (this.projectEngagementType || '').toLowerCase();
    return t.includes('uow') || t.includes('wp') || t.includes('work package') || t.includes('unit of work');
  }
  // Moyens : AT, T&M, TK → masquer Revenus—Résultats
  get isMoyensEngagement(): boolean {
    return !!this.projectEngagementType && !this.isResultatsEngagement;
  }
  months: string[] = [];
  monthStatus: { [period: string]: 'REAL' | 'FORECAST' } = {};
  savingMonthStatus: { [period: string]: boolean } = {};
  resources: ResourceRow[] = [];
  otherCosts: OtherCostRow[] = [];
  deliverables: DeliverableRow[] = [];

  // ── Unit of Work ──────────────────────────────────────────────────
  workTypes: WorkTypeRow[] = [];
  workTickets: WorkTicketRow[] = [];
  showAddWorkTypeModal = false;
  showAddWorkTicketModal = false;
  newWorkType = { name: '', unitLabel: '', unitPrice: null as number|null, plannedQty: null as number|null, durationDays: null as number|null };
  uowActiveView: 'catalogue' | 'tickets' = 'catalogue';
  expandedTypes = new Set<number>(); // accordion state

  toggleType(id: number): void {
    this.expandedTypes.has(id) ? this.expandedTypes.delete(id) : this.expandedTypes.add(id);
    this.cdr.markForCheck();
  }
  isExpanded(id: number): boolean { return this.expandedTypes.has(id); }
  newWorkTicket = { workTypeId: null as number|null, quantity: 1, consultant: '', assignedDate: '', startDate: '', endDate: '', comments: '' };

  // KPIs Unit of Work
  uowTotalPlanned(): number  { return this.workTypes.reduce((s,t) => s + (t.plannedRevenue||0), 0); }
  uowTotalActual(): number   { return this.workTypes.reduce((s,t) => s + (t.actualRevenue||0), 0); }
  uowFtrRate(): number {
    const done = this.workTickets.filter(t => t.firstPass !== null);
    if (!done.length) return 0;
    return (done.filter(t => t.firstPass).length / done.length) * 100;
  }
  uowOtdRate(): number {
    const done = this.workTickets.filter(t => t.onTime !== 'PENDING');
    if (!done.length) return 0;
    return (done.filter(t => t.onTime === 'OTD').length / done.length) * 100;
  }
  ticketsForType(typeId: number): WorkTicketRow[] {
    return this.workTickets.filter(t => t.workTypeId === typeId);
  }

  // CRUD work types
  loadWorkTypes(): void {
    this.api.getWorkTypes(this.projectId).subscribe({
      next: d => { this.workTypes = d; this.cdr.markForCheck(); }
    });
  }
  loadWorkTickets(): void {
    this.api.getWorkTickets(this.projectId).subscribe({
      next: d => { this.workTickets = d; this.cdr.markForCheck(); }
    });
  }
  openAddWorkTypeModal(): void {
    this.newWorkType = { name: '', unitLabel: '', unitPrice: null, plannedQty: null, durationDays: null };
    this.showAddWorkTypeModal = true;
    this.cdr.markForCheck();
  }
  confirmAddWorkType(): void {
    if (!this.newWorkType.name.trim()) return;
    this.api.createWorkType(this.projectId, this.newWorkType).subscribe({
      next: t => { this.workTypes.push(t); this.showAddWorkTypeModal = false; this.cdr.markForCheck(); }
    });
  }
  deleteWorkType(wt: WorkTypeRow): void {
    this.api.deleteWorkType(this.projectId, wt.id).subscribe({
      next: () => { this.workTypes = this.workTypes.filter(t => t.id !== wt.id); this.cdr.markForCheck(); }
    });
  }

  // CRUD work tickets
  openAddWorkTicketModal(typeId?: number): void {
    this.newWorkTicket = { workTypeId: typeId ?? null, quantity: 1, consultant: '', assignedDate: '', startDate: '', endDate: '', comments: '' };
    this.showAddWorkTicketModal = true;
    this.cdr.markForCheck();
  }
  onTicketStartDateChange(): void {
    if (!this.newWorkTicket.startDate || this.newWorkTicket.endDate) return;
    const type = this.workTypes.find(t => t.id === this.newWorkTicket.workTypeId);
    if (type?.durationDays) {
      const start = new Date(this.newWorkTicket.startDate);
      start.setDate(start.getDate() + type.durationDays);
      this.newWorkTicket.endDate = start.toISOString().split('T')[0];
    }
  }
  confirmAddWorkTicket(): void {
    if (!this.newWorkTicket.workTypeId) return;
    this.api.createWorkTicket(this.projectId, this.newWorkTicket).subscribe({
      next: t => {
        this.workTickets.push(t);
        this.loadWorkTypes(); // refresh computed KPIs
        this.showAddWorkTicketModal = false;
        this.cdr.markForCheck();
      }
    });
  }
  updateWorkTicketField(ticket: WorkTicketRow, field: string, value: any): void {
    const prev = (ticket as any)[field];
    (ticket as any)[field] = value;
    this.api.updateWorkTicket(this.projectId, ticket.id, { [field]: value }).subscribe({
      next: updated => { Object.assign(ticket, updated); this.loadWorkTypes(); this.cdr.markForCheck(); },
      error: () => { (ticket as any)[field] = prev; this.cdr.markForCheck(); }
    });
  }
  deleteWorkTicket(ticket: WorkTicketRow): void {
    this.api.deleteWorkTicket(this.projectId, ticket.id).subscribe({
      next: () => { this.workTickets = this.workTickets.filter(t => t.id !== ticket.id); this.loadWorkTypes(); this.cdr.markForCheck(); }
    });
  }

  // ── Work Package modals ───────────────────────────────────────────
  showAddDeliverableModal = false;
  showAddLotModal = false;
  newLotName = '';
  wpLots: string[] = [];
  selectedLot: string | null = null;
  showDeleteLotModal = false;
  pendingDeleteLotName: string | null = null;

  openDeleteLotModal(lotName: string): void {
    this.pendingDeleteLotName = lotName;
    this.showDeleteLotModal = true;
    this.cdr.markForCheck();
  }

  confirmDeleteLot(): void {
    if (!this.pendingDeleteLotName) return;
    this.deleteLot(this.pendingDeleteLotName);
    this.showDeleteLotModal = false;
    this.pendingDeleteLotName = null;
  }

  get allWpLots(): string[] {
    const fromDeliverables = this.deliverables.map(d => d.lotName).filter(Boolean);
    const all = new Set([...this.wpLots, ...fromDeliverables]);
    return Array.from(all).sort();
  }

  openAddLotModal(): void {
    const nums = this.allWpLots
      .map(l => { const m = l.match(/^Lot\s*(\d+)$/i); return m ? +m[1] : 0; })
      .filter(n => n > 0);
    const next = nums.length ? Math.max(...nums) + 1 : 1;
    this.newLotName = `Lot ${next}`;
    this.showAddLotModal = true;
    this.cdr.markForCheck();
  }

  confirmAddLot(): void {
    const name = this.newLotName.trim();
    if (!name || this.wpLots.includes(name)) return;
    this.wpLots.push(name);
    this.wpLots.sort();
    localStorage.setItem(`wp-lots-${this.projectId}`, JSON.stringify(this.wpLots));
    this.selectedLot = name; // auto-sélectionner le lot créé
    this.showAddLotModal = false;
    this.cdr.markForCheck();
  }

  deleteLot(lotName: string): void {
    this.wpLots = this.wpLots.filter(l => l !== lotName);
    localStorage.setItem(`wp-lots-${this.projectId}`, JSON.stringify(this.wpLots));
    this.cdr.markForCheck();
  }
  newDeliverable = { lotName: '', deliverableName: '', discipline: '', owner: '', plannedDate: '', plannedRevenue: null as number | null, priority: 'MEDIUM', comments: '' };

  get lots(): string[] { return this.allWpLots; }

  deliverablesForLot(lot: string): DeliverableRow[] {
    return this.deliverables.filter(d => d.lotName === lot);
  }

  // ── Work Package KPIs ─────────────────────────────────────────────
  wpTotalPlanned(): number  { return this.deliverables.reduce((s,d) => s + (d.plannedRevenue||0), 0); }
  wpTotalRf(): number       { return this.deliverables.reduce((s,d) => s + (d.rfRevenue||0), 0); }
  wpTotalGap(): number      { return this.wpTotalRf() - this.wpTotalPlanned(); }
  wpFtrRate(): number {
    const done = this.deliverables.filter(d => d.firstPass !== null);
    if (!done.length) return 0;
    return (done.filter(d => d.firstPass).length / done.length) * 100;
  }
  wpOtdRate(): number {
    const done = this.deliverables.filter(d => d.onTime !== 'PENDING');
    if (!done.length) return 0;
    return (done.filter(d => d.onTime === 'OTD').length / done.length) * 100;
  }

  // ── Revenue per period (Delivery Date → period column) ────────────
  wpRevenueForPeriod(period: string): number {
    return this.deliverables
      .filter(d => d.deliveryDate && this.dateMatchesPeriod(d.deliveryDate, period))
      .reduce((s, d) => s + (d.rfRevenue || 0), 0);
  }

  private dateMatchesPeriod(dateStr: string, period: string): boolean {
    if (!dateStr) return false;
    if (period.includes('-W')) {
      const [year, week] = period.split('-W').map(Number);
      const d = new Date(dateStr);
      const startOfYear = new Date(year, 0, 1);
      const weekNum = Math.ceil(((d.getTime() - startOfYear.getTime()) / 86400000 + startOfYear.getDay() + 1) / 7);
      return d.getFullYear() === year && weekNum === week;
    }
    if (period.length === 7) return dateStr.startsWith(period); // yyyy-MM
    return dateStr === period; // yyyy-MM-dd
  }

  // ── CRUD deliverables ─────────────────────────────────────────────
  loadDeliverables(): void {
    this.api.getDeliverables(this.projectId).subscribe({
      next: (data) => { this.deliverables = data; this.cdr.markForCheck(); }
    });
  }

  openAddDeliverableModal(lotName = ''): void {
    this.newDeliverable = { lotName, deliverableName: '', discipline: '', owner: '', plannedDate: '', plannedRevenue: null, priority: 'MEDIUM', comments: '' };
    this.showAddDeliverableModal = true;
    this.cdr.markForCheck();
  }

  confirmAddDeliverable(): void {
    if (!this.newDeliverable.deliverableName.trim() || !this.newDeliverable.lotName.trim()) return;
    this.api.createDeliverable(this.projectId, { ...this.newDeliverable }).subscribe({
      next: (d) => { this.deliverables.push(d); this.showAddDeliverableModal = false; this.cdr.markForCheck(); }
    });
  }

  updateDeliverableField(deliverable: DeliverableRow, field: string, value: any): void {
    const prev = (deliverable as any)[field];
    (deliverable as any)[field] = value;
    this.cdr.markForCheck();
    this.api.updateDeliverable(this.projectId, deliverable.id, { [field]: value }).subscribe({
      next: (updated) => { Object.assign(deliverable, updated); this.cdr.markForCheck(); },
      error: () => { (deliverable as any)[field] = prev; this.cdr.markForCheck(); }
    });
  }

  deleteDeliverable(deliverable: DeliverableRow): void {
    this.api.deleteDeliverable(this.projectId, deliverable.id).subscribe({
      next: () => { this.deliverables = this.deliverables.filter(d => d.id !== deliverable.id); this.cdr.markForCheck(); }
    });
  }

  statusLabel(s: string): string {
    return { TO_DO: 'To Do', IN_PROGRESS: 'In Progress', SUBMITTED: 'Submitted', APPROVED: 'Approved', BLOCKED: 'Blocked' }[s] ?? s;
  }

  activeTab: 'onePager' | 'dailyCost' | 'workedDays' | 'otherCosts' | 'rebillCosts' | 'billedDays' | 'dailyRate' | 'revenueMoyens' | 'revenueResultats' | 'risks' | 'issues' | 'opportunities' | 'mip' | 'wip' | 'synthese' = 'onePager';

  // ── One Pager — données KPI opérationnels ─────────────────────
  kpiRisks:   any[] = [];
  kpiIssues:  any[] = [];
  kpiOpps:    any[] = [];
  kpiMips:    any[] = [];
  kpiWipRows: any[] = [];
  tenderMargin: number = 0;

  // ── Engagement modules (Revenus — Résultats) ─────────────────────
  engagementModules = { workPackage: false, unitOfWork: false };
  activeResultTab: 'wp' | 'uow' = 'wp';
  showModuleConfig = false; // afficher l'écran de config

  get hasAnyModule(): boolean {
    return this.engagementModules.workPackage || this.engagementModules.unitOfWork;
  }
  get bothModulesActive(): boolean {
    return this.engagementModules.workPackage && this.engagementModules.unitOfWork;
  }

  toggleModule(m: 'workPackage' | 'unitOfWork'): void {
    this.engagementModules[m] = !this.engagementModules[m];
    this.cdr.markForCheck();
  }

  confirmModules(): void {
    if (!this.hasAnyModule) return;
    localStorage.setItem(`pm-modules-${this.projectId}`, JSON.stringify(this.engagementModules));
    // auto-select first active tab
    this.activeResultTab = this.engagementModules.workPackage ? 'wp' : 'uow';
    this.showModuleConfig = false;
    this.cdr.markForCheck();
  }

  openModuleConfig(): void {
    this.showModuleConfig = true;
    this.cdr.markForCheck();
  }

  private loadModules(): void {
    const saved = localStorage.getItem(`pm-modules-${this.projectId}`);
    if (saved) {
      try {
        this.engagementModules = JSON.parse(saved);
        this.activeResultTab = this.engagementModules.workPackage ? 'wp' : 'uow';
      } catch {}
    }
  }
  loading = true;

  // ── Theme switcher ────────────────────────────────────────────────
  readonly themes = [
    { key: 'theme-navy', label: 'Base',   color: '#2D2359' },
    { key: 'theme-blue', label: 'Blue',   color: '#1B9ED4' },
    { key: '',           label: 'Pink',   color: '#C4337A' },
  ];
  currentThemeKey = localStorage.getItem('pm-theme') ?? '';
  showThemePicker = false;

  get currentTheme() { return this.themes.find(t => t.key === this.currentThemeKey) ?? this.themes[0]; }

  setTheme(key: string): void {
    this.currentThemeKey = key;
    localStorage.setItem('pm-theme', key);
    this.showThemePicker = false;
    this.cdr.markForCheck();
  }
  granularity = 'MONTHLY';
  granularityLocked = false;
  granularityOptions = [
    { value: 'MONTHLY', label: 'Monthly', icon: '📅' },
    { value: 'WEEKLY',  label: 'Weekly',  icon: '📆' },
    { value: 'DAILY',   label: 'Daily',   icon: '🗓' },
  ];
  showGranularityModal = false;
  pendingGranularity = '';

  // ── Currency switcher (display only, data always stored in EUR) ──
  readonly currencyOptions = [
    { value: 'EUR', label: 'Euro',   symbol: '€',   rate: 1 },
    { value: 'USD', label: 'Dollar', symbol: '$',   rate: 1.08 },
    { value: 'MAD', label: 'MAD',   symbol: 'MAD', rate: 10.8 },
  ];
  displayCurrency = 'EUR';

  get currencySymbol(): string {
    return this.currencyOptions.find(c => c.value === this.displayCurrency)?.symbol ?? '€';
  }

  get exchangeRate(): number {
    return this.currencyOptions.find(c => c.value === this.displayCurrency)?.rate ?? 1;
  }

  get currencyRatesInfo(): string {
    const rEUR = 1, rUSD = 1.08, rMAD = 10.8;
    const r = (n: number) => Math.round(n * 1000) / 1000;
    switch (this.displayCurrency) {
      case 'USD': return `1 $ = ${r(rEUR / rUSD)} € · 1 $ = ${r(rMAD / rUSD)} MAD`;
      case 'MAD': return `1 MAD = ${r(rEUR / rMAD)} € · 1 MAD = ${r(rUSD / rMAD)} $`;
      default:    return `1 € = ${rUSD} $ · 1 € = ${rMAD} MAD`;
    }
  }

  convert(eurValue: number): number {
    return Math.round(eurValue * this.exchangeRate * 100) / 100;
  }

  switchCurrency(): void {
    const order = ['EUR', 'USD', 'MAD'];
    const idx = order.indexOf(this.displayCurrency);
    this.displayCurrency = order[(idx + 1) % order.length];
    this.cdr.markForCheck();
  }

  // Current month for past-month protection
  readonly currentMonth: string = (() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  })();

  // ── Validation state ─────────────────────────────────────────────
  validationStatus = 'DRAFT'; // DRAFT | SUBMITTED | VALIDATED | REJECTED
  validatedBy = '';
  rejectionComment = '';
  bumName = '';
  showRejectModal = false;
  rejectComment = '';

  // ── Save error toast ─────────────────────────────────────────────
  saveErrorMsg = '';
  saveErrorTimer: any = null;
  showSaveError(msg: string): void {
    this.saveErrorMsg = msg;
    this.cdr.markForCheck();
    if (this.saveErrorTimer) clearTimeout(this.saveErrorTimer);
    this.saveErrorTimer = setTimeout(() => { this.saveErrorMsg = ''; this.cdr.markForCheck(); }, 5000);
  }

  // ── Confirm modals ────────────────────────────────────────────────
  showSubmitModal = false;
  showValidateModal = false;
  showDeleteModal = false;
  pendingDelete: { type: 'resource' | 'category' | 'resources-bulk' | 'resources-all'; id?: number; ids?: number[]; name: string } | null = null;
  selectedResourceIds = new Set<number>();

  get isEditable(): boolean {
    return this.validationStatus === 'DRAFT' || this.validationStatus === 'REJECTED';
  }

  // ── SDH Import ────────────────────────────────────────────────
  showSdhModal = false;
  sdhResult: { imported: number; skipped: number; errors: string[] } | null = null;

  // Add resource modal
  showAddModal = false;
  newResource = { matricule: '', personName: '', contractType: 'Internal subco' };
  contractTypes = ['Anapec', 'External subco', 'Internal subco'];

  // Matricule warning
  showMatriculeWarning = false;
  addResourceError = '';

  // Bulk paste mode
  addMode: 'single' | 'bulk' = 'single';
  bulkPasteText = '';
  bulkPreview: { personName: string; matricule: string; contractType: string; error?: string }[] = [];
  bulkAdding = false;

  get bulkValidCount(): number { return this.bulkPreview.filter(r => !r.error).length; }
  get bulkErrorCount(): number { return this.bulkPreview.filter(r => !!r.error).length; }

  // Add category modal
  showAddCategoryModal = false;
  newCategoryName = '';
  addingForRebill = false;

  get nonRebillCosts(): OtherCostRow[] { return this.otherCosts.filter(c => !c.isRebill); }
  get rebillCostRows(): OtherCostRow[] { return this.otherCosts.filter(c =>  c.isRebill); }
  nonRebillGrandTotal(): number { return this.nonRebillCosts.reduce((s,c) => s + this.otherCostRowTotal(c), 0); }
  rebillCostGrandTotal(): number { return this.rebillCostRows.reduce((s,c) => s + this.otherCostRowTotal(c), 0); }
  showCustomInput = false;

  // Editing cell tracking
  editingCell: { resourceId?: number; category?: string; month: string; field: string } | null = null;
  editValue = '';

  get isBum(): boolean {
    return this.auth.currentUser()?.role === 'BUM';
  }

  get isAdmin(): boolean {
    return this.auth.currentUser()?.role === 'ADMIN';
  }

  constructor(
    private route: ActivatedRoute,
    private api: ApiService,
    private auth: AuthService,
    public i18n: I18nService,
    private cdr: ChangeDetectorRef,
    private location: Location
  ) {}

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadModules();
    const savedLots = localStorage.getItem(`wp-lots-${this.projectId}`);
    if (savedLots) try { this.wpLots = JSON.parse(savedLots); } catch {}
    this.load();
    this.loadDeliverables();
    this.loadWorkTypes();
    this.loadWorkTickets();
    this.api.getRisks(this.projectId).subscribe({ next: d => { this.kpiRisks = d; this.cdr.markForCheck(); } });
    this.api.getIssues(this.projectId).subscribe({ next: d => { this.kpiIssues = d; this.cdr.markForCheck(); } });
    this.api.getOpportunities(this.projectId).subscribe({ next: d => { this.kpiOpps = d; this.cdr.markForCheck(); } });
    this.api.getMips(this.projectId).subscribe({ next: d => { this.kpiMips = d; this.cdr.markForCheck(); } });
    this.api.getWipTable(this.projectId).subscribe({ next: d => { this.kpiWipRows = d; this.cdr.markForCheck(); } });
    this.api.getProject(this.projectId).subscribe({ next: p => { this.tenderMargin = Number(p.marginBudget) || 0; this.cdr.markForCheck(); } });
  }

  goBack(): void {
    this.location.back();
  }

  load(): void {
    this.loading = true;
    this.api.getProjectManagement(this.projectId).subscribe({
      next: (data: any) => {
        this.projectName = data.projectName;
        this.granularity = data.granularity || 'MONTHLY';
        this.granularityLocked = data.granularityLocked || false;
        this.validationStatus = data.validationStatus || 'DRAFT';
        this.validatedBy = data.validatedBy || '';
        this.rejectionComment = data.rejectionComment || '';
        this.bumName = data.bumName || '';
        this.projectEngagementType = data.engagementType || null;

        // Auto-configurer les modules Résultats depuis le type d'engagement du projet
        if (this.isResultatsEngagement) {
          const t = this.projectEngagementType!.toLowerCase();
          this.engagementModules = {
            workPackage: t.includes('wp') || t.includes('work package'),
            unitOfWork:  t.includes('uow') || t.includes('unit of work')
          };
          this.activeResultTab = this.engagementModules.workPackage ? 'wp' : 'uow';
          localStorage.setItem(`pm-modules-${this.projectId}`, JSON.stringify(this.engagementModules));
        }
        this.projectClientName  = data.clientName        || '';
        this.projectBuTrigram   = data.buTrigram          || '';
        this.projectPmName      = data.pmName             || '';
        this.projectCode        = data.projectCode        || '';
        this.projectBusinessId  = data.projectBusinessId  || '';
        this.startDate = data.startDate || null;
        this.endDate = data.endDate || null;
        this.monthlyForecasts = {};
        const monthlyForecasts = data.monthlyForecasts || {};
        for (const k of Object.keys(monthlyForecasts)) {
          this.monthlyForecasts[k] = {
            revenue: Number(monthlyForecasts[k].revenue) || 0,
            cost: Number(monthlyForecasts[k].cost) || 0,
            cov: monthlyForecasts[k].cov != null ? Number(monthlyForecasts[k].cov) : null
          };
        }
        this.deliveryConfidenceLevel  = data.deliveryConfidenceLevel  || null;
        this.healthScoreValue         = data.healthScoreValue         ?? null;
        this.healthScoreStatus        = data.healthScoreStatus        || null;
        this.pmRemarks                = data.pmRemarks                || '';
        this.varianceActualComment    = data.varianceActualComment    || '';
        this.varianceTrendComment     = data.varianceTrendComment     || '';
        this.varianceLandingComment   = data.varianceLandingComment   || '';
        this.tops                     = data.tops                     || '';
        this.flops                    = data.flops                    || '';
        this.months = data.months;
        this.monthStatus = data.monthStatus || {};
        this.resources = data.resources.map((r: any) => ({
          ...r,
          dailyCosts: this.toNumberMap(r.dailyCosts),
          workedDays: this.toNumberMap(r.workedDays),
          billedDays: this.toNumberMap(r.billedDays),
          dailyRates: this.toNumberMap(r.dailyRates),
        }));
        const validResourceIds = new Set(this.resources.map(r => r.id));
        this.selectedResourceIds.forEach(id => { if (!validResourceIds.has(id)) this.selectedResourceIds.delete(id); });
        this.otherCosts = data.otherCosts.map((c: any) => ({
          ...c,
          amounts: this.toNumberMap(c.amounts),
        }));
        this.loading = false;
        // Si granularité pas encore choisie, ouvrir modal obligatoire
        if (!this.granularityLocked) {
          this.pendingGranularity = '';
          this.showGranularityModal = true;
        }
        this.cdr.markForCheck();
      },
      error: () => { this.loading = false; this.cdr.markForCheck(); }
    });
  }

  private toNumberMap(m: Record<string, any>): Record<string, number> {
    const result: Record<string, number> = {};
    for (const k of Object.keys(m)) result[k] = Number(m[k]) || 0;
    return result;
  }

  // ── Granularity ──────────────────────────────────────────────────
  get granularityLabel(): string {
    return this.granularityOptions.find(o => o.value === this.granularity)?.label ?? this.granularity;
  }

  get granularityIcon(): string {
    return this.granularityOptions.find(o => o.value === this.granularity)?.icon ?? '📅';
  }

  openGranularityModal(): void {
    if (this.granularityLocked) return;
    this.pendingGranularity = this.granularity;
    this.showGranularityModal = true;
    this.cdr.markForCheck();
  }

  confirmGranularity(): void {
    if (!this.pendingGranularity) return;
    this.api.setGranularity(this.projectId, this.pendingGranularity, 'EUR').subscribe({
      next: () => {
        this.showGranularityModal = false;
        this.load();
      },
      error: (err) => {
        alert(err.error?.error || 'Unable to set granularity');
        this.cdr.markForCheck();
      }
    });
  }

  selectGranularity(value: string): void {
    this.pendingGranularity = value;
    this.cdr.markForCheck();
  }

  // ── Validation actions ────────────────────────────────────────
  submitForValidation(): void {
    this.showSubmitModal = true;
    this.cdr.markForCheck();
  }

  confirmSubmit(): void {
    this.showSubmitModal = false;
    this.api.submitForValidation(this.projectId).subscribe({
      next: () => { this.load(); },
      error: (e) => { alert(e.error?.error || 'Erreur lors de la soumission'); this.cdr.markForCheck(); }
    });
  }

  openRejectModal(): void { this.rejectComment = ''; this.showRejectModal = true; this.cdr.markForCheck(); }

  confirmReject(): void {
    if (!this.rejectComment.trim()) return;
    this.api.rejectProject(this.projectId, this.rejectComment).subscribe({
      next: () => { this.showRejectModal = false; this.load(); },
      error: (e) => { alert(e.error?.error || 'Erreur lors du rejet'); this.cdr.markForCheck(); }
    });
  }

  validateProject(): void {
    this.showValidateModal = true;
    this.cdr.markForCheck();
  }

  confirmValidate(): void {
    this.showValidateModal = false;
    this.api.validateProject(this.projectId).subscribe({
      next: () => { this.load(); },
      error: (e) => { alert(e.error?.error || 'Erreur lors de la validation'); this.cdr.markForCheck(); }
    });
  }

  // ── SDH Import ────────────────────────────────────────────────
  onSdhFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    this.api.importSdhFile(this.projectId, file).subscribe({
      next: (result) => {
        this.sdhResult = result;
        this.showSdhModal = true;
        this.load();
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.sdhResult = { imported: 0, skipped: 0, errors: [e.error?.error || 'Erreur import SDH'] };
        this.showSdhModal = true;
        this.cdr.markForCheck();
      }
    });
    input.value = '';
  }

  // ── Past period protection ───────────────────────────────────────
  isPastMonth(p: string): boolean {
    if (this.granularity === 'MONTHLY') return p < this.currentMonth;
    if (this.granularity === 'DAILY')   return p < new Date().toISOString().slice(0, 10);
    if (this.granularity === 'WEEKLY') {
      const currentWeek = this.getCurrentWeekKey();
      return p < currentWeek;
    }
    return false;
  }

  isCurrentMonth(p: string): boolean {
    if (this.granularity === 'MONTHLY') return p === this.currentMonth;
    if (this.granularity === 'DAILY')   return p === new Date().toISOString().slice(0, 10);
    if (this.granularity === 'WEEKLY')  return p === this.getCurrentWeekKey();
    return false;
  }

  // ── Réel / Prévision par mois ──────────────────────────────────────
  monthStatusFor(p: string): 'REAL' | 'FORECAST' {
    return this.monthStatus[p] || (this.isPastMonth(p) || this.isCurrentMonth(p) ? 'REAL' : 'FORECAST');
  }

  toggleMonthStatus(p: string): void {
    if (this.savingMonthStatus[p]) return;
    const next: 'REAL' | 'FORECAST' = this.monthStatusFor(p) === 'REAL' ? 'FORECAST' : 'REAL';
    this.savingMonthStatus[p] = true;
    this.api.updateMonthStatus(this.projectId, [{ month: p, status: next }]).subscribe({
      next: () => {
        this.monthStatus[p] = next;
        this.savingMonthStatus[p] = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.savingMonthStatus[p] = false;
        this.cdr.markForCheck();
      }
    });
  }

  private getCurrentWeekKey(): string {
    const now = new Date();
    const startOfYear = new Date(now.getFullYear(), 0, 1);
    const dayOfYear = Math.floor((now.getTime() - startOfYear.getTime()) / 86400000);
    const weekNum = Math.ceil((dayOfYear + startOfYear.getDay() + 1) / 7);
    return `${now.getFullYear()}-W${String(weekNum).padStart(2, '0')}`;
  }

  // ── Period formatting ────────────────────────────────────────────
  formatMonth(p: string): string {
    if (!p || p === '0000-00') return '';
    if (this.granularity === 'WEEKLY') {
      // "2026-W03" → "S03 26"
      const [y, w] = p.split('-W');
      return `S${w} '${y.slice(2)}`;
    }
    if (this.granularity === 'DAILY') {
      // "2026-01-15" → "15/01"
      const [, mo, dd] = p.split('-');
      return `${dd}/${mo}`;
    }
    // Monthly: "2026-01" → "Jan 26"
    const [y, mo] = p.split('-');
    const months = ['Jan','Fév','Mar','Avr','Mai','Jun','Jul','Aoû','Sep','Oct','Nov','Déc'];
    return `${months[parseInt(mo) - 1]} ${y.slice(2)}`;
  }

  // ── Tab navigation ───────────────────────────────────────────────
  setTab(tab: typeof this.activeTab): void {
    this.activeTab = tab;
    this.editingCell = null;
    this.cdr.markForCheck();
  }

  // ── Resource totals ──────────────────────────────────────────────
  resourceMonthlyTotal(field: keyof Pick<ResourceRow,'dailyCosts'|'workedDays'|'billedDays'|'dailyRates'>, month: string): number {
    return this.resources.reduce((s, r) => s + (r[field][month] || 0), 0);
  }

  resourceMonthlyAvg(field: keyof Pick<ResourceRow,'dailyCosts'|'workedDays'|'billedDays'|'dailyRates'>, month: string): number {
    const vals = this.resources.map(r => r[field][month] || 0).filter(v => v > 0);
    return vals.length ? vals.reduce((s, v) => s + v, 0) / vals.length : 0;
  }

  resourceRowTotal(row: ResourceRow, field: keyof Pick<ResourceRow,'dailyCosts'|'workedDays'|'billedDays'|'dailyRates'>): number {
    if (field === 'dailyRates' || field === 'dailyCosts') {
      const vals = this.months.map(m => row[field][m] || 0).filter(v => v > 0);
      return vals.length ? vals.reduce((s, v) => s + v, 0) / vals.length : 0;
    }
    return this.months.reduce((s, m) => s + (row[field][m] || 0), 0);
  }

  grandTotal(field: keyof Pick<ResourceRow,'dailyCosts'|'workedDays'|'billedDays'|'dailyRates'>): number {
    if (field === 'dailyCosts') {
      const vals = this.resources.map(r => this.resourceRowTotal(r, field)).filter(v => v > 0);
      return vals.length ? vals.reduce((s, v) => s + v, 0) / vals.length : 0;
    }
    return this.resources.reduce((s, r) => s + this.resourceRowTotal(r, field), 0);
  }

  // ── Other cost totals ────────────────────────────────────────────
  otherCostMonthlyTotal(month: string): number {
    return this.otherCosts.reduce((s, c) => s + (c.amounts[month] || 0), 0);
  }

  otherCostRowTotal(row: OtherCostRow): number {
    return this.months.reduce((s, m) => s + (row.amounts[m] || 0), 0);
  }

  otherCostGrandTotal(): number {
    return this.otherCosts.reduce((s, c) => s + this.otherCostRowTotal(c), 0);
  }

  // ── Financial synthesis calculations ────────────────────────────
  laborCostForMonth(m: string): number {
    return this.resources.reduce((s, r) => s + (r.dailyCosts[m] || 0) * (r.workedDays[m] || 0), 0);
  }

  otherCostForMonth(m: string): number {
    // exclude marker month
    return this.otherCosts.reduce((s, c) => s + (c.amounts[m] || 0), 0);
  }

  rebillForMonth(m: string): number {
    return this.otherCosts.filter(c => c.isRebill).reduce((s, c) => s + (c.amounts[m] || 0), 0);
  }

  revenueForMonth(m: string): number {
    const billing = this.resources.reduce((s, r) => s + (r.dailyRates[m] || 0) * (r.billedDays[m] || 0), 0);
    return billing + this.rebillForMonth(m);
  }

  // ── Revenue Moyens helpers ────────────────────────────────────────
  resourceMoyensForMonth(r: ResourceRow, m: string): number {
    return (r.dailyRates[m] || 0) * (r.billedDays[m] || 0);
  }

  resourceMoyensRowTotal(r: ResourceRow): number {
    return this.months.reduce((s, m) => s + this.resourceMoyensForMonth(r, m), 0);
  }

  moyensBillingMonthTotal(m: string): number {
    return this.resources.reduce((s, r) => s + this.resourceMoyensForMonth(r, m), 0);
  }

  moyensGrandTotal(): number {
    return this.months.reduce((s, m) => s + this.revenueForMonth(m), 0);
  }

  rebillRowTotal(): number {
    return this.months.reduce((s, m) => s + this.rebillForMonth(m), 0);
  }

  get hasRebillCosts(): boolean {
    return this.otherCosts.some(c => c.isRebill);
  }

  billingGrandTotal(): number {
    return this.months.reduce((s, m) => s + this.moyensBillingMonthTotal(m), 0);
  }

  nonRebillCostForMonth(m: string): number {
    return this.otherCosts.filter(c => !c.isRebill).reduce((s, c) => s + (c.amounts[m] || 0), 0);
  }

  directCostForMonth(m: string): number {
    return this.laborCostForMonth(m) + this.nonRebillCostForMonth(m);
  }

  marginForMonth(m: string): number {
    return this.revenueForMonth(m) - this.directCostForMonth(m);
  }

  marginPctForMonth(m: string): number {
    const rev = this.revenueForMonth(m);
    return rev > 0 ? (this.marginForMonth(m) / rev) * 100 : 0;
  }

  totalLaborCost(): number    { return this.months.reduce((s, m) => s + this.laborCostForMonth(m), 0); }
  totalOtherCost(): number    { return this.months.reduce((s, m) => s + this.nonRebillCostForMonth(m), 0); }
  totalRebill(): number       { return this.months.reduce((s, m) => s + this.rebillForMonth(m), 0); }
  totalRevenue(): number      { return this.months.reduce((s, m) => s + this.revenueForMonth(m), 0); }
  totalDirectCost(): number   { return this.months.reduce((s, m) => s + this.directCostForMonth(m), 0); }
  totalMargin(): number       { return this.totalRevenue() - this.totalDirectCost(); }
  totalMarginPct(): number    { return this.totalRevenue() > 0 ? (this.totalMargin() / this.totalRevenue()) * 100 : 0; }

  // ══════════════════════════════════════════════════════════════
  // ONE PAGER
  // ══════════════════════════════════════════════════════════════
  workedDaysForMonth(m: string): number {
    return this.resources.reduce((s, r) => s + (r.workedDays[m] || 0), 0);
  }

  billedDaysForMonth(m: string): number {
    return this.resources.reduce((s, r) => s + (r.billedDays[m] || 0), 0);
  }

  /** Les prévisions mensuelles (revenue/cost/cov) sont saisies en K€ : on convertit en € ici. */
  private readonly FORECAST_SCALE = 1000;

  covForMonth(m: string): number {
    return (this.monthlyForecasts[m]?.cov || 0) * this.FORECAST_SCALE;
  }

  budgetRevenueForMonth(m: string): number {
    return (this.monthlyForecasts[m]?.revenue || 0) * this.FORECAST_SCALE;
  }

  budgetCostForMonth(m: string): number {
    return (this.monthlyForecasts[m]?.cost || 0) * this.FORECAST_SCALE;
  }

  /** Dernier mois marqué comme Réel (pour le label "Au: ..."). */
  lastRealMonth(): string | null {
    let last: string | null = null;
    for (const m of this.months) {
      if (this.monthStatusFor(m) === 'REAL') last = m;
    }
    return last;
  }

  sumByStatus(valueFn: (m: string) => number, status: 'REAL' | 'FORECAST' | 'ALL'): number {
    return this.months
      .filter(m => status === 'ALL' || this.monthStatusFor(m) === status)
      .reduce((s, m) => s + valueFn(m), 0);
  }

  budgetRevenueTotal(): number {
    return Object.keys(this.monthlyForecasts).reduce((s, m) => s + this.budgetRevenueForMonth(m), 0);
  }

  budgetCostTotal(): number {
    return Object.keys(this.monthlyForecasts).reduce((s, m) => s + this.budgetCostForMonth(m), 0);
  }

  budgetMarginPct(): number {
    const rev = this.budgetRevenueTotal();
    const cost = this.budgetCostTotal();
    return rev > 0 ? ((rev - cost) / rev) * 100 : 0;
  }

  marginGaugeColor(pct: number): string {
    if (pct >= 20) return '#1b8a4f';
    if (pct >= 0) return '#3B5BDB';
    return '#c62828';
  }

  budgetRevenueByStatus(status: 'REAL' | 'FORECAST' | 'ALL'): number {
    return this.sumByStatus(m => this.budgetRevenueForMonth(m), status);
  }

  /** DVI% = Marge % à l'atterrissage (EAC) − Marge % budgétée (Target) */
  get dviPct(): number {
    return this.totalMarginPct() - this.budgetMarginPct();
  }

  /** DVI€ = DVI% × Revenu à l'atterrissage (EAC) */
  get dviEuro(): number {
    return (this.dviPct / 100) * this.totalRevenue();
  }

  /** OverRun = Coûts à l'atterrissage (EAC) − Coûts budgétés */
  get overRun(): number {
    return this.totalDirectCost() - this.budgetCostTotal();
  }

  /** Variance Δ R-B (Revenue €) : Revenu Réel − Revenu Budget, par période */
  varianceActualRevenue(): number  { return this.sumByStatus(m => this.revenueForMonth(m), 'REAL') - this.budgetRevenueByStatus('REAL'); }
  varianceTrendRevenue(): number   { return this.sumByStatus(m => this.revenueForMonth(m), 'FORECAST') - this.budgetRevenueByStatus('FORECAST'); }
  varianceLandingRevenue(): number { return this.sumByStatus(m => this.revenueForMonth(m), 'ALL') - this.budgetRevenueByStatus('ALL'); }

  get onePagerRows(): OnePagerRow[] {
    const revReal = this.sumByStatus(m => this.revenueForMonth(m), 'REAL');
    const revFcst = this.sumByStatus(m => this.revenueForMonth(m), 'FORECAST');
    const revLand = revReal + revFcst;

    const costReal = this.sumByStatus(m => this.directCostForMonth(m), 'REAL');
    const costFcst = this.sumByStatus(m => this.directCostForMonth(m), 'FORECAST');
    const costLand = costReal + costFcst;

    const budgetRevReal = this.sumByStatus(m => this.budgetRevenueForMonth(m), 'REAL');
    const budgetRevFcst = this.sumByStatus(m => this.budgetRevenueForMonth(m), 'FORECAST');
    const budgetRevLand = budgetRevReal + budgetRevFcst;

    const budgetCostReal = this.sumByStatus(m => this.budgetCostForMonth(m), 'REAL');
    const budgetCostFcst = this.sumByStatus(m => this.budgetCostForMonth(m), 'FORECAST');
    const budgetCostLand = budgetCostReal + budgetCostFcst;

    const workedReal = this.sumByStatus(m => this.workedDaysForMonth(m), 'REAL');
    const workedFcst = this.sumByStatus(m => this.workedDaysForMonth(m), 'FORECAST');
    const workedLand = workedReal + workedFcst;

    const billedReal = this.sumByStatus(m => this.billedDaysForMonth(m), 'REAL');
    const billedFcst = this.sumByStatus(m => this.billedDaysForMonth(m), 'FORECAST');
    const billedLand = billedReal + billedFcst;

    const covReal = this.sumByStatus(m => this.covForMonth(m), 'REAL');
    const covFcst = this.sumByStatus(m => this.covForMonth(m), 'FORECAST');

    const marginReal = revReal - costReal;
    const marginFcst = revFcst - costFcst;
    const marginLand = revLand - costLand;

    const budgetMarginReal = budgetRevReal - budgetCostReal;
    const budgetMarginFcst = budgetRevFcst - budgetCostFcst;
    const budgetMarginLand = budgetRevLand - budgetCostLand;

    const ratio = (num: number, den: number) => den > 0 ? num / den : 0;

    // Dernier mois Réel (colonne "Real <mois>")
    const lm = this.lastRealMonth();
    const revLM        = lm ? this.revenueForMonth(lm)                 : 0;
    const costLM       = lm ? this.directCostForMonth(lm)              : 0;
    const budgetRevLM  = lm ? this.budgetRevenueForMonth(lm) : 0;
    const budgetCostLM = lm ? this.budgetCostForMonth(lm)    : 0;
    const workedLM     = lm ? this.workedDaysForMonth(lm)              : 0;
    const billedLM     = lm ? this.billedDaysForMonth(lm)              : 0;
    const covLM        = lm ? this.covForMonth(lm)                     : 0;
    const marginLM       = revLM - costLM;
    const budgetMarginLM = budgetRevLM - budgetCostLM;

    return [
      { label: 'COV - Client Order Value', real: covReal,         lastMonth: covLM,        forecast: covFcst,        landing: covReal + covFcst, format: 'currency' },
      { label: 'Revenue - Budget (TCV)',   real: budgetRevReal,    lastMonth: budgetRevLM,  forecast: budgetRevFcst,  landing: budgetRevLand,  format: 'currency' },
      { label: 'Revenue - Real',           real: revReal,          lastMonth: revLM,        forecast: revFcst,        landing: revLand,        format: 'currency' },
      { label: 'Cost - Budget',            real: budgetCostReal,   lastMonth: budgetCostLM, forecast: budgetCostFcst, landing: budgetCostLand, format: 'currency' },
      { label: 'Direct Costs - Real',      real: costReal,         lastMonth: costLM,       forecast: costFcst,       landing: costLand,       format: 'currency' },
      { label: '% BM Target',              real: ratio(budgetMarginReal, budgetRevReal) * 100, lastMonth: ratio(budgetMarginLM, budgetRevLM) * 100, forecast: ratio(budgetMarginFcst, budgetRevFcst) * 100, landing: ratio(budgetMarginLand, budgetRevLand) * 100, format: 'percent' },
      { label: 'BM - Real',                real: marginReal,       lastMonth: marginLM,     forecast: marginFcst,     landing: marginLand,     format: 'currency', marginRow: true },
      { label: '% BM Real',               real: ratio(marginReal, revReal) * 100, lastMonth: ratio(marginLM, revLM) * 100, forecast: ratio(marginFcst, revFcst) * 100, landing: ratio(marginLand, revLand) * 100, format: 'percent', marginRow: true },
      { label: 'Worked Day',                real: workedReal, lastMonth: workedLM, forecast: workedFcst, landing: workedLand, format: 'days' },
      { label: 'PROR',                      real: ratio(billedReal, workedReal) * 100, lastMonth: ratio(billedLM, workedLM) * 100, forecast: ratio(billedFcst, workedFcst) * 100, landing: ratio(billedLand, workedLand) * 100, format: 'percent' },
      { label: 'ADR',                       real: ratio(revReal, workedReal),  lastMonth: ratio(revLM, workedLM),   forecast: ratio(revFcst, workedFcst),  landing: ratio(revLand, workedLand),  format: 'rate' },
      { label: 'ADC',                       real: ratio(costReal, workedReal), lastMonth: ratio(costLM, workedLM), forecast: ratio(costFcst, workedFcst), landing: ratio(costLand, workedLand), format: 'rate' },
    ];
  }

  formatRowValue(row: OnePagerRow, value: number): string {
    switch (row.format) {
      case 'percent': return this.fmtPct(value);
      case 'days':    return this.fmt(value, 1);
      default:        return `${this.cfmt(value)} ${this.currencySymbol}`;
    }
  }

  // ══════════════════════════════════════════════════════════════
  // ONE PAGER — KPI OPÉRATIONNELS
  // ══════════════════════════════════════════════════════════════

  private get todayStr(): string { return new Date().toISOString().slice(0, 10); }

  // ── Risks ──────────────────────────────────────────────────────
  private get openRisks(): any[] { return this.kpiRisks.filter(r => r.status !== 'Closed'); }

  get riskHighCriticalPct(): string {
    const open = this.openRisks;
    const hc = open.filter(r => r.rating === 'High' || r.rating === 'Critical');
    return open.length ? `${Math.round(hc.length / open.length * 100)}% (${hc.length} / ${open.length})` : '0% (0 / 0)';
  }

  get riskMitigationPct(): string {
    const open = this.openRisks;
    const with_ = open.filter(r => r.mitigationAction?.trim());
    return open.length ? `${Math.round(with_.length / open.length * 100)}% (${with_.length} / ${open.length})` : '0% (0 / 0)';
  }

  get riskOverduePct(): string {
    const open = this.openRisks;
    const od = open.filter(r => r.deadline && r.deadline < this.todayStr);
    return open.length ? `${Math.round(od.length / open.length * 100)}% (${od.length} / ${open.length})` : '0% (0 / 0)';
  }

  get riskReduction(): string {
    const grossExp = this.openRisks.reduce((s, r) => s + ((r.percentProbability || 0) / 100) * (r.costs || 0), 0);
    const netExp   = this.openRisks.reduce((s, r) => s + (r.net || 0), 0);
    const pct      = grossExp > 0 ? Math.round((grossExp - netExp) / grossExp * 100) : 0;
    return grossExp > 0 ? `${pct}%` : '0%';
  }

  get riskExposure(): string {
    const gross = this.openRisks.reduce((s, r) => s + (r.probEval || 0) * (r.impactEval || 0), 0);
    const net   = this.openRisks.reduce((s, r) => s + ((r.probabilityResidual || 0) / 100) * (r.impactEval || 0), 0);
    return gross > 0 ? `${Math.round(gross)} → ${Math.round(net)}` : '0 → 0';
  }

  // ── Issues ─────────────────────────────────────────────────────
  get openIssues(): any[] { return this.kpiIssues.filter(i => i.status === 'Open' || i.status === 'Reopened'); }

  get issueOpenPct(): string {
    const n = this.kpiIssues.length;
    const o = this.openIssues.length;
    return n ? `${Math.round(o / n * 100)}% (${o} / ${n})` : '0% (0 / 0)';
  }

  get issueHighCriticalPct(): string {
    const open = this.openIssues;
    const hc = open.filter(i => i.severity === 'High' || i.impacts === 'Critical Impact');
    return open.length ? `${Math.round(hc.length / open.length * 100)}% (${hc.length} / ${open.length})` : '0% (0 / 0)';
  }

  get issueOverduePct(): string {
    const open = this.openIssues;
    const od = open.filter(i => i.deadline && i.deadline < this.todayStr);
    return open.length ? `${Math.round(od.length / open.length * 100)}% (${od.length} / ${open.length})` : '0% (0 / 0)';
  }

  get issueNetChange30j(): string {
    const cutoff = new Date(Date.now() - 30 * 864e5).toISOString().slice(0, 10);
    const recent = this.kpiIssues.filter(i => (i.identificationDate || i.createdAt || '') >= cutoff).length;
    const n = this.kpiIssues.length;
    return n ? `${Math.round(recent / n * 100)}% (${recent} / ${n})` : '0% (0 / 0)';
  }

  // ── Opportunities ──────────────────────────────────────────────
  private get pipelineOpps(): any[] { return this.kpiOpps.filter(o => o.status !== 'Won' && o.status !== 'Lost'); }

  get oppPipelineValue(): string {
    const t = this.pipelineOpps.reduce((s, o) => s + (o.estimatedBenefit || 0), 0);
    return `${Math.round(t / 1000)} k€`;
  }

  get oppAvgMargin(): string {
    const p = this.pipelineOpps;
    const totalBenefit = p.reduce((s, o) => s + (o.estimatedBenefit || 0), 0);
    const totalPrice   = p.reduce((s, o) => s + (o.price || 0), 0);
    return totalPrice > 0 ? `${Math.round(totalBenefit / totalPrice * 100)}%` : '0%';
  }

  get oppWinRate(): string {
    const won  = this.kpiOpps.filter(o => o.status === 'Won').length;
    const closed = this.kpiOpps.filter(o => o.status === 'Won' || o.status === 'Lost').length;
    return closed ? `${Math.round(won / closed * 100)}%` : '0%';
  }

  get oppRealizedBenefit(): string {
    const t = this.kpiOpps.filter(o => o.status === 'Won' && o.copilValidation === 'Validated').reduce((s, o) => s + (o.estimatedBenefit || 0), 0);
    return `${Math.round(t / 1000)} k€`;
  }

  // ── MIP ────────────────────────────────────────────────────────
  get mipSecuredGains(): string {
    const t = this.kpiMips.filter(m => m.status === 'Completed').reduce((s, m) => s + (m.realizedGain || 0), 0);
    return `${Math.round(t / 1000)} k€`;
  }

  get mipActivePipeline(): string {
    const t = this.kpiMips.filter(m => m.status !== 'Completed' && m.status !== 'Cancelled').reduce((s, m) => s + (m.plannedGain || 0), 0);
    return `${Math.round(t / 1000)} k€`;
  }

  get mipContributionPct(): string {
    const secured = this.kpiMips.filter(m => m.status === 'Completed').reduce((s, m) => s + (m.realizedGain || 0), 0);
    const forecastMargin = this.budgetRevenueTotal() - this.budgetCostTotal();
    return forecastMargin > 0 ? `${Math.round(secured / forecastMargin * 100)}%` : '0%';
  }

  // ── WIP ────────────────────────────────────────────────────────
  private get pastWipRows(): any[] {
    const ym = this.todayStr.slice(0, 7);
    return this.kpiWipRows.filter(r => r.period <= ym);
  }

  get wipTotalDeclared(): string {
    const t = this.pastWipRows.reduce((s, r) => s + Math.max((r.delta ?? 0), 0), 0);
    return t ? `${this.fmt(t)} €` : '0 €';
  }

  get wipInvoicedPctNum(): number {
    const all  = this.pastWipRows;
    const decl = all.reduce((s, r) => s + (r.declaredAmount || 0), 0);
    const declWithInvoice = all.filter(r => (r.invoicedAmount || 0) > 0)
                               .reduce((s, r) => s + (r.declaredAmount || 0), 0);
    return decl > 0 ? declWithInvoice / decl * 100 : 0;
  }

  get wipInvoicedPct(): string {
    return `${Math.round(this.wipInvoicedPctNum)}%`;
  }

  get wipOverdueInvoices(): string {
    // Overdue Invoices : factures émises (invoicedAmount > 0) sur périodes > 30j (proxy délai paiement)
    const cutoff30 = new Date(Date.now() - 30 * 864e5).toISOString().slice(0, 7);
    const od  = this.pastWipRows.filter(r => r.period < cutoff30 && (r.invoicedAmount || 0) > 0);
    const amt = od.reduce((s, r) => s + (r.invoicedAmount || 0), 0);
    return amt > 0 ? `${this.fmt(amt)} €` : '0 €';
  }

  get wipAging60j(): string {
    // WIP Aging >60j : WIP non facturé (invoicedAmount = 0) sur périodes > 60j / Total WIP
    const cutoff60 = new Date(Date.now() - 60 * 864e5).toISOString().slice(0, 7);
    const aged    = this.pastWipRows.filter(r => r.period < cutoff60 && (r.invoicedAmount || 0) === 0);
    const agedAmt = aged.reduce((s, r) => s + (r.declaredAmount || 0), 0);
    const totalWip = this.pastWipRows.reduce((s, r) => s + Math.max((r.delta ?? 0), 0), 0);
    return totalWip > 0 ? `${Math.round(agedAmt / totalWip * 100)}%` : '0%';
  }

  // ── Health Score — formules RIO Team Manager Handbook ─────────

  // Score Finance (35%) : Écart PM = PM% EAC − PM% Budget EAC
  get scoreFinance(): number {
    const ecart = this.dviPct;
    if (ecart >  0)   return 100;
    if (ecart >= -2)  return 85;
    if (ecart >= -5)  return 70;
    if (ecart >= -8)  return 50;
    if (ecart >= -10) return 30;
    return 15;
  }

  // Score Risks (20%) : Total = union (High/Critical OU Overdue), chaque risque compté 1 fois
  get scoreRisks(): number {
    const flagged = this.openRisks.filter(r =>
      r.rating === 'High' || r.rating === 'Critical' ||
      (r.deadline && r.deadline < this.todayStr)
    ).length;
    if (flagged === 0)  return 100;
    if (flagged <= 2)   return 80;
    if (flagged <= 5)   return 60;
    if (flagged <= 10)  return 40;
    return 20;
  }

  get openCriticalCount(): number { return this.openRisks.filter(r => r.rating === 'Critical').length; }
  get openHighCount():     number { return this.openRisks.filter(r => r.rating === 'High').length; }

  get openIssuesPct(): number {
    const n = this.kpiIssues.length;
    return n > 0 ? (this.openIssues.length / n) * 100 : 0;
  }

  // Score Issues (15%) : Total = issues Critical ET Overdue simultanément
  get scoreIssues(): number {
    const flagged = this.openIssues.filter(i =>
      i.severity === 'Critical' && (i.deadline && i.deadline < this.todayStr)
    ).length;
    if (flagged === 0)  return 100;
    if (flagged <= 3)   return 80;
    if (flagged <= 8)   return 60;
    if (flagged <= 15)  return 40;
    return 20;
  }

  // Score Facturation (20%) : Collection Rate % = wipInvoicedPctNum
  get scoreFacturation(): number {
    const pct = this.wipInvoicedPctNum;
    if (pct >= 95) return 100;
    if (pct >= 90) return 85;
    if (pct >= 80) return 70;
    if (pct >= 70) return 50;
    if (pct >= 60) return 30;
    return 15;
  }

  get mipSecuredEur(): number {
    return this.kpiMips.filter(m => m.status === 'Completed').reduce((s, m) => s + (m.realizedGain || 0), 0);
  }
  get mipTargetEur(): number {
    return this.tenderMargin * 0.05; // 5% de la Tender Margin (marginBudget du projet)
  }

  // Score MIP (10%) : Completion Rate + Delayed MIPs
  get mipCompletionRate(): number {
    if (!this.kpiMips.length) return 0;
    return (this.kpiMips.filter(m => m.status === 'Completed').length / this.kpiMips.length) * 100;
  }

  get mipDelayedCount(): number {
    return this.kpiMips.filter(m =>
      m.status !== 'Completed' && m.deadline && m.deadline < this.todayStr
    ).length;
  }

  get scoreMIP(): number {
    const rate    = this.mipCompletionRate;
    const delayed = this.mipDelayedCount;
    if (rate >= 90 && delayed === 0) return 100;
    if (rate >= 80 && delayed <= 2)  return 80;
    if (rate >= 70)                  return 60;
    if (rate >= 50)                  return 40;
    return 20;
  }

  /** Health Score = Finance×35% + Risks×20% + Issues×15% + Facturation×20% + MIP×10% */
  get healthScoreCalc(): number {
    return Math.round(
      this.scoreFinance     * 0.35 +
      this.scoreRisks       * 0.20 +
      this.scoreIssues      * 0.15 +
      this.scoreFacturation * 0.20 +
      this.scoreMIP         * 0.10
    );
  }

  scoreClass(s: number): string {
    if (s >= 80) return 'score-green';
    if (s >= 50) return 'score-orange';
    return 'score-red';
  }

  // ── Export PDF ────────────────────────────────────────────────────
  @ViewChild('onepagerPanel') onepagerPanel!: ElementRef;
  exportingPdf = false;

  async exportOnePagerPdf(): Promise<void> {
    if (this.exportingPdf) return;
    this.exportingPdf = true;
    this.cdr.markForCheck();
    try {
      const html2canvas = (await import('html2canvas')).default;
      const { jsPDF }   = await import('jspdf');
      const el: HTMLElement = this.onepagerPanel.nativeElement;

      const canvas = await html2canvas(el, {
        scale: 2,
        useCORS: true,
        logging: false,
        backgroundColor: '#f8fafc',
      });

      const imgW  = 297;               // A4 landscape width mm
      const imgH  = (canvas.height * imgW) / canvas.width;
      const doc   = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
      const pages = Math.ceil(imgH / 210);  // A4 landscape height = 210 mm

      for (let p = 0; p < pages; p++) {
        if (p > 0) doc.addPage();
        doc.addImage(
          canvas.toDataURL('image/jpeg', 0.92),
          'JPEG', 0, -p * 210, imgW, imgH
        );
      }

      const filename = `OnePager_${this.projectBusinessId || 'project'}_${new Date().toISOString().slice(0,10)}.pdf`;
      doc.save(filename);
    } finally {
      this.exportingPdf = false;
      this.cdr.markForCheck();
    }
  }

  // ── Fixed tooltip (position: fixed, never clipped by overflow) ────
  tipId  = '';
  tipX   = 0;
  tipY   = 0;

  showTip(event: MouseEvent, id: string): void {
    const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
    this.tipId = id;
    this.tipX  = rect.left - 228;   // open to the LEFT of the icon
    this.tipY  = rect.top + rect.height / 2;
    // Keep tooltip inside the viewport left edge
    if (this.tipX < 8) {
      this.tipX = rect.right + 10;  // fallback: open to the RIGHT
    }
    this.cdr.markForCheck();
  }

  hideTip(): void {
    this.tipId = '';
    this.cdr.markForCheck();
  }

  /** Convertit le Health Score calculé (0-100) en valeur RAYG automatique. */
  get ragFromScore(): string {
    const s = this.healthScoreCalc;
    if (s >= 80) return 'ON_TRACK';
    if (s >= 60) return 'MINOR_RISKS';
    if (s >= 40) return 'AT_RISK';
    return 'OFF_TRACK';
  }

  // ── Charts ────────────────────────────────────────────────────────
  get chartMonthlyTrendData(): ChartData<'bar'> {
    const labels = this.months.map(m => this.formatMonth(m));
    const costColor    = '#f97316';
    const costColorEtc = 'rgba(249,115,22,0.30)';
    const revColor    = '#0ea5e9';
    const revColorEtc = 'rgba(14,165,233,0.30)';

    let cumRev = 0, cumCost = 0;
    const cumRevenue: number[] = [];
    const cumCostArr: number[] = [];
    const margePct: number[] = [];

    this.months.forEach(m => {
      cumRev  += this.convert(this.revenueForMonth(m));
      cumCost += this.convert(this.directCostForMonth(m));
      cumRevenue.push(cumRev);
      cumCostArr.push(cumCost);
      margePct.push(cumRev > 0 ? ((cumRev - cumCost) / cumRev) * 100 : 0);
    });

    const isReal   = this.months.map(m => this.monthStatusFor(m) === 'REAL');
    const targetPct = this.budgetMarginPct();

    return {
      labels,
      datasets: [
        {
          type: 'bar',
          label: 'Cost YtD',
          data: cumCostArr.map((v, i) => isReal[i] ? v : null),
          backgroundColor: costColor,
          yAxisID: 'y',
          order: 3
        },
        {
          type: 'bar',
          label: 'Revenu YtD',
          data: cumRevenue.map((v, i) => isReal[i] ? v : null),
          backgroundColor: revColor,
          yAxisID: 'y',
          order: 3
        },
        {
          type: 'bar',
          label: 'Cost ETC',
          data: cumCostArr.map((v, i) => isReal[i] ? null : v),
          backgroundColor: costColorEtc,
          yAxisID: 'y',
          order: 3
        },
        {
          type: 'bar',
          label: 'Revenu ETC',
          data: cumRevenue.map((v, i) => isReal[i] ? null : v),
          backgroundColor: revColorEtc,
          yAxisID: 'y',
          order: 3
        },
        {
          type: 'line',
          label: '%Marge',
          data: margePct,
          borderColor: '#10b981',
          backgroundColor: '#10b981',
          pointBackgroundColor: '#10b981',
          pointStyle: 'rectRot',
          pointRadius: 4,
          tension: 0.3,
          yAxisID: 'y1',
          order: 1
        },
        {
          type: 'line',
          label: 'PM Target',
          data: this.months.map(() => targetPct),
          borderColor: '#ef4444',
          backgroundColor: '#ef4444',
          pointRadius: 0,
          borderWidth: 2,
          yAxisID: 'y1',
          order: 2
        }
      ]
    } as unknown as ChartData<'bar'>;
  }

  readonly chartMonthlyTrendOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    plugins: {
      legend: { position: 'bottom', labels: { boxWidth: 12, font: { size: 11 } } },
      tooltip: {
        callbacks: {
          label: (ctx: any) => {
            const label = ctx.dataset.label || '';
            const value = ctx.parsed.y;
            if (value == null) return '';
            if (label === '%Marge' || label === 'PM Target') {
              return `${label}: ${this.fmt(value, 1)}%`;
            }
            return `${label}: ${this.fmt(value)} ${this.currencySymbol}`;
          }
        }
      }
    },
    scales: {
      x: { ticks: { font: { size: 10 } } },
      y: { type: 'linear', position: 'left', ticks: { font: { size: 10 } } },
      y1: {
        type: 'linear', position: 'right', min: 0, max: 100,
        ticks: { font: { size: 10 }, callback: (v: any) => v + '%' },
        grid: { drawOnChartArea: false }
      }
    }
  };

  /** Version non cumulée (mois par mois) du graphique "KPI Financier Cumulé". */
  get chartMonthlyBreakdownData(): ChartData<'bar'> {
    const labels = this.months.map(m => this.formatMonth(m));
    const costColor    = '#059669';
    const costColorEtc = 'rgba(5,150,105,0.30)';
    const revColor    = '#f97316';
    const revColorEtc = 'rgba(249,115,22,0.30)';

    const revenue = this.months.map(m => this.convert(this.revenueForMonth(m)));
    const cost    = this.months.map(m => this.convert(this.directCostForMonth(m)));
    const margePct = revenue.map((rev, i) => rev > 0 ? ((rev - cost[i]) / rev) * 100 : 0);

    const isReal    = this.months.map(m => this.monthStatusFor(m) === 'REAL');
    const targetPct = this.budgetMarginPct();

    return {
      labels,
      datasets: [
        {
          type: 'bar',
          label: 'Cost Réel',
          data: cost.map((v, i) => isReal[i] ? v : null),
          backgroundColor: costColor,
          yAxisID: 'y',
          order: 3
        },
        {
          type: 'bar',
          label: 'Revenu Réel',
          data: revenue.map((v, i) => isReal[i] ? v : null),
          backgroundColor: revColor,
          yAxisID: 'y',
          order: 3
        },
        {
          type: 'bar',
          label: 'Cost Prév.',
          data: cost.map((v, i) => isReal[i] ? null : v),
          backgroundColor: costColorEtc,
          yAxisID: 'y',
          order: 3
        },
        {
          type: 'bar',
          label: 'Revenu Prév.',
          data: revenue.map((v, i) => isReal[i] ? null : v),
          backgroundColor: revColorEtc,
          yAxisID: 'y',
          order: 3
        },
        {
          type: 'line',
          label: '%Marge',
          data: margePct,
          borderColor: '#7c3aed',
          backgroundColor: '#7c3aed',
          pointBackgroundColor: '#7c3aed',
          pointStyle: 'rectRot',
          pointRadius: 4,
          tension: 0.3,
          yAxisID: 'y1',
          order: 1
        },
        {
          type: 'line',
          label: 'PM Target',
          data: this.months.map(() => targetPct),
          borderColor: '#dc2626',
          backgroundColor: '#dc2626',
          pointRadius: 0,
          borderWidth: 2,
          yAxisID: 'y1',
          order: 2
        }
      ]
    } as unknown as ChartData<'bar'>;
  }

  get chartMonthlyBreakdownOptions(): ChartOptions<'bar'> {
    return this.chartMonthlyTrendOptions;
  }

  /** Graphique "bullet" : barre Budget (fond) + barre Réel superposée + repère Atterrissage. */
  get chartBudgetVsRealVsLandingData(): ChartData<'bar'> {
    const revReal = this.sumByStatus(m => this.revenueForMonth(m), 'REAL');
    const revLand = this.sumByStatus(m => this.revenueForMonth(m), 'ALL');
    const costReal = this.sumByStatus(m => this.directCostForMonth(m), 'REAL');
    const costLand = this.sumByStatus(m => this.directCostForMonth(m), 'ALL');
    return {
      labels: ['Revenue', 'Direct Cost'],
      datasets: [
        {
          type: 'bar', label: 'Budget',
          data: [this.convert(this.budgetRevenueTotal()), this.convert(this.budgetCostTotal())],
          backgroundColor: '#e2e8f0', borderColor: '#cbd5e1', borderWidth: 1, borderRadius: 4,
          barPercentage: 0.6, categoryPercentage: 0.6, grouped: false, order: 3
        },
        {
          type: 'bar', label: 'Réel',
          data: [this.convert(revReal), this.convert(costReal)],
          backgroundColor: '#3B5BDB', borderRadius: 4,
          barPercentage: 0.6, categoryPercentage: 0.6, grouped: false, order: 2
        },
        {
          type: 'line', label: 'Atterrissage',
          data: [this.convert(revLand), this.convert(costLand)],
          borderColor: '#E91E8C', backgroundColor: '#E91E8C',
          pointStyle: 'line', pointBorderColor: '#E91E8C', pointBackgroundColor: '#E91E8C',
          pointRadius: 16, pointHoverRadius: 16, pointBorderWidth: 3,
          borderWidth: 0, showLine: false, order: 1
        }
      ]
    } as unknown as ChartData<'bar'>;
  }

  readonly chartBudgetVsRealVsLandingOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'bottom', labels: { boxWidth: 12, font: { size: 11 } } },
      tooltip: {
        callbacks: {
          label: (ctx: any) => {
            const value = ctx.parsed.y;
            if (value == null) return '';
            return `${ctx.dataset.label}: ${this.fmt(value)} ${this.currencySymbol}`;
          },
          afterBody: (items: any[]) => {
            const idx = items[0]?.dataIndex;
            if (idx == null) return [];
            const isRevenue = idx === 0;
            const budget = isRevenue ? this.budgetRevenueTotal() : this.budgetCostTotal();
            const real = isRevenue
              ? this.sumByStatus(m => this.revenueForMonth(m), 'REAL')
              : this.sumByStatus(m => this.directCostForMonth(m), 'REAL');
            const land = isRevenue
              ? this.sumByStatus(m => this.revenueForMonth(m), 'ALL')
              : this.sumByStatus(m => this.directCostForMonth(m), 'ALL');
            const fmtEcart = (v: number) => `${v >= 0 ? '+' : ''}${this.fmt(this.convert(v))} ${this.currencySymbol}`;
            return [
              `Écart Réel vs Budget: ${fmtEcart(real - budget)}`,
              `Écart Atterrissage vs Budget: ${fmtEcart(land - budget)}`
            ];
          }
        }
      }
    },
    scales: {
      x: { ticks: { font: { size: 11 } } },
      y: { ticks: { font: { size: 10 } }, beginAtZero: true }
    }
  };

  /** Écart Atterrissage vs Budget (€ bruts, non convertis) — affiché sous le graphe bullet. */
  get budgetGapRevenue(): number {
    return this.sumByStatus(m => this.revenueForMonth(m), 'ALL') - this.budgetRevenueTotal();
  }

  get budgetGapCost(): number {
    return this.sumByStatus(m => this.directCostForMonth(m), 'ALL') - this.budgetCostTotal();
  }

  get chartMarginGaugeData(): ChartData<'doughnut'> {
    const realPct = Math.max(0, Math.min(100, this.totalMarginPct()));
    const targetPct = Math.max(0, Math.min(100, this.budgetMarginPct()));
    const realColor = this.marginGaugeColor(this.totalMarginPct());
    return {
      labels: ['BM Real', 'BM Target'],
      datasets: [
        { label: 'BM Real',   data: [realPct, 100 - realPct],     backgroundColor: [realColor, '#e2e8f0'], borderWidth: 0 },
        { label: 'BM Target', data: [targetPct, 100 - targetPct], backgroundColor: ['#94a3b8', '#f1f5f9'], borderWidth: 0 },
      ]
    };
  }

  readonly chartMarginGaugeOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '65%',
    plugins: { legend: { display: false }, tooltip: { enabled: false } }
  };

  // ── Add / Delete category ────────────────────────────────────────
  openAddCategoryModal(forRebill = false): void {
    this.newCategoryName = '';
    this.showCustomInput = false;
    this.addingForRebill = forRebill;
    this.showAddCategoryModal = true;
    this.cdr.markForCheck();
  }

  selectSuggestion(s: string): void {
    if (s === 'Other') {
      this.newCategoryName = '';
      this.showCustomInput = true;
    } else {
      this.newCategoryName = s;
      this.showCustomInput = false;
    }
    this.cdr.markForCheck();
  }

  confirmAddCategory(): void {
    const name = this.newCategoryName.trim();
    if (!name) return;
    const forRebill = this.addingForRebill;
    this.api.addOtherCostCategory({ projectId: this.projectId, category: name }).subscribe({
      next: () => {
        this.showAddCategoryModal = false;
        if (forRebill) {
          this.api.setCategoryRebill({ projectId: this.projectId, category: name, isRebill: true }).subscribe({
            next: () => this.load()
          });
        } else {
          this.load();
        }
      }
    });
  }

  deleteCategory(category: string): void {
    this.pendingDelete = { type: 'category', name: category };
    this.showDeleteModal = true;
    this.cdr.markForCheck();
  }

  // ── Rebill toggle ────────────────────────────────────────────────
  toggleRebill(row: OtherCostRow): void {
    row.isRebill = !row.isRebill;
    this.cdr.markForCheck();
    this.api.setCategoryRebill({ projectId: this.projectId, category: row.category, isRebill: row.isRebill }).subscribe();
  }

  // ── Inline editing - resource cells ─────────────────────────────
  startEditResource(resourceId: number, month: string, field: string, currentVal: number): void {
    if (!this.isEditable) return;
    this.editingCell = { resourceId, month, field };
    this.editValue = currentVal > 0 ? String(currentVal) : '';
  }

  private resourceFieldMap: Record<string, keyof Pick<ResourceRow,'dailyCosts'|'workedDays'|'billedDays'|'dailyRates'>> = {
    dailyCost: 'dailyCosts', workedDays: 'workedDays', billedDays: 'billedDays', dailyRate: 'dailyRates'
  };

  saveResourceEdit(resource: ResourceRow, month: string, field: string): void {
    const val = parseFloat(this.editValue) || 0;
    const prop = this.resourceFieldMap[field];
    const previousVal = resource[prop][month];
    resource[prop][month] = val;
    this.editingCell = null;
    this.cdr.markForCheck();
    this.api.saveResourceEntry({ resourceId: resource.id, month, [field]: val }).subscribe({
      error: (err) => {
        // Rollback optimistic update
        resource[prop][month] = previousVal;
        const msg = err?.error?.message || err?.error?.error || err?.message || 'Erreur de sauvegarde';
        this.showSaveError('❌ ' + msg);
        this.cdr.markForCheck();
      }
    });
  }

  // ── Copy-paste from Excel into resource cells (dailyCost/workedDays/billedDays) ──
  private parsePastedNumber(raw: string): number | null {
    const cleaned = raw.trim().replace(/\s/g, '').replace(',', '.').replace(/[^\d.\-]/g, '');
    if (cleaned === '' || cleaned === '-') return 0;
    const num = parseFloat(cleaned);
    return isNaN(num) ? null : num;
  }

  onResourcePaste(event: ClipboardEvent, resource: ResourceRow, month: string, field: string): void {
    const text = event.clipboardData?.getData('text');
    if (!text) return;
    event.preventDefault();

    const rows = text.split(/\r\n|\n|\r/).filter(line => line.trim().length > 0);
    if (!rows.length) return;

    const prop = this.resourceFieldMap[field];
    const startResourceIndex = this.resources.indexOf(resource);
    const startMonthIndex = this.months.indexOf(month);
    if (startResourceIndex === -1 || startMonthIndex === -1) return;

    rows.forEach((row, ri) => {
      const target = this.resources[startResourceIndex + ri];
      if (!target) return;
      row.split('\t').forEach((cell, ci) => {
        const m = this.months[startMonthIndex + ci];
        if (!m) return;
        const num = this.parsePastedNumber(cell);
        if (num === null) return;
        const previousVal = target[prop][m];
        target[prop][m] = num;
        this.api.saveResourceEntry({ resourceId: target.id, month: m, [field]: num }).subscribe({
          error: (err) => {
            target[prop][m] = previousVal;
            const msg = err?.error?.message || err?.error?.error || err?.message || 'Erreur de sauvegarde';
            this.showSaveError('❌ ' + msg);
            this.cdr.markForCheck();
          }
        });
      });
    });

    // The focused cell's input is about to be removed from the DOM, which triggers a
    // (blur) -> saveResourceEdit() using the stale `editValue`. Sync it to the pasted
    // value first so that re-save is a no-op instead of overwriting it with 0.
    this.editValue = String(resource[prop][month] ?? 0);
    this.editingCell = null;
    this.cdr.markForCheck();
  }

  // ── Inline editing - other cost cells ───────────────────────────
  startEditCost(category: string, month: string, currentVal: number): void {
    if (!this.isEditable) return;
    this.editingCell = { category, month, field: 'amount' };
    this.editValue = currentVal > 0 ? String(currentVal) : '';
  }

  saveCostEdit(row: OtherCostRow, month: string): void {
    const val = parseFloat(this.editValue) || 0;
    const previousVal = row.amounts[month];
    row.amounts[month] = val;
    this.editingCell = null;
    this.cdr.markForCheck();
    this.api.saveOtherCost({ projectId: this.projectId, category: row.category, month, amount: val, isRebill: row.isRebill }).subscribe({
      error: (err) => {
        row.amounts[month] = previousVal;
        const msg = err?.error?.message || err?.error?.error || err?.message || 'Erreur de sauvegarde';
        this.showSaveError('❌ ' + msg);
        this.cdr.markForCheck();
      }
    });
  }

  cancelEdit(): void { this.editingCell = null; this.cdr.markForCheck(); }

  isEditingResource(resourceId: number, month: string, field: string): boolean {
    return this.editingCell?.resourceId === resourceId && this.editingCell?.month === month && this.editingCell?.field === field;
  }

  isEditingCost(category: string, month: string): boolean {
    return this.editingCell?.category === category && this.editingCell?.month === month;
  }

  // ── Add/Delete resource ──────────────────────────────────────────
  openAddModal(): void {
    this.newResource = { matricule: '', personName: '', contractType: 'CDI' };
    this.addMode = 'single';
    this.addResourceError = '';
    this.bulkPasteText = '';
    this.bulkPreview = [];
    this.showAddModal = true;
    this.cdr.markForCheck();
  }

  confirmAddResource(): void {
    if (!this.newResource.personName.trim()) return;
    if (!this.newResource.matricule.trim()) {
      this.showMatriculeWarning = true;
      this.cdr.markForCheck();
      return;
    }
    this.addResourceError = '';
    this.api.addResource({ projectId: this.projectId, ...this.newResource }).subscribe({
      next: () => { this.showAddModal = false; this.load(); },
      error: (err) => {
        this.addResourceError = err?.error?.error || 'Erreur lors de l\'ajout.';
        this.cdr.markForCheck();
      }
    });
  }

  private matchContractType(raw: string): string {
    const norm = raw.toLowerCase().trim();
    const exact = this.contractTypes.find(c => c.toLowerCase() === norm);
    if (exact) return exact;
    if (norm.includes('anapec')) return 'Anapec';
    if (norm.includes('extern')) return 'External subco';
    if (norm.includes('intern')) return 'Internal subco';
    return 'Internal subco';
  }

  parseBulkPaste(): void {
    this.bulkPreview = this.bulkPasteText
      .split('\n')
      .map(line => line.trim())
      .filter(line => line.length > 0)
      .map(line => {
        const cols = line.split('\t');
        const personName = (cols[0] || '').trim();
        const matricule  = (cols[1] || '').trim();
        const rawContract = (cols[2] || '').trim();
        const contractType = this.matchContractType(rawContract);
        let error: string | undefined;
        if (!personName) error = 'Nom manquant';
        else if (!matricule) error = 'Matricule manquant';
        return { personName, matricule, contractType, error };
      });
    this.cdr.markForCheck();
  }

  confirmBulkAdd(): void {
    const valid = this.bulkPreview.filter(r => !r.error);
    if (!valid.length) return;
    this.bulkAdding = true;
    this.cdr.markForCheck();
    const calls = valid.map(r =>
      lastValueFrom(this.api.addResource({ projectId: this.projectId, ...r }))
    );
    Promise.allSettled(calls).then(() => {
      this.bulkAdding = false;
      this.showAddModal = false;
      this.load();
    });
  }

  deleteResource(resourceId: number, name: string): void {
    this.pendingDelete = { type: 'resource', id: resourceId, name };
    this.showDeleteModal = true;
    this.cdr.markForCheck();
  }

  // ── Bulk selection (Daily Cost tab) ────────────────────────────────
  isResourceSelected(id: number): boolean {
    return this.selectedResourceIds.has(id);
  }

  toggleResourceSelection(id: number): void {
    if (this.selectedResourceIds.has(id)) {
      this.selectedResourceIds.delete(id);
    } else {
      this.selectedResourceIds.add(id);
    }
    this.cdr.markForCheck();
  }

  get allResourcesSelected(): boolean {
    return this.resources.length > 0 && this.resources.every(r => this.selectedResourceIds.has(r.id));
  }

  toggleSelectAllResources(): void {
    if (this.allResourcesSelected) {
      this.selectedResourceIds.clear();
    } else {
      this.resources.forEach(r => this.selectedResourceIds.add(r.id));
    }
    this.cdr.markForCheck();
  }

  deleteSelectedResources(): void {
    if (this.selectedResourceIds.size === 0) return;
    this.pendingDelete = { type: 'resources-bulk', ids: Array.from(this.selectedResourceIds), name: '' };
    this.showDeleteModal = true;
    this.cdr.markForCheck();
  }

  deleteAllResources(): void {
    if (!this.resources.length) return;
    this.pendingDelete = { type: 'resources-all', ids: this.resources.map(r => r.id), name: '' };
    this.showDeleteModal = true;
    this.cdr.markForCheck();
  }

  confirmDelete(): void {
    if (!this.pendingDelete) return;
    this.showDeleteModal = false;
    if (this.pendingDelete.type === 'resource' && this.pendingDelete.id != null) {
      this.api.deleteResource(this.pendingDelete.id).subscribe({ next: () => this.load() });
    } else if (this.pendingDelete.type === 'category') {
      this.api.deleteOtherCostCategory({ projectId: this.projectId, category: this.pendingDelete.name }).subscribe({
        next: () => this.load()
      });
    } else if (this.pendingDelete.type === 'resources-bulk' || this.pendingDelete.type === 'resources-all') {
      const ids = this.pendingDelete.ids || [];
      Promise.allSettled(ids.map(id => lastValueFrom(this.api.deleteResource(id)))).then(() => {
        this.selectedResourceIds.clear();
        this.load();
      });
    }
    this.pendingDelete = null;
  }

  onContractTypeChange(resource: ResourceRow, newType: string): void {
    resource.contractType = newType;
    this.api.updateResourceContractType(resource.id, newType).subscribe();
  }

  // ── Formatting ───────────────────────────────────────────────────
  fmt(v: number, decimals = 0): string {
    if (!v && v !== 0) return '—';
    if (v === 0) return '—';
    return new Intl.NumberFormat('fr-FR', { minimumFractionDigits: decimals, maximumFractionDigits: decimals }).format(v);
  }

  cfmt(v: number, decimals = 0): string {
    return this.fmt(this.convert(v), decimals);
  }

  fmtPct(v: number): string {
    if (!v) return '—';
    const sign = v >= 0 ? '+' : '';
    return `${sign}${v.toFixed(1)}%`;
  }

  /** Montant signé (+/-) avec devise — utilisé pour Δ R-B, DVI€, OverRun */
  cfmtSigned(v: number): string {
    if (!v) return '—';
    const sign = v > 0 ? '+' : '';
    return `${sign}${this.cfmt(v)} ${this.currencySymbol}`;
  }

  marginClass(pct: number): string {
    if (pct >= 20) return 'positive';
    if (pct >= 0) return 'neutral';
    return 'negative';
  }

  /** Pour les écarts (Δ R-B, DVI, OverRun…) : >0 = positive, <0 = negative, =0 = neutral */
  varianceClass(value: number): string {
    if (value > 0) return 'positive';
    if (value < 0) return 'negative';
    return 'neutral';
  }

  // ══════════════════════════════════════════════════════════════
  // GÉNÉRATION BL
  // ══════════════════════════════════════════════════════════════

  openBlModal(period?: string): void {
    const p = period || this.currentMonth;
    const today = new Date();
    const dd = String(today.getDate()).padStart(2, '0');
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const yyyy = today.getFullYear();

    this.blSelectedPeriod = p;
    this.blForm = {
      blDate: `${dd}/${mm}/${yyyy}`,
      blNumber: `BL_${this.projectCode || this.projectId}_${p}`,
      period: this.formatPeriodForBl(p),
      orderNumber: '',
      clientProjectId: this.projectBusinessId,
      supplierContact: this.projectPmName,
      clientContact: '',
      additionalContact: '',
      clientName: this.projectClientName,
      buCode: this.projectBuTrigram,
      projectDescription: this.projectName,
      projectCode: this.projectCode
    };

    // Lignes : une par ressource avec billed_days > 0 pour cette période
    this.blLines = this.resources
      .filter(r => (r.billedDays[p] || 0) > 0)
      .map(r => ({
        designation: r.personName,
        quantity: r.billedDays[p] || 0,
        unitPrice: r.dailyRates[p] || 0,
        total: (r.billedDays[p] || 0) * (r.dailyRates[p] || 0)
      }));

    if (this.blLines.length === 0) {
      this.blLines.push({ designation: 'Prestation', quantity: 0, unitPrice: 0, total: 0 });
    }

    this.showBlModal = true;
    this.cdr.markForCheck();
  }

  closeBlModal(): void {
    this.showBlModal = false;
    this.cdr.markForCheck();
  }

  recalcBlLine(line: { designation: string; quantity: number; unitPrice: number; total: number }): void {
    line.total = Math.round(line.quantity * line.unitPrice * 100) / 100;
    this.cdr.markForCheck();
  }

  get blTotal(): number {
    return this.blLines.reduce((s, l) => s + (l.total || 0), 0);
  }

  addBlLine(): void {
    this.blLines.push({ designation: '', quantity: 0, unitPrice: 0, total: 0 });
    this.cdr.markForCheck();
  }

  removeBlLine(i: number): void {
    this.blLines.splice(i, 1);
    this.cdr.markForCheck();
  }

  async generateBlPdf(): Promise<void> {
    const { jsPDF } = await import('jspdf');
    const autoTable = (await import('jspdf-autotable')).default;

    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    const pageW = 210;
    const margin = 15;

    // ── En-tête SEGULA ──────────────────────────────────────────
    doc.setFillColor(30, 42, 74); // dark navy
    doc.rect(0, 0, pageW, 28, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(18);
    doc.text('SEGULA', margin, 12);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.text('TECHNOLOGIES', margin, 18);

    // Adresse à droite
    doc.setFontSize(7.5);
    doc.setTextColor(200, 210, 230);
    doc.text('Segula Maroc Africa SA', pageW - margin, 8, { align: 'right' });
    doc.text('Casablanca Nearshore Park, 1100 Bd. Al Qods,', pageW - margin, 13, { align: 'right' });
    doc.text('Shore 26, Sidi Maarouf – Casablanca', pageW - margin, 18, { align: 'right' });

    // ── Titre BL ────────────────────────────────────────────────
    doc.setFillColor(240, 244, 255);
    doc.rect(margin, 32, pageW - margin * 2, 10, 'F');
    doc.setDrawColor(100, 130, 200);
    doc.rect(margin, 32, pageW - margin * 2, 10, 'S');
    doc.setTextColor(20, 40, 100);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(12);
    doc.text(`BON DE LIVRAISON — N° : ${this.blForm.blNumber}`, pageW / 2, 38.5, { align: 'center' });

    // ── Informations ────────────────────────────────────────────
    let y = 48;
    doc.setTextColor(50, 50, 50);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8.5);
    doc.text(`Date : ${this.blForm.blDate}`, margin, y);

    y += 7;
    const col1x = margin, col2x = 80, col3x = 145;

    const infoRow = (label: string, value: string, x: number, yy: number) => {
      doc.setFont('helvetica', 'bold'); doc.setFontSize(8);
      doc.text(label, x, yy);
      doc.setFont('helvetica', 'normal');
      doc.text(value || '—', x, yy + 4);
    };

    infoRow('Période :', this.blForm.period, col1x, y);
    infoRow('Business Unit :', this.blForm.buCode, col2x, y);
    infoRow('Description Projet :', this.blForm.projectDescription, col3x, y);

    y += 13;
    infoRow('Fournisseur :', 'SEGULA Maroc Africa', col1x, y);
    infoRow('Client Facturé :', this.blForm.clientName, col2x, y);
    infoRow('Code Projet :', this.blForm.projectCode, col3x, y);

    y += 13;
    infoRow('Interlocuteur fournisseur :', this.blForm.supplierContact, col1x, y);
    infoRow('Interlocuteur client :', this.blForm.clientContact, col2x, y);
    infoRow('Interlocuteur additionnel :', this.blForm.additionalContact, col3x, y);

    y += 13;
    infoRow('Numéro de commande :', this.blForm.orderNumber, col1x, y);
    infoRow('ID Projet Client :', this.blForm.clientProjectId, col2x, y);

    // ── Tableau des prestations ──────────────────────────────────
    y += 14;
    autoTable(doc, {
      startY: y,
      head: [[
        'Désignation des prestations',
        'Numéro de commande',
        'ID Projet Client',
        'Quantité (ETP × Jours)',
        'Prix unitaire (MAD)',
        'TOTAL (MAD HT)'
      ]],
      body: this.blLines.map(l => [
        l.designation,
        this.blForm.orderNumber,
        this.blForm.clientProjectId,
        l.quantity.toLocaleString('fr-FR', { minimumFractionDigits: 2 }),
        this.fmtBlAmount(l.unitPrice),
        this.fmtBlAmount(l.total)
      ]),
      foot: [['', '', '', '', 'TOTAL', this.fmtBlAmount(this.blTotal)]],
      styles: { fontSize: 8, cellPadding: 3 },
      headStyles: { fillColor: [30, 42, 74], textColor: 255, fontStyle: 'bold', fontSize: 8 },
      footStyles: { fillColor: [30, 42, 74], textColor: 255, fontStyle: 'bold', fontSize: 9 },
      columnStyles: {
        0: { cellWidth: 45 },
        1: { cellWidth: 30 },
        2: { cellWidth: 25 },
        3: { halign: 'center', cellWidth: 25 },
        4: { halign: 'right', cellWidth: 28 },
        5: { halign: 'right', cellWidth: 27 }
      },
      alternateRowStyles: { fillColor: [245, 247, 255] },
      margin: { left: margin, right: margin }
    });

    const finalY = (doc as any).lastAutoTable.finalY + 12;

    // ── Signatures ───────────────────────────────────────────────
    const sigW = (pageW - margin * 2 - 10) / 2;
    doc.setFillColor(248, 250, 255);
    doc.rect(margin, finalY, sigW, 30, 'F');
    doc.setDrawColor(200, 210, 230);
    doc.rect(margin, finalY, sigW, 30, 'S');
    doc.setFillColor(248, 250, 255);
    doc.rect(margin + sigW + 10, finalY, sigW, 30, 'F');
    doc.rect(margin + sigW + 10, finalY, sigW, 30, 'S');

    doc.setFont('helvetica', 'bold'); doc.setFontSize(8.5); doc.setTextColor(30, 42, 74);
    doc.text('Fournisseur', margin + sigW / 2, finalY + 6, { align: 'center' });
    doc.text('Pour Segula Maroc Africa', margin + sigW / 2, finalY + 11, { align: 'center' });
    doc.setFont('helvetica', 'normal'); doc.setFontSize(8); doc.setTextColor(80, 80, 80);
    doc.text(`Signature : ${this.blForm.supplierContact}`, margin + 4, finalY + 18);
    doc.text(`Date : ${this.blForm.blDate}`, margin + 4, finalY + 24);

    doc.setFont('helvetica', 'bold'); doc.setFontSize(8.5); doc.setTextColor(30, 42, 74);
    doc.text('Client', margin + sigW + 10 + sigW / 2, finalY + 6, { align: 'center' });
    doc.text(`Pour ${this.blForm.clientName || 'CLIENT'}`, margin + sigW + 10 + sigW / 2, finalY + 11, { align: 'center' });
    doc.setFont('helvetica', 'normal'); doc.setFontSize(8); doc.setTextColor(80, 80, 80);
    doc.text(`Signature :`, margin + sigW + 14, finalY + 18);
    doc.text(`Date :`, margin + sigW + 14, finalY + 24);

    // ── Footer page ──────────────────────────────────────────────
    doc.setFontSize(7); doc.setTextColor(160, 160, 160);
    doc.text('Document généré par SaaS Project Management – SEGULA Technologies – Non signé', pageW / 2, 292, { align: 'center' });

    doc.save(`${this.blForm.blNumber}.pdf`);
  }

  private fmtBlAmount(v: number): string {
    return v.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' €';
  }

  private formatPeriodForBl(period: string): string {
    if (!period) return '';
    const [y, m] = period.split('-');
    const months = ['01','02','03','04','05','06','07','08','09','10','11','12'];
    return `${months[+m - 1] || m}-${y}`;
  }
}
