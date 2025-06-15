import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { PreferenceService } from '../../services/preference.service';

interface Currency {
  value: string;
  viewValue: string;
}

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatCardModule,
    MatInputModule,
    MatSlideToggleModule
  ],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css']
})
export class SettingsComponent implements OnInit {

  availableCurrencies: Currency[] = [
    { value: 'USD', viewValue: 'US Dollar (USD)' },
    { value: 'EUR', viewValue: 'Euro (EUR)' },
    { value: 'GBP', viewValue: 'British Pound (GBP)' },
    { value: 'JPY', viewValue: 'Japanese Yen (JPY)' },
    { value: 'CAD', viewValue: 'Canadian Dollar (CAD)' }
  ];

  selectedCurrency: string = 'USD';
  darkMode: boolean = false;

  constructor(private prefSvc: PreferenceService) { }

  ngOnInit(): void {
    this.selectedCurrency = this.prefSvc.currentCurrency;
    this.darkMode = this.prefSvc.isDarkMode;
  }

  onCurrencyChange(newValue: string): void {
    this.selectedCurrency = newValue;
    this.prefSvc.changeCurrency(newValue);
  }

  onDarkModeChange(enabled: boolean): void {
    this.darkMode = enabled;
    this.prefSvc.setDarkMode(enabled);
  }
}
