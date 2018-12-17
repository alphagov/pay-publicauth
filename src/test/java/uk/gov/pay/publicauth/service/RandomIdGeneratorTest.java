package uk.gov.pay.publicauth.service;

import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.primitives.Chars.asList;
import static org.apache.commons.collections4.CollectionUtils.containsAll;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.publicauth.service.RandomIdGenerator.RANDOM_ID_MAX_LENGTH;
import static uk.gov.pay.publicauth.service.RandomIdGenerator.RANDOM_ID_MIN_LENGTH;
import static uk.gov.pay.publicauth.service.RandomIdGenerator.newId;

public class RandomIdGeneratorTest {

    private static final List<Character> BASE32_DICTIONARY = asList("0123456789abcdefghijklmnopqrstuv".toCharArray());

    @Test
    public void shouldGenerateRandomIds() {
        // given
        final int numbersOfIdsToGenerate = 1000;
        Set<String> randomIds = IntStream.range(0, numbersOfIdsToGenerate)
                .parallel()
                .mapToObj(value -> newId()).collect(Collectors.toSet());

        // then 1. guarantees no duplicates
        assertThat(randomIds.size(), is(numbersOfIdsToGenerate));

        // then 2. expects dictionary in Base32Hex
        randomIds.stream().forEach(id -> {
            assertThat(containsAll(BASE32_DICTIONARY, asList(id.toCharArray())), is(true));
            assertThat(id.length(), greaterThanOrEqualTo(RANDOM_ID_MIN_LENGTH));
            assertThat(id.length(), lessThanOrEqualTo(RANDOM_ID_MAX_LENGTH));
        });
    }
}
