package com.example.test.flow;

import com.google.common.collect.ImmutableList;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.NetworkParameters;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractFlowTests {

    public AbstractFlowTests(Integer numberOfIdentities) {
        this.numberOfIdentities = numberOfIdentities;
    }

    public  final Integer numberOfIdentities ;
    public static final CordaX500Name notaryName = CordaX500Name.parse("O=Notary,L=London,C=GB");
    public static final CordaX500Name bnoName = CordaX500Name.parse("O=BNO-CT,L=New York,C=US");

    public MockNetwork mockNetwork ;
    public List<StartedMockNode> participantsNodes ;

    public MockNetwork getMockNetwork() {
        return mockNetwork;
    }

    @Before
    public void setup() {
        List<MockNetworkNotarySpec> notarySpecs = ImmutableList.of(new MockNetworkNotarySpec(notaryName));

        mockNetwork = new MockNetwork(new MockNetworkParameters().withNotarySpecs(notarySpecs)
                .withNetworkParameters(new NetworkParameters(4, Collections.emptyList(),10485760,524288000, Instant.now(),1, Collections.emptyMap()))
        .withCordappsForAllNodes(ImmutableList.of(TestCordapp.findCordapp("com.example.contract"),
                TestCordapp.findCordapp("com.example.flow"))));

        participantsNodes = createNNode(numberOfIdentities);

        registerFlows();

        mockNetwork.runNetwork();

    }

    private List<StartedMockNode> createNNode(Integer numberOfIdentities) {
        List<StartedMockNode> participantsNodes = new ArrayList<>();
        for(int i = 1 ; i <= numberOfIdentities ;i++){
            StartedMockNode mockNode = createNode(CordaX500Name.parse("O=Participant"+i+",L=London,C=GB"));
            participantsNodes.add(mockNode);
        }
        return  participantsNodes ;
    }


    private StartedMockNode createNode(CordaX500Name name) {
        return mockNetwork.createPartyNode(name) ;
    }

    public abstract void registerFlows();

    @After
    public void tearDown() {
        mockNetwork.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();


    public Party identity(StartedMockNode node ) {
        return node.getInfo().getLegalIdentities().get(0);
    }
}
