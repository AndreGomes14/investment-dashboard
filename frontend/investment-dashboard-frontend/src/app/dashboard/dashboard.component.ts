import { Component, OnInit, ViewChild, HostListener } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialogModule, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { RouterModule } from '@angular/router';
import { MatSidenav } from '@angular/material/sidenav';

// Logout confirmation dialog component
@Component({
  selector: 'logout-confirmation-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule, 
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>Confirm Logout</h2>
    <mat-dialog-content>
      <p>Are you sure you want to logout?</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-button [mat-dialog-close]="true" color="primary">Logout</button>
    </mat-dialog-actions>
  `
})
export class LogoutConfirmationDialog {}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatSidenavModule,
    MatListModule,
    MatDividerModule,
    MatMenuModule,
    MatDialogModule,
    RouterModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  sidenavOpened = true;
  
  @ViewChild('sidenav') sidenav!: MatSidenav;

  constructor(
    private readonly authService: AuthService, 
    private readonly router: Router,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.checkScreenSize();
  }
  
  @HostListener('window:resize', ['$event'])
  onResize() {
    this.checkScreenSize();
  }

  checkScreenSize(): void {
    this.sidenavOpened = window.innerWidth > 768;
  }

  logout(): void {
    const dialogRef = this.dialog.open(LogoutConfirmationDialog, {
      width: '300px'
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === true) {
        this.authService.logout();
        this.router.navigate(['/login']);
      }
    });
  }
}
