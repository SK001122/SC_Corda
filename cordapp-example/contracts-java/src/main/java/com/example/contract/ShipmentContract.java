package com.example.contract;

import com.example.state.ShipmentState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class ShipmentContract implements Contract {

    public static final String ID = "com.example.contract.ShipmentContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        List<CommandWithParties<CommandData>> command = tx.getCommands();
        for (CommandWithParties<CommandData> c : command) {

            if (c.getValue() instanceof Commands.CreateShipment) {
                requireThat(require -> {

                    require.using("Zero input states should be consumed ", tx.getInputStates().size() == 0);
                    require.using("Only one output state of type Shipment State should be created ", ((tx.getOutputStates().size() == 1) &&
                            tx.getOutputStates().stream().filter(it-> it instanceof ShipmentState).collect(Collectors.toList()).size() == 1));

                    ShipmentState outputShipmentState = tx.outputsOfType(ShipmentState.class).get(0);

          List<PublicKey> signers = c.getSigners();

                    require.using("Current distributor must be the signer.",
                           signers.contains(outputShipmentState.getCurrentDistributor().getOwningKey()));

                        return null;
                });
            }

            if (c.getValue() instanceof Commands.TransferShipment) {
                requireThat(require -> {

                    require.using("Only one input state of type Shipment State should be consumed", ((tx.getInputStates().size() == 1) &&
                            tx.inputsOfType(ShipmentState.class).size() == 1));

                    require.using("Only one output state of type Shipment State should be created ", ((tx.getOutputStates().size() == 1) &&
                            tx.outputsOfType(ShipmentState.class).size() == 1));

                    ShipmentState outputShipmentState = tx.outputsOfType(ShipmentState.class).get(0);
                    ShipmentState inputShipmentState = tx.inputsOfType(ShipmentState.class).get(0);


                    List<PublicKey> signers = c.getSigners();

                    require.using("Both previous distributor and the current distributor must be the signers.",
                            signers.containsAll(new HashSet<>(Arrays.asList(outputShipmentState.getCurrentDistributor().getOwningKey(), inputShipmentState.getCurrentDistributor().getOwningKey())) ));

                    return null;
                });

            }

            if (c.getValue() instanceof Commands.DeliverShipment) {
                requireThat(require -> {

                    require.using("Exactly one input state of type Shipment State should be consumed", ((tx.getInputStates().size() == 1) &&
                            tx.inputsOfType(ShipmentState.class).size() == 1));
                    require.using("Zero output states should be created ", tx.getOutputStates().size() == 0);

                    ShipmentState inputShipmentState = tx.inputsOfType(ShipmentState.class).get(0);

                    List<PublicKey> signers = c.getSigners();

                    require.using("Final distributor must be the signer.",
                            signers.contains(inputShipmentState.getCurrentDistributor().getOwningKey()));

                    return null;
                });
            }

        }
    }

    public interface Commands extends CommandData {
        class CreateShipment implements ShipmentContract.Commands {
        }
        class TransferShipment implements ShipmentContract.Commands {
        }
        class DeliverShipment implements ShipmentContract.Commands {
        }
        class DummyCommand implements ShipmentContract.Commands {
        }
    }
}


