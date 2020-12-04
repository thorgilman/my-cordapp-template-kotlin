package com.template.states

import com.template.contracts.DataContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

// *********
// * State *
// *********
@BelongsToContract(DataContract::class)
data class DataState(val data: String,
                     val sourceParty: Party,
                     val destParty: Party,
                     override val participants: List<AbstractParty> = listOf(sourceParty, destParty)) : ContractState
