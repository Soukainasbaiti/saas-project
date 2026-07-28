import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { I18nService } from '../../core/services/i18n.service';

@Component({
  selector: 'app-admin-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-sidebar.component.html',
  styleUrls: ['./admin-sidebar.component.scss']
})
export class AdminSidebarComponent implements OnInit {

  readonly themes = [
    { key: '',           label: 'Pink', color: '#C4337A' },
    { key: 'theme-blue', label: 'Blue', color: '#1B9ED4' },
    { key: 'theme-navy', label: 'Base', color: '#2D2359' },
  ];

  currentThemeKey = localStorage.getItem('pm-theme') ?? '';
  mobileOpen = false;

  constructor(public auth: AuthService, public i18n: I18nService) {}

  ngOnInit(): void {
    this.applyTheme(this.currentThemeKey);
  }

  toggleMobile(): void {
    this.mobileOpen = !this.mobileOpen;
  }

  closeMobile(): void {
    this.mobileOpen = false;
  }

  setTheme(key: string): void {
    this.currentThemeKey = key;
    localStorage.setItem('pm-theme', key);
    this.applyTheme(key);
  }

  private applyTheme(key: string): void {
    const body = document.body;
    body.classList.remove('theme-navy', 'theme-blue');
    if (key) body.classList.add(key);
  }
}
