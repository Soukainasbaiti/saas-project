import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ApiService } from '../../core/services/api.service';
import { ProjectDetailDto } from '../../core/models/project.model';

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, MatSnackBarModule, MatProgressSpinnerModule],
  templateUrl: './project-detail.component.html',
  styleUrls: ['./project-detail.component.scss']
})
export class ProjectDetailComponent implements OnInit {
  project: ProjectDetailDto | null = null;
  loading = true;

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

  fmt(v: number): string {
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
}