import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormControl } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { of, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, tap, filter, catchError, takeUntil } from 'rxjs/operators';
import { MarketDataService, InstrumentSearchResult } from '../../../services/market-data.service';
import {MatSelect} from '@angular/material/select';

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
  customName?: string;
  currentValue?: number;
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
    MatProgressSpinnerModule,
    MatSelect
  ],
  templateUrl: './add-investment-dialog.component.html',
})
export class AddInvestmentDialogComponent implements OnInit, OnDestroy {
  investmentForm: FormGroup;
  searchControl = new FormControl('');
  searchResultsList: InstrumentSearchResult[] = [];
  isLoadingSearch = false;
  selectedInstrument: InstrumentSearchResult | null = null;
  private readonly destroy$ = new Subject<void>();

  availableInvestmentTypes: string[] = ['Equity', 'Crypto', 'ETF', 'Bond', 'Other'];
  availableCurrencies: string[] = ['USD', 'EUR', 'GBP', 'JPY', 'CAD', 'AUD', 'CHF', 'CNY'];

  constructor(
    private readonly fb: FormBuilder,
    private readonly marketDataService: MarketDataService,
    public dialogRef: MatDialogRef<AddInvestmentDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AddInvestmentDialogData
  ) {
    this.investmentForm = this.fb.group({
      type: ['', Validators.required],
      ticker: [{value: '', disabled: true}, Validators.required],
      customName: [''],
      currency: [{value: '', disabled: true}, Validators.required],
      amount: ['', [Validators.required, Validators.min(0.000001)]],
      purchasePrice: ['', [Validators.required, Validators.min(0)]],
      currentValue: ['']
    });

    this.investmentForm.get('type')?.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(typeValue => {
      this.onInvestmentTypeChange(typeValue);
    });

    this.searchControl.valueChanges.pipe(
      takeUntil(this.destroy$),
      debounceTime(500),
      filter(term => (term || '').length >= 2),
      distinctUntilChanged(),
      tap(term => {
        this.isLoadingSearch = true;
        if (this.selectedInstrument && term !== this.displayFn(this.selectedInstrument)) {
          this.resetSelection();
        }
      }),
      switchMap(term =>
        this.marketDataService.searchInstruments(term || '').pipe(
          catchError(err => {
            console.error('Error searching instruments:', err);
            return of([]);
          })
        )
      ),
      tap({
        next: (results: InstrumentSearchResult[]) => {
          this.searchResultsList = results;
          this.isLoadingSearch = false;
        },
        error: err => {
          console.error('Error in search pipeline:', err);
          this.searchResultsList = [];
          this.isLoadingSearch = false;
        }
      })
    ).subscribe();
  }

  ngOnInit(): void {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private onInvestmentTypeChange(typeValue: string): void {
    if (typeValue === 'Other') {
      this.investmentForm.get('customName')?.setValidators([Validators.required]);
      this.investmentForm.get('customName')?.enable();
      this.investmentForm.get('currentValue')?.setValidators([Validators.required, Validators.min(0)]);
      this.investmentForm.get('currentValue')?.enable();
      
      this.investmentForm.get('ticker')?.clearValidators();
      this.investmentForm.get('ticker')?.disable();
      this.investmentForm.get('ticker')?.setValue('');
      
      this.investmentForm.get('currency')?.enable();
      this.investmentForm.get('currency')?.setValidators([Validators.required, Validators.minLength(3), Validators.maxLength(3)]);

      this.searchControl.disable();
      this.searchControl.setValue('');
      this.searchResultsList = [];
      this.selectedInstrument = null;

    } else {
      this.investmentForm.get('customName')?.clearValidators();
      this.investmentForm.get('customName')?.disable();
      this.investmentForm.get('customName')?.setValue('');
      this.investmentForm.get('currentValue')?.clearValidators();
      this.investmentForm.get('currentValue')?.disable();
      this.investmentForm.get('currentValue')?.setValue('');

      this.investmentForm.get('ticker')?.setValidators([Validators.required]);
      this.investmentForm.get('ticker')?.disable();

      this.investmentForm.get('currency')?.disable();
      this.investmentForm.get('currency')?.clearValidators();

      this.searchControl.enable();
    }
    this.investmentForm.get('customName')?.updateValueAndValidity();
    this.investmentForm.get('currentValue')?.updateValueAndValidity();
    this.investmentForm.get('ticker')?.updateValueAndValidity();
    this.investmentForm.get('currency')?.updateValueAndValidity();
  }

  private resetSelection(): void {
    this.selectedInstrument = null;
    this.investmentForm.get('ticker')?.disable();
    this.investmentForm.get('type')?.disable();
    this.investmentForm.get('currency')?.disable();
    this.investmentForm.patchValue({ ticker: '', type: '', currency: '' });
  }

  onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    this.selectedInstrument = event.option.value as InstrumentSearchResult;

    if (this.selectedInstrument) {
      const isCrypto = this.selectedInstrument.type?.toLowerCase() === 'crypto';

      this.investmentForm.patchValue({
        ticker: this.selectedInstrument.symbol,
        type: this.selectedInstrument.type || 'N/A',
        currency: isCrypto ? '' : this.selectedInstrument.currency
      });

      if (isCrypto) {
        this.investmentForm.get('currency')?.enable();
        this.investmentForm.get('currency')?.setValidators([Validators.required, Validators.minLength(3), Validators.maxLength(3)]);
      } else {
        this.investmentForm.get('currency')?.disable();
        this.investmentForm.get('currency')?.clearValidators();
        if (!this.selectedInstrument.currency) {
          this.investmentForm.patchValue({ currency: 'USD' });
        }
      }
      this.investmentForm.get('currency')?.updateValueAndValidity();

      const displayValue = this.displayFn(this.selectedInstrument);
      setTimeout(() => {
        this.searchControl.setValue(displayValue, { emitEvent: false });
      });
    }
  }

  displayFn(instrument: InstrumentSearchResult | null): string {
    return instrument && instrument.name ? `${instrument.name} (${instrument.symbol})` : '';
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    if (this.investmentForm.valid) {
      const formValue = this.investmentForm.getRawValue();
      let result: AddInvestmentDialogResult;

      if (formValue.type === 'Other') {
        result = {
          ticker: formValue.ticker || '',
          type: formValue.type,
          currency: formValue.currency,
          amount: formValue.amount,
          purchasePrice: formValue.purchasePrice,
          portfolioId: this.data.portfolioId,
          customName: formValue.customName,
          currentValue: formValue.currentValue
        };
      } else {
        if (!this.selectedInstrument) {
          console.error("Instrument not selected for non-Other type");
          return;
        }
        result = {
          ticker: formValue.ticker,
          type: formValue.type,
          currency: formValue.currency,
          amount: formValue.amount,
          purchasePrice: formValue.purchasePrice,
          portfolioId: this.data.portfolioId
        };
      }
      this.dialogRef.close(result);
    } else {
      console.error("Form invalid", this.investmentForm.errors, this.investmentForm.value);
      this.investmentForm.markAllAsTouched();
    }
  }
}
