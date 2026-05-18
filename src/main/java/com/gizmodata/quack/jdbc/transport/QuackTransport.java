package com.gizmodata.quack.jdbc.transport;

import com.gizmodata.quack.jdbc.message.QuackMessage;

/** Transport capable of sending one Quack request and returning its response. */
public interface QuackTransport {

    QuackMessage send(QuackMessage request);
}
