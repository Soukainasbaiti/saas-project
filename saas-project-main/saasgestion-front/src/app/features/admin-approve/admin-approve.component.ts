import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';

interface PendingDetail {
  token: string;
  status: string;
  submittedBy: string;
  submittedAt: string;
  expiresAt: string;
  project: any;
}

@Component({
  selector: 'app-admin-approve',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-approve.component.html',
  styleUrls: ['./admin-approve.component.scss']
})
export class AdminApproveComponent implements OnInit {
  token        = '';
  detail: PendingDetail | null = null;
  loading      = true;
  processing   = false;
  errorMsg     = '';
  successMsg   = '';
  rejectReason = '';
  showReject   = false;
  done         = false;
  doneAction   = '';

  // Modale de confirmation
  showModal        = false;
  modalTitle       = '';
  modalMessage     = '';
  modalType: 'approve' | 'reject' = 'approve';

  openConfirm(type: 'approve' | 'reject'): void {
    this.modalType    = type;
    this.modalTitle   = type === 'approve' ? 'Confirmer l\'approbation' : 'Confirmer le rejet';
    this.modalMessage = type === 'approve'
      ? 'Êtes-vous sûr de vouloir approuver ce projet ? Il sera créé dans le système.'
      : 'Êtes-vous sûr de vouloir rejeter ce projet ? Le créateur sera notifié par email.';
    this.showModal = true;
    this.cdr.detectChanges();
  }

  closeModal(): void {
    this.showModal = false;
    this.cdr.detectChanges();
  }

  confirmAction(): void {
    this.showModal = false;
    if (this.modalType === 'approve') {
      this.approve();
    } else {
      this.reject();
    }
  }

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    public  auth: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token') ?? '';
    this.loadDetail();
  }

  loadDetail(): void {
    console.log('[AdminApprove] loadDetail() token=', this.token);
    this.http.get<PendingDetail>(`http://localhost:8080/api/admin/approve/${this.token}`)
      .subscribe({
        next: d => {
          console.log('[AdminApprove] réponse OK:', d);
          this.detail  = d;
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: err => {
          console.error('[AdminApprove] erreur HTTP:', err.status, err.error);
          this.loading  = false;
          this.errorMsg = err.status === 0
            ? 'Impossible de joindre le serveur.'
            : err.status === 404
              ? 'Token invalide ou expiré.'
              : err.error?.error || `Erreur ${err.status}`;
          this.cdr.detectChanges();
        }
      });
  }

  approve(): void {
    this.processing = true;
    this.errorMsg   = '';
    console.log('[AdminApprove] POST approve...');
    this.http.post(`http://localhost:8080/api/admin/approve/${this.token}`, { action: 'approve' })
      .subscribe({
        next: (res) => {
          console.log('[AdminApprove] approve OK:', res);
          this.processing = false;
          this.done       = true;
          this.doneAction = 'approve';
          this.cdr.detectChanges();
        },
        error: err => {
          console.error('[AdminApprove] approve ERREUR:', err.status, err.error);
          this.processing = false;
          this.errorMsg   = err.error?.error || `Erreur ${err.status} lors de l\'approbation.`;
          this.cdr.detectChanges();
        }
      });
  }

  reject(): void {
    if (!this.rejectReason.trim()) {
      this.errorMsg = 'Veuillez saisir un motif de rejet.';
      return;
    }
    this.processing = true;
    this.errorMsg   = '';
    console.log('[AdminApprove] POST reject...');
    this.http.post(`http://localhost:8080/api/admin/approve/${this.token}`, {
      action: 'reject',
      reason: this.rejectReason
    }).subscribe({
      next: (res) => {
        console.log('[AdminApprove] reject OK:', res);
        this.processing = false;
        this.done       = true;
        this.doneAction = 'reject';
        this.cdr.detectChanges();
      },
      error: err => {
        console.error('[AdminApprove] reject ERREUR:', err.status, err.error);
        this.processing = false;
        this.errorMsg   = err.error?.error || `Erreur ${err.status} lors du rejet.`;
        this.cdr.detectChanges();
      }
    });
  }

  fmt(v: number): string {
    if (v == null) return '—';
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency', currency: 'EUR', maximumFractionDigits: 0
    }).format(v);
  }

  formatDate(d: string | null): string {
    if (!d) return '—';
    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    }).format(new Date(d));
  }
}