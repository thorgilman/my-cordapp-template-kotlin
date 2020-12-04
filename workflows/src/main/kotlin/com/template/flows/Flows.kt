package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.DataContract
import com.template.states.DataState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class Initiator(val dataString: String, val destParty: Party) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val command = Command(DataContract.Commands.Create(), listOf(ourIdentity).map { it.owningKey })
        val dataState = DataState("Data", ourIdentity, destParty)
        val stateAndContract = StateAndContract(dataState, DataContract.ID)
        val txBuilder = TransactionBuilder(notary).withItems(stateAndContract, command)

        txBuilder.verify(serviceHub);

        val tx = serviceHub.signInitialTransaction(txBuilder)
        val targetSession = initiateFlow(destParty)
        return subFlow(FinalityFlow(tx, targetSession))
    }
}

@InitiatedBy(Initiator::class)
class Responder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}
