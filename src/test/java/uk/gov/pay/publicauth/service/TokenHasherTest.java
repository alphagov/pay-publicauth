package uk.gov.pay.publicauth.service;

import org.junit.Test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TokenHasherTest {

    @Test
    public void producedHashedTokenShouldBeDifferentToToken() {
        String aToken = randomUUID().toString();
        String hashedToken = new TokenHasher().hash(aToken);
        assertThat(hashedToken, is(not(aToken)));
    }

    @Test
    public void shouldProduceTheSameHashWhenHashingTheSameTokenTwice() {
        String aToken = randomUUID().toString();
        String hashedToken = new TokenHasher().hash(aToken);
        String hashedToken2 = new TokenHasher().hash(aToken);
        assertThat(hashedToken, is(hashedToken2));
    }

}