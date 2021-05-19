package com.example.server;

import com.example.flow.DeliverShipmentFlow;
import com.example.flow.StartShipmentFlow;
import com.example.flow.TransferShipmentFlow;
import com.example.state.ShipmentState;
import com.example.vo.StartShipmentRequestVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.client.jackson.JacksonSupport;
import net.corda.core.contracts.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/api/supplychain/") // The paths for HTTP requests are relative to this base path.
public class SupplyChainController {
    private static final Logger logger = LoggerFactory.getLogger(SupplyChainController.class);
    private final CordaRPCOps proxy;
    private final CordaX500Name me;

    public SupplyChainController(NodeRPCConnection rpc) {
        this.proxy = rpc.getProxy();
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    /** Helpers for filtering the network map cache. */
    public String toDisplayString(X500Name name){
        return BCStyle.INSTANCE.toString(name);
    }

    private boolean isNotary(NodeInfo nodeInfo) {
        return !proxy.notaryIdentities()
                .stream().filter(el -> nodeInfo.isLegalIdentity(el))
                .collect(Collectors.toList()).isEmpty();
    }

    private boolean isMe(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().equals(me);
    }

    private boolean isNetworkMap(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().getOrganisation().equals("Network Map Service");
    }

    @Configuration
        class Plugin {
            @Bean
            public ObjectMapper registerModule() {
                return JacksonSupport.createNonRpcMapper();
            }
        }

        @GetMapping(value = "/peers", produces = APPLICATION_JSON_VALUE)
        public HashMap<String, List<String>> getPeers() {
            HashMap<String, List<String>> myMap = new HashMap<>();

            // Find all nodes that are not notaries, ourself, or the network map.
            Stream<NodeInfo> filteredNodes = proxy.networkMapSnapshot().stream()
                    .filter(el -> !isNotary(el) && !isMe(el) && !isNetworkMap(el));
            // Get their names as strings
            List<String> nodeNames = filteredNodes.map(el -> el.getLegalIdentities().get(0).getName().toString())
                    .collect(Collectors.toList());

            myMap.put("peers", nodeNames);
            return myMap;
    }

    @GetMapping(value = "/me",produces = APPLICATION_JSON_VALUE)
    private HashMap<String, String> whoami(){
        HashMap<String, String> myMap = new HashMap<>();
        myMap.put("me", me.toString());
        return myMap;
    }

    /**
     * Displays all shipment states.
     */
    @GetMapping(value = "/shipments",produces = APPLICATION_JSON_VALUE)
    public List<StateAndRef<ShipmentState>> getShipments() {
        return proxy.vaultQuery(ShipmentState.class).getStates();
    }


    /**
     * API method to initiate shipment
     * @param startShipmentRequest
     * @return ResponseEntity
     * @throws IllegalArgumentException
     */
    @PostMapping (value = "startShipment" , produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> startShipment(@RequestBody StartShipmentRequestVO startShipmentRequest) throws IllegalArgumentException {

        if(startShipmentRequest.getAssestDetails() == null)
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Please provide valid input");

        if(startShipmentRequest.getSupplyChainParticipants() == null)
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Please provide valid input");

        List<Party> parties = new ArrayList<>();

        startShipmentRequest.getSupplyChainParticipants().forEach(it->
                parties.add(proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(it))));

        // Create a new IOU state using the parameters given.
        try {
            // Start the IOUIssueFlow. We block and waits for the flow to return.
            SignedTransaction result = proxy.startTrackedFlowDynamic(StartShipmentFlow.class, parties, startShipmentRequest.getAssestDetails()).getReturnValue().get();
            // Return the response.
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Shipment started successfully for the id =  "+ result.getTx().outputsOfType(ShipmentState.class).get(0).getLinearId().getId().toString() +" .\n ");
            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    /**
     * API method to transfer the shipment
     * @param shipmentId
     * @return ResponseEntity
     * @throws IllegalArgumentException
     */
    @PutMapping (value = "transferShipment/{shipmentId}" , produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> transferShipment(@PathVariable("shipmentId") String shipmentId) throws IllegalArgumentException {

        if(shipmentId == null)
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Please provide valid input");

        try {
            SignedTransaction result = proxy.startTrackedFlowDynamic(TransferShipmentFlow.class, shipmentId).getReturnValue().get();
            // Return the response.
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Shipment "+ result.getTx().outputsOfType(ShipmentState.class).get(0).getLinearId().getId().toString() +" transferred successfully.\n ");
            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    /**
     * API method to deliver the shipment
     * @param shipmentId
     * @return ResponseEntity
     * @throws IllegalArgumentException
     */
    @PutMapping (value = "deliverShipment/{shipmentId}" , produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> deliverShipment(@PathVariable("shipmentId") String shipmentId) throws IllegalArgumentException {

        if(shipmentId== null)
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Please provide valid input");

        try {
            SignedTransaction result = proxy.startTrackedFlowDynamic(DeliverShipmentFlow.class, shipmentId).getReturnValue().get();
            // Return the response.
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Shipment delivered successfully.");
            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }


}