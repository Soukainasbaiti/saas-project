import { Component, OnInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../core/services/api.service';
import { ReferenceDto } from '../../core/models/project.model';

@Component({
  selector: 'app-project-form',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, MatSnackBarModule],
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

  constructor(
    private fb: FormBuilder,
    private api: ApiService,
    private router: Router,
    private snack: MatSnackBar,
    private cdr: ChangeDetectorRef
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

    this.api.createProject(this.form.value).subscribe({
      next: () => {
        this.saving    = false;
        this.submitted = true;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.saving = false;
        this.cdr.markForCheck();

        // Afficher l'erreur email si c'est ça le problème
        const msg = err.error?.error || 'Erreur lors de la soumission du projet';
        this.snack.open(msg, 'Fermer', {
          duration: 5000,
          panelClass: 'snack-error'
        });
      }
    });
  }
}