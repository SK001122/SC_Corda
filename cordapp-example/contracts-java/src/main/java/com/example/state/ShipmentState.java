package com.example.state;

import com.example.contract.ShipmentContract;
import com.example.model.AssetDetails;
import com.example.schema.ShipmentSchemav1;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@BelongsToContract(ShipmentContract.class)
public class ShipmentState implements LinearState, QueryableState {

    private final UniqueIdentifier linearId;
    private final AssetDetails assetDetails;
    private final List<Party> supplyChainParticipants;
    private final Party currentDistributor;

    @ConstructorForDeserialization
    public ShipmentState(UniqueIdentifier linearId, AssetDetails assetDetails, List<Party> supplyChainParticipants, Party currentDistributor) {
        this.linearId = linearId;
        this.assetDetails = assetDetails;
        this.supplyChainParticipants = supplyChainParticipants;
        this.currentDistributor = currentDistributor;
    }

    public ShipmentState(AssetDetails assetDetails, List<Party> supplyChainParticipants, Party currentDistributor) {
        this.linearId = new UniqueIdentifier();
        this.assetDetails = assetDetails;
        this.supplyChainParticipants = supplyChainParticipants;
        this.currentDistributor = currentDistributor;
    }

    public ShipmentState copy(Party currentDistributor) {
        ShipmentState shipmentState = new ShipmentState(this.linearId, this.assetDetails, this.supplyChainParticipants,currentDistributor);
        return shipmentState;
    }

    public AssetDetails getAssetDetails() {
        return assetDetails;
    }

    public List<Party> getSupplyChainParticipants() {
        return supplyChainParticipants;
    }

    public Party getCurrentDistributor() {
        return currentDistributor;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        Set<AbstractParty> participantsList = new HashSet<>();
        participantsList.addAll(supplyChainParticipants);
        return new ArrayList<>(participantsList);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @NotNull
    @Override
    public PersistentState generateMappedObject(@NotNull MappedSchema schema) {
        if (schema instanceof ShipmentSchemav1) {
            return new ShipmentSchemav1.PersistentShipmentState(
                    this.linearId.toString());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @NotNull
    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return Arrays.asList(new ShipmentSchemav1());
    }
}
