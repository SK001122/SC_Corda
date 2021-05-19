package com.example.flow.responders;

import co.paralleluniverse.fibers.Suspendable;
import com.example.flow.StartShipmentFlow;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

@InitiatedBy(StartShipmentFlow.class)
public class StartShipmentResponderFlow extends FlowLogic<SignedTransaction> {

    private final FlowSession otherPartySession;

    public StartShipmentResponderFlow(FlowSession otherPartySession) {
        this.otherPartySession = otherPartySession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        return subFlow(new ReceiveFinalityFlow(otherPartySession));
    }
}
