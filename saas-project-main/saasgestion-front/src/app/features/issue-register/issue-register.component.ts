import { Component, OnInit, Input, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';

export interface IssueRow {
  id: number;
  projectId: number;
  iId: string;
  issue: string;
  severity: string;
  priority: string;
  impacts: string;
  dte: string | null;
  dta: string | null;
  lockdown: string;
  investigation: string;
  sustainableResolution: string;
  exitCriteria: string;
  owner: string;
  qualityLeader: string;
  deadline: string | null;
  communication: string;
  escaladeLevel: string;
  escaladeDate: string | null;
  escaladeDecision: string;
  link: string;
  status: string;
  dtr: string | null;
  resolutionTime: number | null;
  remarks: string;
}

@Component({
  selector: 'app-issue-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './issue-register.component.html',
  styleUrls: ['./issue-register.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class IssueRegisterComponent implements OnInit {

  @Input() projectId!: number;
  @Input() projectRisks: any[] = []; // liste des risks pour le champ Link

  issues: IssueRow[] = [];
  loading = false;

  // Modal detail
  selectedIssue: IssueRow | null = null;
  showModal = false;
  isViewMode = false;
  saving = false;

  // Modal création
  showCreateModal = false;
  newIssue = this.emptyIssue();

  // Modal suppression
  showDeleteConfirm = false;
  pendingDeleteIssue: IssueRow | null = null;

  private readonly todayStr = new Date().toISOString().split('T')[0];

  isOverdue(i: IssueRow): boolean {
    return !!i.deadline && i.status !== 'Closed' && i.deadline < this.todayStr;
  }
  readonly severities = ['High', 'Medium', 'Low'];
  readonly priorities = ['P1-High', 'P2-Medium', 'P3-Low'];
  readonly statuses   = ['Open', 'Closed', 'Reopened'];

  // Matrice Impacts
  private readonly IMPACTS_MATRIX: Record<string, Record<string, string>> = {
    'High':   { 'P1-High':'Critical Impact', 'P2-Medium':'High Impact',     'P3-Low':'Medium Impact' },
    'Medium': { 'P1-High':'High Impact',     'P2-Medium':'Medium Impact',   'P3-Low':'Low Impact' },
    'Low':    { 'P1-High':'Medium Impact',   'P2-Medium':'Low Impact',      'P3-Low':'Negligible Impact' }
  };

  constructor(private api: ApiService, private cdr: ChangeDetectorRef, public i18n: I18nService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.api.getIssues(this.projectId).subscribe({
      next: d => { this.issues = d; this.loading = false; this.cdr.markForCheck(); }
    });
  }

  // ── KPIs ──────────────────────────────────────────────────────
  get totalIssues()    { return this.issues.length; }
  get openCount()      { return this.issues.filter(i => i.status === 'Open').length; }
  get criticalCount()  { return this.issues.filter(i => i.impacts === 'Critical Impact').length; }
  get closedCount()    { return this.issues.filter(i => i.status === 'Closed').length; }

  // ── Helpers ───────────────────────────────────────────────────
  computeImpacts(severity: string, priority: string): string {
    return this.IMPACTS_MATRIX[severity]?.[priority] ?? '';
  }

  impactClass(impact: string): string {
    const map: Record<string, string> = {
      'Critical Impact': 'impact-critical',
      'High Impact':     'impact-high',
      'Medium Impact':   'impact-medium',
      'Low Impact':      'impact-low',
      'Negligible Impact': 'impact-negligible'
    };
    return map[impact] ?? '';
  }

  statusClass(status: string): string {
    return { 'Open':'status-open', 'Closed':'status-closed', 'Reopened':'status-reopened' }[status] ?? '';
  }

  severityClass(s: string): string {
    return { 'High':'sev-high', 'Medium':'sev-medium', 'Low':'sev-low' }[s] ?? '';
  }

  computeResolutionTime(dta: string | null, dtr: string | null): number | null {
    if (!dta || !dtr) return null;
    const d1 = new Date(dta), d2 = new Date(dtr);
    return Math.round((d2.getTime() - d1.getTime()) / 86400000);
  }

  truncate(s: string, max = 70): string {
    return s && s.length > max ? s.substring(0, max) + '…' : s;
  }

  // ── Create ────────────────────────────────────────────────────
  openCreate(): void {
    this.newIssue = this.emptyIssue();
    this.showCreateModal = true;
    this.cdr.markForCheck();
  }

  confirmCreate(): void {
    if (!this.newIssue.issue?.trim() || !this.newIssue.deadline) return;
    this.saving = true;
    this.api.createIssue(this.projectId, this.newIssue).subscribe({
      next: i => {
        this.issues = [...this.issues, i];
        this.showCreateModal = false;
        this.saving = false;
        this.cdr.markForCheck();
      },
      error: () => { this.saving = false; this.cdr.markForCheck(); }
    });
  }

  // ── Detail modal ──────────────────────────────────────────────
  openView(issue: IssueRow): void {
    this.selectedIssue = { ...issue };
    this.isViewMode = true;
    this.showModal = true;
    this.cdr.markForCheck();
  }

  openEdit(issue: IssueRow): void {
    this.selectedIssue = { ...issue };
    this.isViewMode = false;
    this.showModal = true;
    this.cdr.markForCheck();
  }

  closeModal(): void {
    this.showModal = false;
    this.selectedIssue = null;
    this.cdr.markForCheck();
  }

  saveModal(): void {
    if (!this.selectedIssue || this.saving) return;
    this.saving = true;
    // Si DTR renseigné → status Closed
    if (this.selectedIssue.dtr) this.selectedIssue.status = 'Closed';
    // recalculer impacts et resolutionTime
    this.selectedIssue.impacts = this.computeImpacts(this.selectedIssue.severity, this.selectedIssue.priority);
    this.selectedIssue.resolutionTime = this.computeResolutionTime(this.selectedIssue.dta, this.selectedIssue.dtr);

    this.api.updateIssue(this.projectId, this.selectedIssue.id, this.selectedIssue).subscribe({
      next: updated => {
        this.issues = this.issues.map(i => i.id === updated.id ? updated : i);
        this.saving = false;
        this.closeModal();
        this.cdr.markForCheck();
      },
      error: () => { this.saving = false; this.cdr.markForCheck(); }
    });
  }

  // ── Delete ────────────────────────────────────────────────────
  openDeleteConfirm(issue: IssueRow, event?: Event): void {
    event?.stopPropagation();
    this.pendingDeleteIssue = issue;
    this.showDeleteConfirm = true;
    this.cdr.markForCheck();
  }

  confirmDelete(): void {
    if (!this.pendingDeleteIssue) return;
    const issue = this.pendingDeleteIssue;
    this.showDeleteConfirm = false;
    this.pendingDeleteIssue = null;
    this.api.deleteIssue(this.projectId, issue.id).subscribe({
      next: () => {
        this.issues = this.issues.filter(i => i.id !== issue.id);
        this.cdr.markForCheck();
      }
    });
  }

  cancelDelete(): void {
    this.showDeleteConfirm = false;
    this.pendingDeleteIssue = null;
    this.cdr.markForCheck();
  }

  private emptyIssue(): any {
    return {
      issue: '', severity: '', priority: '', deadline: null,
      dte: null, dta: null, lockdown: '', investigation: '',
      sustainableResolution: '', exitCriteria: '', owner: '',
      qualityLeader: '', communication: '', escaladeLevel: '',
      escaladeDate: null, escaladeDecision: '', link: '',
      status: 'Open', dtr: null, remarks: ''
    };
  }
}
