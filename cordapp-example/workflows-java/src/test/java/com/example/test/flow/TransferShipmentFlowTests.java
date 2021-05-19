package com.example.test.flow;

import com.example.contract.ShipmentContract;
import com.example.flow.TransferShipmentFlow;
import com.example.flow.responders.TransferShipmentResponderFlow;
import com.example.model.AssetDetails;
import com.example.state.ShipmentState;
import com.google.common.collect.ImmutableList;
import kotlin.jvm.functions.Function0;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.testing.node.StartedMockNode;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TransferShipmentFlowTests extends AbstractFlowTests{

    public TransferShipmentFlowTests() {
        super(4);
    }

    @Override
    public void registerFlows() {
        for (StartedMockNode node : participantsNodes) {
            node.registerInitiatedFlow(TransferShipmentResponderFlow.class);
        }
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        SignedTransaction signedTx = modelTransferShipmentTxn();
        signedTx.verifyRequiredSignatures();
    }

    public SignedTransaction modelTransferShipmentTxn() throws ExecutionException, InterruptedException {

        //Making DummyData
        AssetDetails assetDetails = new AssetDetails();
        List<Party> supplyChainParticipants = new ArrayList<>();

        //Adding all nodes as parties excluding ourselves
        //TODO: Add a check for duplication of party, validation of nodes
        for(StartedMockNode node : participantsNodes){
            if(node != participantsNodes.get(0)){
                supplyChainParticipants.add(node.getInfo().getLegalIdentities().get(0));
            }
        }

        ShipmentState shipmentState = new ShipmentState(assetDetails,supplyChainParticipants,participantsNodes.get(0).getInfo().getLegalIdentities().get(0));

        final Command command = new Command(new ShipmentContract.Commands.CreateShipment(), ImmutableList.of(participantsNodes.get(0).getInfo().getLegalIdentities().get(0).getOwningKey()));

        final TransactionBuilder transactionBuilder = new TransactionBuilder(mockNetwork.getDefaultNotaryIdentity()).
                withItems(new StateAndContract(shipmentState, ShipmentContract.ID), command);

        participantsNodes.get(0).transaction(new Function0<Boolean>() {

            @Override
            public Boolean invoke() {
                try {
                    transactionBuilder.verify(participantsNodes.get(0).getServices());
                    return true ;
                } catch (AttachmentResolutionException e) {
                    e.printStackTrace();
                    return false ;

                } catch (TransactionResolutionException e) {
                    e.printStackTrace();
                    return false ;

                } catch (TransactionVerificationException e) {
                    e.printStackTrace();
                    return false ;

                }
            }
        });

        SignedTransaction signedDummytxn = participantsNodes.get(0).getServices().signInitialTransaction(transactionBuilder);
        participantsNodes.get(0).getServices().recordTransactions(signedDummytxn);
        participantsNodes.get(1).getServices().recordTransactions(signedDummytxn);
        participantsNodes.get(2).getServices().recordTransactions(signedDummytxn);
        participantsNodes.get(3).getServices().recordTransactions(signedDummytxn);



        TransferShipmentFlow transferShipmentFlow = new TransferShipmentFlow(shipmentState.getLinearId().toString());
        CordaFuture<SignedTransaction> future = participantsNodes.get(1).startFlow(transferShipmentFlow);
        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        return  signedTx ;
    }
}
