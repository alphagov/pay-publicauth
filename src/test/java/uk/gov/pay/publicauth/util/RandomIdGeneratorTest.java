package uk.gov.pay.publicauth.util;

import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.primitives.Chars.asList;
import static org.apache.commons.collections4.CollectionUtils.containsAll;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.publicauth.util.RandomIdGenerator.newId;

public class RandomIdGeneratorTest {

    private static final List<Character> BASE32_DICTIONARY = asList("0123456789abcdefghijklmnopqrstuv".toCharArray());

    @Test
    public void shouldGenerateRandomIds() throws Exception {
        // given
        Set<String> randomIds = IntStream.range(0, 100)
                .parallel()
                .mapToObj(value -> newId()).collect(Collectors.toSet());

        // then 1. guarantees no duplicates
        assertThat(randomIds.size(), is(100));

        // then 2. expects dictionary in Base32Hex
        randomIds.stream().forEach(id -> assertThat(containsAll(BASE32_DICTIONARY, asList(id.toCharArray())), is(true)));
    }
}
