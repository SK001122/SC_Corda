package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.ShipmentContract;
import com.example.state.ShipmentState;
import com.example.utils.Constants;
import com.example.utils.SupplyChainService;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@StartableByRPC
@InitiatingFlow
public class DeliverShipmentFlow extends FlowLogic<SignedTransaction> {

    private static final Logger logger = LoggerFactory.getLogger(DeliverShipmentFlow.class);

    private final ProgressTracker progressTracker = new ProgressTracker(
            Constants.PROCESSING_TRANSACTION, Constants.SHARING_TRANSACTION, Constants.CONFIRMING_TRANSACTION);

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    private final String shipmentStateLinearID;

    public DeliverShipmentFlow(String shipmentStateLinearID) {
        this.shipmentStateLinearID = shipmentStateLinearID;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        logger.info("Inside Deliver Shipment flow");

        SupplyChainService supplyChainService = getServiceHub().cordaService(SupplyChainService.class);

        TransactionBuilder transactionBuilder = new TransactionBuilder();
        transactionBuilder.setNotary(obtainNotary());
        StateAndRef<ShipmentState> inputShipmentState = null;

        try {
            inputShipmentState   = supplyChainService.getShipmentStateFromLinearID(shipmentStateLinearID);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        if(inputShipmentState == null){
            throw new FlowException("Unable to fetch ShipmentState lined to ID= "+shipmentStateLinearID);
        }

        Command deliverShipmentCommand = new Command<CommandData>(new ShipmentContract.Commands.DeliverShipment(), getOurIdentity().getOwningKey());

        transactionBuilder.addInputState(inputShipmentState).addCommand(deliverShipmentCommand);

        progressTracker.setCurrentStep(Constants.PROCESSING_TRANSACTION);
        //verifyAndSign on our node
        SignedTransaction txnSignedByMe = verifyAndSignTxn(transactionBuilder);

        progressTracker.setCurrentStep(Constants.SHARING_TRANSACTION);
        //Creating flow-sessions with all the participants to open a connection channel
        List<AbstractParty> participantsExcludingMyself = inputShipmentState.getState().getData().getParticipants()
                .stream().filter(it -> !it.getOwningKey().equals(getOurIdentity().getOwningKey())).collect(Collectors.toList());

        Set sessionSet = new HashSet<FlowSession>();
        for (AbstractParty riskParticipant : participantsExcludingMyself) {
            FlowSession otherPartySession = initiateFlow((Party) riskParticipant);
            sessionSet.add(otherPartySession);
        }

        //Broadcast transaction to all nodes, so that they can store it in their node
        SignedTransaction finalRecordedTxn = recordTransaction(txnSignedByMe,sessionSet);
        progressTracker.setCurrentStep(Constants.CONFIRMING_TRANSACTION);

        return finalRecordedTxn;

    }

    private SignedTransaction verifyAndSignTxn(TransactionBuilder txBuilder) throws FlowException {
        logger.info("verifyAndSignRiskContractRequest");
        txBuilder.verify(getServiceHub());
        return getServiceHub().signInitialTransaction(txBuilder);
    }

    @Suspendable
    private SignedTransaction recordTransaction(SignedTransaction fullySignedTxn, Set<FlowSession> sessions) throws FlowException {
        logger.info("recordTransaction");
        SignedTransaction finalSignedTx = subFlow(new FinalityFlow(fullySignedTxn, sessions));
        return finalSignedTx;
    }

    private Party obtainNotary() {
        return getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
    }
}
