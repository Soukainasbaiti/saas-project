import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.scss']
})
export class ChangePasswordComponent {
  form: FormGroup;
  loading      = false;
  errorMsg     = '';
  showNew      = false;
  showConfirm  = false;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router
  ) {
    this.form = this.fb.group({
      newPassword:     ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[A-Z])(?=.*[0-9]).{8,}$/)
      ]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const pw  = group.get('newPassword')?.value;
    const cpw = group.get('confirmPassword')?.value;
    return pw && cpw && pw !== cpw ? { mismatch: true } : null;
  }

  get strength(): 'weak' | 'medium' | 'strong' {
    const pw = this.form.get('newPassword')?.value || '';
    if (pw.length < 8) return 'weak';
    const hasUpper  = /[A-Z]/.test(pw);
    const hasDigit  = /[0-9]/.test(pw);
    const hasSymbol = /[^A-Za-z0-9]/.test(pw);
    if (hasUpper && hasDigit && hasSymbol && pw.length >= 12) return 'strong';
    if ((hasUpper && hasDigit) || pw.length >= 10) return 'medium';
    return 'weak';
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading  = true;
    this.errorMsg = '';

    const newPw = this.form.get('newPassword')!.value;

    this.auth.changePassword(newPw).subscribe({
      next: () => {
        this.loading = false;
        // Déconnecter et faire re-login avec le nouveau MDP
        this.auth.logout();
      },
      error: err => {
        this.loading  = false;
        this.errorMsg = err.error?.message || 'Erreur lors du changement de mot de passe.';
      }
    });
  }
}
