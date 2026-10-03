package com.comp90018.app.features.friends

import com.comp90018.app.data.social.FriendSummary

/**
 * People bundled with the app so a new explorer can try direct messaging before
 * they have built up their own Firebase friend network.
 */
internal val StarterContacts = listOf(
    FriendSummary("starter_contact_01", "amelia_chen", displayName = "Amelia Chen"),
    FriendSummary("starter_contact_02", "ben_carter", displayName = "Ben Carter"),
    FriendSummary("starter_contact_03", "chloe_nguyen", displayName = "Chloe Nguyen"),
    FriendSummary("starter_contact_04", "daniel_kim", displayName = "Daniel Kim"),
    FriendSummary("starter_contact_05", "elena_rossi", displayName = "Elena Rossi"),
    FriendSummary("starter_contact_06", "felix_martin", displayName = "Felix Martin"),
    FriendSummary("starter_contact_07", "grace_wong", displayName = "Grace Wong"),
    FriendSummary("starter_contact_08", "henry_clarke", displayName = "Henry Clarke"),
    FriendSummary("starter_contact_09", "isla_patel", displayName = "Isla Patel"),
    FriendSummary("starter_contact_10", "jack_wilson", displayName = "Jack Wilson"),
    FriendSummary("starter_contact_11", "kira_tan", displayName = "Kira Tan"),
    FriendSummary("starter_contact_12", "leo_anderson", displayName = "Leo Anderson"),
    FriendSummary("starter_contact_13", "maya_singh", displayName = "Maya Singh"),
    FriendSummary("starter_contact_14", "noah_brown", displayName = "Noah Brown"),
    FriendSummary("starter_contact_15", "olivia_lee", displayName = "Olivia Lee"),
    FriendSummary("starter_contact_16", "peter_zhao", displayName = "Peter Zhao"),
    FriendSummary("starter_contact_17", "quinn_murphy", displayName = "Quinn Murphy"),
    FriendSummary("starter_contact_18", "ruby_johnson", displayName = "Ruby Johnson"),
    FriendSummary("starter_contact_19", "samira_khan", displayName = "Samira Khan"),
    FriendSummary("starter_contact_20", "theo_evans", displayName = "Theo Evans"),
)

internal fun mergeWithStarterContacts(firebaseFriends: List<FriendSummary>): List<FriendSummary> =
    (firebaseFriends + StarterContacts)
        .distinctBy(FriendSummary::uid)
        .sortedBy { it.displayLabel.lowercase() }

internal val FriendSummary.displayLabel: String
    get() = displayName.ifBlank { username }

internal val FriendSummary.isStarterContact: Boolean
    get() = uid.startsWith("starter_contact_")
