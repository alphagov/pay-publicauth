package uk.gov.pay.publicauth.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresExtension;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

@ExtendWith(DropwizardAppWithPostgresExtension.class)
public class PublicAuthRevokeTokenResourceIT {

    private Integer applicationPort;

    @BeforeEach
    public void setup(Integer port) {
        applicationPort = port;
    }
    
    @Test
    public void revokeAllTokensForAnAccountSuccessfully() {
        String accountId = "1";
        
        Map<String, String> token1 = Map.of("account_id", accountId,
                "description", "Bellatrix's token",
                "token_account_type", "live",
                "created_by", "bellatrix@lestrange.hp");
        
        Map<String, String> token2 = Map.of("account_id", accountId,
                "description", "Sirius's token",
                "token_account_type", "test",
                "created_by", "sirius@black.hp");

        Map<String, String> token3 = Map.of("account_id", "2",
                "description", "Buckbeak's token",
                "token_account_type", "test",
                "created_by", "buck@beak.hp");
        
        createToken(token1);
        createToken(token2);
        createToken(token3);

        assertRevokedStatusForTokens(accountId, false, 2);
        assertRevokedStatusForTokens("2", false, 1);
        
        given().port(applicationPort).accept(JSON).delete(format("/v1/frontend/auth/%s/revoke-all", accountId))
                .then().statusCode(200);

        assertRevokedStatusForTokens(accountId, true, 2);
        assertRevokedStatusForTokens("2", false, 1);
    }

    private void assertRevokedStatusForTokens(String accountId, boolean isRevoked, int expectedNumberOfTokens) {
        List<Map<String, String>> retrievedTokens = given().port(applicationPort).accept(JSON)
                .param("state", isRevoked ? "revoked" : "active")
                .get("/v1/frontend/auth/" + accountId)
                .then().statusCode(200)
                .body("tokens", hasSize(expectedNumberOfTokens))
                .extract().path("tokens");

        retrievedTokens.forEach(token -> assertThat(token.containsKey("revoked"), is(isRevoked)));
    }

    private void createToken(Map<String, String> token1) {
        given().port(applicationPort).accept(JSON).contentType(JSON).body(token1).post("/v1/frontend/auth")
                .then().statusCode(200);
    }
}
