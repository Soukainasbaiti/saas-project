import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ApiService } from '../../core/services/api.service';
import { ProjectDetailDto } from '../../core/models/project.model';
import { parseNum } from '../../core/utils/parse-num';

interface DisplayMonth {
  month: string;
  label: string;
  revenue: number;
  cost: number;
  cov: number | null;
}

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, MatSnackBarModule, MatProgressSpinnerModule],
  templateUrl: './project-detail.component.html',
  styleUrls: ['./project-detail.component.scss']
})
export class ProjectDetailComponent implements OnInit {
  project: ProjectDetailDto | null = null;
  loading = true;

  // ── Édition COV (Client Order Value) mensuel ──────────────────
  editingCov = false;
  savingCov = false;
  covEdits: { [month: string]: number | null } = {};

  constructor(
    private route: ActivatedRoute, private router: Router,
    private api: ApiService, private snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.getProject(id).subscribe({
      next: p => { this.project = p; this.loading = false; },
      error: () => { this.loading = false; this.router.navigate(['/']); }
    });
  }

  archive(): void {
    if (!this.project || !confirm('Archiver ce projet ?')) return;
    this.api.archiveProject(this.project.id).subscribe({
      next: () => { this.snack.open('Projet archivé', 'Fermer', { duration: 3000, panelClass: 'snack-success' }); this.router.navigate(['/']); }
    });
  }

  fmt(v: number | null | undefined): string {
    if (v == null) return '—';
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(v);
  }
  fmtPct(v: number): string { return v == null ? '—' : (v * 100).toFixed(1) + '%'; }
  marginClass(v: number): string {
    if (v >= 0.25) return 'marge-high'; if (v >= 0.10) return 'marge-mid'; return 'marge-low';
  }
  formatDate(d: string | null): string {
    if (!d) return '—';
    return new Intl.DateTimeFormat('fr-FR').format(new Date(d));
  }

  monthLabel(month: string): string {
    const MONTHS = ['Jan','Fév','Mar','Avr','Mai','Jun','Jul','Aoû','Sep','Oct','Nov','Déc'];
    const [y, m] = month.split('-').map(Number);
    return `${MONTHS[m - 1]} ${y}`;
  }

  monthMargin(revenue: number, cost: number): string {
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

  get forecastTotalRevenue(): number {
    return (this.project?.monthlyForecasts ?? []).reduce((s, f) => s + (f.revenue || 0), 0);
  }

  get forecastTotalCost(): number {
    return (this.project?.monthlyForecasts ?? []).reduce((s, f) => s + (f.cost || 0), 0);
  }

  // ── Mois affichés : prévisions enregistrées, ou mois générés à partir
  //    des dates du projet si rien n'a encore été saisi ──────────────
  get displayMonths(): DisplayMonth[] {
    const p = this.project;
    if (!p) return [];

    if (p.monthlyForecasts && p.monthlyForecasts.length > 0) {
      return p.monthlyForecasts.map(f => ({
        month: f.month,
        label: this.monthLabel(f.month),
        revenue: f.revenue || 0,
        cost: f.cost || 0,
        cov: f.cov ?? null
      }));
    }

    if (!p.startDate || !p.endDate) return [];

    const [sy, sm] = p.startDate.split('-').map(Number);
    const [ey, em] = p.endDate.split('-').map(Number);
    const months: DisplayMonth[] = [];
    let y = sy, m = sm;
    while (y < ey || (y === ey && m <= em)) {
      const key = `${y}-${String(m).padStart(2, '0')}`;
      months.push({ month: key, label: this.monthLabel(key), revenue: 0, cost: 0, cov: null });
      m++;
      if (m > 12) { m = 1; y++; }
    }
    return months;
  }

  get forecastTotalCov(): number | null {
    const values = this.displayMonths.map(m => m.cov).filter((v): v is number => v != null);
    return values.length === 0 ? null : values.reduce((s, v) => s + v, 0);
  }

  // ── Édition COV ─────────────────────────────────────────────────
  startEditCov(): void {
    this.covEdits = {};
    for (const m of this.displayMonths) {
      this.covEdits[m.month] = m.cov;
    }
    this.editingCov = true;
  }

  cancelEditCov(): void {
    this.editingCov = false;
    this.covEdits = {};
  }

  // ── Saisie directe COV : accepte "514000", "514.000" ou "514,000"
  //    (séparateur de milliers) sans tronquer le montant ─────────────
  onCovCellBlur(event: Event, month: string): void {
    const input = event.target as HTMLInputElement;
    const parsed = parseNum(input.value);
    this.covEdits[month] = parsed;
    input.value = parsed != null ? String(parsed) : '';
  }

  saveCov(): void {
    if (!this.project) return;
    this.savingCov = true;
    const updates = this.displayMonths.map(m => ({ month: m.month, cov: this.covEdits[m.month] ?? null }));
    this.api.updateMonthlyForecasts(this.project.id, updates).subscribe({
      next: forecasts => {
        if (this.project) this.project.monthlyForecasts = forecasts;
        this.editingCov = false;
        this.savingCov = false;
        this.snack.open('COV mis à jour avec succès.', 'Fermer', { duration: 3000, panelClass: 'snack-success' });
      },
      error: () => {
        this.savingCov = false;
        this.snack.open('Erreur lors de la mise à jour du COV.', 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      }
    });
  }
}