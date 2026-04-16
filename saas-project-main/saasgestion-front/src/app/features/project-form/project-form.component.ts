import { Component, OnInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { ReferenceDto } from '../../core/models/project.model';

export interface MonthlyRow {
  month: string;   // "2026-01"
  label: string;   // "Jan 2026"
  revenue: number | null;
  cost:    number | null;
}

@Component({
  selector: 'app-project-form',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, FormsModule, MatSnackBarModule],
  templateUrl: './project-form.component.html',
  styleUrls: ['./project-form.component.scss']
})
export class ProjectFormComponent implements OnInit {
  form: FormGroup;
  saving    = false;
  submitted = false;

  bus:             ReferenceDto[] = [];
  customers:       ReferenceDto[] = [];
  industries:      ReferenceDto[] = [];
  disciplines:     ReferenceDto[] = [];
  engagements:     ReferenceDto[] = [];
  functions:       ReferenceDto[] = [];
  frontFinanciers: ReferenceDto[] = [];
  pms:             ReferenceDto[] = [];
  years = Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - 1 + i);

  // ── Modale prévisions mensuelles ──────────────────────────────
  showMonthlyModal = false;
  monthlyRows: MonthlyRow[] = [];
  pasteApplied = false;
  pasteMode: 'column' | 'row' = 'column';
  revPasted  = false;
  costPasted = false;

  get dashboardRoute(): string {
    return this.auth.isAdmin() ? '/admin' : '/';
  }

  get canOpenMonthly(): boolean {
    const s = this.form.get('startDate')?.value;
    const e = this.form.get('endDate')?.value;
    return !!s && !!e && e >= s;
  }

  get totalRevenue(): number {
    return this.monthlyRows.reduce((s, r) => s + (r.revenue || 0), 0);
  }

  get totalCost(): number {
    return this.monthlyRows.reduce((s, r) => s + (r.cost || 0), 0);
  }

  get totalMargin(): number {
    const rev = this.totalRevenue;
    return rev === 0 ? 0 : ((rev - this.totalCost) / rev) * 100;
  }

  constructor(
    private fb: FormBuilder,
    private api: ApiService,
    private router: Router,
    private snack: MatSnackBar,
    private cdr: ChangeDetectorRef,
    public auth: AuthService
  ) {
    this.form = this.fb.group({
      projectCode:             [''],
      projectYear:             [new Date().getFullYear()],
      frontFinancier:          ['', Validators.required],
      projectManagerId:        [null, Validators.required],
      buId:                    [null, Validators.required],
      customerId:              [null, Validators.required],
      industryId:              [null, Validators.required],
      engineeringDisciplineId: [null, Validators.required],
      functionName:            ['', Validators.required],
      engagementId:            [null, Validators.required],
      activity:                ['', [Validators.required, Validators.maxLength(200)]],
      technicalOffice:         ['Back Office'],
      status:                  ['On Going'],
      majorProject:            [false],
      startDate:               [null, Validators.required],
      endDate:                 [null, Validators.required],
      revenueBudget:           [0, [Validators.required, Validators.min(0)]],
      costBudget:              [0, [Validators.required, Validators.min(0)]],
      projectNameLegacy:       ['']
    }, { validators: this.dateValidator });
  }

  ngOnInit(): void {
    this.api.getBus().subscribe(d => { this.bus = d; this.cdr.markForCheck(); });
    this.api.getCustomers().subscribe(d => { this.customers = d; this.cdr.markForCheck(); });
    this.api.getIndustries().subscribe(d => { this.industries = d; this.cdr.markForCheck(); });
    this.api.getDisciplines().subscribe(d => { this.disciplines = d; this.cdr.markForCheck(); });
    this.api.getEngagements().subscribe(d => { this.engagements = d; this.cdr.markForCheck(); });
    this.api.getFunctions().subscribe(d => { this.functions = d; this.cdr.markForCheck(); });
    this.api.getFrontFinanciers().subscribe(d => { this.frontFinanciers = d; this.cdr.markForCheck(); });
    this.api.getPMs().subscribe(d => { this.pms = d; this.cdr.markForCheck(); });
  }

  dateValidator(group: AbstractControl): ValidationErrors | null {
    const start = group.get('startDate')?.value;
    const end   = group.get('endDate')?.value;
    if (start && end && end < start) return { dateInvalid: true };
    return null;
  }

  // ── Ouvrir la modale et générer les mois ──────────────────────
  openMonthlyModal(): void {
    const startVal = this.form.get('startDate')?.value;
    const endVal   = this.form.get('endDate')?.value;
    if (!startVal || !endVal) return;

    // startVal est au format YYYY-MM-DD (input type=date)
    const [sy, sm] = startVal.split('-').map(Number);
    const [ey, em] = endVal.split('-').map(Number);
    const start = new Date(sy, sm - 1, 1);
    const end   = new Date(ey, em - 1, 1);

    // Conserver les valeurs déjà saisies si on rouvre
    const existing = new Map(this.monthlyRows.map(r => [r.month, r]));

    const rows: MonthlyRow[] = [];
    const cur = new Date(start);
    const MONTHS_FR = ['Jan','Fév','Mar','Avr','Mai','Jun','Jul','Aoû','Sep','Oct','Nov','Déc'];

    while (cur <= end) {
      const y = cur.getFullYear();
      const m = cur.getMonth();
      const key = `${y}-${String(m + 1).padStart(2, '0')}`;
      rows.push(existing.get(key) ?? {
        month: key,
        label: `${MONTHS_FR[m]} ${y}`,
        revenue: null,
        cost: null
      });
      cur.setMonth(cur.getMonth() + 1);
    }

    this.monthlyRows = rows;
    this.pasteApplied = false;
    this.pasteMode    = 'column';
    this.revPasted    = false;
    this.costPasted   = false;
    this.showMonthlyModal = true;
    this.cdr.markForCheck();
  }

  closeMonthlyModal(): void {
    this.showMonthlyModal = false;
    this.cdr.markForCheck();
  }

  // ── Valider la modale → alimenter revenueBudget / costBudget ──
  confirmMonthly(): void {
    this.form.patchValue({
      revenueBudget: this.totalRevenue,
      costBudget:    this.totalCost
    });
    this.showMonthlyModal = false;
    this.cdr.markForCheck();
  }

  // ── Coller depuis Excel / Google Sheets – mode colonne (Ctrl+V global) ──
  onPaste(event: ClipboardEvent): void {
    if (this.pasteMode !== 'column') return;
    const target = event.target as HTMLElement;
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') return;

    event.preventDefault();
    const text = event.clipboardData?.getData('text/plain') ?? '';
    const lines = text.trim().split('\n').filter(l => l.trim());

    lines.forEach((line, i) => {
      if (i >= this.monthlyRows.length) return;
      const parts = line.split('\t');
      const rev  = this.parseNum(parts[0]);
      const cost = this.parseNum(parts[1]);
      if (rev  !== null) this.monthlyRows[i].revenue = rev;
      if (cost !== null) this.monthlyRows[i].cost    = cost;
    });

    this.pasteApplied = true;
    this.cdr.markForCheck();
    setTimeout(() => { this.pasteApplied = false; this.cdr.markForCheck(); }, 2500);
  }

  // ── Coller depuis Excel / Google Sheets – mode ligne (zones dédiées) ──
  onPasteRow(event: ClipboardEvent, field: 'revenue' | 'cost'): void {
    event.preventDefault();
    const text = event.clipboardData?.getData('text/plain') ?? '';
    // Prend la première ligne non vide : valeurs séparées par tabulation
    const line = text.trim().split('\n').find(l => l.trim()) ?? '';
    const values = line.split('\t');
    values.forEach((v, i) => {
      if (i < this.monthlyRows.length) {
        const num = this.parseNum(v.trim());
        if (num !== null) this.monthlyRows[i][field] = num;
      }
    });
    if (field === 'revenue') {
      this.revPasted = true;
      setTimeout(() => { this.revPasted = false; this.cdr.markForCheck(); }, 2500);
    } else {
      this.costPasted = true;
      setTimeout(() => { this.costPasted = false; this.cdr.markForCheck(); }, 2500);
    }
    this.cdr.markForCheck();
  }

  private parseNum(s: string | undefined): number | null {
    if (!s) return null;
    const n = parseFloat(s.replace(/\s/g, '').replace(',', '.'));
    return isNaN(n) ? null : n;
  }

  // ── Formulaire principal ──────────────────────────────────────
  get marginPreview(): number {
    const r = this.form.get('revenueBudget')?.value || 0;
    const c = this.form.get('costBudget')?.value || 0;
    return r === 0 ? 0 : ((r - c) / r) * 100;
  }

  get marginEur(): number {
    return (this.form.get('revenueBudget')?.value || 0) -
           (this.form.get('costBudget')?.value || 0);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.cdr.markForCheck();
      return;
    }

    this.saving = true;
    this.cdr.markForCheck();

    const payload = {
      ...this.form.value,
      monthlyForecasts: this.monthlyRows
        .filter(r => r.revenue !== null || r.cost !== null)
        .map(r => ({ month: r.month, revenue: r.revenue ?? 0, cost: r.cost ?? 0 }))
    };

    if (this.auth.isAdmin()) {
      // Création directe sans circuit d'approbation
      this.api.createProjectDirect(payload).subscribe({
        next: () => {
          this.saving = false;
          this.cdr.markForCheck();
          this.snack.open('Projet créé avec succès.', 'OK', { duration: 3000 });
          this.router.navigate(['/admin']);
        },
        error: (err) => {
          this.saving = false;
          this.cdr.markForCheck();
          const msg = err.error?.error || 'Erreur lors de la création du projet';
          this.snack.open(msg, 'Fermer', { duration: 5000 });
        }
      });
    } else {
      // PM : soumettre pour approbation
      this.api.createProject(payload).subscribe({
        next: () => {
          this.saving    = false;
          this.submitted = true;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.saving = false;
          this.cdr.markForCheck();
          const msg = err.error?.error || 'Erreur lors de la soumission du projet';
          this.snack.open(msg, 'Fermer', { duration: 5000, panelClass: 'snack-error' });
        }
      });
    }
  }
}
