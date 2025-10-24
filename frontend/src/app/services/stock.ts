import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';

export interface StockData {
  id: number;
  symbol: string;
  date: string;
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

  // Backendde bir sembol için veri senkronizasyonu başlatır (POST /api/stocks/sync/{symbol} çağrısı yapar).
  syncStock(symbol: string): Observable<string> {
    if (!this.isBrowser) {
      return of('SSR - No sync available'); // Return default message for SSR
    }
    // responseType: 'text' kullanarak ve Observable<string> olarak cast ederek metin cevabı alıyoruz.
    return this.http.post(`${this.apiUrl}/sync/${encodeURIComponent(symbol)}`, null, {
      responseType: 'text',
    }) as Observable<string>;
  }
}
