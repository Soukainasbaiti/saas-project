import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Location } from '@angular/common';
import { lastValueFrom } from 'rxjs';
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
  imports: [CommonModule, FormsModule, RouterModule, RiskRegisterComponent, IssueRegisterComponent, OpportunityRegisterComponent, MipRegisterComponent, WipRegisterComponent],
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
    this.newLotName = '';
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

  activeTab: 'dailyCost' | 'workedDays' | 'otherCosts' | 'rebillCosts' | 'billedDays' | 'dailyRate' | 'revenueMoyens' | 'revenueResultats' | 'risks' | 'issues' | 'opportunities' | 'mip' | 'wip' | 'synthese' = 'dailyCost';

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
  pendingDelete: { type: 'resource' | 'category'; id?: number; name: string } | null = null;

  get isEditable(): boolean {
    return this.validationStatus === 'DRAFT' || this.validationStatus === 'REJECTED';
  }

  // ── SDH Import ────────────────────────────────────────────────
  showSdhModal = false;
  sdhResult: { imported: number; skipped: number; errors: string[] } | null = null;

  // Add resource modal
  showAddModal = false;
  newResource = { matricule: '', personName: '', contractType: 'CDI' };
  contractTypes = ['Stagiaire', 'ANAPEC', 'CDI', 'CDD', 'Sous-traitant interne', 'Sous-traitant externe'];

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
        this.months = data.months;
        this.resources = data.resources.map((r: any) => ({
          ...r,
          dailyCosts: this.toNumberMap(r.dailyCosts),
          workedDays: this.toNumberMap(r.workedDays),
          billedDays: this.toNumberMap(r.billedDays),
          dailyRates: this.toNumberMap(r.dailyRates),
        }));
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

  saveResourceEdit(resource: ResourceRow, month: string, field: string): void {
    const val = parseFloat(this.editValue) || 0;
    const fieldMap: Record<string, keyof Pick<ResourceRow,'dailyCosts'|'workedDays'|'billedDays'|'dailyRates'>> = {
      dailyCost: 'dailyCosts', workedDays: 'workedDays', billedDays: 'billedDays', dailyRate: 'dailyRates'
    };
    const prop = fieldMap[field];
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
        const contractType = this.contractTypes.find(
          c => c.toLowerCase() === rawContract.toLowerCase()
        ) || 'Internal';
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

  confirmDelete(): void {
    if (!this.pendingDelete) return;
    this.showDeleteModal = false;
    if (this.pendingDelete.type === 'resource' && this.pendingDelete.id != null) {
      this.api.deleteResource(this.pendingDelete.id).subscribe({ next: () => this.load() });
    } else if (this.pendingDelete.type === 'category') {
      this.api.deleteOtherCostCategory({ projectId: this.projectId, category: this.pendingDelete.name }).subscribe({
        next: () => this.load()
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

  marginClass(pct: number): string {
    if (pct >= 20) return 'positive';
    if (pct >= 0) return 'neutral';
    return 'negative';
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
