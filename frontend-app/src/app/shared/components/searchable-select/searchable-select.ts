import { Component, EventEmitter, forwardRef, Input, OnChanges, Output } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface SearchableSelectOption {
  value: string;
  label: string;
  searchText?: string;
  detail?: string;
}

@Component({
  selector: 'app-searchable-select',
  templateUrl: './searchable-select.html',
  styleUrl: './searchable-select.scss',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => SearchableSelect),
    multi: true,
  }],
})
export class SearchableSelect implements ControlValueAccessor, OnChanges {
  @Input() inputId = 'searchable-select';
  @Input() options: SearchableSelectOption[] = [];
  @Input() placeholder = 'Buscar...';
  @Input() emptyMessage = 'No hay opciones disponibles.';
  @Input() loadingMessage = 'Buscando...';
  @Input() ariaLabel = 'Opciones';
  @Input() loading = false;
  @Output() readonly searchChange = new EventEmitter<string>();
  @Output() readonly dropdownChange = new EventEmitter<boolean>();
  @Output() readonly optionSelected = new EventEmitter<SearchableSelectOption>();

  query = '';
  isOpen = false;
  isDisabled = false;
  activeIndex = -1;
  private selectedValue = '';
  private lastSelectedOption?: SearchableSelectOption;
  private filterQuery = '';

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  get filteredOptions(): SearchableSelectOption[] {
    const term = this.normalize(this.filterQuery.trim());
    if (!term) return this.options;

    return this.options.filter((option) =>
      this.normalize(`${option.label} ${option.searchText ?? ''}`).includes(term),
    );
  }

  ngOnChanges(): void {
    if (!this.isOpen) this.syncQueryToSelection();
  }

  writeValue(value: string | null): void {
    if (this.selectedValue !== (value ?? '')) {
      this.lastSelectedOption = this.options.find((option) => option.value === value);
    } else {
      this.lastSelectedOption = this.options.find((option) => option.value === value)
        ?? this.lastSelectedOption;
    }
    this.selectedValue = value ?? '';
    this.syncQueryToSelection();
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.isDisabled = disabled;
  }

  open(): void {
    if (this.isDisabled) return;
    this.query = this.selectedOption()?.label ?? '';
    this.filterQuery = '';
    this.activeIndex = -1;
    this.setOpen(true);
  }

  onQueryChange(query: string): void {
    this.query = query;
    this.filterQuery = query;
    this.activeIndex = -1;
    this.setOpen(true);
    this.searchChange.emit(query);
  }

  onBlur(): void {
    setTimeout(() => {
      this.setOpen(false);
      this.syncQueryToSelection();
      this.onTouched();
    }, 150);
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.setOpen(false);
      this.syncQueryToSelection();
      return;
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      if (!this.isOpen) this.open();
      this.activeIndex = Math.min(this.activeIndex + 1, this.filteredOptions.length - 1);
    } else if (event.key === 'ArrowUp' && this.isOpen) {
      event.preventDefault();
      this.activeIndex = Math.max(this.activeIndex - 1, 0);
    } else if (event.key === 'Enter' && this.isOpen && this.activeIndex >= 0) {
      event.preventDefault();
      const option = this.filteredOptions[this.activeIndex];
      if (option) this.select(option);
    }
  }

  select(option: SearchableSelectOption): void {
    this.selectedValue = option.value;
    this.lastSelectedOption = option;
    this.query = option.label;
    this.filterQuery = '';
    this.setOpen(false);
    this.onChange(option.value);
    this.onTouched();
    this.optionSelected.emit(option);
  }

  optionId(index: number): string {
    return `${this.inputId}-option-${index}`;
  }

  isSelected(option: SearchableSelectOption): boolean {
    return option.value === this.selectedValue;
  }

  private selectedOption(): SearchableSelectOption | undefined {
    return this.options.find((option) => option.value === this.selectedValue)
      ?? (this.lastSelectedOption?.value === this.selectedValue ? this.lastSelectedOption : undefined);
  }

  private syncQueryToSelection(): void {
    this.query = this.selectedOption()?.label ?? '';
    this.filterQuery = '';
  }

  private setOpen(open: boolean): void {
    if (this.isOpen === open) return;
    this.isOpen = open;
    this.dropdownChange.emit(open);
  }

  private normalize(value: string): string {
    return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('es');
  }
}
