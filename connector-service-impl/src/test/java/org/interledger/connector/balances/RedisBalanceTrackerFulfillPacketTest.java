package org.interledger.connector.balances;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.util.Collection;
import java.util.UUID;

/**
 * Unit tests for {@link RedisBalanceTracker} that validates the script and balance-change functionality for handling
 * Fulfill packets.
 */
@RunWith(Parameterized.class)
@ContextConfiguration(classes = {AbstractRedisBalanceTrackerTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RedisBalanceTrackerFulfillPacketTest extends AbstractRedisBalanceTrackerTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Autowired
  private RedisBalanceTracker balanceTracker;

  @Autowired
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private AccountSettings accountSettingsMock;
  @Mock
  private AccountBalance accountBalanceMock;

  /**
   * Required-args Constructor.
   */
  public RedisBalanceTrackerFulfillPacketTest(
    final long existingAccountBalance,
    final long existingPrepaidBalance,
    final long prepareAmount,
    final long expectedBalanceInRedis,
    final long expectedPrepaidAmountInRedis
  ) {
    super(
      existingAccountBalance, existingPrepaidBalance, prepareAmount, expectedBalanceInRedis,
      expectedPrepaidAmountInRedis
    );
  }

  @Parameterized.Parameters
  public static Collection<Object[]> errorCodes() {
    return ImmutableList.of(
      // existing_clearing_balance, existing_prepaid_amount,
      // settle_threshold, settle_to,
      // prepare_amount,
      // expected_balance, expected_prepaid_amount

      // clearingBalance = 0, prepaid_amount = 0
      new Object[]{ZERO, ZERO, PREPARE_ONE, ONE, ZERO},
      // clearingBalance = 0, prepaid_amount > 0
      new Object[]{ZERO, ONE, PREPARE_ONE, ONE, ONE},
      // clearingBalance = 0, prepaid_amount < 0
      new Object[]{ZERO, NEGATIVE_ONE, PREPARE_ONE, ONE, NEGATIVE_ONE},

      // clearingBalance > 0, prepaid_amount = 0
      new Object[]{ONE, ZERO, PREPARE_ONE, TWO, ZERO},
      // clearingBalance > 0, prepaid_amount > 0
      new Object[]{ONE, ONE, PREPARE_ONE, TWO, ONE},
      // clearingBalance > 0, prepaid_amount < 0
      new Object[]{ONE, NEGATIVE_ONE, PREPARE_ONE, TWO, NEGATIVE_ONE},

      // clearingBalance < 0, prepaid_amount = 0
      new Object[]{NEGATIVE_ONE, ZERO, PREPARE_ONE, ZERO, ZERO},
      // clearingBalance < 0, prepaid_amount > 0
      new Object[]{NEGATIVE_ONE, ONE, PREPARE_ONE, ZERO, ONE},
      // clearingBalance < 0, prepaid_amount < 0
      new Object[]{NEGATIVE_ONE, NEGATIVE_ONE, PREPARE_ONE, ZERO, NEGATIVE_ONE},

      // Prepaid amt > from_amt
      new Object[]{NEGATIVE_ONE, TEN, PREPARE_ONE, ZERO, TEN},
      // Prepaid_amt < from_amt, but > 0
      new Object[]{TEN, ONE, PREPARE_TEN, 20L, ONE},

      // prepare packet 0 amount (no-op)
      new Object[]{TEN, ONE, PREPARE_ZERO, TEN, ONE}
    );
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    AccountBalanceSettings balanceSettingsMock = mock(AccountBalanceSettings.class);
    when(accountSettingsMock.balanceSettings()).thenReturn(balanceSettingsMock);
    when(accountSettingsMock.accountId()).thenReturn(ACCOUNT_ID);
    when(accountSettingsMock.assetScale()).thenReturn(2); // Hard-coded since this is not being tested in this class.

    when(accountBalanceMock.clearingBalance()).thenReturn(existingClearingBalance);
    when(accountBalanceMock.prepaidAmount()).thenReturn(existingPrepaidBalance);
  }

  @Override
  protected RedisTemplate getRedisTemplate() {
    return this.redisTemplate;
  }

  /////////////////
  // Fulfill Script (Null Checks)
  /////////////////

  @Test(expected = NullPointerException.class)
  public void updateBalanceForFulfillWithNullAccountId() {
    try {
      balanceTracker.updateBalanceForFulfill(null, ONE);
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("destinationAccountSettings must not be null");
      throw e;
    }
  }

  /////////////////
  // Fulfill Script (No Account in Redis)
  /////////////////

  /**
   * Verify the correct operation when no account exists in Redis.
   */
  @Test
  public void updateBalanceForFulfillWhenNoAccountInRedis() {
    final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
    when(accountSettingsMock.accountId()).thenReturn(accountId);
    balanceTracker.updateBalanceForFulfill(accountSettingsMock, ONE);

    final AccountBalance loadedBalance = balanceTracker.balance(accountId);
    assertThat(loadedBalance.clearingBalance()).isEqualTo(ONE);
    assertThat(loadedBalance.prepaidAmount()).isEqualTo(ZERO);
    assertThat(loadedBalance.netBalance().longValue()).isEqualTo(ONE);
  }

  @Test
  public void updateBalanceForFulfillWithParamterizedValues() {
    this.initializeAccount(ACCOUNT_ID, this.existingClearingBalance, this.existingPrepaidBalance);

    balanceTracker.updateBalanceForFulfill(accountSettingsMock, this.prepareAmount);

    final AccountBalance loadedBalance = balanceTracker.balance(ACCOUNT_ID);
    assertThat(loadedBalance.clearingBalance()).isEqualTo(expectedClearingBalanceInRedis);
    assertThat(loadedBalance.prepaidAmount()).isEqualTo(expectedPrepaidAmountInRedis);
    assertThat(loadedBalance.netBalance().longValue()).isEqualTo(expectedClearingBalanceInRedis + expectedPrepaidAmountInRedis);
  }

  @Test
  public void fulfillmountCantBeNegative() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("amount `-1` cannot be negative!");
    balanceTracker.updateBalanceForFulfill(accountSettingsMock, -1);
  }

}
