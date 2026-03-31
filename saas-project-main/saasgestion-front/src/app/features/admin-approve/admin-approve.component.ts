import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
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

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    public  auth: AuthService
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token') ?? '';
    this.loadDetail();
  }

  loadDetail(): void {
    this.http.get<PendingDetail>(`http://localhost:8080/api/admin/approve/${this.token}`)
      .subscribe({
        next:  d => { this.detail = d; this.loading = false; },
        error: err => {
          this.loading  = false;
          this.errorMsg = err.status === 404
            ? 'Token invalide ou expiré.'
            : err.error?.error || 'Erreur de chargement.';
        }
      });
  }

  approve(): void {
    if (!confirm('Confirmer l\'approbation de ce projet ?')) return;
    this.processing = true;
    this.http.post(`http://localhost:8080/api/admin/approve/${this.token}`, { action: 'approve' })
      .subscribe({
        next: () => {
          this.processing = false;
          this.done       = true;
          this.doneAction = 'approve';
        },
        error: err => {
          this.processing = false;
          this.errorMsg   = err.error?.error || 'Erreur lors de l\'approbation.';
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
    this.http.post(`http://localhost:8080/api/admin/approve/${this.token}`, {
      action: 'reject',
      reason: this.rejectReason
    }).subscribe({
      next: () => {
        this.processing = false;
        this.done       = true;
        this.doneAction = 'reject';
      },
      error: err => {
        this.processing = false;
        this.errorMsg   = err.error?.error || 'Erreur lors du rejet.';
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