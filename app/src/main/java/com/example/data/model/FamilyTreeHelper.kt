package com.example.data.model

object FamilyTreeHelper {

    /**
     * Traces the lineage path between a given target person (e.g., recorded owner)
     * and the User, or traces ancestor down to User.
     * Returns a list of strings representing the steps:
     * e.g., ["Late Baikuntha Nath Nayak (Great-grandfather)", "Late Bhabani Charan Nayak (Grandfather)", "Subash Chandra Nayak (Father)", "Ramesh Chandra Nayak (User)"]
     */
    fun traceLineageToUser(
        personId: Long?,
        allMembers: List<FamilyMemberEntity>
    ): List<String> {
        if (allMembers.isEmpty()) return emptyList()

        val memberMap = allMembers.associateBy { it.id }
        val userMember = allMembers.firstOrNull { it.isUser || it.relationshipToUser.equals("Self", ignoreCase = true) }
            ?: allMembers.firstOrNull { it.generationLevel == 4 }
            ?: allMembers.lastOrNull()

        if (personId == null) {
            // Default ancestor down to user chain
            if (userMember == null) return emptyList()
            return buildChainToUser(userMember, memberMap)
        }

        val startPerson = memberMap[personId] ?: return emptyList()
        if (userMember == null || startPerson.id == userMember.id) {
            return listOf("${startPerson.fullName} (${startPerson.relationshipToUser.ifEmpty { "Self / User" }})")
        }

        // Check if startPerson is ancestor of userMember
        val chain = buildChainFromAncestorToDescendant(startPerson, userMember, memberMap)
        if (chain.isNotEmpty()) return chain

        // Check if userMember is ancestor of startPerson
        val reverseChain = buildChainFromAncestorToDescendant(userMember, startPerson, memberMap)
        if (reverseChain.isNotEmpty()) return reverseChain

        // Fallback: list start person with their relationship to user
        return listOf(
            "${startPerson.fullName} (${startPerson.relationshipToUser})",
            "${userMember.fullName} (User)"
        )
    }

    private fun buildChainToUser(user: FamilyMemberEntity, memberMap: Map<Long, FamilyMemberEntity>): List<String> {
        val ancestors = mutableListOf<FamilyMemberEntity>()
        var curr: FamilyMemberEntity? = user
        while (curr != null) {
            ancestors.add(0, curr)
            curr = curr.fatherId?.let { memberMap[it] } ?: curr.motherId?.let { memberMap[it] }
        }
        return ancestors.map { formatPersonWithRole(it) }
    }

    private fun buildChainFromAncestorToDescendant(
        ancestor: FamilyMemberEntity,
        descendant: FamilyMemberEntity,
        memberMap: Map<Long, FamilyMemberEntity>
    ): List<String> {
        val path = mutableListOf<FamilyMemberEntity>()
        fun dfs(curr: FamilyMemberEntity): Boolean {
            path.add(curr)
            if (curr.id == ancestor.id) return true
            val father = curr.fatherId?.let { memberMap[it] }
            if (father != null && dfs(father)) return true
            val mother = curr.motherId?.let { memberMap[it] }
            if (mother != null && dfs(mother)) return true
            path.removeAt(path.size - 1)
            return false
        }

        if (dfs(descendant)) {
            // path is from descendant up to ancestor, so reverse it
            return path.reversed().map { formatPersonWithRole(it) }
        }
        return emptyList()
    }

    private fun formatPersonWithRole(p: FamilyMemberEntity): String {
        val role = when {
            p.isUser -> "User"
            p.relationshipToUser.isNotBlank() -> p.relationshipToUser
            p.generationLevel == 1 -> "Great-grandfather"
            p.generationLevel == 2 -> "Grandfather"
            p.generationLevel == 3 -> "Father"
            p.generationLevel == 4 -> "User"
            else -> "Family Heir"
        }
        return "${p.fullName} ($role)"
    }

    /**
     * Converts a list of chain steps into formatted text with down arrows '↓'
     */
    fun formatLineageWithArrows(steps: List<String>): String {
        if (steps.isEmpty()) return ""
        return steps.joinToString(separator = "\n↓\n")
    }
}
