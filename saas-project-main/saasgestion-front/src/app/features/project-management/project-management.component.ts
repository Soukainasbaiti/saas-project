import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Location } from '@angular/common';
import { ApiService } from '../../core/services/api.service';

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

@Component({
  selector: 'app-project-management',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './project-management.component.html',
  styleUrls: ['./project-management.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProjectManagementComponent implements OnInit {

  projectId!: number;
  projectName = '';
  months: string[] = [];
  resources: ResourceRow[] = [];
  otherCosts: OtherCostRow[] = [];

  activeTab: 'dailyCost' | 'workedDays' | 'otherCosts' | 'billedDays' | 'dailyRate' | 'synthese' = 'dailyCost';
  loading = true;
  granularity = 'MONTHLY';
  granularityLocked = false;
  granularityOptions = [
    { value: 'MONTHLY', label: 'Monthly', icon: '📅' },
    { value: 'WEEKLY',  label: 'Weekly',  icon: '📆' },
    { value: 'DAILY',   label: 'Daily',   icon: '🗓' },
  ];
  showGranularityModal = false;
  pendingGranularity = '';
  pendingCurrency = '';
  modalStep: 1 | 2 = 1;
  currency = 'EUR';

  readonly currencyOptions = [
    { value: 'EUR', label: 'Euro',          symbol: '€',   flag: '🇪🇺' },
    { value: 'USD', label: 'US Dollar',     symbol: '$',   flag: '🇺🇸' },
    { value: 'MAD', label: 'Dirham (MAD)',  symbol: 'MAD', flag: '🇲🇦' },
  ];

  get currencySymbol(): string {
    return this.currencyOptions.find(c => c.value === this.currency)?.symbol ?? '€';
  }

  // Current month for past-month protection
  readonly currentMonth: string = (() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  })();

  // Add resource modal
  showAddModal = false;
  newResource = { matricule: '', personName: '', contractType: 'Internal' };
  contractTypes = ['Internal', 'External', 'Freelance', 'Subcontractor', 'Stagiaire'];

  // Add category modal
  showAddCategoryModal = false;
  newCategoryName = '';
  showCustomInput = false;

  // Editing cell tracking
  editingCell: { resourceId?: number; category?: string; month: string; field: string } | null = null;
  editValue = '';

  constructor(
    private route: ActivatedRoute,
    private api: ApiService,
    private cdr: ChangeDetectorRef,
    private location: Location
  ) {}

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
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
        this.currency = data.currency || 'EUR';
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
          this.pendingCurrency = '';
          this.modalStep = 1;
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

  nextStep(): void {
    if (!this.pendingGranularity) return;
    this.modalStep = 2;
    this.cdr.markForCheck();
  }

  confirmGranularity(): void {
    if (!this.pendingGranularity || !this.pendingCurrency) return;
    this.api.setGranularity(this.projectId, this.pendingGranularity, this.pendingCurrency).subscribe({
      next: () => {
        this.currency = this.pendingCurrency;
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

  directCostForMonth(m: string): number {
    return this.laborCostForMonth(m) + this.otherCostForMonth(m);
  }

  marginForMonth(m: string): number {
    return this.revenueForMonth(m) - this.directCostForMonth(m);
  }

  marginPctForMonth(m: string): number {
    const rev = this.revenueForMonth(m);
    return rev > 0 ? (this.marginForMonth(m) / rev) * 100 : 0;
  }

  totalLaborCost(): number    { return this.months.reduce((s, m) => s + this.laborCostForMonth(m), 0); }
  totalOtherCost(): number    { return this.months.reduce((s, m) => s + this.otherCostForMonth(m), 0); }
  totalRebill(): number       { return this.months.reduce((s, m) => s + this.rebillForMonth(m), 0); }
  totalRevenue(): number      { return this.months.reduce((s, m) => s + this.revenueForMonth(m), 0); }
  totalDirectCost(): number   { return this.months.reduce((s, m) => s + this.directCostForMonth(m), 0); }
  totalMargin(): number       { return this.totalRevenue() - this.totalDirectCost(); }
  totalMarginPct(): number    { return this.totalRevenue() > 0 ? (this.totalMargin() / this.totalRevenue()) * 100 : 0; }

  // ── Add / Delete category ────────────────────────────────────────
  openAddCategoryModal(): void {
    this.newCategoryName = '';
    this.showCustomInput = false;
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
    this.api.addOtherCostCategory({ projectId: this.projectId, category: name }).subscribe({
      next: () => { this.showAddCategoryModal = false; this.load(); }
    });
  }

  deleteCategory(category: string): void {
    if (!confirm(`Supprimer la catégorie "${category}" et toutes ses données ?`)) return;
    this.api.deleteOtherCostCategory({ projectId: this.projectId, category }).subscribe({
      next: () => this.load()
    });
  }

  // ── Rebill toggle ────────────────────────────────────────────────
  toggleRebill(row: OtherCostRow): void {
    row.isRebill = !row.isRebill;
    this.cdr.markForCheck();
    this.api.setCategoryRebill({ projectId: this.projectId, category: row.category, isRebill: row.isRebill }).subscribe();
  }

  // ── Inline editing - resource cells ─────────────────────────────
  startEditResource(resourceId: number, month: string, field: string, currentVal: number): void {
    if (this.isPastMonth(month)) return; // protect past months
    this.editingCell = { resourceId, month, field };
    this.editValue = currentVal > 0 ? String(currentVal) : '';
  }

  saveResourceEdit(resource: ResourceRow, month: string, field: string): void {
    const val = parseFloat(this.editValue) || 0;
    const fieldMap: Record<string, keyof Pick<ResourceRow,'dailyCosts'|'workedDays'|'billedDays'|'dailyRates'>> = {
      dailyCost: 'dailyCosts', workedDays: 'workedDays', billedDays: 'billedDays', dailyRate: 'dailyRates'
    };
    const prop = fieldMap[field];
    resource[prop][month] = val;
    this.editingCell = null;
    this.cdr.markForCheck();
    this.api.saveResourceEntry({ resourceId: resource.id, month, [field]: val }).subscribe();
  }

  // ── Inline editing - other cost cells ───────────────────────────
  startEditCost(category: string, month: string, currentVal: number): void {
    if (this.isPastMonth(month)) return; // protect past months
    this.editingCell = { category, month, field: 'amount' };
    this.editValue = currentVal > 0 ? String(currentVal) : '';
  }

  saveCostEdit(row: OtherCostRow, month: string): void {
    const val = parseFloat(this.editValue) || 0;
    row.amounts[month] = val;
    this.editingCell = null;
    this.cdr.markForCheck();
    this.api.saveOtherCost({ projectId: this.projectId, category: row.category, month, amount: val, isRebill: row.isRebill }).subscribe();
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
    this.newResource = { matricule: '', personName: '', contractType: 'Internal' };
    this.showAddModal = true;
    this.cdr.markForCheck();
  }

  confirmAddResource(): void {
    if (!this.newResource.personName.trim()) return;
    this.api.addResource({ projectId: this.projectId, ...this.newResource }).subscribe({
      next: () => { this.showAddModal = false; this.load(); }
    });
  }

  deleteResource(resourceId: number, name: string): void {
    if (!confirm(`Supprimer la ressource "${name}" ?`)) return;
    this.api.deleteResource(resourceId).subscribe({ next: () => this.load() });
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
}
