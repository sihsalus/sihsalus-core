package org.openmrs.module.stockmanagement.api.dto.reporting;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class StockItemInventoryExpiryForecast extends StockItemInventoryForecast {

  private BigDecimal loss;

  private BigDecimal stockItemConsumptionRate;

  private List<BigDecimal> forecastBalances;

  public BigDecimal getLoss() {
    return loss;
  }

  public void setLoss(BigDecimal loss) {
    this.loss = loss;
  }

  public BigDecimal getStockItemConsumptionRate() {
    return stockItemConsumptionRate;
  }

  public void setStockItemConsumptionRate(BigDecimal stockItemConsumptionRate) {
    this.stockItemConsumptionRate = stockItemConsumptionRate;
  }

  public List<BigDecimal> getForecastBalances() {
    return forecastBalances == null ? null : new ArrayList<>(forecastBalances);
  }

  public void setForecastBalances(List<BigDecimal> forecastBalances) {
    this.forecastBalances = forecastBalances == null ? null : new ArrayList<>(forecastBalances);
  }

  public void addForecastBalance(BigDecimal forecastBalance) {
    if (forecastBalances == null) {
      forecastBalances = new ArrayList<>();
    }
    forecastBalances.add(forecastBalance);
  }
}
