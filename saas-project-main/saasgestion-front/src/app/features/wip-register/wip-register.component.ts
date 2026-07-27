import { Component, Input, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';

interface WipDocument {
  id?: number;
  year?: number;
  month?: number;
  documentType?: string;
  fileName?: string;
  extractedAmount?: number;
  confirmedAmount?: number;
  uploadedAt?: string;
}

interface WipTableRow {
  period: string;
  declaredAmount: number;
  invoicedAmount: number;
  delta: number;
  docCount: number;
  documents: WipDocument[];
}

interface WipSummary {
  totalDeclared: number;
  totalInvoiced: number;
  totalOrderAmount: number | null;
  orderPeriod: string | null;
  remainingOnOrder: number | null;
}

@Component({
  selector: 'app-wip-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './wip-register.component.html',
  styleUrls: ['./wip-register.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WipRegisterComponent implements OnInit {
  @Input() projectId!: number;

  rows: WipTableRow[] = [];
  summary: WipSummary | null = null;
  loading = false;

  // Filtres
  selectedYear: number = new Date().getFullYear();
  hideEmpty = true;

  // Drawer state
  drawerOpen = false;
  drawerRow: WipTableRow | null = null;
  uploading: { BL_SIGNE?: boolean; BON_COMMANDE?: boolean } = {};

  // Confirmation modal
  confirmModal = false;
  confirmDoc: WipDocument | null = null;
  confirmAmount = 0;

  // Delete modal
  deleteDocModal = false;
  deleteDocTarget: WipDocument | null = null;

  // Toast feedback
  toastMsg = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any = null;

  constructor(
    public i18n: I18nService,
    private api: ApiService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void { this.load(); this.loadSummary(); }

  loadSummary(): void {
    this.api.getWipSummary(this.projectId).subscribe({
      next: s => { this.summary = s; this.cdr.markForCheck(); }
    });
  }

  load(): void {
    this.loading = true;
    this.api.getWipTable(this.projectId).subscribe({
      next: data => {
        this.rows = data;
        this.loading = false;
        // Sélectionner l'année courante si elle a des mois, sinon la plus récente dispo
        const years = this.availableYears;
        if (years.length && !years.includes(this.selectedYear))
          this.selectedYear = years[years.length - 1];
        // Rafraîchir le drawer si ouvert
        if (this.drawerRow) {
          const updated = this.rows.find(r => r.period === this.drawerRow!.period);
          if (updated) this.drawerRow = updated;
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.showToast(this.i18n.t('wr.toast.load_error'), 'error');
        this.cdr.markForCheck();
      }
    });
  }

  // ── Toast feedback ───────────────────────────────────────────────
  showToast(msg: string, type: 'success' | 'error' = 'success'): void {
    this.toastMsg = msg;
    this.toastType = type;
    this.cdr.markForCheck();
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => { this.toastMsg = ''; this.cdr.markForCheck(); }, 4000);
  }

  dismissToast(): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastMsg = '';
    this.cdr.markForCheck();
  }

  // ── Drawer ──────────────────────────────────────────────────────
  openDrawer(row: WipTableRow): void {
    this.drawerRow = row;
    this.drawerOpen = true;
    this.cdr.markForCheck();
  }

  closeDrawer(): void {
    this.drawerOpen = false;
    this.drawerRow = null;
    this.cdr.markForCheck();
  }

  // ── Upload document ─────────────────────────────────────────────
  onFileSelected(event: Event, docType: string): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length || !this.drawerRow) return;
    const file = input.files[0];
    const [y, m] = this.drawerRow.period.split('-').map(Number);

    this.uploading[docType as keyof typeof this.uploading] = true;
    this.cdr.markForCheck();

    this.api.uploadWipDocument(this.projectId, y, m, docType, file).subscribe({
      next: doc => {
        this.uploading[docType as keyof typeof this.uploading] = false;
        if (docType === 'BL_SIGNE' && doc.extractedAmount != null) {
          this.confirmDoc = doc;
          this.confirmAmount = doc.extractedAmount;
          this.confirmModal = true;
        } else if (docType === 'BON_COMMANDE') {
          // Pas d'extraction automatique pour le BC : on ouvre directement la saisie du montant.
          this.confirmDoc = doc;
          this.confirmAmount = doc.confirmedAmount || 0;
          this.confirmModal = true;
        } else {
          this.showToast(this.i18n.t('wr.toast.doc_added'), 'success');
        }
        this.load();
        this.loadSummary();
        this.cdr.markForCheck();
      },
      error: () => {
        this.uploading[docType as keyof typeof this.uploading] = false;
        this.showToast(this.i18n.t('wr.toast.upload_failed'), 'error');
        this.cdr.markForCheck();
      }
    });
    input.value = '';
  }

  // ── Confirmation montant ────────────────────────────────────────
  confirmAmountSave(): void {
    if (!this.confirmDoc?.id) return;
    this.api.confirmWipDocumentAmount(this.projectId, this.confirmDoc.id, this.confirmAmount).subscribe({
      next: () => {
        this.confirmModal = false;
        this.confirmDoc = null;
        this.showToast(this.i18n.t('wr.toast.amount_confirmed'), 'success');
        this.load();
        this.loadSummary();
        this.cdr.markForCheck();
      },
      error: () => {
        this.showToast(this.i18n.t('wr.toast.confirm_error'), 'error');
        this.cdr.markForCheck();
      }
    });
  }

  // Permet de saisir/modifier le montant d'un Bon de Commande déjà uploadé
  // (pas d'extraction automatique possible pour ce type de document).
  editOrderAmount(doc: WipDocument): void {
    this.confirmDoc = doc;
    this.confirmAmount = doc.confirmedAmount || 0;
    this.confirmModal = true;
    this.cdr.markForCheck();
  }

  cancelConfirm(): void {
    this.confirmModal = false;
    this.confirmDoc = null;
    this.cdr.markForCheck();
  }

  // ── Téléchargement ──────────────────────────────────────────────
  downloadDoc(doc: WipDocument): void {
    if (!doc.id) return;
    this.api.downloadWipDocument(this.projectId, doc.id).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = doc.fileName || 'document.pdf';
      a.click(); URL.revokeObjectURL(url);
    });
  }

  // ── Suppression document ────────────────────────────────────────
  openDeleteDoc(doc: WipDocument): void {
    this.deleteDocTarget = doc;
    this.deleteDocModal = true;
    this.cdr.markForCheck();
  }

  cancelDeleteDoc(): void {
    this.deleteDocTarget = null;
    this.deleteDocModal = false;
    this.cdr.markForCheck();
  }

  confirmDeleteDoc(): void {
    if (!this.deleteDocTarget?.id) return;
    this.api.deleteWipDocument(this.projectId, this.deleteDocTarget.id).subscribe({
      next: () => {
        this.deleteDocTarget = null;
        this.deleteDocModal = false;
        this.showToast(this.i18n.t('wr.toast.doc_deleted'), 'success');
        this.load();
        this.loadSummary();
        this.cdr.markForCheck();
      },
      error: () => {
        this.deleteDocModal = false;
        this.showToast(this.i18n.t('wr.toast.delete_error'), 'error');
        this.cdr.markForCheck();
      }
    });
  }

  // ── Helpers ─────────────────────────────────────────────────────
  getDoc(row: WipTableRow, type: string): WipDocument | undefined {
    return row.documents.find(d => d.documentType === type);
  }

  invoicedHint(row: WipTableRow): string {
    const bl = this.getDoc(row, 'BL_SIGNE');
    const bc = this.getDoc(row, 'BON_COMMANDE');
    if (!bl) return '— Pas de BL';
    if (!bc) return '— Bon de commande manquant';
    if (bl.confirmedAmount == null) return '— Montant à confirmer';
    return '— Pas de BL';
  }

  // ── Filtres ──────────────────────────────────────────────────────
  private get currentPeriod(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  }

  get availableYears(): number[] {
    const cp = this.currentPeriod;
    return [...new Set(
      this.rows
        .filter(r => r.period <= cp)
        .map(r => +r.period.split('-')[0])
    )].sort((a, b) => a - b);
  }

  get filteredRows(): WipTableRow[] {
    const cp = this.currentPeriod;
    return this.rows.filter(r => {
      if (r.period > cp) return false;
      if (+r.period.split('-')[0] !== this.selectedYear) return false;
      if (this.hideEmpty && r.declaredAmount === 0 && r.docCount === 0) return false;
      return true;
    });
  }

  selectYear(y: number): void { this.selectedYear = y; this.cdr.markForCheck(); }
  toggleHideEmpty(): void { this.hideEmpty = !this.hideEmpty; this.cdr.markForCheck(); }

  // KPIs sur tous les mois passés (pas filtrés par année)
  private get pastRows(): WipTableRow[] {
    const cp = this.currentPeriod;
    return this.rows.filter(r => r.period <= cp);
  }

  get totalDeclared(): number { return this.pastRows.reduce((s, r) => s + (r.declaredAmount || 0), 0); }
  get totalInvoiced(): number { return this.pastRows.reduce((s, r) => s + (r.invoicedAmount || 0), 0); }
  get totalDelta(): number    { return this.pastRows.reduce((s, r) => s + (r.delta || 0), 0); }

  deltaClass(d: number): string {
    if (d === 0) return 'delta-zero';
    return d > 0 ? 'delta-pos' : 'delta-neg';
  }

  // Reste sur commande : positif = favorable (encore du budget) -> vert,
  // négatif = défavorable (commande dépassée) -> orange. Réutilise les
  // classes delta-pos/delta-neg existantes mais avec la logique inversée.
  orderRemainingClass(v: number): string {
    if (v === 0) return 'delta-zero';
    return v > 0 ? 'delta-neg' : 'delta-pos';
  }

  fmt(v: number | undefined | null): string {
    if (v == null) return '—';
    return v.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' €';
  }

  fmtPeriod(period: string): string {
    if (!period) return '—';
    const [y, m] = period.split('-');
    const months = ['Jan','Fév','Mar','Avr','Mai','Jun','Jul','Aoû','Sep','Oct','Nov','Déc'];
    return `${months[+m - 1] || m} ${y}`;
  }
}
