package tech.arcent.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.arcent.R

@Composable
internal fun LandingSection(
    onGoogle: () -> Unit,
    onLocal: () -> Unit,
) {
    Button(
        onClick = { onGoogle() },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF799C92)),
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.google),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(text = stringResource(R.string.action_sign_with_google), fontSize = 16.sp, color = Color.White)
    }
    Spacer(Modifier.height(20.dp))
    /*
     * locally hosted
     */
    OutlinedButton(
        onClick = { onLocal() },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.sqlite_logo),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.provider_sqlite_local),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.secondaryContainer,
        )
    }
    Spacer(Modifier.height(32.dp))
    FooterText()
}

/*
 * Footer Text
 */
@Composable
internal fun FooterText() {
    val accent = Color(0xFF799C92)

    val termsPrefix = stringResource(R.string.terms_prefix)
    val termsService = stringResource(R.string.terms_service)
    val privacyPolicy = stringResource(R.string.privacy_policy)
    val annotated: AnnotatedString =
        remember(termsPrefix, termsService, privacyPolicy) {
            buildAnnotatedString {
                append(termsPrefix)
                pushStringAnnotation(tag = "terms", annotation = "terms")
                withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Medium)) { append(termsService) }
                pop()
                append(" & ")
                pushStringAnnotation(tag = "privacy", annotation = "privacy")
                withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Medium)) { append(privacyPolicy) }
                pop()
            }
        }
    val uriHandler = LocalUriHandler.current
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ClickableText(
            text = annotated,
            modifier = Modifier.fillMaxWidth(),
            style =
                MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFFB0B0B0),
                ),
        ) { offset ->
            annotated.getStringAnnotations(start = offset, end = offset).firstOrNull()?.let { ann ->
                /*
                 * External pages when clicked
                 */
                when (ann.tag) {
                    "terms" -> uriHandler.openUri("https://arcent.tech/terms")
                    "privacy" -> uriHandler.openUri("https://arcent.tech/privacy")
                }
            }
        }
    }
}
