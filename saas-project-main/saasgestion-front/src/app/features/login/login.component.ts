import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

const REMEMBERED_EMAIL_KEY = 'remembered_email';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  form: FormGroup;
  loading      = false;
  errorMsg     = '';
  showPassword = false;
  year         = new Date().getFullYear();

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.form = this.fb.group({
      email:    ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });

    const rememberedEmail = localStorage.getItem(REMEMBERED_EMAIL_KEY);
    if (rememberedEmail) {
      this.form.patchValue({ email: rememberedEmail });
    }
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading  = true;
    this.errorMsg = '';

    this.auth.login(this.form.value).subscribe({
      next: res => {
        this.loading = false;
        localStorage.setItem(REMEMBERED_EMAIL_KEY, this.form.value.email);
        if (res.forceChange) {
          this.router.navigate(['/change-password']);
        } else {
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl')
            || (res.role === 'ADMIN' ? '/admin' : '/');
          this.router.navigate([returnUrl]);
        }
      },
      error: err => {
        this.loading  = false;
        this.errorMsg = err.status === 401
          ? 'Email ou mot de passe incorrect.'
          : 'Erreur de connexion. Veuillez réessayer.';
      }
    });
  }
}