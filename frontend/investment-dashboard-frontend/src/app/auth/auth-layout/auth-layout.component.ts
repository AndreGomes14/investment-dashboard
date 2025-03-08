import { Component } from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {NgOptimizedImage} from '@angular/common';

@Component({
  selector: 'app-auth-layout',
  templateUrl: './auth-layout.component.html',
  standalone: true,
  imports: [RouterOutlet, NgOptimizedImage],
  styleUrls: ['./auth-layout.component.scss']
})
export class AuthLayoutComponent {
  currentYear = new Date().getFullYear();
}
