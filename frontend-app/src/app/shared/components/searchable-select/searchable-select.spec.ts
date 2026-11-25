import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SearchableSelect } from './searchable-select';

describe('SearchableSelect', () => {
  let fixture: ComponentFixture<SearchableSelect>;
  let component: SearchableSelect;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SearchableSelect],
    }).compileComponents();
    fixture = TestBed.createComponent(SearchableSelect);
    component = fixture.componentInstance;
    component.options = [
      { value: '1', label: 'María Gómez', searchText: '10203040', detail: 'CC 10203040' },
      { value: '2', label: 'Carlos Ruiz', searchText: '50607080' },
    ];
    fixture.detectChanges();
  });

  it('filters options by name or document and ignores accents', () => {
    component.onQueryChange('maria');
    expect(component.filteredOptions.map((option) => option.value)).toEqual(['1']);
    component.onQueryChange('506070');
    expect(component.filteredOptions.map((option) => option.value)).toEqual(['2']);
  });

  it('shows all available options when opened from a selected value', () => {
    component.writeValue('1');
    component.open();

    expect(component.query).toBe('María Gómez');
    expect(component.filteredOptions).toHaveLength(2);
  });

  it('keeps the query empty when the empty-value option is selected', () => {
    component.options = [{ value: '', label: 'Todos los profesionales' }, ...component.options];
    component.writeValue('');
    component.open();

    expect(component.query).toBe('');
    expect(component.filteredOptions).toHaveLength(3);
  });

  it('selects an option and reports the selected value', () => {
    const change = vi.fn();
    component.registerOnChange(change);
    const selected = vi.fn();
    component.optionSelected.subscribe(selected);

    component.select(component.options[1]);

    expect(change).toHaveBeenCalledWith('2');
    expect(selected).toHaveBeenCalledWith(component.options[1]);
    expect(component.query).toBe('Carlos Ruiz');
    expect(component.isOpen).toBe(false);
  });
});
