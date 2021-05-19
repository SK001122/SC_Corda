package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.ShipmentContract;
import com.example.model.AssetDetails;
import com.example.state.ShipmentState;
import com.example.utils.Constants;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@InitiatingFlow
@StartableByRPC
public class StartShipmentFlow extends FlowLogic<SignedTransaction> {

    private List<Party> supplyChainParticipants;
    private AssetDetails assetDetails;

    private static final Logger logger = LoggerFactory.getLogger(StartShipmentFlow.class);

    private final ProgressTracker progressTracker = new ProgressTracker(
            Constants.PROCESSING_TRANSACTION, Constants.SHARING_TRANSACTION, Constants.CONFIRMING_TRANSACTION);

    public StartShipmentFlow(List<Party> supplyChainParticipants, AssetDetails assetDetails) {
        this.supplyChainParticipants = supplyChainParticipants;
        this.assetDetails = assetDetails;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        logger.info("Inside StartshipmentFlow");

        TransactionBuilder transactionBuilder = new TransactionBuilder();
        transactionBuilder.setNotary(obtainNotary());

        //create new state
        if(supplyChainParticipants == null) {
            supplyChainParticipants = new ArrayList<>();
//            throw new FlowException("Supplychain Participants cannot be passed as null");
        }

        List<Party> participantsInclMe = new ArrayList<>(Arrays.asList(getOurIdentity()));
        participantsInclMe.addAll(supplyChainParticipants);
        ShipmentState shipmentState = new ShipmentState(assetDetails,participantsInclMe,getOurIdentity());

        //create new command
        Command createShipmentCommand = new Command<CommandData>(new ShipmentContract.Commands.CreateShipment(), getOurIdentity().getOwningKey());

        transactionBuilder.addOutputState(shipmentState).addCommand(createShipmentCommand);

        progressTracker.setCurrentStep(Constants.PROCESSING_TRANSACTION);
        //verifyAndSign on our node
        SignedTransaction txnSignedByMe = verifyAndSignTxn(transactionBuilder);

        progressTracker.setCurrentStep(Constants.SHARING_TRANSACTION);
        //Creating flow-sessions with all the participants to open a connection channel
        List<AbstractParty> participantsExcludingMyself = shipmentState.getParticipants()
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

    private Party obtainNotary() {
        return getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
    }

    private SignedTransaction verifyAndSignTxn(TransactionBuilder txBuilder) throws FlowException {
        logger.info("verifyAndSignRiskContractRequest");
        txBuilder.verify(getServiceHub());
        return getServiceHub().signInitialTransaction(txBuilder);
    }

    @Suspendable
    protected SignedTransaction collectRiskContractPartySignatures(SignedTransaction partSignedTx, List<Party> signatories) throws FlowException {
        Set sessionSet = new HashSet();
        List<Party> partiesMinusMe = signatories.stream().filter(it -> !it.equals(getOurIdentity())).collect(Collectors.toList());

        for (Party party : partiesMinusMe) {
            FlowSession otherPartySession = initiateFlow(party);
            sessionSet.add(otherPartySession);
        }


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
