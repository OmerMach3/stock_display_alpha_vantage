import { NgModule, provideBrowserGlobalErrorListeners } from '@angular/core';
import { BrowserModule, provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import { App } from './app';
import { StockView } from './components/stock-view/stock-view';
import { StockChartComponent } from './components/stock-chart/stock-chart';

@NgModule({
  declarations: [App],
  imports: [BrowserModule, HttpClientModule, FormsModule, StockView, StockChartComponent],
  providers: [provideBrowserGlobalErrorListeners(), provideClientHydration(withEventReplay())],
  bootstrap: [App],
})
export class AppModule {}
