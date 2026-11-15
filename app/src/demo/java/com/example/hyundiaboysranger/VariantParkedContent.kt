package com.example.hyundiaboysranger

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Green = Color(0xFF00513B)
private val Ink = Color(0xFF111C19)
private val Muted = Color(0xFF293A34)
private val Panel = Brush.linearGradient(listOf(Color(0xFFF6F8F7), Color(0xFFE2EBEB), Color(0xFFB8CBD0)))
private val CardBrush = Brush.linearGradient(listOf(Color.White, Color(0xFFF4F8F8), Color(0xFFE6EEF0)))

/** The parking gate stays outside this screen; this journey uses no vehicle SDK. */
@Composable
internal fun VariantParkedContent(evidence: ParkingEvidence, currentParking: () -> ParkingEvidence) {
    var regularApp by rememberSaveable { mutableStateOf(false) }
    if (regularApp) DrivingHelperApp(parkingEvidence = evidence, currentParking = currentParking)
    else DemoJourney(onRegularApp = { regularApp = true })
}

@Composable
private fun DemoJourney(onRegularApp: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var profile by rememberSaveable { mutableStateOf("") }
    var concern by rememberSaveable { mutableStateOf("") }
    var checks by rememberSaveable { mutableIntStateOf(0) }
    var route by rememberSaveable { mutableStateOf("") }
    var saved by rememberSaveable { mutableStateOf(false) }
    val titles = listOf("사용자 선택", "걱정 상황", "출발 전 점검", "경로 비교", "조작 설명", "시연 복습")
    key(step) {
        Column(Modifier.fillMaxSize().background(Panel).verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(42.dp).background(Green, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text("동", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text("동행", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text("${step + 1} / 6", color = Green, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(6) { i -> Box(Modifier.weight(1f).height(4.dp).background(if (i <= step) Green else Color(0xFFABBCB9), RoundedCornerShape(4.dp))) }
            }
            Text(titles[step] + if (step > 0) " · $profile" else "", color = Green, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            when (step) {
                0 -> {
                    DemoHeading("어떤 운전자이신가요?", "나에게 맞는 유형을 선택해 주세요.")
                    DemoChoices {
                        DemoCard("고령자", "날씨에 따른 차량 조작을 익혀요.", "◉", Modifier.weight(1f)) { profile = "고령자"; step = 1 }
                        DemoCard("초보자", "차량 조작을 차근차근 익혀요.", "◎", Modifier.weight(1f)) { profile = "초보자"; step = 1 }
                    }
                    DemoButton("기존 앱 열기", onRegularApp)
                }
                1 -> {
                    DemoHeading("어떤 상황이 걱정되나요?", "먼저 연습하고 싶은 상황을 하나 골라주세요.")
                    DemoChoices {
                        DemoCard("비 오는 날 와이퍼", "와이퍼 레버의 위치와 단계를 확인해요.", "☂", Modifier.weight(1f)) { concern = "비 오는 날 와이퍼"; step = 2 }
                        DemoCard("유리 김서림", "김서림 제거 버튼을 확인해요.", "◌", Modifier.weight(1f)) { concern = "유리 김서림"; step = 2 }
                    }
                }
                2 -> {
                    DemoHeading("출발 전 직접 점검해요", "직접 확인한 항목을 선택하세요. 차량 센서 확인이 아닙니다.")
                    listOf("좌석과 거울", "안전벨트", "문과 주변").forEachIndexed { i, label ->
                        val checked = checks and (1 shl i) != 0
                        DemoSurface {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = checked, onCheckedChange = { value -> checks = if (value) checks or (1 shl i) else checks and (1 shl i).inv() }, colors = CheckboxDefaults.colors(checkedColor = Green))
                                Text(label, style = MaterialTheme.typography.titleMedium, color = Ink)
                            }
                        }
                    }
                    Text("${Integer.bitCount(checks)} / 3 확인", color = Muted)
                    DemoButton("시연용 경로 비교 →", { step = 3 })
                }
                3 -> {
                    DemoHeading("두 경로를 비교해 보세요", "아래 수치는 가상 예시이며 실제 도로, 소요 시간 또는 안전도를 뜻하지 않습니다.")
                    DemoChoices {
                        DemoCard("가상 경로 A", "20분  ·  방향 전환 5회", "↗", Modifier.weight(1f), routeIllustration = 0) { route = "가상 경로 A · 20분 · 회전 5회"; step = 4 }
                        DemoCard("가상 경로 B", "35분  ·  방향 전환 3회", "↗", Modifier.weight(1f), routeIllustration = 1) { route = "가상 경로 B · 35분 · 회전 3회"; step = 4 }
                    }
                }
                4 -> {
                    DemoHeading(concern, "선택한 시연용 경로: $route")
                    DemoSurface { Text(if (concern == "비 오는 날 와이퍼") "차량 설명서에서 와이퍼 레버의 위치와 작동 단계를 확인해 보세요." else "차량 설명서에서 앞 유리 김서림 제거 버튼과 공조 조절 위치를 확인해 보세요.", modifier = Modifier.padding(18.dp), color = Ink, style = MaterialTheme.typography.bodyLarge) }
                    Text("설명용 화면이며 차량 조작이나 작동을 감지하지 않습니다.", color = Muted)
                    DemoButton("어려운 항목을 시연 복습에 추가", { saved = true; step = 5 })
                    DemoButton("시연 복습 보기 →", { step = 5 })
                }
                5 -> {
                    DemoHeading("오늘 연습을 돌아봐요", "이 복습은 시연 화면의 임시 기록이며 기존 복습 저장소를 변경하지 않습니다.")
                    listOf("연습 상황" to concern, "선택한 경로" to route, "출발 전 점검" to "${Integer.bitCount(checks)} / 3 확인", "다시 볼 조작" to if (saved) concern else "추가된 항목이 없습니다.").forEach { (label, value) ->
                        DemoSurface { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Muted); Text(value, color = Ink, fontWeight = FontWeight.SemiBold) } }
                    }
                    DemoButton(if (saved) "설명 다시 확인" else "조작 설명으로 돌아가기", { step = 4 })
                    DemoButton("시연 종료", { step = 0; profile = ""; concern = ""; checks = 0; route = ""; saved = false })
                }
            }
            if (step in 1..5) DemoButton("‹ 이전", { step-- })
            Text("차량 신호 감지나 실제 경로 계산이 아닌 시연용 예시입니다.", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DemoHeading(title: String, description: String) {
    Text(title, color = Ink, fontSize = 32.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold)
    Text(description, color = Muted, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun DemoChoices(content: @Composable RowScope.() -> Unit) {
    BoxWithConstraints {
        if (maxWidth < 480.dp) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(12.dp), content = content) }
        else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun DemoSurface(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.Transparent, shadowElevation = 4.dp) {
        Box(Modifier.background(CardBrush), contentAlignment = Alignment.CenterStart) { content() }
    }
}

@Composable
private fun DemoCard(title: String, description: String, icon: String, modifier: Modifier = Modifier, routeIllustration: Int? = null, onClick: () -> Unit) {
    Surface(modifier = modifier, onClick = onClick, shape = RoundedCornerShape(14.dp), color = Color.Transparent, shadowElevation = 4.dp) {
        Column(Modifier.background(CardBrush).padding(24.dp).heightIn(min = 180.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.size(42.dp).background(Color(0xFFD9EEE4), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) { Text(icon, color = Green, fontSize = 22.sp) }
                Text("→", color = Green, fontSize = 22.sp)
            }
            Text(title, color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (routeIllustration != null) DemoRouteIllustration(routeIllustration)
            Text(description, color = Muted, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Decorative fictional route, not a map or navigation result. */
@Composable
private fun DemoRouteIllustration(variant: Int) {
    Canvas(Modifier.fillMaxWidth().height(110.dp).background(Color(0xFFE7F0EC), RoundedCornerShape(10.dp))) {
        val w = size.width
        val h = size.height
        val road = Color(0xFFCADBD4)
        listOf(.2f, .4f, .6f, .8f).forEach { y ->
            drawLine(road, Offset(w * .06f, h * y), Offset(w * .94f, h * y), 2.dp.toPx())
        }
        listOf(.2f, .4f, .6f, .8f).forEach { x ->
            drawLine(road, Offset(w * x, h * .08f), Offset(w * x, h * .92f), 2.dp.toPx())
        }
        // Each interior point is one right-angle direction change: A has five, B has three.
        val points = if (variant == 0) listOf(
            Offset(.10f, .80f), Offset(.20f, .80f), Offset(.20f, .60f),
            Offset(.50f, .60f), Offset(.50f, .40f), Offset(.80f, .40f), Offset(.80f, .20f),
        ) else listOf(
            Offset(.10f, .80f), Offset(.40f, .80f), Offset(.40f, .60f),
            Offset(.80f, .60f), Offset(.80f, .20f),
        )
        val path = Path().apply {
            moveTo(w * points.first().x, h * points.first().y)
            points.drop(1).forEach { lineTo(w * it.x, h * it.y) }
        }
        drawPath(path, Green, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(Green, 5.dp.toPx(), Offset(w * points.first().x, h * points.first().y))
        drawCircle(Green, 5.dp.toPx(), Offset(w * points.last().x, h * points.last().y))
    }
}

@Composable
private fun DemoButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.heightIn(min = 54.dp), shape = RoundedCornerShape(11.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text(label, fontWeight = FontWeight.Bold) }
}
