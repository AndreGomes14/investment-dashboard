import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './dashboard.component';
import { PortfolioComponent } from './portfolio/portfolio.component';
import { InvestmentsComponent } from './investments/investments.component';
import { PlanningComponent } from './planning/planning.component';
import { ReportsComponent } from './reports/reports.component';
import { SettingsComponent } from './settings/settings.component';
import { AiAssistantComponent } from './ai-assistant/ai-assistant.component';

const routes: Routes = [
  {
    path: '',
    component: DashboardComponent,
    children: [
      {
        path: '',
        redirectTo: 'portfolio',
        pathMatch: 'full'
      },
      {
        path: 'portfolio',
        component: PortfolioComponent
      },
      {
        path: 'investments',
        component: InvestmentsComponent
      },
      {
        path: 'planning',
        component: PlanningComponent
      },
      {
        path: 'reports',
        component: ReportsComponent
      },
      {
        path: 'settings',
        component: SettingsComponent
      },
      {
        path: 'ai-assistant',
        component: AiAssistantComponent
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class DashboardRoutingModule { } 