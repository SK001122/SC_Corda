package com.example.test.flow;

import com.example.flow.StartShipmentFlow;
import com.example.flow.responders.StartShipmentResponderFlow;
import com.example.model.AssetDetails;
import com.example.state.ShipmentState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.StartedMockNode;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class StartShipmentFlowTests extends AbstractFlowTests{
    public StartShipmentFlowTests() {
        super(4);
    }

    @Override
    public void registerFlows() {
        //TODO: Make sure only manufacturer node is able to run startshipmentflow
        for (StartedMockNode node : participantsNodes) {
            node.registerInitiatedFlow(StartShipmentResponderFlow.class);
        }
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        SignedTransaction signedTx = modelStartShipmentTxn();
        signedTx.verifyRequiredSignatures();
    }

    @Test
    public void flowRecordsATransactionInAllPartiesTransactionStorages() throws Exception {
        SignedTransaction signedTx = modelStartShipmentTxn();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : participantsNodes) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void recordedTransactionHasNoInputsAndASingleOutputShipmentState() throws Exception {
        SignedTransaction signedTx = modelStartShipmentTxn();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : participantsNodes) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(signedTx.getId());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assert (txOutputs.size() == 1);

            ShipmentState recordedState = (ShipmentState) txOutputs.get(0).getData();
            assertEquals(recordedState.getCurrentDistributor(), participantsNodes.get(0).getInfo().getLegalIdentities().get(0));
        }
    }

    @Test
    public void flowRecordsTheShipmentStateInAllPartiesVaults() throws Exception {
        SignedTransaction signedTx = modelStartShipmentTxn();

        // We check the recorded Shipment State in both vaults.
        for (StartedMockNode node : participantsNodes) {
            node.transaction(() -> {
                List<StateAndRef<ShipmentState>> shipmentStates = node.getServices().getVaultService().queryBy(ShipmentState.class).getStates();
                assertEquals(1, shipmentStates.size());
                ShipmentState recordedState = shipmentStates.get(0).getState().getData();
                assertEquals(recordedState.getCurrentDistributor(), participantsNodes.get(0).getInfo().getLegalIdentities().get(0));
                return null;
            });
        }
    }

    public SignedTransaction modelStartShipmentTxn () throws ExecutionException, InterruptedException {

        //Making DummyData
        AssetDetails assetDetails = new AssetDetails();
        List<Party> supplyChainParticipants = new ArrayList<>();

        //Adding all nodes as parties exclusign ourselves
        //TODO: Add a check for duplication of party, validation of nodes
        for(StartedMockNode node : participantsNodes){
            if(node != participantsNodes.get(0)){
                supplyChainParticipants.add(node.getInfo().getLegalIdentities().get(0));
            }
        }

        StartShipmentFlow startShipmentFlow = new StartShipmentFlow(supplyChainParticipants,assetDetails);
        CordaFuture<SignedTransaction> future = participantsNodes.get(0).startFlow(startShipmentFlow);
        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        return  signedTx ;
    }

}
