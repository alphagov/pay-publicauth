package uk.gov.pay.publicauth.service;

import java.sql.ResultSet;
import java.sql.SQLException;

public class LongPostgresMetric extends PostgresMetric<Long> {
    LongPostgresMetric(String name, Long value) {
        super(name, value);
    }

    void setValueFromResultSet(ResultSet resultSet) throws SQLException {
        value = resultSet.getLong(getName());
    }
}
