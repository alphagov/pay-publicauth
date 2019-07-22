package uk.gov.pay.publicauth.service;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DoublePostgresMetric extends PostgresMetric<Double> {
    DoublePostgresMetric(String name, Double value) {
        super(name, value);
    }

    void setValueFromResultSet(ResultSet resultSet) throws SQLException {
        value = resultSet.getDouble(getName());
    }
}
