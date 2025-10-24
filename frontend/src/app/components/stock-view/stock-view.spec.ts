import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StockView } from './stock-view';

describe('StockView', () => {
  let component: StockView;
  let fixture: ComponentFixture<StockView>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [StockView]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StockView);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
