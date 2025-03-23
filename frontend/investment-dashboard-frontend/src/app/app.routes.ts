import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'auth/login',
    pathMatch: 'full'
  },
  {
    path: 'overview',
    redirectTo: 'portfolio',
    pathMatch: 'full'
  },
  {
    path: 'portfolio',
    loadComponent: () => import('./dashboard/portfolio/portfolio.component').then(m => m.PortfolioComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'investments',
    loadComponent: () => import('./dashboard/investments/investments.component').then(m => m.InvestmentsComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'planning',
    loadComponent: () => import('./dashboard/planning/planning.component').then(m => m.PlanningComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'analytics',
    loadComponent: () => import('./dashboard/analytics/analytics.component').then(m => m.AnalyticsComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'settings',
    loadComponent: () => import('./dashboard/settings/settings.component').then(m => m.SettingsComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'ai-assistant',
    loadComponent: () => import('./dashboard/ai-assistant/ai-assistant.component').then(m => m.AiAssistantComponent),
    canActivate: [AuthGuard]
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
    redirectTo: 'auth/login'
  }
];
