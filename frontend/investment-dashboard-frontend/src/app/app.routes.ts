import { Routes } from '@angular/router';
import {AuthGuard} from './guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
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
