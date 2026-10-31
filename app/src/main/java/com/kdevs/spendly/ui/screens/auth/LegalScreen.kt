package com.kdevs.spendly.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.theme.*

data class LegalSection(
    val head: String,
    val body: String,
)

val privacy =
    listOf(
        LegalSection(str(R.string.what_we_collect), str(R.string.your_name_and_email_when_you)),
        LegalSection(str(R.string.where_it_is_stored), str(R.string.in_your_private_area_of_google)),
        LegalSection(str(R.string.chat), str(R.string.messages_and_photos_are_visible_only)),
        LegalSection(str(R.string.friend_code), str(R.string.your_friend_code_lets_people_send)),
        LegalSection(str(R.string.reports), str(R.string.if_you_report_a_person_we)),
        LegalSection(str(R.string.your_choices), str(R.string.you_can_change_your_name_sign)),
        LegalSection(str(R.string.contact), str(R.string.questions_about_privacy_can_be_sent)),
    )

val terms =
    listOf(
        LegalSection(str(R.string.using_spendly), str(R.string.spendly_helps_you_record_and_understan)),
        LegalSection(str(R.string.your_numbers), str(R.string.balances_are_worked_out_from_what)),
        LegalSection(str(R.string.settling_up), str(R.string.recording_a_settlement_in_a_group)),
        LegalSection(str(R.string.be_kind_in_chat), str(R.string.no_harassment_spam_or_illegal_content)),
        LegalSection(str(R.string.your_account), str(R.string.keep_your_login_safe_you_can)),
        LegalSection(str(R.string.changes), str(R.string.we_may_update_the_app_and)),
    )

/** Privacy Policy and Terms open as normal in-app screens, not a web page. */
@Composable
fun LegalScreen(
    nav: NavController,
    privacy: Boolean,
) {
    val items = if (privacy) com.kdevs.spendly.ui.screens.auth.privacy else terms
    ScreenScaffold(if (privacy) str(R.string.privacy_policy) else str(R.string.terms_of_service), onBack = { nav.popBackStack() }) {
        Hint(str(R.string.last_updated_26_october_2026))
        items.forEachIndexed { i, s ->
            Card {
                Text(str(R.string.text_, i + 1, s.head), style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(6.dp))
                Text(s.body, color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
    }
}
