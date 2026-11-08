package com.example.hyundiaboysranger

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun RouteComparisonScreen(preferences: RoutePreferences, detailed: Boolean) {
    val demo = BuildConfig.IS_DEMO
    val routes = if (demo) VariantRouteProvider.candidates() else SdkRouteProvider().candidates()
    Text(stringResource(R.string.route_compare_title), style = MaterialTheme.typography.titleLarge)
    if (routes.isEmpty()) {
        Text(stringResource(R.string.route_compare_unavailable))
    } else {
        Text(stringResource(R.string.route_compare_demo))
        rankRoutes(routes, preferences).take(if (detailed) 3 else 2).forEachIndexed { index, item ->
            Text(stringResource(R.string.route_compare_row, item.route.id, item.route.minutes, item.score,
                item.route.turns, item.route.narrowSections, item.route.highwayPercent,
                if (index == 0) stringResource(R.string.route_example_recommended) else ""))
        }
    }
}
