import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StockService, StockData } from '../../services/stock';
import { StockChartComponent } from '../stock-chart/stock-chart';

@Component({
  selector: 'app-stock-view',
  standalone: true,
  imports: [CommonModule, FormsModule, StockChartComponent],
  templateUrl: './stock-view.html',
  styleUrls: ['./stock-view.css'],
})
export class StockView implements OnInit {
  stocks: string[] = ['IBM', 'AAPL', 'MSFT', 'GOOGL'];
  selectedStockIndex: number = 0;
  stockData: StockData[] = [];
  filteredStockData: StockData[] = [];

  // Arayüz durumu için değişkenler
  loading: boolean = false;
  syncing: boolean = false;
  lastUpdated: string | null = null;

  startDate: string = '';
  endDate: string = '';
  minPrice: number | null = null;
  maxPrice: number | null = null;

  constructor(private stockService: StockService) {}

  ngOnInit(): void {
    this.loadSymbols();
  }

  loadSymbols(): void {
    this.stockService.getSymbols().subscribe((symbols) => {
      this.stocks = symbols;
      if (this.stocks.length > 0) {
        this.selectedStockIndex = 0;
        this.loadStockData();
      }
    });
  }

  loadStockData(): void {
    if (this.stocks.length === 0) return;
    const selectedStock = this.stocks[this.selectedStockIndex];
    this.loading = true;
    this.stockService.getStocks(selectedStock).subscribe({
      next: (data) => {
        this.stockData = data;
        this.applyFilters();
        this.lastUpdated = new Date().toISOString();
      },
      error: (err) => {
        console.error('Hisse senedi verisi yüklenirken hata oluştu', err);
      },
      complete: () => (this.loading = false),
    });
  }

  onStockChange(): void {
    this.loadStockData();
  }

  // Veriyi arka uçtan yeniden yükleyerek tazele
  refreshData(): void {
    this.loadStockData();
  }

  // Arka uç senkronizasyon endpoint'ini tetikle ve tamamlandığında veriyi yenile
  syncNow(): void {
    const selectedStock = this.stocks[this.selectedStockIndex];
    this.syncing = true;
    this.stockService.syncStock(selectedStock).subscribe({
      next: (msg) => {
        console.log('Senkronizasyon yanıtı:', msg);
      },
      error: (err) => console.error('Senkronizasyon başarısız', err),
      complete: () => {
        this.syncing = false;
        // Senkronizasyondan sonra veriyi yeniden çek
        this.loadStockData();
      },
    });
  }

  applyFilters(): void {
    this.filteredStockData = this.stockData.filter((data) => {
      const date = new Date(data.date);
      const isAfterStartDate = !this.startDate || date >= new Date(this.startDate);
      const isBeforeEndDate = !this.endDate || date <= new Date(this.endDate);
      const isAboveMinPrice = this.minPrice === null || data.close >= this.minPrice;
      const isBelowMaxPrice = this.maxPrice === null || data.close <= this.maxPrice;
      return isAfterStartDate && isBeforeEndDate && isAboveMinPrice && isBelowMaxPrice;
    });
  }

  // ngFor için trackBy fonksiyonu
  trackById(index: number, item: StockData): number {
    return item.id;
  }
}
