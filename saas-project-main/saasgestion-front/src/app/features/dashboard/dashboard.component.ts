import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { I18nService } from '../../core/services/i18n.service';
import { ProjectListDto, ProjectDetailDto, DashboardStats, ReferenceDto } from '../../core/models/project.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule, ReactiveFormsModule,
    MatTableModule, MatInputModule, MatSelectModule,
    MatFormFieldModule, MatButtonModule, MatIconModule,
    MatTooltipModule, MatProgressSpinnerModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {

  stats: DashboardStats | null = null;
  projects: ProjectListDto[] = [];
  totalElements = 0;
  loading = false;
  selectedYear = new Date().getFullYear();

  selectedProject: ProjectDetailDto | null = null;
  modalLoading = false;
  showModal = false;

  bus: ReferenceDto[] = [];
  customers: ReferenceDto[] = [];
  industries: ReferenceDto[] = [];
  frontFinanciers: ReferenceDto[] = [];
  allProjects: ProjectListDto[] = [];

  filterForm: FormGroup;

  // ── Propriétés simples initialisées dans ngOnInit ─────────────
  currentUser: { fullName: string; role: string; email: string } | null = null;
  isAdmin = false;
  bumPending: any[] = [];
  bumPendingLoading = false;
  showNotifDropdown = false;

  pmRejected: any[] = [];
  pmRejectedLoading = false;
  showPmNotifDropdown = false;

  // ── Historique BUM ─────────────────────────────────────────
  bumHistory: any[] = [];
  bumHistoryLoading = false;
  showHistoryModal = false;
  bumHistoryFilter: 'ALL' | 'SUBMITTED' | 'VALIDATED' | 'REJECTED' = 'ALL';

  get filteredBumHistory(): any[] {
    if (this.bumHistoryFilter === 'ALL') return this.bumHistory;
    return this.bumHistory.filter(h => h.action === this.bumHistoryFilter);
  }

  get isBum(): boolean {
    return this.auth.currentUser()?.role === 'BUM';
  }

  get isPm(): boolean {
    return this.auth.currentUser()?.role === 'PM';
  }

  displayedColumns = [
    'projectName', 'frontFinancier', 'bu', 'customer', 'countries',
    'activity', 'revenueBudget', 'costBudget', 'marginBudget',
    'projectMargin', 'status', 'actions'
  ];

  constructor(
    private api: ApiService,
    private auth: AuthService,
    public i18n: I18nService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.filterForm = this.fb.group({
      year:          [null],
      buId:          [null],
      status:        [null],
      customerId:    [null],
      industryId:    [null],
      customerGroup: [null],
      country:       [null]
    });
  }

  ngOnInit(): void {
    // Lire une seule fois après init — évite ExpressionChangedAfterChecked
    this.currentUser = this.auth.currentUser();
    this.isAdmin     = this.auth.isAdmin();
    this.cdr.detectChanges();

    this.loadRefData();
    this.loadStats();
    this.loadProjects();
    if (this.isBum) { this.loadBumPendingProjects(); this.loadBumHistory(); }
    if (this.isPm)  { this.loadPmRejectedProjects(); }
    // Filtres backend → rechargement complet
    ['buId', 'status', 'customerId', 'year'].forEach(k =>
      this.filterForm.get(k)!.valueChanges.subscribe(() => this.loadProjects())
    );
    // Filtres client → filtrage local uniquement
    ['industryId', 'customerGroup', 'country'].forEach(k =>
      this.filterForm.get(k)!.valueChanges.subscribe(() => this.applyClientFilters())
    );
  }

  logout(): void {
    this.auth.logout();
  }

  loadRefData(): void {
    this.api.getBus().subscribe(d => { this.bus = d; this.cdr.detectChanges(); });
    this.api.getCustomers().subscribe(d => { this.customers = d; this.cdr.detectChanges(); });
    this.api.getIndustries().subscribe(d => { this.industries = d; this.cdr.detectChanges(); });
    this.api.getFrontFinanciers().subscribe(d => { this.frontFinanciers = d; this.cdr.detectChanges(); });
  }

  get customerGroups(): string[] {
    return [...new Set(this.allProjects.map(p => p.customerGroup).filter(Boolean))].sort();
  }

  get uniqueCountries(): string[] {
    const names = new Set<string>();
    this.allProjects.forEach(p => (p.countries || []).forEach(c => names.add(c.countryName)));
    return [...names].sort();
  }

  loadStats(): void {
    this.api.getDashboardStats(new Date().getFullYear()).subscribe(s => {
      this.stats = s;
      this.cdr.detectChanges();
    });
  }

  loadProjects(): void {
    this.loading = true;
    const f = this.filterForm.value;
    this.api.getProjects({
      buId:       f.buId       || undefined,
      status:     f.status     || undefined,
      customerId: f.customerId || undefined,
      year:       f.year       || undefined,
      page: 0,
      size: 1000
    }).subscribe({
      next: r => {
        this.allProjects = r.content;
        this.applyClientFilters();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  applyClientFilters(): void {
    const f = this.filterForm.value;
    let filtered = this.allProjects;
    if (f.industryId)    filtered = filtered.filter(p => p.industryId    === +f.industryId);
    if (f.customerGroup) filtered = filtered.filter(p => p.customerGroup === f.customerGroup);
    if (f.country)       filtered = filtered.filter(p => (p.countries || []).some(c => c.countryName === f.country));
    this.projects      = filtered;
    this.totalElements = filtered.length;
    this.cdr.detectChanges();
  }

  openModal(id: number): void {
    this.showModal    = true;
    this.modalLoading = true;
    this.selectedProject = null;
    this.api.getProject(id).subscribe({
      next: p => {
        this.selectedProject = p;
        this.modalLoading    = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.modalLoading = false;
        this.showModal    = false;
        this.cdr.detectChanges();
      }
    });
  }

  closeModal(): void {
    this.showModal       = false;
    this.selectedProject = null;
  }

  fmt(v: number): string {
    if (v == null) return '—';
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency', currency: 'EUR', maximumFractionDigits: 0
    }).format(v);
  }

  fmtCompact(v: number): string {
    if (v == null) return '—';
    const abs = Math.abs(v);
    const sign = v < 0 ? '-' : '';
    const units: { value: number; suffix: string }[] = [
      { value: 1_000_000_000, suffix: 'B' },
      { value: 1_000_000,     suffix: 'M' },
      { value: 1_000,         suffix: 'K' },
    ];
    let result = '';
    let remainder = abs;
    for (const { value, suffix } of units) {
      const part = Math.floor(remainder / value);
      if (part > 0) { result += part + suffix; remainder %= value; }
    }
    if (!result) result = String(Math.round(abs));
    else if (remainder > 0) result += Math.round(remainder);
    return sign + result + ' €';
  }

  fmtPct(v: number): string { return v == null ? '—' : (v * 100).toFixed(1) + '%'; }

  marginClass(v: number): string {
    if (v >= 0.25) return 'marge-high';
    if (v >= 0.10) return 'marge-mid';
    return 'marge-low';
  }

  buClass(trigram: string): string {
    const map: Record<string, string> = {
      MEC: 'bu-mec', PDM: 'bu-pdm', SDV: 'bu-sdv',
      DIV: 'bu-div', SCL: 'bu-scl', MIS: 'bu-mis', ACT: 'bu-mis'
    };
    return map[trigram] || 'bu-default';
  }

  formatDate(d: string | null): string {
    if (!d) return '—';
    return new Intl.DateTimeFormat('fr-FR').format(new Date(d));
  }

  monthLabel(month: string): string {
    const M = ['Jan','Fév','Mar','Avr','Mai','Jun','Jul','Aoû','Sep','Oct','Nov','Déc'];
    const [y, m] = month.split('-').map(Number);
    return `${M[m - 1]} ${y}`;
  }

  monthMarginPct(revenue: number, cost: number): string {
    if (!revenue) return '—';
    return (((revenue - cost) / revenue) * 100).toFixed(0) + '%';
  }

  monthMarginClass(revenue: number, cost: number): string {
    if (!revenue) return '';
    const pct = ((revenue - cost) / revenue) * 100;
    if (pct >= 25) return 'marge-high';
    if (pct >= 10) return 'marge-mid';
    return 'marge-low';
  }

  toggleNotifDropdown(): void {
    this.showNotifDropdown = !this.showNotifDropdown;
    if (this.showNotifDropdown) { this.loadBumPendingProjects(); }
    this.cdr.detectChanges();
  }

  loadBumPendingProjects(): void {
    this.bumPendingLoading = true;
    this.api.getBumPending().subscribe({
      next: data => { this.bumPending = data; this.bumPendingLoading = false; this.cdr.detectChanges(); },
      error: ()   => { this.bumPendingLoading = false; this.cdr.detectChanges(); }
    });
  }

  loadBumHistory(): void {
    this.bumHistoryLoading = true;
    this.api.getBumHistory().subscribe({
      next: data => { this.bumHistory = data; this.bumHistoryLoading = false; this.cdr.detectChanges(); },
      error: ()   => { this.bumHistoryLoading = false; this.cdr.detectChanges(); }
    });
  }

  togglePmNotifDropdown(): void {
    this.showPmNotifDropdown = !this.showPmNotifDropdown;
    if (this.showPmNotifDropdown) { this.loadPmRejectedProjects(); }
    this.cdr.detectChanges();
  }

  loadPmRejectedProjects(): void {
    this.pmRejectedLoading = true;
    this.api.getPmRejected().subscribe({
      next: data => { this.pmRejected = data; this.pmRejectedLoading = false; this.cdr.detectChanges(); },
      error: ()   => { this.pmRejectedLoading = false; this.cdr.detectChanges(); }
    });
  }

  get forecastTotalRevenue(): number {
    return (this.selectedProject?.monthlyForecasts ?? []).reduce((s, f) => s + (f.revenue || 0), 0);
  }

  get forecastTotalCost(): number {
    return (this.selectedProject?.monthlyForecasts ?? []).reduce((s, f) => s + (f.cost || 0), 0);
  }

  // ── Chart BU ─────────────────────────────────────────────────
  get buChartData(): { bu: string; revenue: number; count: number; pct: number }[] {
    const map = new Map<string, { revenue: number; count: number }>();
    for (const p of this.projects) {
      const bu = p.buTrigram || 'Autre';
      const curr = map.get(bu) ?? { revenue: 0, count: 0 };
      map.set(bu, { revenue: curr.revenue + (p.revenueBudget ?? 0), count: curr.count + 1 });
    }
    const items = [...map.entries()].map(([bu, d]) => ({ bu, revenue: d.revenue, count: d.count, pct: 0 }));
    const maxRev = Math.max(...items.map(i => i.revenue), 1);
    return items
      .map(i => ({ ...i, pct: (i.revenue / maxRev) * 100 }))
      .sort((a, b) => b.revenue - a.revenue)
      .slice(0, 8);
  }
}