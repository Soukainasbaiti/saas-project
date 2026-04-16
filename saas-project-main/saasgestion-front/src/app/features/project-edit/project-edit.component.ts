import { Component, OnInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../core/services/api.service';
import { ReferenceDto } from '../../core/models/project.model';

@Component({
  selector: 'app-project-edit',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, MatSnackBarModule],
  templateUrl: './project-edit.component.html',
  styleUrls: ['./project-edit.component.scss']
})
export class ProjectEditComponent implements OnInit {
  form!: FormGroup;
  token       = '';
  loading     = true;
  saving      = false;
  submitted   = false;
  errorMsg    = '';

  bus:             ReferenceDto[] = [];
  customers:       ReferenceDto[] = [];
  industries:      ReferenceDto[] = [];
  disciplines:     ReferenceDto[] = [];
  engagements:     ReferenceDto[] = [];
  functions:       ReferenceDto[] = [];
  frontFinanciers: ReferenceDto[] = [];
  pms:             ReferenceDto[] = [];
  years = Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - 1 + i);

  constructor(
    private fb:    FormBuilder,
    private api:   ApiService,
    private http:  HttpClient,
    private route: ActivatedRoute,
    private router: Router,
    private snack: MatSnackBar,
    private cdr:   ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token') ?? '';

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

    // Charger les données de référence et le projet rejeté en parallèle
    Promise.all([
      this.loadReferenceData(),
      this.loadRejectedProject()
    ]).then(() => {
      this.loading = false;
      this.cdr.markForCheck();
    });
  }

  private loadReferenceData(): Promise<void> {
    return new Promise(resolve => {
      let count = 0;
      const done = () => { if (++count === 8) resolve(); };
      this.api.getBus().subscribe(d            => { this.bus             = d; done(); });
      this.api.getCustomers().subscribe(d      => { this.customers       = d; done(); });
      this.api.getIndustries().subscribe(d     => { this.industries      = d; done(); });
      this.api.getDisciplines().subscribe(d    => { this.disciplines     = d; done(); });
      this.api.getEngagements().subscribe(d    => { this.engagements     = d; done(); });
      this.api.getFunctions().subscribe(d      => { this.functions       = d; done(); });
      this.api.getFrontFinanciers().subscribe(d => { this.frontFinanciers = d; done(); });
      this.api.getPMs().subscribe(d            => { this.pms             = d; done(); });
    });
  }

  private loadRejectedProject(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.http.get<any>(`http://localhost:8080/api/projects/pending/${this.token}/edit`)
        .subscribe({
          next: data => {
            const p = data.project;
            this.form.patchValue({
              projectCode:             p.projectCode       || '',
              projectYear:             p.projectYear       || new Date().getFullYear(),
              frontFinancier:          p.frontFinancier    || '',
              projectManagerId:        p.projectManagerId,
              buId:                    p.buId,
              customerId:              p.customerId,
              industryId:              p.industryId,
              engineeringDisciplineId: p.engineeringDisciplineId,
              functionName:            p.functionName      || '',
              engagementId:            p.engagementId,
              activity:                p.activity          || '',
              technicalOffice:         p.technicalOffice   || 'Back Office',
              status:                  p.status            || 'On Going',
              majorProject:            p.majorProject      || false,
              startDate:               p.startDate         || null,
              endDate:                 p.endDate           || null,
              revenueBudget:           p.revenueBudget     || 0,
              costBudget:              p.costBudget        || 0,
              projectNameLegacy:       p.projectNameLegacy || ''
            });
            resolve();
          },
          error: err => {
            this.errorMsg = err.status === 409
              ? err.error?.error
              : err.status === 404
              ? 'Ce projet a été supprimé.'
              : 'Impossible de charger le projet.';
            this.loading = false;
            this.cdr.markForCheck();
            reject();
          }
        });
    });
  }

  dateValidator(group: AbstractControl): ValidationErrors | null {
    const start = group.get('startDate')?.value;
    const end   = group.get('endDate')?.value;
    if (start && end && end < start) return { dateInvalid: true };
    return null;
  }

  get marginPreview(): number {
    const r = this.form.get('revenueBudget')?.value || 0;
    const c = this.form.get('costBudget')?.value || 0;
    return r === 0 ? 0 : ((r - c) / r) * 100;
  }

  get marginEur(): number {
    return (this.form.get('revenueBudget')?.value || 0) -
           (this.form.get('costBudget')?.value || 0);
  }

  showDeleteModal = false;

  openDeleteModal(): void {
    this.showDeleteModal = true;
    this.cdr.markForCheck();
  }

  closeDeleteModal(): void {
    this.showDeleteModal = false;
    this.cdr.markForCheck();
  }

  deleteProject(): void {
    this.showDeleteModal = false;
    this.saving = true;
    this.cdr.markForCheck();

    this.http.delete(`http://localhost:8080/api/projects/pending/${this.token}`)
      .subscribe({
        next: () => {
          this.saving = false;
          this.snack.open('Projet supprimé définitivement.', 'OK', { duration: 4000 });
          this.router.navigate(['/']);
        },
        error: err => {
          this.saving = false;
          this.cdr.markForCheck();
          const msg = err.error?.error || 'Erreur lors de la suppression';
          this.snack.open(msg, 'Fermer', { duration: 5000, panelClass: 'snack-error' });
        }
      });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.cdr.markForCheck();
      return;
    }

    this.saving = true;
    this.cdr.markForCheck();

    this.http.post(
      `http://localhost:8080/api/projects/pending/${this.token}/resubmit`,
      this.form.value
    ).subscribe({
      next: () => {
        this.saving    = false;
        this.submitted = true;
        this.cdr.markForCheck();
      },
      error: err => {
        this.saving = false;
        this.cdr.markForCheck();
        const msg = err.error?.error || 'Erreur lors de la soumission';
        this.snack.open(msg, 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      }
    });
  }
}
