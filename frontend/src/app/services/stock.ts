import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { isPlatformBrowser } from '@angular/common';

export interface StockData {
  id: number;
  symbol: string;
  date: string; // ISO date or datetime string
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

@Injectable({
  providedIn: 'root',
})
export class StockService {
  private apiUrl = '/api/stocks';
  private isBrowser: boolean;

  constructor(private http: HttpClient, @Inject(PLATFORM_ID) private platformId: Object) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  getSymbols(): Observable<string[]> {
    if (!this.isBrowser) {
      return of(['AAPL', 'MSFT', 'GOOGL', 'IBM']); // Return default symbols for SSR
    }
    return this.http.get<string[]>(`${this.apiUrl}/symbols`);
  }

  getStocks(symbol: string): Observable<StockData[]> {
    if (!this.isBrowser) {
      return of([]); // Return empty array for SSR
    }
    return this.http.get<StockData[]>(`${this.apiUrl}/${symbol}`);
  }

  // Günlük (son 1000 gün) veri
  getStocksDaily(symbol: string): Observable<StockData[]> {
    if (!this.isBrowser) {
      return of([]);
    }
    return this.http.get<StockData[]>(`${this.apiUrl}/${encodeURIComponent(symbol)}/daily`);
  }

  // Intraday 5min veri - backend DTO'yu StockData formatına dönüştürür
  getStocksIntraday(symbol: string): Observable<StockData[]> {
    if (!this.isBrowser) {
      return of([]);
    }
    return this.http.get<any[]>(`${this.apiUrl}/${encodeURIComponent(symbol)}/intraday`).pipe(
      map((arr: any[]) =>
        arr.map((p, idx) => ({
          id: idx + 1,
          symbol: p.symbol ?? symbol,
          date: p.timestamp,
          open: Number(p.open),
          high: Number(p.high),
          low: Number(p.low),
          close: Number(p.close),
          volume: Number(p.volume),
        }))
      )
    );
  }

  // Günlük veri senkronizasyonu
  syncStockDaily(symbol: string): Observable<string> {
    if (!this.isBrowser) {
      return of('SSR - No sync available');
    }
    return this.http.post(`${this.apiUrl}/syncDaily/${encodeURIComponent(symbol)}`, null, {
      responseType: 'text',
    }) as Observable<string>;
  }

  // Aylık veri senkronizasyonu
  syncStockMonthly(symbol: string): Observable<string> {
    if (!this.isBrowser) {
      return of('SSR - No sync available');
    }
    return this.http.post(`${this.apiUrl}/syncMonthly/${encodeURIComponent(symbol)}`, null, {
      responseType: 'text',
    }) as Observable<string>;
  }
}
