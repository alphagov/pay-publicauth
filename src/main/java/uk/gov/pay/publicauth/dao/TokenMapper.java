package uk.gov.pay.publicauth.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.pay.publicauth.model.ServiceMode;
import uk.gov.pay.publicauth.model.TokenEntity;
import uk.gov.pay.publicauth.model.TokenLink;
import uk.gov.pay.publicauth.model.TokenPaymentType;
import uk.gov.pay.publicauth.model.TokenSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;


public class TokenMapper implements RowMapper<TokenEntity> {

    @Override
    public TokenEntity map(ResultSet rs, StatementContext ctx) throws SQLException {
        TokenEntity.Builder tokenBuilder = new TokenEntity.Builder()
                .withTokenLink(TokenLink.of(rs.getString("token_link")))
                .withDescription(rs.getString("description"))
                .withAccountId(rs.getString("account_id"))
                .withTokenSource(TokenSource.valueOf(rs.getString("type")))
                .withRevokedDate(getZonedDateTime(rs, "revoked").orElse(null))
                .withIssuedDate(getZonedDateTime(rs, "issued").orElse(null))
                .withLastUsedDate(getZonedDateTime(rs, "last_used").orElse(null))
                .withCreatedBy(rs.getString(("created_by")))
                .withTokenPaymentType(
                        Optional.ofNullable(rs.getString("token_type"))
                                .map(TokenPaymentType::valueOf)
                                .orElse(TokenPaymentType.CARD))
                .withServiceExternalId(rs.getString("service_external_id"))
                .withServiceMode(ServiceMode.valueOf(rs.getString("service_mode")));

        return tokenBuilder.build();
    }

    private Optional<ZonedDateTime> getZonedDateTime(ResultSet rs, String columnLabel) throws SQLException {
        LocalDateTime dateTime = rs.getObject(columnLabel, LocalDateTime.class);

        return Optional.ofNullable(dateTime)
                .map(t -> ZonedDateTime.ofInstant(dateTime.toInstant(ZoneOffset.UTC), ZoneId.of("UTC")));
    }
}
