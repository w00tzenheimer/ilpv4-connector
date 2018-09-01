package com.sappenin.ilpv4;

import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

public interface IlpConnector {

  ConnectorSettings getConnectorSettings();

  /**
   * Handle an incoming prepare-packet by either fulfilling it (if local) or by forwarding it to a remote peer.
   *
   * Repeat calls to this method using the same transfer information must be idempotent.
   *
   * @param sourceAccountAddress     The {@link InterledgerAddress} for the account that sent this incoming ILP packet.
   * @param interledgerPreparePacket An {@link InterledgerPreparePacket} containing data about an ILP payment.
   *
   * @throws InterledgerProtocolException If the response from the remote peer is a rejection.
   */
  CompletableFuture<InterledgerFulfillPacket> handleIncomingData(
    InterledgerAddress sourceAccountAddress, InterledgerPreparePacket interledgerPreparePacket
  ) throws InterledgerProtocolException;

  /**
   * Handle an incoming request to transfer {@code amount} units of an asset from the callers account to this
   * connector.
   */
  CompletableFuture<Void> handleIncomingMoney(BigInteger amount);


}