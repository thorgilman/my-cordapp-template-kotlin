package com.template

import com.template.flows.Initiator
import com.template.flows.Responder
import com.template.states.DataState
import net.corda.core.contracts.ContractState
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.internal.ContractJarTestUtils.makeTestJar
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test


class FlowTests {

    lateinit var mockNetwork: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode

    @Before
    fun setup() {
        mockNetwork = MockNetwork(
                listOf("com.template"),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
        )
        a = mockNetwork.createNode(MockNodeParameters())
        b = mockNetwork.createNode(MockNodeParameters())
        val startedNodes = arrayListOf(a, b)
        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach { it.registerInitiatedFlow(Responder::class.java) }
        mockNetwork.runNetwork()
    }

    @After
    fun tearDown() {

    }

    @Test
    fun `test`() {
        a.runFlow(Initiator("Data", b.identity()))

        assert(a.getStates<DataState>().size == 1)
        assert(b.getStates<DataState>().size == 1)
    }


    fun StartedMockNode.identity(): Party {
        return this.info.legalIdentities[0]
    }

    inline fun <reified T: ContractState> StartedMockNode.getStates(): List<ContractState> {
        return services.vaultService.queryBy<T>().states.map{it.state.data}
    }

    fun StartedMockNode.runFlow(flowLogic: FlowLogic<SignedTransaction>): SignedTransaction {
        val future = this.startFlow(flowLogic)
        mockNetwork.runNetwork()
        return future.getOrThrow()
    }

}