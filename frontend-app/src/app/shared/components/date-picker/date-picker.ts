import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface CalendarDay {
  iso: string;
  number: number;
}

@Component({
  selector: 'app-date-picker',
  templateUrl: './date-picker.html',
  styleUrl: './date-picker.scss',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DatePicker),
    multi: true,
  }],
})
export class DatePicker implements ControlValueAccessor {
  @Input() inputId = 'date-picker';
  @Input() label = 'Fecha';
  @Input() emptyLabel = 'Todas las fechas';
  @Input() ariaLabel = 'Seleccionar fecha';
  @Input() availableDates: ReadonlySet<string> | null = null;

  readonly weekdays = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];
  selectedDate = '';
  calendarMonth = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
  isOpen = false;
  isDisabled = false;

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  writeValue(value: string | null): void {
    this.selectedDate = value ?? '';
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

  toggleCalendar(): void {
    if (this.isDisabled) return;
    if (this.isOpen) {
      this.closeCalendar();
      return;
    }

    const selected = this.parseDate(this.selectedDate);
    const month = selected ?? new Date();
    this.calendarMonth = new Date(month.getFullYear(), month.getMonth(), 1);
    this.isOpen = true;
  }

  closeCalendar(): void {
    this.isOpen = false;
  }

  changeMonth(offset: number): void {
    this.calendarMonth = new Date(
      this.calendarMonth.getFullYear(),
      this.calendarMonth.getMonth() + offset,
      1,
    );
  }

  calendarMonthLabel(): string {
    return new Intl.DateTimeFormat('es-CO', {
      month: 'long',
      year: 'numeric',
    }).format(this.calendarMonth);
  }

  calendarCells(): Array<CalendarDay | null> {
    const year = this.calendarMonth.getFullYear();
    const month = this.calendarMonth.getMonth();
    const firstWeekday = (new Date(year, month, 1).getDay() + 6) % 7;
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const cells: Array<CalendarDay | null> = [];

    for (let index = 0; index < firstWeekday; index += 1) cells.push(null);
    for (let day = 1; day <= daysInMonth; day += 1) {
      cells.push({
        iso: `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`,
        number: day,
      });
    }
    while (cells.length % 7 !== 0) cells.push(null);

    return cells;
  }

  selectDate(date: string): void {
    if (!this.isDateAvailable(date)) return;
    this.selectedDate = date;
    this.isOpen = false;
    this.onChange(date);
    this.onTouched();
  }

  isDateAvailable(date: string): boolean {
    return this.availableDates === null || this.availableDates.has(date);
  }

  private parseDate(value: string): Date | null {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return null;
    const date = new Date(`${value}T12:00:00`);
    return Number.isNaN(date.getTime()) ? null : date;
  }
}
