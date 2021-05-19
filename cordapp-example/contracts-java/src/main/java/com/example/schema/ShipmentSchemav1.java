package com.example.schema;

import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.Collections;


public class ShipmentSchemav1 extends MappedSchema {

    public ShipmentSchemav1()
    {
        //TODO: Use Immutable List in place of list wherever it has been used in contracts and states package.
        super(ShipmentSchema.class, 1, Collections.singletonList(PersistentShipmentState.class));
    }

    @Nullable
    @Override
    public String getMigrationResource() {
        return "shipment.changelog-master";
    }

    @Entity
    @Table(name = "ShipmentState")
    public static class PersistentShipmentState extends PersistentState {

        @Column(name = "linearId")
        private final String linearId;

        public String getLinearId() {
            return linearId;
        }


        public PersistentShipmentState() {
            this.linearId = null;
        }

        public PersistentShipmentState(String linearId) {
            this.linearId = linearId;

        }
    }
}
