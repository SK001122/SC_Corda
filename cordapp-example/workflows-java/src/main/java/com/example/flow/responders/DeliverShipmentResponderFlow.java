package com.example.flow.responders;

import co.paralleluniverse.fibers.Suspendable;
import com.example.flow.DeliverShipmentFlow;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

@InitiatedBy(DeliverShipmentFlow.class)
public class DeliverShipmentResponderFlow extends FlowLogic<SignedTransaction> {

    private final FlowSession otherPartySession;

    public DeliverShipmentResponderFlow(FlowSession otherPartySession) {
        this.otherPartySession = otherPartySession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        return subFlow(new ReceiveFinalityFlow(otherPartySession));
    }
}
