package com.bringyour.network.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bringyour.network.R

import com.bringyour.network.ui.theme.URNetworkTheme
import com.bringyour.network.ui.theme.AppTheme

@Composable
fun NoLocationsFound() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.bgTinted, shape = RoundedCornerShape(12.dp))
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = AppTheme.textMuted
        )

        Spacer(modifier = Modifier.width(16.dp))

        Row {
            Text(
                stringResource(id = R.string.no_providers_found),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Preview
@Composable
private fun NoLocationsFoundPreview() {
    URNetworkTheme {
        NoLocationsFound()
    }
}