import { Component } from '@angular/core';
import {RouterOutlet} from '@angular/router';

@Component({
  selector: 'app-auth-layout',
  templateUrl: './auth-layout.component.html',
  standalone: true,
  imports: [RouterOutlet],
  styleUrls: ['./auth-layout.component.scss']
})
export class AuthLayoutComponent {
  currentYear = new Date().getFullYear();
}
