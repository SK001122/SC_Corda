package com.example.utils;

import com.example.state.ShipmentState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.serialization.SingletonSerializeAsToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static net.corda.core.node.services.vault.QueryCriteriaUtils.DEFAULT_PAGE_NUM;

@CordaService
public class SupplyChainService extends SingletonSerializeAsToken {

    private ServiceHub serviceHub;
    private static final int PAGE_SIZE = 10000;
    static private final Logger logger = LoggerFactory.getLogger(SupplyChainService.class);


    public SupplyChainService(ServiceHub serviceHub) {
        this.serviceHub = serviceHub;
    }

    public StateAndRef<ShipmentState> getShipmentStateFromLinearID(String shipmentStateLinearID) throws NoSuchFieldException {

        //QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
        logger.info("getShipmentStateFromLinearID() for Linear ID : {}",shipmentStateLinearID );
        if (shipmentStateLinearID == null) {
            throw new IllegalArgumentException("ShipmentState Linear id cannot be null");
        } else {
            List<UUID> shipmentLinearIDs = Collections.singletonList(UUID.fromString(shipmentStateLinearID));
            logger.info("Shipment Linear Id passed {}" , shipmentLinearIDs);

            QueryCriteria linearCriteriaAll = new QueryCriteria.LinearStateQueryCriteria(null, shipmentLinearIDs);
            Vault.Page<ShipmentState> shipmentStatePage = serviceHub.getVaultService().queryBy(ShipmentState.class, linearCriteriaAll, new PageSpecification(DEFAULT_PAGE_NUM, PAGE_SIZE));

            if (shipmentStatePage.getStates().size() == 0) {
                throw new IllegalArgumentException("No ShipmentState found related to ShipmentState linear ID passed= " + shipmentStateLinearID);
            }

            StateAndRef<ShipmentState> shipmentStateAndRef = shipmentStatePage.getStates().get(0);

            return shipmentStateAndRef;
        }

    }
}
