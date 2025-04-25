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
export class AddInvestmentDialogComponent implements OnInit, OnDestroy {
  investmentForm: FormGroup;
  searchControl = new FormControl('');
  searchResultsList: InstrumentSearchResult[] = [];
  isLoadingSearch = false;
  selectedInstrument: InstrumentSearchResult | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly marketDataService: MarketDataService,
    public dialogRef: MatDialogRef<AddInvestmentDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AddInvestmentDialogData
  ) {
    this.investmentForm = this.fb.group({
      ticker: [{value: '', disabled: true}, Validators.required],
      type: [{value: '', disabled: true}, Validators.required],
      currency: [{value: '', disabled: true}, Validators.required],
      amount: ['', [Validators.required, Validators.min(0.000001)]],
      purchasePrice: ['', [Validators.required, Validators.min(0)]]
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

  ngOnInit(): void { /* TODO document why this method 'ngOnInit' is empty */  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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
      this.investmentForm.patchValue({
        ticker: this.selectedInstrument.symbol,
        type: this.selectedInstrument.type,
        currency: this.selectedInstrument.currency
      });
      this.investmentForm.get('ticker')?.enable();
      this.investmentForm.get('type')?.enable();
      this.investmentForm.get('currency')?.enable();

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
    if (this.investmentForm.valid && this.selectedInstrument) {
      const result: AddInvestmentDialogResult = {
        ...this.investmentForm.getRawValue(),
        portfolioId: this.data.portfolioId
      };
      this.dialogRef.close(result);
    } else {
      console.error("Form invalid or instrument not selected", this.investmentForm.errors);
    }
  }
}
