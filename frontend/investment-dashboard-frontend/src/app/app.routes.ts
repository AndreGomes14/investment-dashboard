import { Routes } from '@angular/router';
import {AuthGuard} from './guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'portfolio',
    pathMatch: 'full'
  },
  {
    path: 'portfolio',
    loadComponent: () => import('./dashboard/portfolio/portfolio.component').then(m => m.PortfolioComponent)
  },
  {
    path: 'investments',
    loadComponent: () => import('./dashboard/investments/investments.component').then(m => m.InvestmentsComponent)
  },
  {
    path: 'planning',
    loadComponent: () => import('./dashboard/planning/planning.component').then(m => m.PlanningComponent)
  },
  {
    path: 'analytics',
    loadComponent: () => import('./dashboard/analytics/analytics.component').then(m => m.AnalyticsComponent)
  },
  {
    path: 'settings',
    loadComponent: () => import('./dashboard/settings/settings.component').then(m => m.SettingsComponent)
  },
  {
    path: 'auth',
    loadChildren: () => import('./auth/auth.module').then(m => m.AuthModule)
  },
  {
    path: 'dashboard',
    loadChildren: () => import('./dashboard/dashboard.module').then(m => m.DashboardModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'login',
    redirectTo: 'auth/login'
  },
  {
    path: 'register',
    redirectTo: 'auth/register'
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
