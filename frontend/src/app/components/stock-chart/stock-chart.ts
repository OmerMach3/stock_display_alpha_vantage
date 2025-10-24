import {
  Component,
  Input,
  OnChanges,
  SimpleChanges,
  ViewChild,
  AfterViewInit,
  ElementRef,
  OnDestroy,
  Inject,
  PLATFORM_ID,
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { StockData } from '../../services/stock';

// Dynamically import Chart.js to avoid SSR issues
let Chart: any;
let registerables: any;

@Component({
  selector: 'app-stock-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stock-chart.html',
  styleUrls: ['./stock-chart.css'],
})
export class StockChartComponent implements OnChanges, AfterViewInit, OnDestroy {
  @Input() stockData: StockData[] = [];
  @Input() selectedStock: string = '';
  @ViewChild('chartCanvas', { static: true }) chartCanvas!: ElementRef<HTMLCanvasElement>;

  private chart: any = null;
  private isBrowser: boolean;

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  async ngAfterViewInit(): Promise<void> {
    if (this.isBrowser) {
      await this.loadChartJs();
      this.createChart();
    }
  }

  private async loadChartJs(): Promise<void> {
    if (!Chart) {
      const chartModule = await import('chart.js');
      Chart = chartModule.Chart;
      registerables = chartModule.registerables;
      Chart.register(...registerables);
    }
  }

  async ngOnChanges(changes: SimpleChanges): Promise<void> {
    if (changes['stockData'] && this.isBrowser) {
      if (!Chart) {
        await this.loadChartJs();
      }
      if (this.chart) {
        this.updateChart();
      } else if (this.chartCanvas) {
        this.createChart();
      }
    }
  }

  private createChart(): void {
    if (!this.chartCanvas || !Chart) return;

    const ctx = this.chartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const chartData = this.prepareChartData();

    const config: any = {
      type: 'line',
      data: chartData,
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: `${this.selectedStock} Stock Price Over Time`,
            font: {
              size: 16,
            },
          },
          legend: {
            display: true,
            position: 'top',
          },
        },
        scales: {
          x: {
            type: 'category',
            title: {
              display: true,
              text: 'Month',
            },
            ticks: {
              maxRotation: 45,
              minRotation: 0,
            },
          },
          y: {
            title: {
              display: true,
              text: 'Price ($)',
            },
            beginAtZero: false,
          },
        },
        elements: {
          line: {
            tension: 0.1,
          },
          point: {
            radius: 3,
            hoverRadius: 6,
          },
        },
      },
    };

    this.chart = new Chart(ctx, config);
  }

  private updateChart(): void {
    if (!this.chart) return;

    const chartData = this.prepareChartData();
    this.chart.data = chartData;

    // Update chart title with current stock
    if (this.chart.options.plugins?.title) {
      this.chart.options.plugins.title.text = `${this.selectedStock} Stock Price Over Time`;
    }

    this.chart.update();
  }

  private prepareChartData(): any {
    // Sort data by date
    const sortedData = [...this.stockData].sort(
      (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime()
    );

    // Group data by month and calculate monthly averages
    const monthlyData = this.groupByMonth(sortedData);

    const labels = Object.keys(monthlyData);
    const closePrices = Object.values(monthlyData).map((data) => data.avgClose);
    const openPrices = Object.values(monthlyData).map((data) => data.avgOpen);
    const highPrices = Object.values(monthlyData).map((data) => data.maxHigh);
    const lowPrices = Object.values(monthlyData).map((data) => data.minLow);

    return {
      labels: labels,
      datasets: [
        {
          label: 'Close Price',
          data: closePrices,
          borderColor: '#3b82f6',
          backgroundColor: '#3b82f6',
          fill: false,
          tension: 0.1,
        },
        {
          label: 'Open Price',
          data: openPrices,
          borderColor: '#10b981',
          backgroundColor: '#10b981',
          fill: false,
          tension: 0.1,
        },
        {
          label: 'High',
          data: highPrices,
          borderColor: '#f59e0b',
          backgroundColor: '#f59e0b',
          fill: false,
          tension: 0.1,
          borderDash: [5, 5],
        },
        {
          label: 'Low',
          data: lowPrices,
          borderColor: '#ef4444',
          backgroundColor: '#ef4444',
          fill: false,
          tension: 0.1,
          borderDash: [5, 5],
        },
      ],
    };
  }

  private groupByMonth(data: StockData[]): { [key: string]: any } {
    const monthlyData: { [key: string]: any } = {};

    data.forEach((item) => {
      const date = new Date(item.date);
      const monthKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;

      if (!monthlyData[monthKey]) {
        monthlyData[monthKey] = {
          dates: [],
          opens: [],
          closes: [],
          highs: [],
          lows: [],
        };
      }

      monthlyData[monthKey].dates.push(item.date);
      monthlyData[monthKey].opens.push(item.open);
      monthlyData[monthKey].closes.push(item.close);
      monthlyData[monthKey].highs.push(item.high);
      monthlyData[monthKey].lows.push(item.low);
    });

    // Calculate averages and extremes for each month
    Object.keys(monthlyData).forEach((month) => {
      const monthData = monthlyData[month];
      monthlyData[month] = {
        avgOpen: this.calculateAverage(monthData.opens),
        avgClose: this.calculateAverage(monthData.closes),
        maxHigh: Math.max(...monthData.highs),
        minLow: Math.min(...monthData.lows),
      };
    });

    return monthlyData;
  }

  private calculateAverage(numbers: number[]): number {
    return numbers.reduce((sum, num) => sum + num, 0) / numbers.length;
  }

  ngOnDestroy(): void {
    if (this.chart) {
      this.chart.destroy();
    }
  }
}
