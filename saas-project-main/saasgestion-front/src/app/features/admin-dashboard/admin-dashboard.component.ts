import { Component, OnInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { forkJoin } from 'rxjs';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../core/services/api.service';
import { AdminSidebarComponent } from '../admin-sidebar/admin-sidebar.component';
import { ProjectListDto, ProjectDetailDto, ReferenceDto } from '../../core/models/project.model';
import * as XLSX from 'xlsx';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, FormsModule, MatSnackBarModule, AdminSidebarComponent],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {

  projects: ProjectListDto[] = [];
  loading = true;
  searchTerm = '';
  filterStatus = '';

  // Détail modal
  selectedProject: ProjectDetailDto | null = null;
  showDetailModal = false;

  // Suppression modal
  showDeleteModal = false;
  deleteTarget: ProjectListDto | null = null;
  deleting = false;

  // Edition modal
  showEditModal = false;
  editForm!: FormGroup;
  editTarget: ProjectListDto | null = null;
  saving = false;

  // Données de référence
  bus:        ReferenceDto[] = [];
  customers:  ReferenceDto[] = [];
  industries: ReferenceDto[] = [];
  disciplines: ReferenceDto[] = [];
  engagements: ReferenceDto[] = [];
  pms:        ReferenceDto[] = [];

  // ── Export ──────────────────────────────────────────────────
  showExportModal = false;
  exportFormat: 'excel' | 'pdf' = 'excel';
  exportFilters = {
    status:         '',
    buId:           '',
    customerName:   '',
    industryName:   '',
    projectManager: '',
    engagementType: '',
    majorProject:   ''
  };

  constructor(
    private api: ApiService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
    private snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadProjects();
    this.loadRefData();
    this.initEditForm();
  }

  initEditForm(): void {
    this.editForm = this.fb.group({
      frontFinancier:          ['', Validators.required],
      projectManagerId:        [null, Validators.required],
      buId:                    [null, Validators.required],
      customerId:              [null, Validators.required],
      industryId:              [null, Validators.required],
      engineeringDisciplineId: [null, Validators.required],
      functionName:            [''],
      engagementId:            [null, Validators.required],
      activity:                ['', [Validators.required, Validators.maxLength(200)]],
      technicalOffice:         ['Back Office'],
      status:                  ['On Going'],
      majorProject:            [false],
      startDate:               [null, Validators.required],
      endDate:                 [null, Validators.required],
      revenueBudget:           [0, [Validators.required, Validators.min(0)]],
      costBudget:              [0, [Validators.required, Validators.min(0)]],
      projectNameLegacy:       [''],
      projectCode:             [''],
      projectYear:             [new Date().getFullYear()],
      projectId:               [''],
    }, { validators: this.dateValidator });
  }

  dateValidator(g: AbstractControl): ValidationErrors | null {
    const s = g.get('startDate')?.value;
    const e = g.get('endDate')?.value;
    if (s && e && e < s) return { dateInvalid: true };
    return null;
  }

  loadProjects(): void {
    this.loading = true;
    this.api.getProjects({ size: 200 }).subscribe({
      next: r => {
        this.projects = r.content;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => { this.loading = false; this.cdr.markForCheck(); }
    });
  }

  loadRefData(): void {
    this.api.getBus().subscribe(d => { this.bus = d; this.cdr.markForCheck(); });
    this.api.getCustomers().subscribe(d => { this.customers = d; this.cdr.markForCheck(); });
    this.api.getIndustries().subscribe(d => { this.industries = d; this.cdr.markForCheck(); });
    this.api.getDisciplines().subscribe(d => { this.disciplines = d; this.cdr.markForCheck(); });
    this.api.getEngagements().subscribe(d => { this.engagements = d; this.cdr.markForCheck(); });
    this.api.getPMs().subscribe(d => { this.pms = d; this.cdr.markForCheck(); });
  }

  get filteredProjects(): ProjectListDto[] {
    return this.projects.filter(p => {
      const matchSearch = !this.searchTerm ||
        p.projectName?.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        p.activity?.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        p.customerName?.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchStatus = !this.filterStatus || p.status === this.filterStatus;
      return matchSearch && matchStatus;
    });
  }

  get onGoingCount(): number {
    return this.projects.filter(p => p.status === 'On Going').length;
  }

  get totalRevenue(): number {
    return this.projects.reduce((s, p) => s + (p.revenueBudget ?? 0), 0);
  }

  get totalMargin(): number {
    return this.projects.reduce((s, p) => s + ((p.revenueBudget ?? 0) - (p.costBudget ?? 0)), 0);
  }

  // ── Détail ──────────────────────────────────────────────────
  openDetail(p: ProjectListDto): void {
    this.api.getProject(p.id).subscribe({
      next: detail => {
        this.selectedProject = detail;
        this.showDetailModal = true;
        this.cdr.markForCheck();
      }
    });
  }

  closeDetail(): void {
    this.showDetailModal = false;
    this.selectedProject = null;
    this.cdr.markForCheck();
  }

  // ── Suppression ─────────────────────────────────────────────
  openDelete(p: ProjectListDto): void {
    this.deleteTarget = p;
    this.showDeleteModal = true;
    this.cdr.markForCheck();
  }

  closeDelete(): void {
    this.showDeleteModal = false;
    this.deleteTarget = null;
    this.cdr.markForCheck();
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.deleting = true;
    this.api.archiveProject(this.deleteTarget.id).subscribe({
      next: () => {
        this.projects = this.projects.filter(p => p.id !== this.deleteTarget!.id);
        this.deleting = false;
        this.showDeleteModal = false;
        this.deleteTarget = null;
        this.snack.open('Projet supprimé.', 'OK', { duration: 3000 });
        this.cdr.markForCheck();
      },
      error: () => {
        this.deleting = false;
        this.cdr.markForCheck();
        this.snack.open('Erreur lors de la suppression.', 'Fermer', { duration: 4000 });
      }
    });
  }

  // ── Édition ─────────────────────────────────────────────────
  openEdit(p: ProjectListDto): void {
    this.editTarget = p;
    this.api.getProject(p.id).subscribe({
      next: detail => {
        this.editForm.patchValue({
          frontFinancier:          detail.frontFinancier || '',
          projectManagerId:        detail.projectManagerId,
          buId:                    detail.buId,
          customerId:              detail.customerId,
          industryId:              detail.industryId,
          engineeringDisciplineId: detail.engineeringDisciplineId,
          functionName:            detail.functionName || '',
          engagementId:            detail.engagementId,
          activity:                detail.activity || '',
          technicalOffice:         detail.technicalOffice || 'Back Office',
          status:                  detail.status || 'On Going',
          majorProject:            detail.majorProject || false,
          startDate:               detail.startDate || null,
          endDate:                 detail.endDate || null,
          revenueBudget:           detail.revenueBudget || 0,
          costBudget:              detail.costBudget || 0,
          projectNameLegacy:       detail.projectNameLegacy || '',
          projectCode:             detail.projectCode || '',
          projectYear:             detail.projectYear || new Date().getFullYear(),
          projectId:               detail.projectId || '',
        });
        this.showEditModal = true;
        this.cdr.markForCheck();
      }
    });
  }

  closeEdit(): void {
    this.showEditModal = false;
    this.editTarget = null;
    this.cdr.markForCheck();
  }

  submitEdit(): void {
    if (this.editForm.invalid || !this.editTarget) {
      this.editForm.markAllAsTouched();
      this.cdr.markForCheck();
      return;
    }
    this.saving = true;
    this.cdr.markForCheck();

    this.api.updateProject(this.editTarget.id, this.editForm.value).subscribe({
      next: () => {
        this.saving = false;
        this.showEditModal = false;
        this.snack.open('Projet mis à jour.', 'OK', { duration: 3000 });
        this.loadProjects();
        this.cdr.markForCheck();
      },
      error: err => {
        this.saving = false;
        this.cdr.markForCheck();
        this.snack.open(err.error?.error || 'Erreur lors de la mise à jour.', 'Fermer', { duration: 5000 });
      }
    });
  }

  fmt(v: number | null): string {
    if (v == null) return '—';
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(v);
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
    if (pct >= 25) return 'c-green';
    if (pct >= 10) return 'c-orange';
    return 'c-red';
  }

  get forecastTotalRevenue(): number {
    return (this.selectedProject?.monthlyForecasts ?? []).reduce((s, f) => s + (f.revenue || 0), 0);
  }

  get forecastTotalCost(): number {
    return (this.selectedProject?.monthlyForecasts ?? []).reduce((s, f) => s + (f.cost || 0), 0);
  }

  // ── Export helpers ──────────────────────────────────────────
  openExportModal(): void {
    this.exportFilters = { status: '', buId: '', customerName: '',
      industryName: '', projectManager: '', engagementType: '', majorProject: '' };
    this.exportFormat = 'excel';
    this.showExportModal = true;
    this.cdr.markForCheck();
  }

  closeExportModal(): void { this.showExportModal = false; this.cdr.markForCheck(); }

  get exportPreview(): ProjectListDto[] {
    const f = this.exportFilters;
    // Mapper le nom d'industrie sélectionné vers son trigramme
    const industryTrigram = f.industryName
      ? (this.industries.find(i => i.label === f.industryName)?.code ?? '')
      : '';
    return this.projects.filter(p => (
      (!f.status         || p.status === f.status) &&
      (!f.buId           || p.buId === f.buId) &&
      (!f.customerName   || p.customerName === f.customerName) &&
      (!industryTrigram  || p.industryTrigram === industryTrigram) &&
      (!f.projectManager || p.projectManager === f.projectManager) &&
      (!f.engagementType || p.engagementType === f.engagementType) &&
      (!f.majorProject   || String(p.majorProject) === f.majorProject)
    ));
  }

  get uniqueValues() {
    return {
      statuses:        ['On Going', 'Planned', 'Closed', 'On Hold', 'Canceled'],
      buIds:           this.bus.map(b => String(b.id)).filter(Boolean).sort(),
      customers:       this.customers.map(c => c.label).filter(Boolean).sort(),
      industries:      this.industries.map(i => i.label).filter(Boolean).sort(),
      projectManagers: this.pms.map(pm => pm.label).filter(Boolean).sort(),
      engagementTypes: [...new Set(this.engagements.map(e => e.code || e.label).filter(Boolean))].sort() as string[]
    };
  }

  runExport(): void {
    const rows = this.exportPreview;
    if (rows.length === 0) {
      this.snack.open('Aucun projet à exporter avec ces filtres.', 'OK', { duration: 3000 });
      return;
    }
    // Charger le détail complet de chaque projet pour avoir tous les champs
    const ref = this.snack.open('Chargement des données...', '', { duration: 10000 });
    forkJoin(rows.map(p => this.api.getProject(p.id))).subscribe({
      next: details => {
        ref.dismiss();
        if (this.exportFormat === 'excel') this.exportExcel(details);
        else                               this.exportPdf(details);
        this.closeExportModal();
        this.cdr.markForCheck();
      },
      error: () => {
        ref.dismiss();
        this.snack.open('Erreur lors de la récupération des données.', 'Fermer', { duration: 4000 });
      }
    });
  }

  /** Formatage nombre sans caractères UTF-8 spéciaux (pour PDF) */
  private fmtPdf(v: number | null | undefined): string {
    if (v == null) return '—';
    return Math.round(v).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
  }

  private exportExcel(rows: ProjectDetailDto[]): void {
    const data = rows.map(p => ({
      'Project ID':          p.projectId || '',
      'Nom Projet':          p.projectName || p.activity,
      'Front Financier':     p.frontFinancier,
      'BU ID':               p.buId,
      'Business Unit':       p.buName,
      'Client':              p.customerName,
      'Trigramme Industrie': p.industryTrigram || '',
      'Chef de Projet':      p.projectManager || '',
      'Type Engagement':     p.engagementType || '',
      'Statut':              p.status,
      'Projet Majeur':       p.majorProject ? 'Oui' : 'Non',
      'Bureau Technique':    p.technicalOffice || '',
      'Date Début':          p.startDate || '',
      'Date Fin':            p.endDate   || '',
      'Revenue Budget (EUR)': p.revenueBudget,
      'Cout Budget (EUR)':    p.costBudget,
      'Marge (EUR)':          p.marginBudget,
      'Marge (%)':            p.revenueBudget > 0
        ? +((p.marginBudget / p.revenueBudget) * 100).toFixed(1) : 0
    }));

    const ws = XLSX.utils.json_to_sheet(data);
    ws['!cols'] = [14,34,16,10,28,18,26,14,12,14,14,12,12,18,18,14,10].map(w => ({ wch: w }));
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Projets');
    const date = new Date().toISOString().slice(0, 10);
    XLSX.writeFile(wb, `projets_export_${date}.xlsx`);
    this.snack.open(`Export Excel : ${rows.length} projet(s).`, 'OK', { duration: 3000 });
  }

  private exportPdf(rows: ProjectDetailDto[]): void {
    const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
    const pageW = doc.internal.pageSize.getWidth();
    const today = new Date().toLocaleDateString('fr-FR');

    // ── Header banner ────────────────────────────────────────────
    doc.setFillColor(0, 102, 204);
    doc.rect(0, 0, pageW, 22, 'F');

    // Title
    doc.setFontSize(15); doc.setTextColor(255, 255, 255);
    doc.setFont('helvetica', 'bold');
    doc.text('Export Projets - SEGULA', 14, 10);

    // Subtitle / meta
    doc.setFontSize(8); doc.setFont('helvetica', 'normal');
    doc.setTextColor(180, 215, 255);
    doc.text(`Genere le ${today}  |  ${rows.length} projet(s)`, 14, 17);

    // Right-side date
    doc.setFontSize(8); doc.setTextColor(255, 255, 255);
    doc.setFont('helvetica', 'normal');
    doc.text(today, pageW - 14, 12.5, { align: 'right' });

    // ── Thin accent line ─────────────────────────────────────────
    doc.setDrawColor(0, 153, 255);
    doc.setLineWidth(0.5);
    doc.line(0, 22, pageW, 22);

    // ── Summary KPI boxes ────────────────────────────────────────
    const totalRev  = rows.reduce((s, p) => s + (Number(p.revenueBudget) || 0), 0);
    const totalCost = rows.reduce((s, p) => s + (Number(p.costBudget)    || 0), 0);
    const totalMarg = rows.reduce((s, p) => s + (Number(p.marginBudget)  || 0), 0);
    const avgMarg   = totalRev > 0 ? ((totalMarg / totalRev) * 100).toFixed(1) + '%' : '—';

    const kpis = [
      { label: 'Projets',  value: String(rows.length),          color: [0, 102, 204] as [number,number,number] },
      { label: 'Revenue',  value: this.fmtPdf(totalRev),         color: [5, 150, 105] as [number,number,number] },
      { label: 'Cout',     value: this.fmtPdf(totalCost),        color: [220, 38, 38] as [number,number,number] },
      { label: 'Marge',    value: this.fmtPdf(totalMarg),        color: [5, 150, 105] as [number,number,number] },
      { label: 'Marge %',  value: avgMarg,                       color: [124, 58, 237] as [number,number,number] },
    ];
    const boxW = (pageW - 28) / kpis.length;
    kpis.forEach((k, i) => {
      const x = 14 + i * (boxW + 2);
      doc.setFillColor(248, 250, 252);
      doc.roundedRect(x, 25, boxW, 14, 2, 2, 'F');
      doc.setDrawColor(226, 232, 240);
      doc.setLineWidth(0.3);
      doc.roundedRect(x, 25, boxW, 14, 2, 2, 'S');

      doc.setFontSize(7); doc.setTextColor(100, 116, 139);
      doc.setFont('helvetica', 'normal');
      doc.text(k.label, x + boxW / 2, 30, { align: 'center' });

      doc.setFontSize(9); doc.setFont('helvetica', 'bold');
      doc.setTextColor(...k.color);
      doc.text(k.value, x + boxW / 2, 36, { align: 'center' });
    });

    // ── Table ────────────────────────────────────────────────────
    autoTable(doc, {
      startY: 42,
      head: [[
        'Project ID', 'Nom Projet', 'Front Fin.', 'BU', 'Client',
        'Trig. Ind.', 'Chef de Projet', 'Type Eng.',
        'Statut', 'Majeur', 'Bureau Tech.',
        'Revenue', 'Cout', 'Marge', 'Marge %'
      ]],
      body: rows.map(p => [
        p.projectId || '—',
        (p.projectName || p.activity || '').substring(0, 30),
        p.frontFinancier,
        p.buTrigram,
        p.customerName,
        p.industryTrigram || '—',
        p.projectManager || '—',
        p.engagementType || '—',
        p.status,
        p.majorProject ? 'Oui' : 'Non',
        p.technicalOffice || '—',
        this.fmtPdf(p.revenueBudget),
        this.fmtPdf(p.costBudget),
        this.fmtPdf(p.marginBudget),
        p.revenueBudget > 0 ? ((p.marginBudget / p.revenueBudget) * 100).toFixed(1) + '%' : '—'
      ]),
      styles: {
        fontSize: 6.8,
        cellPadding: { top: 3, bottom: 3, left: 3, right: 3 },
        font: 'helvetica',
        lineColor: [226, 232, 240],
        lineWidth: 0.2
      },
      headStyles: {
        fillColor: [0, 102, 204],
        textColor: [255, 255, 255],
        fontStyle: 'bold',
        fontSize: 7,
        cellPadding: { top: 4, bottom: 4, left: 3, right: 3 }
      },
      alternateRowStyles: { fillColor: [248, 250, 255] },
      willDrawCell: (data) => {
        if (data.section === 'body') {
          const statusIdx = 8;
          if (data.column.index === statusIdx) {
            const val = String(data.cell.raw);
            const colors: Record<string, [number,number,number]> = {
              'On Going': [5, 150, 105],
              'Planned':  [0, 102, 204],
              'Closed':   [100, 116, 139],
              'On Hold':  [202, 138, 4],
              'Canceled': [220, 38, 38],
            };
            if (colors[val]) {
              doc.setTextColor(...colors[val]);
            }
          }
          // Marge % coloration
          if (data.column.index === 14) {
            const pct = parseFloat(String(data.cell.raw));
            if (!isNaN(pct)) {
              doc.setTextColor(pct >= 0 ? 5 : 220, pct >= 0 ? 150 : 38, pct >= 0 ? 105 : 38);
            }
          }
        }
      },
      columnStyles: {
        0:  { cellWidth: 16 },
        1:  { cellWidth: 38 },
        2:  { cellWidth: 14 },
        3:  { cellWidth: 16 },
        4:  { cellWidth: 24 },
        5:  { cellWidth: 13 },
        6:  { cellWidth: 24 },
        7:  { cellWidth: 14 },
        8:  { cellWidth: 16, fontStyle: 'bold' },
        9:  { cellWidth: 11, halign: 'center' },
        10: { cellWidth: 18 },
        11: { halign: 'right' },
        12: { halign: 'right' },
        13: { halign: 'right' },
        14: { halign: 'right', fontStyle: 'bold' }
      },
      didDrawPage: (data) => {
        // Footer on each page
        const pageCount = (doc as any).internal.getNumberOfPages();
        const curPage   = data.pageNumber;
        doc.setFontSize(7); doc.setTextColor(150);
        doc.setFont('helvetica', 'normal');
        doc.text(
          `SEGULA Technologies  |  Export confidentiel  |  Page ${curPage} / ${pageCount}`,
          pageW / 2, doc.internal.pageSize.getHeight() - 5,
          { align: 'center' }
        );
        doc.setDrawColor(226, 232, 240);
        doc.setLineWidth(0.3);
        doc.line(14, doc.internal.pageSize.getHeight() - 8, pageW - 14, doc.internal.pageSize.getHeight() - 8);
      }
    });

    const date = new Date().toISOString().slice(0, 10);
    doc.save(`projets_export_${date}.pdf`);
    this.snack.open(`Export PDF : ${rows.length} projet(s).`, 'OK', { duration: 3000 });
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  statusClass(s: string): string {
    const m: Record<string, string> = {
      'On Going': 'badge-ongoing', 'Closed': 'badge-closed',
      'Planned': 'badge-planned', 'On Hold': 'badge-hold', 'Canceled': 'badge-canceled'
    };
    return m[s] ?? '';
  }
}
