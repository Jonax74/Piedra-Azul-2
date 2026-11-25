import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DatePicker } from './date-picker';

describe('DatePicker', () => {
  let fixture: ComponentFixture<DatePicker>;
  let component: DatePicker;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DatePicker],
    }).compileComponents();
    fixture = TestBed.createComponent(DatePicker);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('shows the selected month and builds a Monday-first calendar grid', () => {
    component.writeValue('2024-02-29');
    component.toggleCalendar();

    expect(component.calendarMonthLabel()).toContain('febrero');
    expect(component.calendarCells().filter(Boolean)).toHaveLength(29);
    expect(component.calendarCells().length % 7).toBe(0);
  });

  it('updates the form value when a date is selected', () => {
    const change = vi.fn();
    const touched = vi.fn();
    component.registerOnChange(change);
    component.registerOnTouched(touched);
    component.toggleCalendar();

    component.selectDate(component.calendarCells().find((day) => day?.number === 15)!.iso);

    expect(change).toHaveBeenCalledWith(component.selectedDate);
    expect(touched).toHaveBeenCalledOnce();
    expect(component.isOpen).toBe(false);
  });

  it('does not select dates outside the available-date set', () => {
    const change = vi.fn();
    component.availableDates = new Set(['2024-02-15']);
    component.registerOnChange(change);

    component.selectDate('2024-02-16');

    expect(component.selectedDate).toBe('');
    expect(change).not.toHaveBeenCalled();
  });
});
