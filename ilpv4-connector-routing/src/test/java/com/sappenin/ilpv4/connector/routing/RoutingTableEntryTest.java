package com.sappenin.ilpv4.connector.routing;

import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerAddress;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

/**
 * Unit tests for {@link RoutingTableEntry}.
 */
public class RoutingTableEntryTest {

  private static final InterledgerAddressPrefix GLOBAL_TARGET_PREFIX = InterledgerAddressPrefix.of("g");
  private static final InterledgerAddress CONNECTOR_ACCOUNT_BOB = InterledgerAddress.of("g.mainhub.bob");
  private static final InterledgerAddress CONNECTOR_ACCOUNT_CONNIE = InterledgerAddress.of("g.mainhub.connie");

  private static final Pattern ACCEPT_ALL_SOURCES_PATTERN = Pattern.compile("(.*?)");
  private static final Pattern ACCEPT_NO_SOURCES_PATTERN = Pattern.compile("(.*)");

  @Test
  public void testDefaultValues() {
    final RoutingTableEntry routingTableEntry1 = ImmutableRoutingTableEntry.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopAccount(CONNECTOR_ACCOUNT_CONNIE)
      .build();

    assertThat(routingTableEntry1.getTargetPrefix(), is(GLOBAL_TARGET_PREFIX));
    assertThat(routingTableEntry1.getNextHopAccount(), is(CONNECTOR_ACCOUNT_CONNIE));
    assertThat(routingTableEntry1.getExpiresAt().isPresent(), is(false));
    assertThat(routingTableEntry1.getSourcePrefixRestrictionRegex().pattern(),
      is(ACCEPT_ALL_SOURCES_PATTERN.pattern()));
  }

  @Test
  public void testEqualsHashCode() {
    final RoutingTableEntry routingTableEntry1 = ImmutableRoutingTableEntry.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopAccount(CONNECTOR_ACCOUNT_CONNIE)
      .build();

    final RoutingTableEntry routingTableEntry2 = ImmutableRoutingTableEntry.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopAccount(CONNECTOR_ACCOUNT_CONNIE)
      .build();

    final RoutingTableEntry routingTableEntry3 = ImmutableRoutingTableEntry.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopAccount(CONNECTOR_ACCOUNT_CONNIE)
      .sourcePrefixRestrictionRegex(ACCEPT_NO_SOURCES_PATTERN)
      .build();

    assertThat(routingTableEntry1, is(routingTableEntry2));
    assertThat(routingTableEntry2, is(routingTableEntry1));
    assertThat(routingTableEntry1.hashCode(), is(routingTableEntry2.hashCode()));

    assertThat(routingTableEntry1, is(routingTableEntry2));
    assertThat(routingTableEntry2, is(routingTableEntry1));
    assertThat(routingTableEntry1.hashCode(), is(routingTableEntry2.hashCode()));

    assertThat(routingTableEntry1, is(not(routingTableEntry3)));
    assertThat(routingTableEntry2, is(not(routingTableEntry3)));
    assertThat(routingTableEntry1.hashCode(), is(not(routingTableEntry3.hashCode())));

    assertThat(routingTableEntry2, is(not(routingTableEntry3)));
    assertThat(routingTableEntry2, is(not(routingTableEntry3)));
    assertThat(routingTableEntry2.hashCode(), is(not(routingTableEntry3.hashCode())));
  }

  @Test
  public void testNotEqualsHashCode() {
    final RoutingTableEntry routingTableEntry1 = ImmutableRoutingTableEntry.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopAccount(CONNECTOR_ACCOUNT_BOB)
      .build();
    final RoutingTableEntry routingTableEntry2 = ImmutableRoutingTableEntry.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopAccount(CONNECTOR_ACCOUNT_CONNIE)
      .sourcePrefixRestrictionRegex(ACCEPT_NO_SOURCES_PATTERN)
      .build();
    final RoutingTableEntry routingTableEntry3 = ImmutableRoutingTableEntry.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopAccount(CONNECTOR_ACCOUNT_CONNIE)
      .sourcePrefixRestrictionRegex(ACCEPT_NO_SOURCES_PATTERN)
      .build();

    assertThat(routingTableEntry1, is(not(routingTableEntry2)));
    assertThat(routingTableEntry2, is(not(routingTableEntry1)));
    assertThat(routingTableEntry1.hashCode(), is(not(routingTableEntry2.hashCode())));

    assertThat(routingTableEntry1, is(not(routingTableEntry3)));
    assertThat(routingTableEntry1, is(not(routingTableEntry3)));
    assertThat(routingTableEntry1.hashCode(), is(not(routingTableEntry3.hashCode())));

    assertThat(routingTableEntry2, is(routingTableEntry3));
    assertThat(routingTableEntry2, is(routingTableEntry3));
    assertThat(routingTableEntry2.hashCode(), is(routingTableEntry3.hashCode()));
  }

}