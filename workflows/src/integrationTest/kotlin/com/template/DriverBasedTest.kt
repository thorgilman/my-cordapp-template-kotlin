package com.template

import com.template.flows.Initiator
import com.template.states.DataState
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.Permissions
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.node.User
import org.junit.Test
import java.util.concurrent.Future
import kotlin.test.assertEquals

class DriverBasedTest {
    private val aX500Name = CordaX500Name("PartyA", "", "US")
    private val bX500Name = CordaX500Name("PartyB", "", "US")

    @Test
    fun `node test`() = withDriver {

        val aProxy = setupNode(aX500Name)
        val bProxy = setupNode(bX500Name)

        aProxy.startFlow(::Initiator, "Data", bProxy.nodeInfo().legalIdentities[0]).returnValue.toCompletableFuture().getOrThrow()

        assertEquals(1, aProxy.vaultQuery(DataState::class.java).states.size)
        assertEquals(1, bProxy.vaultQuery(DataState::class.java).states.size)
    }

    private fun DriverDSL.setupNode(x500Name: CordaX500Name): CordaRPCOps {
        val username = x500Name.organisation
        val user = User(username, "password", permissions = setOf("ALL"))
        val handle = startNode(providedName=x500Name, rpcUsers= listOf(user)).getOrThrow()
        val client = CordaRPCClient(handle.rpcAddress)
        val proxy = client.start(username, "password").proxy
        return proxy
    }

    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
        DriverParameters(isDebug = true, startNodesInProcess = true)
    ) { test() }

    // Makes an RPC call to retrieve another node's name from the network map.
    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name

    // Resolves a list of futures to a list of the promised values.
    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }

    // Starts multiple nodes simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
        .map { startNode(providedName = it.name) }
        .waitForAll()
}