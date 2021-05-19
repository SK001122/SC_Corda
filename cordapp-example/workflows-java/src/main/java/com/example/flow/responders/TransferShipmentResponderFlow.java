package com.example.flow.responders;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.ShipmentContract;
import com.example.flow.TransferShipmentFlow;
import com.example.state.ShipmentState;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;


@InitiatedBy(TransferShipmentFlow.class)
public class TransferShipmentResponderFlow extends FlowLogic<SignedTransaction> {

    private final FlowSession otherPartyFlow;

    static private final Logger logger = LoggerFactory.getLogger(TransferShipmentResponderFlow.class);

    public TransferShipmentResponderFlow(FlowSession otherPartyFlow) {
        this.otherPartyFlow = otherPartyFlow;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        Boolean isSignatory = otherPartyFlow.receive(Boolean.class).unwrap(it -> it);

        class signTxFlow extends SignTransactionFlow {
            private signTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                super(otherPartyFlow, progressTracker);
                logger.info("Calling super constructor");
            }

            @Override
            protected void checkTransaction(SignedTransaction stx) {

                logger.info("Before verification of contract in the Responder of TransferShipmentFlow");

                List<Command<?>> commands = stx.getTx().getCommands();
                List<StateRef> inputStates = stx.getInputs();
                List<ContractState> outputStates = stx.getTx().getOutputStates();
                requireThat(require -> {
                    require.using("Commands should exactly be equal to one.", commands.size() == 1);

                    CommandData command = commands.get(0).getValue();

                    logger.info("Command fetched= "+command.toString());

                    require.using("Commands should be of type TransferShipment.", command instanceof ShipmentContract.Commands.TransferShipment);


                    require.using("Total number of Input States should exactly be equal to One", inputStates.size() == 1);

                    //TODO: How to fetch specifically for inputOfType() as is possible on outputsOfType()?
                    ShipmentState inputShipmentState = null;
                    try {
                        inputShipmentState  = (ShipmentState) getServiceHub().loadState(stx.getTx().getInputs().get(0)).getData();
                    } catch (TransactionResolutionException e) {
                        e.printStackTrace();
                    }

                    logger.info("Input State fetched in responder Flow= "+inputShipmentState.toString());

                    //TODO: remove null check if instanceOf operator takes care of it by itselves.
                    require.using("Input State should be of type ShipmentState", inputShipmentState != null);

                    require.using("Total number of Output States should exactly be equal to One", outputStates.size() == 1);

                    List<ShipmentState> outputStatesOfShipmentStateType = stx.getTx().outputsOfType(ShipmentState.class);
                    ShipmentState outputShipmentState = outputStatesOfShipmentStateType.size() == 1 ? outputStatesOfShipmentStateType.get(0) : null;

                    require.using("Output State should be of type ShipmentState", outputShipmentState != null);

                    int currentDistributorIndex = inputShipmentState.getSupplyChainParticipants().indexOf(inputShipmentState.getCurrentDistributor());
                    Party expectedNextDistributor = outputShipmentState.getSupplyChainParticipants().get(currentDistributorIndex + 1);

                    require.using("New ownership of the shipment should be with "+expectedNextDistributor.getName().getOrganisation()
                            , outputShipmentState.getCurrentDistributor().getOwningKey().toString().equalsIgnoreCase(expectedNextDistributor.getOwningKey().toString()));

                    return null;
                });

                logger.info("Post verification of contract in the Responder of TransferShipmentFlow");
            }
        }

        logger.info("The boolean ifSignatory is recieved as= "+isSignatory);

        try {
            if(isSignatory){
                SignedTransaction signedTransaction = subFlow(new signTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
                logger.info("Signing Transfer Shipment Txn from Party= "+getOurIdentity().getName().getOrganisation());
                return subFlow(new ReceiveFinalityFlow(otherPartyFlow,signedTransaction.getId()));}
            if(!isSignatory){
                logger.info("Just recording the txn without signing it from Party= "+getOurIdentity().getName().getOrganisation());
                return subFlow(new ReceiveFinalityFlow(otherPartyFlow));}

            else {
                throw new IllegalArgumentException("Boolean of whether is a Signatory or not is not passed. Please specify that.");
            }
        }  catch (FlowException e) {
            try {
                throw new FlowException("Failed due to " + e.getMessage());
            } catch (FlowException e1) {
                e1.printStackTrace();
                throw new FlowException("Failed due to " + e1.getMessage());

            }
//            e.printStackTrace();
//            return null;
        }

    }
}
