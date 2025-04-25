import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormControl } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner'; // For loading indicator
import { Observable, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs/operators';
import {MarketDataService} from '../../../services/market-data.service';
import {InstrumentSearchResult} from '../../../services/search.service';

export interface AddInvestmentDialogData {
  portfolioId: number;
  portfolioName: string;
}

export interface AddInvestmentDialogResult {
  ticker: string;
  type: string;
  currency: string;
  amount: number;
  purchasePrice: number;
  portfolioId: number;
}


@Component({
  selector: 'app-add-investment-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatAutocompleteModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './add-investment-dialog.component.html',
})
export class AddInvestmentDialogComponent implements OnInit {
  investmentForm: FormGroup;
  searchControl = new FormControl('');
  searchResults$: Observable<InstrumentSearchResult[]>;
  isLoadingSearch = false;
  selectedInstrument: InstrumentSearchResult | null = null;

  // Use Subject for debouncing search input
  private readonly searchTerms = new Subject<string>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly marketDataService: MarketDataService,
    public dialogRef: MatDialogRef<AddInvestmentDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AddInvestmentDialogData
  ) {
    this.investmentForm = this.fb.group({
      ticker: [{value: '', disabled: true}, Validators.required], // Start disabled
      type: [{value: '', disabled: true}, Validators.required], // Start disabled
      currency: [{value: '', disabled: true}, Validators.required], // Start disabled
      amount: ['', [Validators.required, Validators.min(0.000001)]],
      purchasePrice: ['', [Validators.required, Validators.min(0)]]
    });

    // Setup observable pipeline for search results
    this.searchResults$ = this.searchTerms.pipe(
      debounceTime(600), // Wait 300ms after last keystroke
      distinctUntilChanged(), // Ignore if query hasn't changed
      tap(() => this.isLoadingSearch = true), // Show loading indicator
      switchMap((term: string) => this.marketDataService.searchInstruments(term)), // Use marketDataService
      tap(() => this.isLoadingSearch = false) // Hide loading indicator
    );
  }

  ngOnInit(): void {
    // Fields are already started disabled in form builder
  }

  // Push search term into the Subject stream
  search(term: string): void {
    // Reset selection if user types again
    this.selectedInstrument = null;
    this.investmentForm.get('ticker')?.disable();
    this.investmentForm.get('type')?.disable();
    this.investmentForm.get('currency')?.disable();
    this.investmentForm.patchValue({ ticker: '', type: '', currency: '' });

    this.searchTerms.next(term);
  }

  // When an option is selected from the autocomplete
  onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    this.selectedInstrument = event.option.value as InstrumentSearchResult;
    console.log('Selected Instrument:', this.selectedInstrument);

    if (this.selectedInstrument) {
      // Patch the form with data from selected instrument
      this.investmentForm.patchValue({
        ticker: this.selectedInstrument.symbol,
        type: this.selectedInstrument.type,
        currency: this.selectedInstrument.currency
      });
      // Re-enable the fields
      this.investmentForm.get('ticker')?.enable();
      this.investmentForm.get('type')?.enable();
      this.investmentForm.get('currency')?.enable();

      this.searchControl.setValue(''); // Clear search input display
      this.searchTerms.next(''); // Push empty string to clear results list
    }
  }

  // Helper to display the name in the autocomplete option
  displayFn(instrument: InstrumentSearchResult): string {
    // Handle initial state or if user clears input
    return instrument && instrument.name ? `${instrument.name} (${instrument.symbol})` : '';
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    if (this.investmentForm.valid && this.selectedInstrument) {
      const result: AddInvestmentDialogResult = {
        ...this.investmentForm.getRawValue(),
        portfolioId: this.data.portfolioId // Add portfolioId from injected data
      };
      this.dialogRef.close(result);
    } else {
      console.error("Form invalid or instrument not selected", this.investmentForm.errors);
    }
  }
}
