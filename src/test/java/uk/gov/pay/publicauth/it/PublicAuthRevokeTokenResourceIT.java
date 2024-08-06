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
        
        createToken(token1);
        createToken(token2);

        List<Map<String, String>> retrievedTokens = given().port(applicationPort).accept(JSON)
                .param("state", "active")
                .get("/v1/frontend/auth/" + accountId)
                .then().statusCode(200)
                .body("tokens", hasSize(2))
                .extract().path("tokens");
        
        retrievedTokens.forEach(token -> assertThat(token.containsKey("revoked"), is(false)));
        
        given().port(applicationPort).accept(JSON).delete(format("/v1/frontend/auth/%s/revoke-all", accountId))
                .then().statusCode(200);

        retrievedTokens = given().port(applicationPort).accept(JSON)
                .param("state", "revoked")
                .get("/v1/frontend/auth/" + accountId)
                .then().statusCode(200)
                .body("tokens", hasSize(2))
                .extract().path("tokens");

        retrievedTokens.forEach(token -> assertThat(token.containsKey("revoked"), is(true)));
    }

    private void createToken(Map<String, String> token1) {
        given().port(applicationPort).accept(JSON).contentType(JSON).body(token1).post("/v1/frontend/auth")
                .then().statusCode(200);
    }
}
