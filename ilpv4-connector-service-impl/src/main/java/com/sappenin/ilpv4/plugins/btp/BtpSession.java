package com.sappenin.ilpv4.plugins.btp;

import org.interledger.btp.BtpErrorCode;
import org.interledger.btp.BtpRuntimeException;
import org.interledger.core.InterledgerAddress;
import org.springframework.web.socket.server.HandshakeHandler;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A session for BTP. This is required because the BTP Auth protocol does not rely on standard Basic Auth parameters in
 * a typical websocket URL, so the normal process of doing Auth during a websocket handshake (e.g., via {@link
 * HandshakeHandler} does not apply.
 */
public class BtpSession {

  public static final String CREDENTIALS_KEY = "ILP-BTP-Credentials";

  // A given BTP session has only a single counterparty.
  private final InterledgerAddress peerAccountAddress;
  private final AtomicReference<BtpSessionCredentials> btpSessionCredentials;

  /**
   * No-Args Constructor.
   *
   * @param peerAccountAddress
   */
  public BtpSession(final InterledgerAddress peerAccountAddress) {
    this.peerAccountAddress = Objects.requireNonNull(peerAccountAddress);
    this.btpSessionCredentials = new AtomicReference<>();

    //    this.webSocketCredentials = Optional.ofNullable(
    //      Objects.requireNonNull(webSocketSession).getAttributes().get(CREDENTIALS_KEY)
    //    )
    //      .map(obj -> (BtpSocketHandler.WebSocketCredentials) obj)
    //      .orElseThrow(() -> new RuntimeException("No Credentials found in WebSocket Session!"));
  }

  /**
   * Sets a valid set of {@link BtpSessionCredentials} into this session. Note that callers should not attempt this
   * method before properly validating the credentials being set.
   *
   * @param btpSessionCredentials
   */
  public void setValidAuthentication(final BtpSessionCredentials btpSessionCredentials) {
    Objects.requireNonNull(btpSessionCredentials);

    // Either this session has never been authenticated, or it has, and we only want to honor the same authentication
    // (i.e., a repeat of the same credentials).
    if (this.btpSessionCredentials.get() != null) {
      final boolean success = this.btpSessionCredentials.compareAndSet(null, btpSessionCredentials);
      if (!success) {
        new BtpRuntimeException(BtpErrorCode.F00_NotAcceptedError, String.format("BTP Session already authenticated!"));
      }
    } else {
      // The BTP Session is already authenticated, so only allow authenticating with the same credentials.
      final boolean success = this.btpSessionCredentials.compareAndSet(btpSessionCredentials, btpSessionCredentials);
      if (!success) {
        new BtpRuntimeException(BtpErrorCode.F00_NotAcceptedError, String.format("BTP Session already authenticated!"));
      }
    }
  }

  /**
   * A BTP Session is considered to be authenticated if it has a non-empty instance of BtpCredentials. This can only
   * occur if the BTP Auth sub-protocol is followed properly.
   *
   * @return {@code true} if this BtpSession is properly authenticated; {@code false} otherwise.
   */
  public boolean isAuthenticated() {
    return this.btpSessionCredentials.get() != null;
  }

  //  public WebSocketSession getWebSocketSession() {
  //    return webSocketSession;
  //  }


  public AtomicReference<BtpSessionCredentials> getBtpSessionCredentials() {
    return btpSessionCredentials;
  }

  public InterledgerAddress getPeerAccountAddress() {
    return peerAccountAddress;
  }
}
