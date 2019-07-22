package uk.gov.pay.publicauth.service;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

import java.sql.ResultSet;
import java.sql.SQLException;

abstract class PostgresMetric<T> implements Gauge<T> {
    private final String name;
    protected T value;

    PostgresMetric(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
    }

    String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    void register(MetricRegistry registry, String prefix) {
        registry.<Gauge<T>>register(String.format("%s%s", prefix, name), this);
    }

    abstract void setValueFromResultSet(ResultSet resultSet) throws SQLException;
}
