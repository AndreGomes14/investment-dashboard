import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';

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
    MatInputModule
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

  constructor() { }

  ngOnInit(): void {
    console.log('Settings component initialized. Default currency:', this.selectedCurrency);
  }

  onCurrencyChange(newValue: string): void {
    this.selectedCurrency = newValue;
    console.log('Currency changed to:', this.selectedCurrency);
  }
}
