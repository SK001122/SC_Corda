package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.ShipmentContract;
import com.example.state.ShipmentState;
import com.example.utils.Constants;
import com.example.utils.SupplyChainService;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.TransactionSignature;
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
public class TransferShipmentFlow extends FlowLogic<SignedTransaction> {

    private static final Logger logger = LoggerFactory.getLogger(TransferShipmentFlow.class);

    private final ProgressTracker progressTracker = new ProgressTracker(
            Constants.PROCESSING_TRANSACTION, Constants.SHARING_TRANSACTION, Constants.CONFIRMING_TRANSACTION);

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    private final String shipmentStateLinearID;

    public TransferShipmentFlow(String shipmentStateLinearID) {
        this.shipmentStateLinearID = shipmentStateLinearID;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        logger.info("Inside Transfer Shipment flow");

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

        ShipmentState outputShipmentState = inputShipmentState.getState().getData().copy(getOurIdentity());

        Command transferShipmentCommand = new Command<CommandData>(new ShipmentContract.Commands.TransferShipment(), ImmutableList.of(getOurIdentity().getOwningKey()
                ,inputShipmentState.getState().getData().getCurrentDistributor().getOwningKey()));

        transactionBuilder.addInputState(inputShipmentState).addOutputState(outputShipmentState).addCommand(transferShipmentCommand);

        progressTracker.setCurrentStep(Constants.PROCESSING_TRANSACTION);
        //verifyAndSign on our node
        SignedTransaction txnSignedByMe = verifyAndSignRiskContractRequest(transactionBuilder);

        StateAndRef<ShipmentState> finalInputShipmentState = inputShipmentState;

        List<AbstractParty> participantsExcludingMyselfAndPrevOwner = inputShipmentState.getState().getData().getParticipants()
                .stream().filter(
                        (it ->
                                (!it.getOwningKey().equals(getOurIdentity().getOwningKey())) &&
                                        !it.getOwningKey().equals(finalInputShipmentState.getState().getData().getCurrentDistributor().getOwningKey())))
                .collect(Collectors.toList());


        Set sessionSetWithoutSignatory = new HashSet<FlowSession>();
        for (AbstractParty riskParticipant : participantsExcludingMyselfAndPrevOwner) {
            FlowSession otherPartySession = initiateFlow((Party) riskParticipant);
            otherPartySession.send(false);
            sessionSetWithoutSignatory.add(otherPartySession);
        }

        Set signatorySessionSet = new HashSet<FlowSession>();
        FlowSession signatorySession = initiateFlow(inputShipmentState.getState().getData().getCurrentDistributor());
        signatorySessionSet.add(signatorySession);

        signatorySession.send(true);


        SignedTransaction signedTransaction = collectRiskContractPartySignatures(txnSignedByMe,signatorySessionSet);

        Set allSessionSets = new HashSet<FlowSession>();
        allSessionSets.addAll(sessionSetWithoutSignatory);
        allSessionSets.addAll(signatorySessionSet);

        SignedTransaction recordedTxn = recordTransaction(signedTransaction, allSessionSets);

        TransactionSignature publicKey = recordedTxn.getSigs().get(0);

        return recordedTxn;
    }

    private Party obtainNotary() {
        return getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
    }

    private SignedTransaction verifyAndSignRiskContractRequest(TransactionBuilder txBuilder) throws FlowException {
        logger.info("verifyAndSignRiskContractRequest");
        txBuilder.verify(getServiceHub());
        return getServiceHub().signInitialTransaction(txBuilder);
    }

    @Suspendable
    protected SignedTransaction collectRiskContractPartySignatures(SignedTransaction partSignedTx, Set sessionSet) throws FlowException {

        final SignedTransaction fullyRiskContractSignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, sessionSet, CollectSignaturesFlow.Companion.tracker()));

        return fullyRiskContractSignedTx;
    }

    @Suspendable
    private SignedTransaction recordTransaction(SignedTransaction fullySignedTxn, Set<FlowSession> sessions) throws FlowException {
        logger.info("recordTransaction");
        SignedTransaction finalSignedTx = subFlow(new FinalityFlow(fullySignedTxn, sessions));
        return finalSignedTx;
    }



}
